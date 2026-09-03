package ac.kampus.pembayaran.payment;

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

import java.time.Instant;

/** Catatan tiap perpindahan status pembayaran, otomatis maupun oleh admin. */
@Entity
@Table(name = "verification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "payment_id", nullable = false)
	private Long paymentId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "from_status", columnDefinition = "payment_status")
	private PaymentStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "to_status", nullable = false, columnDefinition = "payment_status")
	private PaymentStatus toStatus;

	/** Kosong berarti keputusan otomatis oleh sistem. */
	@Column(name = "admin_id")
	private Long adminId;

	@Column(length = 500)
	private String note;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
