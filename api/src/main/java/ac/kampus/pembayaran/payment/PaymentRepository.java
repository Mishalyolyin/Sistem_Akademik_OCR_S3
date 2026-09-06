package ac.kampus.pembayaran.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository
		extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

	/** Dipakai StudentTierLockPolicy: golongan terkunci begitu ada unggahan. */
	boolean existsByStudentId(Long studentId);

	@EntityGraph(attributePaths = { "student", "student.studyClass", "installment", "paymentPlan" })
	Optional<Payment> findWithDetailsById(Long id);

	@Override
	@EntityGraph(attributePaths = { "student", "student.studyClass", "installment", "paymentPlan" })
	Page<Payment> findAll(@Nullable Specification<Payment> spec, Pageable pageable);

	/** Baca status terkini tanpa memuat seluruh entitas. */
	@Query("SELECT p.status FROM Payment p WHERE p.id = :id")
	Optional<PaymentStatus> findStatusById(@Param("id") Long id);

	/**
	 * Pembayaran yang masih dianggap sah pada satu tagihan.
	 *
	 * <p>Dipakai sebelum membatalkan tagihan: uang yang sudah diterima tidak
	 * boleh hilang hanya karena tagihannya dicoret. Yang ditolak sengaja tidak
	 * ikut — penolakan berarti uang itu memang tidak pernah diakui masuk.
	 */
	@Query("""
			SELECT COUNT(p) FROM Payment p
			WHERE p.paymentPlan.id = :planId AND p.status IN :statuses
			""")
	long countOnPlan(
			@Param("planId") Long planId,
			@Param("statuses") Collection<PaymentStatus> statuses);

	@Query("""
			SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
			WHERE p.paymentPlan.id = :planId AND p.status IN :statuses
			""")
	BigDecimal sumOnPlan(
			@Param("planId") Long planId,
			@Param("statuses") Collection<PaymentStatus> statuses);
}
