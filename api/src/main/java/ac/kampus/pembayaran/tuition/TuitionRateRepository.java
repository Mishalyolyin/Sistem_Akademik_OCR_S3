package ac.kampus.pembayaran.tuition;

import ac.kampus.pembayaran.common.PaymentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TuitionRateRepository extends JpaRepository<TuitionRate, Long> {

	Optional<TuitionRate> findByCategoryAndAcademicYearAndActiveTrue(
			PaymentCategory category, String academicYear);

	List<TuitionRate> findAllByOrderByAcademicYearDescCategoryAsc();
}
