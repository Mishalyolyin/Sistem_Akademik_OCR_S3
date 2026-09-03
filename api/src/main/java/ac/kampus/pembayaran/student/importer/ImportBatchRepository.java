package ac.kampus.pembayaran.student.importer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

	List<ImportBatch> findTop20ByOrderByCreatedAtDesc();
}
