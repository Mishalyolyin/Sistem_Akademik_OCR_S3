package ac.kampus.pembayaran.tuition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountTierRateRepository extends JpaRepository<DiscountTierRate, String> {

	/** Urutan tampilan diatur admin, bukan mengikuti abjad kodenya. */
	List<DiscountTierRate> findAllByOrderBySortOrderAscTierAsc();

	List<DiscountTierRate> findByActiveTrueOrderBySortOrderAscTierAsc();
}
