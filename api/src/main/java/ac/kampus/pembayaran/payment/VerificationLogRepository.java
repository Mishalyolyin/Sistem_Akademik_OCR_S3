package ac.kampus.pembayaran.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {

	List<VerificationLog> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);
}
