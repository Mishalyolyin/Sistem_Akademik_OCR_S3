package ac.kampus.pembayaran.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Jejak audit tiap kali admin mengubah nominal satu cicilan.
 *
 * <p>Sengaja terpisah dari tabel adjustments: yang ini mengubah "berapa yang
 * HARUS dibayar", sedangkan adjustment mengubah "berapa yang SUDAH dibayar".
 * Kalau digabung, riwayat uang masuk dan riwayat perubahan tagihan bercampur
 * dan sulit ditelusuri saat ada sengketa.
 */
@Entity
@Table(name = "installment_amount_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentAmountChange {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "installment_id", nullable = false)
	private Long installmentId;

	@Column(name = "old_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal oldAmount;

	@Column(name = "new_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal newAmount;

	@Column(nullable = false, length = 500)
	private String reason;

	@Column(name = "admin_id", nullable = false)
	private Long adminId;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
