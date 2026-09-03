package ac.kampus.pembayaran.adjustment;

import ac.kampus.pembayaran.billing.Installment;
import ac.kampus.pembayaran.billing.InstallmentRepository;
import ac.kampus.pembayaran.billing.InstallmentStatus;
import ac.kampus.pembayaran.billing.PaymentPlan;
import ac.kampus.pembayaran.billing.PaymentPlanRepository;
import ac.kampus.pembayaran.billing.PlanStatus;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mutasi uang yang dibuat admin secara manual.
 *
 * <p>Dipakai untuk dua kasus yang tidak punya jalan keluar lain:
 * <ol>
 *   <li>Kelebihan bayar yang muncul saat nominal cicilan diturunkan di bawah
 *       yang sudah dibayar. Nominalnya berubah, {@code amount_paid} sengaja
 *       tidak disentuh, jadi selisihnya dipindahkan ke saldo lewat sini.</li>
 *   <li>Koreksi golongan potongan yang baru ketahuan setelah mahasiswa
 *       mengunggah bukti. Golongan sudah terkunci saat itu.</li>
 * </ol>
 *
 * <p>Setiap mutasi mengunci baris sasarannya dan menulis satu baris audit
 * beserta nilai saldo sesudahnya, supaya aliran uang tetap bisa ditelusuri
 * tanpa memutar ulang seluruh riwayat.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdjustmentService {

	/** Sama dengan aturan alasan pada ubah nominal cicilan, supaya konsisten. */
	private static final int ALASAN_MINIMAL = 5;

	private final AdjustmentRepository repository;
	private final StudentRepository studentRepository;
	private final InstallmentRepository installmentRepository;
	private final PaymentPlanRepository planRepository;

	@Transactional(readOnly = true)
	public List<Adjustment> riwayat(Long studentId) {
		if (!studentRepository.existsById(studentId)) {
			throw NotFoundException.of("Mahasiswa", studentId);
		}
		return repository.findByStudentIdOrderByCreatedAtDescIdDesc(studentId);
	}

	/**
	 * Menambah atau mengurangi saldo mahasiswa.
	 *
	 * @param amount bertanda; positif menambah, negatif mengurangi
	 */
	@Transactional
	public Adjustment sesuaikanSaldo(Long studentId, BigDecimal amount, String reason, Long adminId) {
		String alasan = periksaAlasan(reason);
		periksaNominal(amount);

		Student student = studentRepository.findByIdForUpdate(studentId)
				.orElseThrow(() -> NotFoundException.of("Mahasiswa", studentId));

		BigDecimal saldoBaru = student.getWalletBalance().add(amount);
		if (saldoBaru.signum() < 0) {
			throw new BusinessRuleException(
					"Saldo tidak boleh minus. Saldo sekarang %s, penyesuaian yang diminta %s."
							.formatted(student.getWalletBalance().toPlainString(),
									amount.toPlainString()));
		}

		student.setWalletBalance(saldoBaru);
		studentRepository.save(student);

		log.info("Saldo mahasiswa {} disesuaikan {} oleh admin {}, jadi {}",
				student.getNim(), amount, adminId, saldoBaru);

		return catat(studentId, null, amount, saldoBaru, alasan, adminId);
	}

	/**
	 * Menambah atau mengurangi uang yang tercatat sudah masuk ke satu cicilan.
	 *
	 * @param amount bertanda; positif menambah, negatif mengurangi
	 */
	@Transactional
	public Adjustment sesuaikanCicilan(Long studentId, Long installmentId, BigDecimal amount,
			String reason, Long adminId) {
		String alasan = periksaAlasan(reason);
		periksaNominal(amount);

		Installment cicilan = installmentRepository.findByIdForUpdate(installmentId)
				.orElseThrow(() -> NotFoundException.of("Cicilan", installmentId));

		Long pemilik = cicilan.getPaymentPlan().getStudent().getId();
		if (!pemilik.equals(studentId)) {
			throw new BusinessRuleException(
					"Cicilan itu milik mahasiswa lain, tidak bisa disesuaikan di sini.");
		}

		BigDecimal dibayarBaru = cicilan.getAmountPaid().add(amount);
		if (dibayarBaru.signum() < 0) {
			throw new BusinessRuleException(
					"Uang yang tercatat masuk tidak boleh minus. Sekarang tercatat %s."
							.formatted(cicilan.getAmountPaid().toPlainString()));
		}
		// Kelebihan sengaja tidak boleh menumpuk di cicilan: di sana ia tidak
		// kelihatan di mana pun dan tidak ikut terhitung sebagai uang mahasiswa.
		// Tempatnya di saldo, yang memang dibuat untuk itu.
		if (dibayarBaru.compareTo(cicilan.getAmount()) > 0) {
			throw new BusinessRuleException(
					("Penyesuaian ini membuat cicilan terbayar %s dari tagihan %s. "
							+ "Kelebihan tidak boleh menumpuk di cicilan; masukkan ke saldo mahasiswa.")
							.formatted(dibayarBaru.toPlainString(),
									cicilan.getAmount().toPlainString()));
		}

		cicilan.setAmountPaid(dibayarBaru);
		cicilan.refreshStatus();
		installmentRepository.save(cicilan);

		perbaruiStatusPlan(cicilan.getPaymentPlan().getId());

		log.info("Cicilan {} disesuaikan {} oleh admin {}, terbayar jadi {}",
				installmentId, amount, adminId, dibayarBaru);

		return catat(studentId, installmentId, amount, dibayarBaru, alasan, adminId);
	}

	// --- Pembantu ---

	private String periksaAlasan(String reason) {
		String alasan = reason == null ? "" : reason.trim();
		if (alasan.length() < ALASAN_MINIMAL) {
			throw new BusinessRuleException(
					"Alasan penyesuaian wajib diisi, minimal %d karakter.".formatted(ALASAN_MINIMAL));
		}
		return alasan;
	}

	private void periksaNominal(BigDecimal amount) {
		if (amount == null || amount.signum() == 0) {
			throw new BusinessRuleException(
					"Nominal penyesuaian tidak boleh nol. Isi positif untuk menambah, "
							+ "negatif untuk mengurangi.");
		}
	}

	/**
	 * Tagihan yang seluruh cicilannya lunas ikut ditutup, dan yang tadinya
	 * tertutup dibuka lagi bila penyesuaian membuatnya belum lunas.
	 */
	private void perbaruiStatusPlan(Long planId) {
		PaymentPlan plan = planRepository.findWithInstallmentsById(planId).orElse(null);
		if (plan == null || plan.getStatus() == PlanStatus.CANCELLED) {
			return;
		}

		boolean lunasSemua = plan.getInstallments().stream()
				.allMatch(i -> i.getStatus() == InstallmentStatus.PAID);

		PlanStatus statusBaru = lunasSemua ? PlanStatus.COMPLETED : PlanStatus.ACTIVE;
		if (plan.getStatus() != statusBaru) {
			plan.setStatus(statusBaru);
			planRepository.save(plan);
		}
	}

	private Adjustment catat(Long studentId, Long installmentId, BigDecimal amount,
			BigDecimal balanceAfter, String reason, Long adminId) {
		return repository.save(Adjustment.builder()
				.studentId(studentId)
				.installmentId(installmentId)
				.amount(amount)
				.balanceAfter(balanceAfter)
				.reason(reason)
				.adminId(adminId)
				.build());
	}
}
