package ac.kampus.pembayaran.tuition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** Persentase potongan per golongan. Hanya berlaku untuk UKT. */
@Entity
@Table(name = "discount_tier_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountTierRate {

	/** Kode golongan, dipakai apa adanya di API dan berkas import. */
	@Id
	@Column(name = "tier", nullable = false, length = 40)
	private String tier;

	@Column(nullable = false, length = 60)
	private String label;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal percent;

	/** Golongan lama dinonaktifkan, bukan dihapus, supaya riwayatnya tetap utuh. */
	@Column(nullable = false)
	private boolean active;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	/** Menerapkan potongan ke tarif dasar, dibulatkan ke rupiah penuh. */
	public BigDecimal applyTo(BigDecimal baseAmount) {
		BigDecimal multiplier = BigDecimal.ONE
				.subtract(percent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
		return baseAmount.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
	}
}
