package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.billing.Installment;
import ac.kampus.pembayaran.billing.InstallmentRepository;
import ac.kampus.pembayaran.billing.InstallmentStatus;
import ac.kampus.pembayaran.billing.PaymentPlan;
import ac.kampus.pembayaran.billing.PaymentPlanRepository;
import ac.kampus.pembayaran.billing.PlanStatus;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.settings.SystemSettingService;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Membagikan uang yang sudah terverifikasi ke cicilan.
 *
 * <p>Urutan prioritasnya:
 * <ol>
 *   <li>Cicilan yang dituju saat mengunggah, bila ada.</li>
 *   <li>Cicilan terlama yang belum lunas pada tagihan yang sama.</li>
 *   <li>Sisanya masuk saldo mahasiswa.</li>
 * </ol>
 *
 * <p>Seluruh proses dalam satu transaksi dengan penguncian baris, supaya dua
 * pembayaran yang diverifikasi bersamaan tidak menghitung sisa dari kondisi
 * yang sama lalu saling menimpa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAllocationService {

	private final PaymentRepository paymentRepository;
	private final InstallmentRepository installmentRepository;
	private final PaymentPlanRepository planRepository;
	private final StudentRepository studentRepository;
	private final VerificationLogRepository logRepository;
	private final SystemSettingService settings;

	@Transactional
	public void allocate(Long paymentId) {
		Payment payment = paymentRepository.findWithDetailsById(paymentId)
				.orElseThrow(() -> NotFoundException.of("Pembayaran", paymentId));

		if (payment.getAllocatedAt() != null) {
			log.info("Pembayaran {} sudah dialokasikan pada {}", paymentId, payment.getAllocatedAt());
			return;
		}
		if (!payment.getStatus().sudahDiverifikasi()) {
			log.warn("Pembayaran {} berstatus {}, belum boleh dialokasikan.",
					paymentId, payment.getStatus());
			return;
		}

		BigDecimal sisa = payment.getAmount();
		BigDecimal toleransi = settings.getAmount(
				SystemSettingService.PAYMENT_TOLERANCE_AMOUNT, BigDecimal.ZERO);

		// 1. Cicilan yang dituju saat mengunggah.
		if (payment.getInstallment() != null) {
			Installment target = installmentRepository
					.findByIdForUpdate(payment.getInstallment().getId())
					.orElseThrow(() -> NotFoundException.of(
							"Cicilan", payment.getInstallment().getId()));

			if (target.getStatus() == InstallmentStatus.PAID) {
				// Uangnya TIDAK dialihkan diam-diam ke cicilan lain atau ke saldo:
				// itu membuat aliran uang sulit ditelusuri saat ada sengketa.
				// Serahkan ke manusia.
				tandaiPerluDitinjau(payment,
						"Cicilan tujuan sudah lunas. Tentukan sendiri alokasinya.");
				return;
			}

			sisa = bayarkan(target, sisa, toleransi);
		}

		// 2. Cicilan terlama yang belum lunas pada tagihan yang sama.
		if (sisa.signum() > 0 && payment.getPaymentPlan() != null) {
			PaymentPlan plan = planRepository
					.findWithInstallmentsById(payment.getPaymentPlan().getId())
					.orElse(null);

			if (plan != null) {
				List<Installment> belumLunas = plan.getInstallments().stream()
						.filter(i -> i.getStatus() != InstallmentStatus.PAID)
						.sorted(Comparator
								.comparing(Installment::getDueDate)
								.thenComparing(Installment::getInstallmentNo))
						.toList();

				for (Installment cicilan : belumLunas) {
					if (sisa.signum() <= 0) break;
					Installment terkunci = installmentRepository
							.findByIdForUpdate(cicilan.getId())
							.orElse(null);
					if (terkunci != null) {
						sisa = bayarkan(terkunci, sisa, BigDecimal.ZERO);
					}
				}

				perbaruiStatusPlan(plan);
			}
		}

		// 3. Kelebihan masuk saldo mahasiswa.
		if (sisa.signum() > 0) {
			Student student = studentRepository.findById(payment.getStudent().getId())
					.orElseThrow(() -> NotFoundException.of("Mahasiswa", payment.getStudent().getId()));

			student.setWalletBalance(student.getWalletBalance().add(sisa));
			studentRepository.save(student);

			log.info("Pembayaran {}: kelebihan {} masuk saldo {}", paymentId, sisa, student.getNim());
		}

		payment.setAllocatedAt(Instant.now());
		paymentRepository.save(payment);
		log.info("Pembayaran {} selesai dialokasikan.", paymentId);
	}

	/**
	 * Membayarkan sebagian uang ke satu cicilan.
	 *
	 * @param toleransiBuang sisa sekecil ini dianggap biaya bank atau kode unik
	 *                       yang memang tidak pernah diterima kampus, jadi
	 *                       dihapus alih-alih dibawa ke cicilan berikutnya.
	 * @return sisa uang yang belum terpakai
	 */
	private BigDecimal bayarkan(Installment cicilan, BigDecimal uang, BigDecimal toleransiBuang) {
		BigDecimal kurang = cicilan.outstanding();
		if (kurang.signum() <= 0) {
			return uang;
		}

		BigDecimal dibayar = uang.min(kurang);
		cicilan.setAmountPaid(cicilan.getAmountPaid().add(dibayar));
		cicilan.refreshStatus();
		installmentRepository.save(cicilan);

		BigDecimal sisa = uang.subtract(dibayar);

		if (sisa.signum() > 0 && sisa.compareTo(toleransiBuang) <= 0) {
			log.info("Cicilan {}: sisa {} dibuang karena masih dalam toleransi (kode unik bank).",
					cicilan.getId(), sisa);
			return BigDecimal.ZERO;
		}

		return sisa;
	}

	private void perbaruiStatusPlan(PaymentPlan plan) {
		boolean lunasSemua = plan.getInstallments().stream()
				.allMatch(i -> i.getStatus() == InstallmentStatus.PAID);

		PlanStatus statusBaru = lunasSemua ? PlanStatus.COMPLETED : PlanStatus.ACTIVE;
		if (plan.getStatus() != statusBaru && plan.getStatus() != PlanStatus.CANCELLED) {
			plan.setStatus(statusBaru);
			planRepository.save(plan);
		}
	}

	private void tandaiPerluDitinjau(Payment payment, String alasan) {
		PaymentStatus statusLama = payment.getStatus();
		payment.setStatus(PaymentStatus.NEEDS_REVIEW);
		paymentRepository.save(payment);

		logRepository.save(VerificationLog.builder()
				.paymentId(payment.getId())
				.fromStatus(statusLama)
				.toStatus(PaymentStatus.NEEDS_REVIEW)
				.note(alasan)
				.build());

		log.warn("Pembayaran {}: {}", payment.getId(), alasan);
	}
}
