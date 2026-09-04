package ac.kampus.pembayaran.adjustment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjustmentRepository extends JpaRepository<Adjustment, Long> {

	List<Adjustment> findByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);

	/** Dipakai sebelum menghapus mahasiswa. */
	boolean existsByStudentId(Long studentId);
}
