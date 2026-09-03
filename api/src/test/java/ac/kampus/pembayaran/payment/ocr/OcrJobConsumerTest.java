package ac.kampus.pembayaran.payment.ocr;

import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.payment.Payment;
import ac.kampus.pembayaran.payment.PaymentAllocationService;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.payment.PaymentStatus;
import ac.kampus.pembayaran.payment.VerificationLogRepository;
import ac.kampus.pembayaran.settings.SystemSettingService;
import ac.kampus.pembayaran.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Keputusan otomatis atas hasil pembacaan bukti.
 *
 * <p>Titik paling rawan di kelas ini bukan penentuan statusnya, melainkan kapan
 * nominal pembayaran boleh ditulis ulang. Hasil OCR bisa datang setelah admin
 * memutuskan manual dan setelah uangnya dibagikan ke cicilan; kalau saat itu
 * nominalnya ikut berubah, pembukuan jadi timpang tanpa ada yang sadar.
 */
class OcrJobConsumerTest {

	private PaymentRepository paymentRepository;
	private OcrClient ocrClient;
	private PaymentAllocationService allocationService;
	private OcrJobConsumer consumer;

	@BeforeEach
	void setUp() {
		paymentRepository = mock(PaymentRepository.class);
		VerificationLogRepository logRepository = mock(VerificationLogRepository.class);
		FileStorageService storage = mock(FileStorageService.class);
		ocrClient = mock(OcrClient.class);
		SystemSettingService settings = mock(SystemSettingService.class);
		allocationService = mock(PaymentAllocationService.class);

		consumer = new OcrJobConsumer(paymentRepository, logRepository, storage,
				ocrClient, settings, allocationService);

		when(storage.resolve(anyString())).thenReturn(Path.of("bukti.png"));
		when(settings.get(anyString())).thenReturn(Optional.empty());
		when(settings.getInteger(anyString())).thenReturn(Optional.empty());
		when(settings.getAmount(anyString(), any())).thenReturn(BigDecimal.ZERO);
		when(settings.getPercentAsFraction(SystemSettingService.OCR_CONFIDENCE_THRESHOLD, 0.80))
				.thenReturn(0.80);
		when(settings.getPercentAsFraction(SystemSettingService.OCR_AUTO_REJECT_THRESHOLD, 0.10))
				.thenReturn(0.10);
	}

	// --- Pembantu penyusun kondisi awal ---

	private Payment pembayaran(String nominal, PaymentStatus status, Instant allocatedAt) {
		return pembayaran(nominal, status, allocatedAt, null);
	}

	private Payment pembayaran(String nominal, PaymentStatus status, Instant allocatedAt,
			Instant verifiedAt) {
		Payment payment = Payment.builder()
				.id(7L)
				.student(Student.builder().id(1L).nim("2612600001").name("Uji Coba").build())
				.amount(new BigDecimal(nominal))
				.proofFilePath("bukti.png")
				.status(status)
				.allocatedAt(allocatedAt)
				.verifiedAt(verifiedAt)
				.build();

		when(paymentRepository.findWithDetailsById(7L)).thenReturn(Optional.of(payment));
		// Status di database dibaca ulang menjelang keputusan, karena admin bisa
		// saja memutuskan manual selagi pekerjaan ini mengantre.
		when(paymentRepository.findStatusById(7L)).thenReturn(Optional.of(status));
		return payment;
	}

	private void ocrMembaca(String nominal, double keyakinan) {
		Map<String, Object> hasil = new HashMap<>();
		hasil.put("confidence", keyakinan);
		hasil.put("extracted_amount", new BigDecimal(nominal).doubleValue());
		hasil.put("verification_status", "ok");
		hasil.put("flags", List.of());
		when(ocrClient.read(any(), any())).thenReturn(hasil);
	}

	@SuppressWarnings("unchecked")
	private static List<String> catatan(Payment payment) {
		return (List<String>) payment.getOcrData().get("flags");
	}

	// --- Kapan nominal boleh ditulis ulang ---

	@Test
	@DisplayName("belum diputuskan: nominal disesuaikan ke angka yang terbaca di bukti")
	void nominalDisesuaikanSelagiMasihPending() {
		Payment payment = pembayaran("1000000", PaymentStatus.PENDING, null);
		ocrMembaca("1200000", 1.0);

		consumer.handle(new OcrJobMessage(7L));

		assertThat(payment.getAmount()).isEqualByComparingTo("1200000");
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.NEEDS_REVIEW);
	}

	@Test
	@DisplayName("sudah diverifikasi manual: nominal TIDAK ikut ditulis ulang")
	void nominalTidakDitimpaSetelahKeputusanManual() {
		// Admin sempat memverifikasi manual selagi pekerjaan OCR mengantre,
		// dan uangnya sudah dibagikan ke cicilan.
		Payment payment = pembayaran("1000000", PaymentStatus.VERIFIED, Instant.now());
		ocrMembaca("1200000", 1.0);

		consumer.handle(new OcrJobMessage(7L));

		// Inti perbaikannya: sebelumnya nominal tetap tertulis 1.200.000 padahal
		// cicilan hanya menerima 1.000.000 dan alokasi tidak pernah dihitung
		// ulang, sehingga kuitansi mencetak angka yang tidak pernah masuk.
		assertThat(payment.getAmount()).isEqualByComparingTo("1000000");
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
	}

	@Test
	@DisplayName("sudah diverifikasi manual: selisihnya tetap dicatat supaya admin tahu")
	void selisihTetapDicatatWalauNominalTidakDiubah() {
		Payment payment = pembayaran("1000000", PaymentStatus.VERIFIED, Instant.now());
		ocrMembaca("1200000", 1.0);

		consumer.handle(new OcrJobMessage(7L));

		assertThat(catatan(payment))
				.anySatisfy(c -> assertThat(c).contains("Selisih nominal"))
				.anySatisfy(c -> assertThat(c).contains("dibiarkan apa adanya"));
	}

	@Test
	@DisplayName("sudah ditolak manual: status dan nominal tidak disentuh, alokasi tidak dipanggil")
	void penolakanManualTidakDitimpa() {
		Payment payment = pembayaran("1000000", PaymentStatus.REJECTED, null);
		ocrMembaca("1000000", 1.0);

		consumer.handle(new OcrJobMessage(7L));

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
		assertThat(payment.getAmount()).isEqualByComparingTo("1000000");
		verify(allocationService, never()).allocate(anyLong());
	}

	@Test
	@DisplayName("uang sudah dialokasikan: nominal tidak diubah walau statusnya masih boleh diubah")
	void nominalTidakDiubahBilaUangSudahDibagikan() {
		// Pengaman lapis kedua: yang menentukan bukan cuma statusnya, tapi juga
		// apakah uangnya sudah terlanjur masuk ke cicilan.
		Payment payment = pembayaran("1000000", PaymentStatus.NEEDS_REVIEW, Instant.now());
		ocrMembaca("1200000", 1.0);

		consumer.handle(new OcrJobMessage(7L));

		assertThat(payment.getAmount()).isEqualByComparingTo("1000000");
	}

	@Test
	@DisplayName("sudah diverifikasi manual: waktu verifikasi admin tidak ikut ditimpa")
	void waktuVerifikasiAdminTidakDitimpa() {
		// Kalau waktunya ikut berubah, jejak audit mencatat pembayaran ini
		// diverifikasi admin pada jam saat OCR selesai, bukan saat admin menekan
		// tombolnya.
		Instant diverifikasiAdmin = Instant.parse("2026-09-03T10:00:00Z");
		Payment payment = pembayaran("1200000", PaymentStatus.VERIFIED, null, diverifikasiAdmin);
		ocrMembaca("1200000", 0.99);

		consumer.handle(new OcrJobMessage(7L));

		assertThat(payment.getVerifiedAt()).isEqualTo(diverifikasiAdmin);
	}

	// --- Keputusan otomatis ---

	@Test
	@DisplayName("keyakinan tinggi dan nominal cocok: terverifikasi otomatis lalu dialokasikan")
	void cocokDanYakinMakaAutoVerified() {
		Payment payment = pembayaran("1200000", PaymentStatus.PENDING, null);
		ocrMembaca("1200000", 0.97);

		consumer.handle(new OcrJobMessage(7L));

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTO_VERIFIED);
		assertThat(payment.getVerifiedAt()).isNotNull();
		verify(allocationService).allocate(7L);
	}

	@Test
	@DisplayName("keyakinan di bawah ambang bawah: ditolak otomatis dengan alasan yang bisa dibaca mahasiswa")
	void keyakinanTerlaluRendahMakaDitolak() {
		Payment payment = pembayaran("1200000", PaymentStatus.PENDING, null);
		ocrMembaca("1200000", 0.05);

		consumer.handle(new OcrJobMessage(7L));

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
		assertThat(payment.getRejectReason()).isNotBlank();
		verify(allocationService, never()).allocate(anyLong());
	}

	@Test
	@DisplayName("keyakinan sedang: perlu ditinjau, tidak dialokasikan")
	void keyakinanSedangMakaPerluDitinjau() {
		Payment payment = pembayaran("1200000", PaymentStatus.PENDING, null);
		ocrMembaca("1200000", 0.50);

		consumer.handle(new OcrJobMessage(7L));

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.NEEDS_REVIEW);
		verify(allocationService, never()).allocate(anyLong());
	}

	@Test
	@DisplayName("pembayaran sudah terhapus: pesan diabaikan tanpa melempar galat")
	void pembayaranTidakDitemukan() {
		when(paymentRepository.findWithDetailsById(7L)).thenReturn(Optional.empty());

		consumer.handle(new OcrJobMessage(7L));

		verify(ocrClient, never()).read(any(), any());
		verify(allocationService, never()).allocate(anyLong());
	}
}
