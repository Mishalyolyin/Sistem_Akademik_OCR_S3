package ac.kampus.pembayaran.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

	/**
	 * Kunci baris cicilan selama transaksi berjalan. Wajib dipakai sebelum
	 * mengubah nominal atau mengalokasikan pembayaran, supaya dua permintaan
	 * bersamaan tidak saling menimpa.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM Installment i WHERE i.id = :id")
	Optional<Installment> findByIdForUpdate(@Param("id") Long id);

	@Query("SELECT COALESCE(SUM(i.amount), 0) FROM Installment i WHERE i.paymentPlan.id = :planId")
	BigDecimal sumAmountByPlan(@Param("planId") Long planId);
}
