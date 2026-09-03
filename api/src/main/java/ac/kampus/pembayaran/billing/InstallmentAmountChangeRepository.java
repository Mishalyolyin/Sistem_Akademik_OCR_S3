package ac.kampus.pembayaran.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstallmentAmountChangeRepository
		extends JpaRepository<InstallmentAmountChange, Long> {

	List<InstallmentAmountChange> findByInstallmentIdOrderByCreatedAtDesc(Long installmentId);
}
