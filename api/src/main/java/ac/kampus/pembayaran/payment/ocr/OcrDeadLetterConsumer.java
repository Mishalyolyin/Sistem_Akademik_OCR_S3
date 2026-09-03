package ac.kampus.pembayaran.payment.ocr;

import ac.kampus.pembayaran.payment.Payment;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.payment.PaymentStatus;
import ac.kampus.pembayaran.payment.VerificationLog;
import ac.kampus.pembayaran.payment.VerificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Menangani pekerjaan OCR yang gagal sampai percobaan ulangnya habis.
 *
 * <p>Tanpa ini, bukti bayar akan menggantung di status PENDING selamanya kalau
 * service OCR mati — dan tidak ada seorang pun yang tahu. Di sini statusnya
 * ditandai FAILED supaya muncul di daftar admin dan bisa dibaca ulang lewat
 * tombol "baca ulang".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrDeadLetterConsumer {

	private final PaymentRepository paymentRepository;
	private final VerificationLogRepository logRepository;

	@RabbitListener(queues = RabbitConfig.DLQ)
	@Transactional
	public void handle(OcrJobMessage message) {
		Long paymentId = message.paymentId();
		log.error("Pembacaan OCR pembayaran {} gagal setelah semua percobaan ulang.", paymentId);

		Payment payment = paymentRepository.findById(paymentId).orElse(null);
		if (payment == null) {
			return;
		}

		// Jangan timpa keputusan yang sudah diambil admin selagi pekerjaan gagal.
		if (!payment.getStatus().bolehDitimpaOcr()) {
			log.info("Pembayaran {} sudah berstatus {}, tidak diubah.", paymentId, payment.getStatus());
			return;
		}

		PaymentStatus statusLama = payment.getStatus();
		payment.setStatus(PaymentStatus.FAILED);
		paymentRepository.save(payment);

		logRepository.save(VerificationLog.builder()
				.paymentId(paymentId)
				.fromStatus(statusLama)
				.toStatus(PaymentStatus.FAILED)
				.note("Pembacaan OCR gagal berulang kali. Periksa service OCR, lalu baca ulang.")
				.build());
	}
}
