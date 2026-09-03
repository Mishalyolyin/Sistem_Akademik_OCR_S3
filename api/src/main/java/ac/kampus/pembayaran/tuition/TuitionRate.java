package ac.kampus.pembayaran.tuition;

import ac.kampus.pembayaran.common.PaymentCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/** Tarif dasar per kategori, sebelum potongan golongan. */
@Entity
@Table(name = "tuition_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TuitionRate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "payment_category")
	private PaymentCategory category;

	@Column(name = "academic_year", nullable = false, length = 9)
	private String academicYear;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;
}
