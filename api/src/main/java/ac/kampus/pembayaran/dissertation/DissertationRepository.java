package ac.kampus.pembayaran.dissertation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DissertationRepository extends JpaRepository<DissertationDetail, Long> {

	boolean existsByStudentId(Long studentId);
}
