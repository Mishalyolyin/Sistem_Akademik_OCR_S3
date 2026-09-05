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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Satu baris rincian alokasi: sekian rupiah dari pembayaran ini masuk ke sana.
 *
 * <p>Jumlah seluruh baris satu pembayaran selalu sama persis dengan nominalnya —
 * termasuk sisa yang dibuang karena toleransi, yang ikut dicatat supaya tidak
 * ada rupiah yang hilang tanpa keterangan.
 */
@Entity
@Table(name = "payment_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "payment_id", nullable = false)
	private Long paymentId;

	/** Terisi hanya untuk {@link AllocationKind#INSTALLMENT}. */
	@Column(name = "installment_id")
	private Long installmentId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "allocation_kind")
	private AllocationKind kind;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	/** Terisi saat alokasinya dibatalkan; barisnya tidak pernah dihapus. */
	@Column(name = "reversed_at")
	private Instant reversedAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
