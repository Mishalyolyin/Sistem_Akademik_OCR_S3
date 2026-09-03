package ac.kampus.pembayaran.adjustment;

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
 * Satu baris mutasi uang yang dibuat admin secara manual.
 *
 * <p>Sengaja terpisah dari {@code installment_amount_changes}: yang itu mencatat
 * perubahan BESAR TAGIHAN, yang ini mencatat perpindahan UANG. Kalau digabung,
 * riwayat "tagihan berubah" dan "uang masuk" bercampur di satu log dan sulit
 * ditelusuri saat ada sengketa.
 *
 * <p>{@link #installmentId} kosong berarti penyesuaian mengenai saldo mahasiswa,
 * bukan cicilan tertentu.
 */
@Entity
@Table(name = "adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adjustment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "installment_id")
	private Long installmentId;

	/** Bertanda: positif menambah, negatif mengurangi. */
	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	/** Nilai sasaran sesudah penyesuaian, supaya riwayat bisa dibaca apa adanya. */
	@Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
	private BigDecimal balanceAfter;

	@Column(nullable = false)
	private String reason;

	@Column(name = "admin_id", nullable = false)
	private Long adminId;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	/** Sasarannya cicilan tertentu, bukan saldo mahasiswa. */
	public boolean mengenaiCicilan() {
		return installmentId != null;
	}
}
