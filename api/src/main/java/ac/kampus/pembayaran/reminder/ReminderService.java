package ac.kampus.pembayaran.reminder;

import ac.kampus.pembayaran.billing.Installment;
import ac.kampus.pembayaran.billing.InstallmentRepository;
import ac.kampus.pembayaran.billing.InstallmentStatus;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.settings.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Pengingat jatuh tempo cicilan lewat WhatsApp.
 *
 * <p>Yang menentukan bukan pengirimnya melainkan aturannya, dan aturannya
 * berpihak pada tidak mengganggu: satu pengingat per cicilan per jenis,
 * selamanya. Mahasiswa yang cicilannya lewat tempo dua bulan tidak menerima
 * enam puluh pesan.
 *
 * <p>Pesan yang gagal terkirim sengaja TIDAK dianggap sudah dikirim, jadi ia
 * dicoba lagi keesokan harinya — tapi jejaknya tetap dicatat supaya kegagalan
 * yang berulang kelihatan.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

	private static final Locale LOKAL_ID = Locale.of("id", "ID");

	private static final DateTimeFormatter TANGGAL =
			DateTimeFormatter.ofPattern("d MMMM yyyy", LOKAL_ID);

	private final InstallmentRepository installmentRepository;
	private final ReminderLogRepository logRepository;
	private final SystemSettingService settings;
	private final WhatsAppSender sender;

	/** Ringkasan satu putaran, dipakai log dan endpoint uji coba. */
	public record Hasil(int diperiksa, int terkirim, int dilewati, int gagal) {
		static Hasil kosong() {
			return new Hasil(0, 0, 0, 0);
		}
	}

	/**
	 * Menjalankan satu putaran pengingat untuk hari yang diberikan.
	 *
	 * <p>Tanggalnya diterima sebagai parameter, bukan diambil dari jam sistem di
	 * dalam sini — supaya aturannya bisa diuji untuk hari mana pun tanpa memutar
	 * jam mesin.
	 */
	@Transactional
	public Hasil jalankan(LocalDate hariIni) {
		if (!settings.get(ReminderSettings.ENABLED).map(Boolean::parseBoolean).orElse(false)) {
			log.debug("Pengingat dimatikan di pengaturan, tidak ada yang dikirim.");
			return Hasil.kosong();
		}

		int hariSebelum = settings.getInteger(ReminderSettings.DAYS_BEFORE)
				.filter(hari -> hari >= 0)
				.orElse(ReminderSettings.DAYS_BEFORE_DEFAULT);
		boolean ikutLewatTempo = settings.get(ReminderSettings.OVERDUE_ENABLED)
				.map(Boolean::parseBoolean)
				.orElse(true);

		// Satu kueri untuk keduanya: yang sudah lewat tempo sampai yang jatuh
		// tempo beberapa hari lagi. Jenis pengingatnya ditentukan per baris.
		LocalDate dari = ikutLewatTempo ? LocalDate.EPOCH : hariIni;
		LocalDate sampai = hariIni.plusDays(hariSebelum);

		List<Installment> kandidat = installmentRepository.findPerluDiingatkan(
				dari, sampai, InstallmentStatus.PAID);

		int terkirim = 0;
		int dilewati = 0;
		int gagal = 0;

		for (Installment cicilan : kandidat) {
			ReminderKind jenis = cicilan.getDueDate().isBefore(hariIni)
					? ReminderKind.LEWAT_TEMPO
					: ReminderKind.JATUH_TEMPO;

			// Pengingat "menjelang" hanya untuk hari yang tepat, bukan setiap
			// hari sepanjang rentangnya — kalau tidak, tiga hari sebelum jatuh
			// tempo berarti tiga pesan.
			if (jenis == ReminderKind.JATUH_TEMPO
					&& !cicilan.getDueDate().equals(sampai)) {
				continue;
			}

			switch (kirimSatu(cicilan, jenis)) {
				case SENT -> terkirim++;
				case SKIPPED -> dilewati++;
				case FAILED -> gagal++;
			}
		}

		Hasil hasil = new Hasil(kandidat.size(), terkirim, dilewati, gagal);
		if (terkirim > 0 || gagal > 0) {
			log.info("Pengingat {}: {} diperiksa, {} terkirim, {} dilewati, {} gagal",
					hariIni, hasil.diperiksa(), terkirim, dilewati, gagal);
		}
		return hasil;
	}

	private ReminderStatus kirimSatu(Installment cicilan, ReminderKind jenis) {
		Student mahasiswa = cicilan.getPaymentPlan().getStudent();

		// Sudah pernah benar-benar terkirim; yang gagal tidak dihitung supaya
		// bisa dicoba lagi.
		if (logRepository.existsByInstallmentIdAndKindAndStatus(
				cicilan.getId(), jenis, ReminderStatus.SENT)) {
			return ReminderStatus.SKIPPED;
		}

		String nomor = GatewayWhatsAppSender.rapikanNomor(mahasiswa.getPhone());
		String pesan = susunPesan(cicilan, mahasiswa);

		// Gateway belum diatur: pesannya hanya diperlihatkan di log, dan dicatat
		// SKIPPED — BUKAN SENT. Kalau dicatat terkirim, satu putaran uji coba
		// menghabiskan jatah pengingat yang memang cuma sekali, dan begitu
		// gateway benar-benar dipasang mahasiswa itu tidak akan pernah
		// diingatkan sama sekali.
		if (!sender.siap()) {
			log.info("Gateway WhatsApp belum diatur, pengingat TIDAK dikirim. Tujuan {}: {}",
					nomor, pesan);
			catat(cicilan, mahasiswa, jenis, nomor, pesan, ReminderStatus.SKIPPED,
					"Gateway WhatsApp belum diatur; pesan hanya dicatat, belum dikirim.");
			return ReminderStatus.SKIPPED;
		}

		if (nomor == null) {
			// Dicatat, bukan didiamkan: admin perlu tahu siapa yang tidak
			// terjangkau pengingat supaya bisa menghubunginya dengan cara lain.
			catat(cicilan, mahasiswa, jenis, null, pesan, ReminderStatus.SKIPPED,
					"Nomor telepon belum diisi atau tidak berbentuk nomor Indonesia.");
			return ReminderStatus.SKIPPED;
		}

		try {
			sender.kirim(nomor, pesan);
			catat(cicilan, mahasiswa, jenis, nomor, pesan, ReminderStatus.SENT, null);
			return ReminderStatus.SENT;
		} catch (RuntimeException e) {
			// Satu kiriman yang gagal tidak boleh menghentikan sisanya.
			log.warn("Pengingat cicilan {} gagal dikirim: {}", cicilan.getId(), e.getMessage());
			catat(cicilan, mahasiswa, jenis, nomor, pesan, ReminderStatus.FAILED, e.getMessage());
			return ReminderStatus.FAILED;
		}
	}

	private void catat(Installment cicilan, Student mahasiswa, ReminderKind jenis,
			String nomor, String pesan, ReminderStatus status, String galat) {

		logRepository.save(ReminderLog.builder()
				.installmentId(cicilan.getId())
				.studentId(mahasiswa.getId())
				.kind(jenis)
				.phone(nomor)
				.message(pesan)
				.status(status)
				.error(galat)
				.build());
	}

	/**
	 * Menyusun isi pesan dari template yang bisa diedit admin.
	 *
	 * <p>Penanda yang tidak dikenal dibiarkan apa adanya, bukan dikosongkan:
	 * salah ketik penanda jadi kelihatan di pesan uji coba, alih-alih diam-diam
	 * menghasilkan kalimat yang bolong.
	 */
	String susunPesan(Installment cicilan, Student mahasiswa) {
		String template = settings.getOrDefault(
				ReminderSettings.MESSAGE_TEMPLATE,
				"Halo {nama}, cicilan {kategori} ke-{cicilan} sebesar {nominal} "
						+ "jatuh tempo {tanggal}. Sisa: {sisa}.");

		return template
				.replace("{nama}", nullAman(mahasiswa.getName()))
				.replace("{nim}", nullAman(mahasiswa.getNim()))
				.replace("{kategori}", cicilan.getPaymentPlan().getCategory().label())
				.replace("{cicilan}", String.valueOf(cicilan.getInstallmentNo()))
				.replace("{nominal}", rupiah(cicilan.getAmount()))
				.replace("{sisa}", rupiah(cicilan.outstanding()))
				.replace("{tanggal}", cicilan.getDueDate().format(TANGGAL));
	}

	private static String nullAman(String nilai) {
		return nilai == null ? "" : nilai;
	}

	private static String rupiah(BigDecimal nilai) {
		NumberFormat format = NumberFormat.getNumberInstance(LOKAL_ID);
		format.setMaximumFractionDigits(0);
		return "Rp " + format.format(nilai == null ? BigDecimal.ZERO : nilai);
	}
}
