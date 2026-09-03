package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Penyesuaian nominal satu cicilan oleh admin.
 *
 * <p>Ini jalan keluar untuk kasus khusus, bukan alur utama — nominal normalnya
 * terhitung otomatis dari golongan potongan mahasiswa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallmentBillingService {

	private final InstallmentRepository installmentRepository;
	private final PaymentPlanRepository planRepository;
	private final InstallmentAmountChangeRepository changeRepository;

	@Transactional
	public InstallmentAmountChange updateAmount(
			Long installmentId, BigDecimal newAmount, String reason, Long adminId) {

		if (newAmount == null || newAmount.signum() <= 0) {
			throw new BusinessRuleException("Nominal harus lebih besar dari nol.");
		}
		if (reason == null || reason.trim().length() < 5) {
			throw new BusinessRuleException("Alasan wajib diisi, minimal 5 karakter.");
		}

		// Kunci baris cicilan supaya dua admin tidak mengubah nominal bersamaan.
		Installment installment = installmentRepository.findByIdForUpdate(installmentId)
				.orElseThrow(() -> NotFoundException.of("Cicilan", installmentId));

		BigDecimal oldAmount = installment.getAmount();
		if (oldAmount.compareTo(newAmount) == 0) {
			throw new BusinessRuleException("Nominal barunya sama dengan yang sekarang.");
		}

		installment.setAmount(newAmount);
		// Status dihitung ulang dari amount_paid dibanding amount BARU.
		// amount_paid TIDAK disentuh: kalau nominal turun di bawah yang sudah
		// dibayar, cicilan jadi lunas tapi kelebihannya tidak dikembalikan
		// otomatis — itu urusan fitur Penyesuaian, supaya audit tidak campur.
		installment.refreshStatus();
		installmentRepository.save(installment);

		PaymentPlan plan = installment.getPaymentPlan();
		plan.setTotalAmount(installmentRepository.sumAmountByPlan(plan.getId()));
		planRepository.save(plan);

		InstallmentAmountChange change = changeRepository.save(InstallmentAmountChange.builder()
				.installmentId(installment.getId())
				.oldAmount(oldAmount)
				.newAmount(newAmount)
				.reason(reason.trim())
				.adminId(adminId)
				.build());

		log.info("Cicilan {} diubah dari {} ke {} oleh admin {}: {}",
				installmentId, oldAmount, newAmount, adminId, reason.trim());
		return change;
	}
}
