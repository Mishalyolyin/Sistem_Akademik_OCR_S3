package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.billing.Installment;
import ac.kampus.pembayaran.billing.PaymentPlan;
import ac.kampus.pembayaran.student.Student;
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
import java.util.Map;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_plan_id")
	private PaymentPlan paymentPlan;

	/** Cicilan yang dituju. Kosong berarti dialokasikan otomatis (Fase 5). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "installment_id")
	private Installment installment;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(name = "proof_file_path", nullable = false, length = 500)
	private String proofFilePath;

	@Column(name = "bank_name", length = 60)
	private String bankName;

	@Column(name = "payment_proof_date")
	private LocalDate paymentProofDate;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "payment_status")
	private PaymentStatus status;

	/** Hasil mentah service OCR, disimpan apa adanya untuk penelusuran. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ocr_data", columnDefinition = "jsonb")
	private Map<String, Object> ocrData;

	@Column(name = "ocr_confidence", precision = 5, scale = 4)
	private BigDecimal ocrConfidence;

	@Column(name = "verified_at")
	private Instant verifiedAt;

	@Column(name = "verified_by")
	private Long verifiedBy;

	@Column(name = "reject_reason", length = 500)
	private String rejectReason;

	@Column(name = "allocated_at")
	private Instant allocatedAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;
}
