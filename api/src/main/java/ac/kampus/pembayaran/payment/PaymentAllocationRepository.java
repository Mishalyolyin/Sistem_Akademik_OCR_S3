package ac.kampus.pembayaran.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

	List<PaymentAllocation> findByPaymentIdOrderByIdAsc(Long paymentId);

	/** Rincian yang masih berlaku, yaitu yang belum dibatalkan. */
	List<PaymentAllocation> findByPaymentIdAndReversedAtIsNullOrderByIdAsc(Long paymentId);
}
