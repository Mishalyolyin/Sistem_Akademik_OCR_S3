package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.PaymentCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, Long> {

	@EntityGraph(attributePaths = "installments")
	Optional<PaymentPlan> findWithInstallmentsById(Long id);

	@EntityGraph(attributePaths = "installments")
	List<PaymentPlan> findByStudentIdOrderByCategoryAscSemesterNumberAsc(Long studentId);

	boolean existsByStudentIdAndCategoryAndAcademicYearAndTermAndStatusNot(
			Long studentId, PaymentCategory category, String academicYear,
			AcademicTerm term, PlanStatus status);

	boolean existsByStudentIdAndCategoryAndStatusNot(
			Long studentId, PaymentCategory category, PlanStatus status);

	/**
	 * Berapa semester UKT yang sudah pernah dibuatkan tagihan.
	 *
	 * <p>Kategori dan status dikirim sebagai PARAMETER, bukan ditulis langsung
	 * sebagai literal enum di JPQL. Kalau ditulis sebagai literal, Hibernate
	 * mengecastnya ke nama kelas Java ({@code ::PaymentCategory}) yang tidak
	 * dikenal PostgreSQL — tipe aslinya bernama {@code payment_category}.
	 */
	long countByStudentIdAndCategoryAndStatusNot(
			Long studentId, PaymentCategory category, PlanStatus status);

	@Query("""
			SELECT COALESCE(MAX(p.semesterNumber), 0) FROM PaymentPlan p
			WHERE p.student.id = :studentId
			  AND p.category = :category
			  AND p.status <> :excludedStatus
			""")
	int lastSemesterNumber(
			@Param("studentId") Long studentId,
			@Param("category") PaymentCategory category,
			@Param("excludedStatus") PlanStatus excludedStatus);

	@EntityGraph(attributePaths = "installments")
	Optional<PaymentPlan> findByStudentIdAndCategoryAndStatusNot(
			Long studentId, PaymentCategory category, PlanStatus status);
}
