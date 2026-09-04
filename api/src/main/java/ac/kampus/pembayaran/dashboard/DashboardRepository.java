package ac.kampus.pembayaran.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Kueri agregat untuk dashboard.
 *
 * <p>Sengaja memakai SQL langsung, bukan JPA. Angka ringkasan begini butuh
 * penjumlahan lintas tabel; menghitungnya lewat entitas berarti memuat ribuan
 * baris ke memori hanya untuk dijumlahkan.
 */
@Repository
@RequiredArgsConstructor
public class DashboardRepository {

	private final JdbcClient jdbc;

	public long countActiveStudents() {
		return jdbc.sql("SELECT COUNT(*) FROM students WHERE active")
				.query(Long.class).single();
	}

	/**
	 * Jumlah bukti bayar per status, dalam satu kueri.
	 *
	 * <p>Sengaja tidak satu kueri per status: dashboard memerlukan kelimanya
	 * sekaligus, dan memecahnya berarti lima kali bolak-balik ke database untuk
	 * pertanyaan yang sebenarnya sama.
	 */
	public DashboardController.RingkasanStatus countPaymentsByStatus() {
		Map<String, Long> jumlah = new HashMap<>();
		jdbc.sql("SELECT status::text AS status, COUNT(*) AS jumlah FROM payments GROUP BY status")
				.query((rs, n) -> Map.entry(rs.getString("status"), rs.getLong("jumlah")))
				.list()
				.forEach(baris -> jumlah.put(baris.getKey(), baris.getValue()));

		return new DashboardController.RingkasanStatus(
				jumlah.getOrDefault("PENDING", 0L),
				jumlah.getOrDefault("NEEDS_REVIEW", 0L),
				jumlah.getOrDefault("REJECTED", 0L),
				jumlah.getOrDefault("VERIFIED", 0L) + jumlah.getOrDefault("AUTO_VERIFIED", 0L),
				jumlah.getOrDefault("FAILED", 0L));
	}

	public BigDecimal totalBilled() {
		return jdbc.sql("""
						SELECT COALESCE(SUM(amount), 0) FROM installments i
						JOIN payment_plans p ON p.id = i.payment_plan_id
						WHERE p.status <> 'CANCELLED'
						""")
				.query(BigDecimal.class).single();
	}

	public BigDecimal totalCollected() {
		return jdbc.sql("""
						SELECT COALESCE(SUM(amount_paid), 0) FROM installments i
						JOIN payment_plans p ON p.id = i.payment_plan_id
						WHERE p.status <> 'CANCELLED'
						""")
				.query(BigDecimal.class).single();
	}

	public BigDecimal totalWalletBalance() {
		return jdbc.sql("SELECT COALESCE(SUM(wallet_balance), 0) FROM students")
				.query(BigDecimal.class).single();
	}

	public List<DashboardController.KategoriRingkas> summaryByCategory() {
		return jdbc.sql("""
						SELECT p.category::text                      AS kategori,
						       COUNT(DISTINCT p.id)                  AS jumlah,
						       COALESCE(SUM(i.amount), 0)            AS tertagih,
						       COALESCE(SUM(i.amount_paid), 0)       AS terkumpul
						FROM payment_plans p
						LEFT JOIN installments i ON i.payment_plan_id = p.id
						WHERE p.status <> 'CANCELLED'
						GROUP BY p.category
						ORDER BY p.category
						""")
				.query((rs, n) -> new DashboardController.KategoriRingkas(
						rs.getString("kategori"),
						rs.getLong("jumlah"),
						rs.getBigDecimal("tertagih"),
						rs.getBigDecimal("terkumpul")))
				.list();
	}

	public List<DashboardController.KelasRingkas> summaryByClass() {
		return jdbc.sql("""
						SELECT COALESCE(c.name, 'Tanpa kelas')       AS kelas,
						       COUNT(DISTINCT s.id)                  AS jumlah_mahasiswa,
						       COALESCE(SUM(i.amount), 0)            AS tertagih,
						       COALESCE(SUM(i.amount_paid), 0)       AS terkumpul
						FROM students s
						LEFT JOIN study_classes c ON c.id = s.study_class_id
						LEFT JOIN payment_plans p ON p.student_id = s.id AND p.status <> 'CANCELLED'
						LEFT JOIN installments i ON i.payment_plan_id = p.id
						WHERE s.active
						GROUP BY c.name
						ORDER BY kelas
						""")
				.query((rs, n) -> new DashboardController.KelasRingkas(
						rs.getString("kelas"),
						rs.getLong("jumlah_mahasiswa"),
						rs.getBigDecimal("tertagih"),
						rs.getBigDecimal("terkumpul")))
				.list();
	}

	/** Bukti bayar terbaru yang belum diputuskan, untuk kartu antrean. */
	public List<DashboardController.AntreanItem> verificationQueue() {
		return jdbc.sql("""
						SELECT pay.id,
						       s.name                     AS nama,
						       s.nim,
						       pl.category::text          AS kategori,
						       pay.amount,
						       pay.status::text           AS status,
						       pay.ocr_confidence,
						       pay.created_at
						FROM payments pay
						JOIN students s ON s.id = pay.student_id
						LEFT JOIN payment_plans pl ON pl.id = pay.payment_plan_id
						WHERE pay.status IN ('PENDING', 'NEEDS_REVIEW', 'FAILED')
						ORDER BY pay.created_at DESC
						LIMIT 8
						""")
				.query((rs, n) -> new DashboardController.AntreanItem(
						rs.getLong("id"),
						rs.getString("nama"),
						rs.getString("nim"),
						rs.getString("kategori"),
						rs.getBigDecimal("amount"),
						rs.getString("status"),
						rs.getBigDecimal("ocr_confidence"),
						toInstant(rs.getTimestamp("created_at"))))
				.list();
	}

	private static java.time.Instant toInstant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}
}
