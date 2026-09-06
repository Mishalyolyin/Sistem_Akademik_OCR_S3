package ac.kampus.pembayaran.payment.ocr;

import ac.kampus.pembayaran.student.StudentDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrJobPublisher {

	private final RabbitTemplate rabbitTemplate;

	/**
	 * Pesan baru dikirim SETELAH transaksi berhasil di-commit. Kalau dikirim di
	 * tengah transaksi, pekerja bisa membaca database sebelum barisnya tersimpan
	 * dan mengira pembayarannya tidak ada.
	 */
	public void publishAfterCommit(Long paymentId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							publish(paymentId);
						}
					});
		} else {
			publish(paymentId);
		}
	}

	/** Sama seperti di atas, untuk dokumen wajib mahasiswa. */
	public void publishDocumentAfterCommit(Long studentId, StudentDocument jenis) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							publishDocument(studentId, jenis);
						}
					});
		} else {
			publishDocument(studentId, jenis);
		}
	}

	private void publishDocument(Long studentId, StudentDocument jenis) {
		rabbitTemplate.convertAndSend(
				RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY_DOKUMEN,
				new DocumentOcrJobMessage(studentId, jenis));
		log.info("Pembacaan {} mahasiswa {} masuk antrean", jenis, studentId);
	}

	private void publish(Long paymentId) {
		rabbitTemplate.convertAndSend(
				RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, new OcrJobMessage(paymentId));
		log.info("Pekerjaan OCR untuk pembayaran {} masuk antrean", paymentId);
	}
}
