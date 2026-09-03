package ac.kampus.pembayaran.tuition;

import ac.kampus.pembayaran.student.DiscountTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountTierRateRepository extends JpaRepository<DiscountTierRate, DiscountTier> {

	List<DiscountTierRate> findAllByOrderByPercentAsc();
}
