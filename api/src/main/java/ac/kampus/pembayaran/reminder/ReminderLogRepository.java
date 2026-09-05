package ac.kampus.pembayaran.reminder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

	/**
	 * Sudah pernah benar-benar terkirim? Yang gagal sengaja tidak dihitung,
	 * supaya kirimannya dicoba lagi keesokan harinya.
	 */
	boolean existsByInstallmentIdAndKindAndStatus(
			Long installmentId, ReminderKind kind, ReminderStatus status);

	List<ReminderLog> findTop50ByOrderByCreatedAtDesc();
}
