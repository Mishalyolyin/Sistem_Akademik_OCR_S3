package ac.kampus.pembayaran.payment.ocr;

import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.payment.Payment;
import ac.kampus.pembayaran.payment.PaymentAllocationService;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.payment.PaymentStatus;
import ac.kampus.pembayaran.payment.VerificationLog;
import ac.kampus.pembayaran.payment.VerificationLogRepository;
import ac.kampus.pembayaran.settings.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memproses satu bukti bayar: memanggil service OCR lalu memutuskan statusnya.
 *
 * <p>Keputusan ada di sini, bukan di service OCR. Service OCR hanya membaca;
 * ambang keyakinan dan toleransi selisih nominal disimpan di pengaturan sistem
 * dan bisa diubah admin.
 *
 * <p>Bila pemrosesan melempar exception, pesan dikembalikan ke RabbitMQ untuk
 * dicoba lagi. Setelah percobaan habis, pesannya masuk dead-letter queue dan
 * pembayaran ditandai FAILED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrJobConsumer {

	private final PaymentRepository paymentRepository;
	private final VerificationLogRepository logRepository;
	private final FileStorageService storage;
	private final OcrClient ocrClient;
	private final SystemSettingService settings;
	private final PaymentAllocationService allocationService;

	@RabbitListener(queues = RabbitConfig.QUEUE)
	@Transactional
	public void handle(OcrJobMessage message) {
		Long paymentId = message.paymentId();

		Payment payment = paymentRepository.findWithDetailsById(paymentId).orElse(null);
		if (payment == null) {
			log.warn("Pembayaran {} tidak ditemukan, pesan diabaikan.", paymentId);
			return;
		}

		log.info("Membaca bukti pembayaran {}", paymentId);

		Map<String, Object> hasil = ocrClient.read(
				storage.resolve(payment.getProofFilePath()),
				OcrClient.OcrRequest.forPayment(
						settings.get(SystemSettingService.BANK_ACCOUNT_NUMBER).orElse(null),
						settings.get(SystemSettingService.OCR_BLACKLIST_KEYWORDS).orElse(null),
						settings.getInteger(SystemSettingService.OCR_DATE_VALIDATION_DAYS).orElse(null),
						payment.getStudent().getName()));

		terapkanHasil(payment, hasil);
	}

	private void terapkanHasil(Payment payment, Map<String, Object> hasil) {
		double keyakinan = asDouble(hasil.get("confidence"), 0);
		BigDecimal nominalTerbaca = asBigDecimal(hasil.get("extracted_amount"));
		String statusOcr = asString(hasil.get("verification_status"));

		double ambangTerima = settings.getPercentAsFraction(
				SystemSettingService.OCR_CONFIDENCE_THRESHOLD, 0.80);
		double ambangTolak = settings.getPercentAsFraction(
				SystemSettingService.OCR_AUTO_REJECT_THRESHOLD, 0.10);
		BigDecimal toleransi = settings.getAmount(
				SystemSettingService.PAYMENT_TOLERANCE_AMOUNT, BigDecimal.ZERO);

		List<String> catatan = new ArrayList<>(asStringList(hasil.get("flags")));

		// Selisih nominal di luar toleransi berarti bukti tidak cocok dengan tagihan.
		// Nominalnya sengaja BELUM disentuh di sini; penyesuaian baru dilakukan
		// setelah dipastikan pembayaran ini memang masih boleh diubah OCR.
		boolean nominalTidakCocok = false;
		if (nominalTerbaca != null && nominalTerbaca.signum() > 0) {
			BigDecimal selisih = payment.getAmount().subtract(nominalTerbaca).abs();
			if (selisih.compareTo(toleransi) > 0) {
				nominalTidakCocok = true;
				catatan.add("Selisih nominal: diklaim %s, terbaca %s"
						.formatted(payment.getAmount().toPlainString(),
								nominalTerbaca.toPlainString()));
			}
		}

		PaymentStatus statusBaru;
		if ("rejected".equalsIgnoreCase(statusOcr)) {
			statusBaru = PaymentStatus.REJECTED;
			payment.setRejectReason("Ditolak otomatis oleh pembacaan bukti.");
		} else if (keyakinan < ambangTolak) {
			statusBaru = PaymentStatus.REJECTED;
			catatan.add("Ditolak otomatis: keyakinan hanya %.0f%%, kemungkinan bukan bukti bayar."
					.formatted(keyakinan * 100));
			payment.setRejectReason("Bukti tidak terbaca. Unggah ulang dengan gambar yang jelas.");
		} else if (keyakinan >= ambangTerima
				&& !nominalTidakCocok
				&& nominalTerbaca != null
				&& nominalTerbaca.signum() > 0) {
			// verifiedAt baru diisi setelah dipastikan hasil ini memang berlaku;
			// mengisinya di sini akan menimpa waktu verifikasi milik admin.
			statusBaru = PaymentStatus.AUTO_VERIFIED;
		} else {
			statusBaru = PaymentStatus.NEEDS_REVIEW;
		}

		// Admin bisa saja sudah memutuskan secara manual selagi pekerjaan ini
		// mengantre. Keputusan manusia tidak boleh ditimpa hasil otomatis
		// yang datang belakangan.
		PaymentStatus statusTerkini = paymentRepository.findStatusById(payment.getId())
				.orElse(payment.getStatus());

		// Nominal hanya boleh disesuaikan selama uangnya belum dibagikan ke
		// cicilan. Sesudah dialokasikan, mengubah nominal membuat pembukuan
		// timpang: cicilan terlanjur menerima angka yang lama dan alokasi tidak
		// pernah dihitung ulang, sementara kuitansi mencetak angka yang baru.
		boolean bolehSesuaikanNominal =
				statusTerkini.bolehDitimpaOcr() && payment.getAllocatedAt() == null;

		if (nominalTidakCocok && !bolehSesuaikanNominal) {
			catatan.add("Nominal dibiarkan apa adanya: pembayaran sudah diputuskan "
					+ "manual atau uangnya sudah dibagikan ke cicilan. Perlu ditinjau admin.");
		}

		Map<String, Object> ocrData = new HashMap<>(hasil);
		ocrData.put("flags", catatan);

		// Gambar praprosesnya dipindahkan ke berkas, lalu DIBUANG dari JSON.
		// Membiarkannya di sana berarti puluhan kilobyte base64 ikut masuk ke
		// kolom jsonb, ikut terkirim tiap kali halaman forensik dibuka, dan
		// ikut tercetak mentah-mentah di ekspor dataset.
		simpanGambarPraproses(payment, ocrData);

		payment.setOcrData(ocrData);
		payment.setOcrConfidence(BigDecimal.valueOf(keyakinan).setScale(4, RoundingMode.HALF_UP));
		payment.setBankName(asString(hasil.get("bank_name")));
		payment.setPaymentProofDate(asLocalDate(hasil.get("extracted_date")));

		if (!statusTerkini.bolehDitimpaOcr()) {
			log.info("Pembayaran {} sudah diputuskan manual ({}). "
							+ "Hasil OCR disimpan tanpa mengubah status maupun nominal.",
					payment.getId(), statusTerkini);
			payment.setStatus(statusTerkini);
			paymentRepository.save(payment);
			return;
		}

		if (nominalTidakCocok && bolehSesuaikanNominal) {
			// Nominal disesuaikan ke yang benar-benar tertulis di bukti.
			payment.setAmount(nominalTerbaca);
		}

		if (statusBaru == PaymentStatus.AUTO_VERIFIED) {
			payment.setVerifiedAt(Instant.now());
		}

		PaymentStatus statusLama = payment.getStatus();
		payment.setStatus(statusBaru);
		paymentRepository.save(payment);

		logRepository.save(VerificationLog.builder()
				.paymentId(payment.getId())
				.fromStatus(statusLama)
				.toStatus(statusBaru)
				// adminId kosong = keputusan otomatis
				.note("Keyakinan %.0f%%%s".formatted(
						keyakinan * 100,
						nominalTidakCocok ? ", nominal tidak cocok" : ""))
				.build());

		log.info("Pembayaran {} -> {} (keyakinan {}%)",
				payment.getId(), statusBaru, Math.round(keyakinan * 100));

		// Verifikasi otomatis langsung diikuti pembagian uang ke cicilan.
		if (statusBaru == PaymentStatus.AUTO_VERIFIED) {
			allocationService.allocate(payment.getId());
		}
	}

	/**
	 * Memindahkan gambar hasil praproses dari jawaban OCR ke penyimpanan berkas.
	 *
	 * <p>Yang disimpan adalah gambar yang benar-benar dibaca Tesseract, bukan
	 * berkas asli yang diunggah mahasiswa — itulah yang berguna untuk menelusuri
	 * pembacaan yang meleset, dan itulah bahan mentah dataset gambar nanti.
	 * Piksel yang tidak disimpan hari ini tidak bisa dipulihkan besok.
	 *
	 * <p>Gagal menyimpan tidak menggagalkan apa pun: pembacaannya sudah selesai
	 * dan hasilnya tetap sah tanpa gambar pendampingnya.
	 */
	private void simpanGambarPraproses(Payment payment, Map<String, Object> ocrData) {
		Object base64 = ocrData.remove("processed_image_b64");
		if (!(base64 instanceof String teks) || teks.isBlank()) {
			return;
		}

		try {
			byte[] isi = java.util.Base64.getDecoder().decode(teks);
			storage.storeGenerated(isi, "bukti-praproses", "png")
					.ifPresent(payment::setProcessedFilePath);
		} catch (IllegalArgumentException e) {
			log.warn("Gambar praproses pembayaran {} bukan base64 yang sah, dilewati.",
					payment.getId());
		}
	}

	// --- Pembacaan nilai dari JSON yang tipenya tidak dijamin ---

	private static double asDouble(Object value, double fallback) {
		return value instanceof Number number ? number.doubleValue() : fallback;
	}

	private static BigDecimal asBigDecimal(Object value) {
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
		}
		return null;
	}

	private static String asString(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static LocalDate asLocalDate(Object value) {
		if (value == null) return null;
		try {
			return LocalDate.parse(String.valueOf(value));
		} catch (Exception e) {
			// Tanggal yang tidak terbaca hanya dilewati; tidak menggagalkan pekerjaan.
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> asStringList(Object value) {
		if (value instanceof List<?> list) {
			return (List<String>) list.stream().map(String::valueOf).toList();
		}
		return List.of();
	}
}
