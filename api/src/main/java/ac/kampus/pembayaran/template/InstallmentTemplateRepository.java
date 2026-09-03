package ac.kampus.pembayaran.template;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.PaymentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstallmentTemplateRepository extends JpaRepository<InstallmentTemplate, Long> {

	Optional<InstallmentTemplate> findByCategoryAndTermAndActiveTrue(
			PaymentCategory category, AcademicTerm term);

	List<InstallmentTemplate> findAllByOrderByCategoryAscTermAsc();
}
