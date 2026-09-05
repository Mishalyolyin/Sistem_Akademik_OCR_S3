package ac.kampus.pembayaran.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

	/**
	 * Kunci baris cicilan selama transaksi berjalan. Wajib dipakai sebelum
	 * mengubah nominal atau mengalokasikan pembayaran, supaya dua permintaan
	 * bersamaan tidak saling menimpa.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM Installment i WHERE i.id = :id")
	Optional<Installment> findByIdForUpdate(@Param("id") Long id);

	@Query("SELECT COALESCE(SUM(i.amount), 0) FROM Installment i WHERE i.paymentPlan.id = :planId")
	BigDecimal sumAmountByPlan(@Param("planId") Long planId);

	/**
	 * Cicilan yang perlu diingatkan pada rentang tanggal tertentu.
	 *
	 * <p>Mahasiswa dan tagihannya ikut diambil sekaligus: penjadwal membaca nama,
	 * telepon, dan kategori tiap barisnya, dan tanpa ini satu pengingat berubah
	 * jadi tiga kueri tambahan per mahasiswa.
	 *
	 * <p>Status lunas dikirim sebagai parameter, bukan ditulis sebagai literal
	 * enum di JPQL — literal membuat Hibernate mengecast ke nama kelas Java
	 * sementara tipe PostgreSQL-nya bernama lain, dan kueri gagal saat berjalan.
	 */
	@Query("""
			SELECT i FROM Installment i
			JOIN FETCH i.paymentPlan p
			JOIN FETCH p.student s
			WHERE i.dueDate BETWEEN :dari AND :sampai
			  AND i.status <> :lunas
			  AND s.active = true
			ORDER BY i.dueDate ASC, i.id ASC
			""")
	List<Installment> findPerluDiingatkan(
			@Param("dari") LocalDate dari,
			@Param("sampai") LocalDate sampai,
			@Param("lunas") InstallmentStatus lunas);
}
