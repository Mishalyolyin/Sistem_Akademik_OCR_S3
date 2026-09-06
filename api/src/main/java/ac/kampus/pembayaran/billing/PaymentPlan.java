package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.student.Student;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Satu tagihan untuk satu mahasiswa, satu kategori, satu semester.
 *
 * <p>{@code baseAmount} dan {@code discountPercent} dibekukan saat plan dibuat.
 * Mengubah tarif atau persen potongan belakangan tidak menyentuh plan ini —
 * hanya berlaku untuk plan berikutnya.
 */
@Entity
@Table(name = "payment_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(name = "installment_template_id")
	private Long installmentTemplateId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "payment_category")
	private PaymentCategory category;

	@Column(name = "academic_year", nullable = false, length = 9)
	private String academicYear;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "term", nullable = false, columnDefinition = "academic_term")
	private AcademicTerm term;

	/** 1 sampai 6, hanya untuk kategori UKT. */
	@Column(name = "semester_number")
	private Integer semesterNumber;

	@Column(name = "base_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal baseAmount;

	@Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal discountPercent;

	@Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "plan_status")
	private PlanStatus status;

	@OneToMany(mappedBy = "paymentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("installmentNo ASC")
	@Builder.Default
	private List<Installment> installments = new ArrayList<>();

	/**
	 * Jejak pembatalan. Terisi bersamaan dengan status CANCELLED, dan kosong
	 * selama tidak — pasangannya dijaga CHECK di migrasi V12, bukan hanya di
	 * sini, supaya tidak ada jalan lain yang bisa memisahkannya.
	 */
	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "cancelled_by")
	private Long cancelledBy;

	@Column(name = "cancel_reason", length = 500)
	private String cancelReason;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	public void addInstallment(Installment installment) {
		installment.setPaymentPlan(this);
		installments.add(installment);
	}

	/** Jumlah yang sudah dibayar di seluruh cicilan plan ini. */
	public BigDecimal amountPaid() {
		return installments.stream()
				.map(Installment::getAmountPaid)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal remaining() {
		return totalAmount.subtract(amountPaid()).max(BigDecimal.ZERO);
	}

	public boolean isFullyPaid() {
		return !installments.isEmpty()
				&& installments.stream().allMatch(i -> i.getStatus() == InstallmentStatus.PAID);
	}
}
