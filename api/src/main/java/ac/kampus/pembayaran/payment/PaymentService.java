package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.billing.Installment;
import ac.kampus.pembayaran.billing.InstallmentRepository;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.payment.ocr.OcrJobPublisher;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final VerificationLogRepository logRepository;
	private final StudentRepository studentRepository;
	private final InstallmentRepository installmentRepository;
	private final FileStorageService storage;
	private final OcrJobPublisher publisher;
	private final PaymentAllocationService allocationService;

	/**
	 * Menerima bukti bayar lalu mengantrekan pembacaan OCR.
	 *
	 * <p>Berkas disimpan dan barisnya dibuat lebih dulu; pembacaan berjalan di
	 * belakang layar supaya pengunggah tidak menunggu OCR selesai.
	 */
	@Transactional
	public Payment upload(
			Long studentId, Long installmentId, BigDecimal amount, MultipartFile proof) {

		if (amount == null || amount.signum() <= 0) {
			throw new BusinessRuleException("Nominal harus lebih besar dari nol.");
		}

		// Kelas ikut diambil karena respons menampilkannya, dan relasi lazy
		// tidak bisa diakses lagi setelah transaksi tutup (open-in-view mati).
		Student student = studentRepository.findWithClassById(studentId)
				.orElseThrow(() -> NotFoundException.of("Mahasiswa", studentId));

		Installment installment = null;
		if (installmentId != null) {
			installment = installmentRepository.findById(installmentId)
					.orElseThrow(() -> NotFoundException.of("Cicilan", installmentId));

			Long pemilik = installment.getPaymentPlan().getStudent().getId();
			if (!pemilik.equals(studentId)) {
				throw new BusinessRuleException(
						"Cicilan itu milik mahasiswa lain, tidak bisa dibayarkan di sini.");
			}
			if (installment.getStatus() == ac.kampus.pembayaran.billing.InstallmentStatus.PAID) {
				throw new BusinessRuleException("Cicilan itu sudah lunas.");
			}
		}

		String path = storage.store(proof, "bukti-bayar");

		Payment payment = paymentRepository.save(Payment.builder()
				.student(student)
				.installment(installment)
				.paymentPlan(installment == null ? null : installment.getPaymentPlan())
				.amount(amount)
				.proofFilePath(path)
				.status(PaymentStatus.PENDING)
				.build());

		logRepository.save(VerificationLog.builder()
				.paymentId(payment.getId())
				.toStatus(PaymentStatus.PENDING)
				.note("Bukti diunggah, menunggu pembacaan OCR")
				.build());

		publisher.publishAfterCommit(payment.getId());
		return payment;
	}

	/** Keputusan manual admin. Selalu menang atas hasil OCR yang datang belakangan. */
	@Transactional
	public Payment decide(Long paymentId, boolean terima, String catatan, Long adminId) {
		Payment payment = paymentRepository.findWithDetailsById(paymentId)
				.orElseThrow(() -> NotFoundException.of("Pembayaran", paymentId));

		if (!terima && (catatan == null || catatan.trim().length() < 5)) {
			throw new BusinessRuleException("Alasan penolakan wajib diisi, minimal 5 karakter.");
		}
		if (payment.getStatus().sudahDiverifikasi() && terima) {
			throw new BusinessRuleException("Pembayaran ini sudah diverifikasi.");
		}

		PaymentStatus statusLama = payment.getStatus();
		payment.setStatus(terima ? PaymentStatus.VERIFIED : PaymentStatus.REJECTED);
		payment.setVerifiedAt(Instant.now());
		payment.setVerifiedBy(adminId);
		payment.setRejectReason(terima ? null : catatan.trim());
		paymentRepository.save(payment);

		logRepository.save(VerificationLog.builder()
				.paymentId(payment.getId())
				.fromStatus(statusLama)
				.toStatus(payment.getStatus())
				.adminId(adminId)
				.note(catatan == null || catatan.isBlank() ? null : catatan.trim())
				.build());

		// Uang baru dibagikan ke cicilan setelah pembayaran diterima.
		if (terima) {
			allocationService.allocate(payment.getId());
		}

		log.info("Pembayaran {} diputuskan {} oleh admin {}",
				paymentId, payment.getStatus(), adminId);
		return payment;
	}

	/**
	 * Membatalkan keputusan yang sudah diambil, beserta uang yang telanjur
	 * dibagikan.
	 *
	 * <p>Ini satu-satunya jalan keluar dari kekeliruan yang paling mahal di
	 * sistem ini: bukti palsu yang telanjur diverifikasi, atau tombol yang salah
	 * pencet. Tanpa jalur ini, satu-satunya cara membetulkannya adalah menyentuh
	 * database langsung — sementara barisnya tetap berbunyi VERIFIED, kuitansinya
	 * tetap bisa dicetak, dan jejak auditnya tetap menyatakan uang itu masuk.
	 *
	 * <p>Alasannya wajib, sama seperti penolakan dan perubahan nominal: yang
	 * dibatalkan di sini adalah pernyataan bahwa kampus menerima sejumlah uang.
	 */
	@Transactional
	public Payment batalkanKeputusan(Long paymentId, String alasan, Long adminId) {
		if (alasan == null || alasan.trim().length() < 5) {
			throw new BusinessRuleException(
					"Alasan pembatalan wajib diisi, minimal 5 karakter.");
		}

		Payment payment = paymentRepository.findWithDetailsById(paymentId)
				.orElseThrow(() -> NotFoundException.of("Pembayaran", paymentId));

		PaymentStatus statusLama = payment.getStatus();
		if (!statusLama.sudahDiputuskan()) {
			throw new BusinessRuleException(
					("Pembayaran ini berstatus %s, belum ada keputusan yang bisa dibatalkan. "
							+ "Untuk mengulang pembacaan OCR-nya, pakai Baca ulang.")
							.formatted(statusLama));
		}

		// Uangnya ditarik lebih dulu: kalau penarikannya ditolak — misalnya
		// saldonya sudah terpakai — statusnya tidak boleh terlanjur berubah.
		allocationService.reverse(paymentId);

		payment.setStatus(PaymentStatus.NEEDS_REVIEW);
		payment.setVerifiedAt(null);
		payment.setVerifiedBy(null);
		payment.setRejectReason(null);
		paymentRepository.save(payment);

		logRepository.save(VerificationLog.builder()
				.paymentId(payment.getId())
				.fromStatus(statusLama)
				.toStatus(PaymentStatus.NEEDS_REVIEW)
				.adminId(adminId)
				.note("Keputusan dibatalkan: " + alasan.trim())
				.build());

		log.warn("Pembayaran {} dibatalkan dari {} oleh admin {}: {}",
				paymentId, statusLama, adminId, alasan.trim());
		return payment;
	}

	/** Mengantrekan ulang pembacaan, misalnya setelah service OCR sempat mati. */
	@Transactional
	public void requeue(Long paymentId) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> NotFoundException.of("Pembayaran", paymentId));

		if (payment.getStatus().sudahDiverifikasi()) {
			throw new BusinessRuleException(
					"Pembayaran ini sudah diverifikasi, tidak perlu dibaca ulang.");
		}

		payment.setStatus(PaymentStatus.PENDING);
		paymentRepository.save(payment);
		publisher.publishAfterCommit(paymentId);
	}
}
