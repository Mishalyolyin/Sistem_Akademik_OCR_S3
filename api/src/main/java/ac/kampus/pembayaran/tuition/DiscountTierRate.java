package ac.kampus.pembayaran.tuition;

import ac.kampus.pembayaran.student.DiscountTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
public class DiscountTierRate {

	@Id
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "tier", nullable = false, columnDefinition = "discount_tier")
	private DiscountTier tier;

	@Column(nullable = false, length = 60)
	private String label;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal percent;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	/** Menerapkan potongan ke tarif dasar, dibulatkan ke rupiah penuh. */
	public BigDecimal applyTo(BigDecimal baseAmount) {
		BigDecimal multiplier = BigDecimal.ONE
				.subtract(percent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
		return baseAmount.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
	}
}
