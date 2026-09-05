package ac.kampus.pembayaran.forensic;

import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.payment.Payment;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.payment.PaymentSpecifications;
import ac.kampus.pembayaran.payment.PaymentStatus;
import ac.kampus.pembayaran.payment.VerificationLog;
import ac.kampus.pembayaran.payment.VerificationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Forensik OCR: hasil pembacaan mentah di balik tiap keputusan otomatis.
 *
 * <p>Keputusan atas bukti bayar diambil mesin — terverifikasi, perlu ditinjau,
 * atau ditolak — berdasarkan keyakinan pembacaan dan kecocokan nominal. Kalau
 * keputusannya terasa salah, yang perlu dilihat adalah apa yang sebenarnya
 * terbaca, bukan kesimpulannya. Itu yang disajikan di sini.
 *
 * <p>Dibuka untuk ADMIN dan DEVELOPER. Sebelum ini DEVELOPER adalah peran yatim:
 * dikecualikan dari seluruh endpoint admin, dan satu-satunya kemampuannya
 * membuka gambar bukti — sementara hasil OCR mentah justru hanya bisa dilihat
 * admin lewat detail pembayaran.
 */
@RestController
@RequestMapping("/forensik")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
@Tag(name = "Forensik OCR")
public class ForensicController {

	private final PaymentRepository paymentRepository;
	private final VerificationLogRepository logRepository;

	public record BarisForensik(
			Long paymentId,
			String namaMahasiswa,
			String nim,
			PaymentStatus status,
			BigDecimal nominalDiklaim,
			/** Pecahan 0–1, bukan persen — sama seperti yang tersimpan. */
			BigDecimal keyakinan,
			/** Hasil OCR belum pernah tersimpan: pekerjaannya gagal atau belum jalan. */
			boolean adaHasilOcr,
			/** Jumlah catatan yang dikumpulkan mesin saat memutuskan. */
			int jumlahCatatan,
			Instant diunggah
	) {
	}

	public record RiwayatKeputusan(
			PaymentStatus dari,
			PaymentStatus ke,
			/** Kosong berarti keputusan otomatis oleh sistem, bukan oleh admin. */
			Long adminId,
			boolean otomatis,
			String catatan,
			Instant waktu
	) {
		static RiwayatKeputusan from(VerificationLog log) {
			return new RiwayatKeputusan(
					log.getFromStatus(), log.getToStatus(), log.getAdminId(),
					log.getAdminId() == null, log.getNote(), log.getCreatedAt());
		}
	}

	public record DetailForensik(
			Long paymentId,
			String namaMahasiswa,
			String nim,
			PaymentStatus status,
			BigDecimal nominalDiklaim,
			BigDecimal keyakinan,
			String alasanDitolak,
			Instant diunggah,
			Instant diverifikasi,
			/**
			 * Hasil pembacaan apa adanya, tanpa dirapikan. Bentuknya mengikuti
			 * jawaban service OCR dan bisa berubah tanpa mengubah kode ini —
			 * memang itu gunanya: yang dicari saat menelusuri justru field yang
			 * tidak terduga.
			 */
			Map<String, Object> ocrMentah,
			List<String> catatan,
			List<RiwayatKeputusan> riwayat
	) {
	}

	@GetMapping("/pembayaran")
	@Operation(summary = "Daftar bukti bayar beserta keyakinan pembacaannya")
	@Transactional(readOnly = true)
	public Page<BarisForensik> daftar(
			@RequestParam(required = false) List<PaymentStatus> status,
			@RequestParam(required = false) String cari,
			/**
			 * Hanya yang keyakinannya di bawah ini. Satuannya sama dengan kolom
			 * yang disaring dan dengan field {@code keyakinan} di jawaban:
			 * pecahan 0–1, bukan persen. 0.8 berarti 80%.
			 */
			@RequestParam(required = false) BigDecimal maksKeyakinan,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		Specification<Payment> spec = Specification.allOf(
				PaymentSpecifications.hasStatus(status),
				PaymentSpecifications.studentMatches(cari),
				keyakinanDiBawah(maksKeyakinan));

		var pageable = PageRequest.of(page, Math.min(size, 100),
				Sort.by("createdAt").descending());

		return paymentRepository.findAll(spec, pageable).map(ForensicController::toBaris);
	}

	@GetMapping("/pembayaran/{id}")
	@Operation(summary = "Hasil OCR mentah dan riwayat keputusan satu bukti bayar")
	@Transactional(readOnly = true)
	public DetailForensik detail(@PathVariable Long id) {
		Payment payment = paymentRepository.findWithDetailsById(id)
				.orElseThrow(() -> NotFoundException.of("Pembayaran", id));

		Map<String, Object> ocr = payment.getOcrData();

		return new DetailForensik(
				payment.getId(),
				payment.getStudent().getName(),
				payment.getStudent().getNim(),
				payment.getStatus(),
				payment.getAmount(),
				payment.getOcrConfidence(),
				payment.getRejectReason(),
				payment.getCreatedAt(),
				payment.getVerifiedAt(),
				ocr,
				catatanDari(ocr),
				logRepository.findByPaymentIdOrderByCreatedAtAsc(id).stream()
						.map(RiwayatKeputusan::from)
						.toList());
	}

	/**
	 * Pembacaan yang keyakinannya rendah. Yang belum punya hasil OCR sama sekali
	 * ikut masuk: justru itu kasus yang paling perlu ditelusuri, dan kalau
	 * disaring keluar ia tidak muncul di mana pun.
	 */
	private static Specification<Payment> keyakinanDiBawah(BigDecimal batas) {
		if (batas == null) return null;
		return (root, query, cb) -> cb.or(
				cb.isNull(root.get("ocrConfidence")),
				cb.lessThan(root.get("ocrConfidence"), batas));
	}

	private static BarisForensik toBaris(Payment p) {
		return new BarisForensik(
				p.getId(),
				p.getStudent().getName(),
				p.getStudent().getNim(),
				p.getStatus(),
				p.getAmount(),
				p.getOcrConfidence(),
				p.getOcrData() != null && !p.getOcrData().isEmpty(),
				catatanDari(p.getOcrData()).size(),
				p.getCreatedAt());
	}

	/** Catatan yang dikumpulkan mesin saat memutuskan, disimpan di bawah kunci "flags". */
	@SuppressWarnings("unchecked")
	private static List<String> catatanDari(Map<String, Object> ocrData) {
		if (ocrData == null) return List.of();
		Object flags = ocrData.get("flags");
		if (!(flags instanceof List<?> daftar)) return List.of();

		return ((List<Object>) daftar).stream().map(String::valueOf).toList();
	}
}
