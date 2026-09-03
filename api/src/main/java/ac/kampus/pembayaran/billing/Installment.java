package ac.kampus.pembayaran.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import java.time.LocalDate;

@Entity
@Table(name = "installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_plan_id", nullable = false)
	private PaymentPlan paymentPlan;

	@Column(name = "installment_no", nullable = false)
	private int installmentNo;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	/** Nominal yang harus dibayar. Bisa disesuaikan admin lewat audit. */
	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	/** Nominal yang sudah masuk. Tidak pernah diubah oleh fitur ubah nominal. */
	@Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
	private BigDecimal amountPaid;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "installment_status")
	private InstallmentStatus status;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	/** Sisa yang masih harus dibayar untuk cicilan ini. */
	public BigDecimal outstanding() {
		return amount.subtract(amountPaid).max(BigDecimal.ZERO);
	}

	/** Hitung ulang status dari amount_paid dibanding amount saat ini. */
	public void refreshStatus() {
		this.status = InstallmentStatus.of(amountPaid, amount);
	}
}
