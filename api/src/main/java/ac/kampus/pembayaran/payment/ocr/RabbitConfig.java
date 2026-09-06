package ac.kampus.pembayaran.payment.ocr;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Antrean pekerjaan OCR. Padanan Laravel Queue, tapi memakai broker pesan
 * sungguhan karena itu pola yang lazim di lingkungan enterprise.
 *
 * <p>Pesan yang gagal diproses berulang kali tidak dibuang, melainkan dipindah
 * ke dead-letter queue agar bisa diperiksa manusia.
 */
@Configuration
public class RabbitConfig {

	public static final String EXCHANGE = "pembayaran.ocr";
	public static final String QUEUE = "pembayaran.ocr.jobs";
	public static final String ROUTING_KEY = "ocr.process";

	public static final String DLX = "pembayaran.ocr.dlx";
	public static final String DLQ = "pembayaran.ocr.jobs.dlq";

	/**
	 * Antrean terpisah untuk dokumen wajib mahasiswa.
	 *
	 * <p>Dipisah dari antrean bukti bayar karena keduanya punya kepentingan yang
	 * berbeda: bukti bayar menahan uang dan harus segera diputuskan, sementara
	 * pembacaan dokumen hanya membantu admin memeriksa. Menumpuknya di satu
	 * antrean membuat unggahan dokumen massal saat pendaftaran menunda
	 * pembacaan bukti bayar yang justru mendesak.
	 */
	public static final String QUEUE_DOKUMEN = "pembayaran.ocr.dokumen";
	public static final String ROUTING_KEY_DOKUMEN = "ocr.dokumen";
	public static final String DLQ_DOKUMEN = "pembayaran.ocr.dokumen.dlq";

	@Bean
	DirectExchange ocrExchange() {
		return new DirectExchange(EXCHANGE, true, false);
	}

	@Bean
	Queue ocrQueue() {
		return QueueBuilder.durable(QUEUE)
				.deadLetterExchange(DLX)
				.deadLetterRoutingKey(ROUTING_KEY)
				.build();
	}

	@Bean
	Binding ocrBinding() {
		return BindingBuilder.bind(ocrQueue()).to(ocrExchange()).with(ROUTING_KEY);
	}

	@Bean
	DirectExchange ocrDeadLetterExchange() {
		return new DirectExchange(DLX, true, false);
	}

	@Bean
	Queue ocrDeadLetterQueue() {
		return QueueBuilder.durable(DLQ).build();
	}

	@Bean
	Binding ocrDeadLetterBinding() {
		return BindingBuilder.bind(ocrDeadLetterQueue())
				.to(ocrDeadLetterExchange())
				.with(ROUTING_KEY);
	}

	@Bean
	Queue ocrDocumentQueue() {
		return QueueBuilder.durable(QUEUE_DOKUMEN)
				.deadLetterExchange(DLX)
				.deadLetterRoutingKey(ROUTING_KEY_DOKUMEN)
				.build();
	}

	@Bean
	Binding ocrDocumentBinding() {
		return BindingBuilder.bind(ocrDocumentQueue()).to(ocrExchange()).with(ROUTING_KEY_DOKUMEN);
	}

	@Bean
	Queue ocrDocumentDeadLetterQueue() {
		return QueueBuilder.durable(DLQ_DOKUMEN).build();
	}

	@Bean
	Binding ocrDocumentDeadLetterBinding() {
		return BindingBuilder.bind(ocrDocumentDeadLetterQueue())
				.to(ocrDeadLetterExchange())
				.with(ROUTING_KEY_DOKUMEN);
	}

	@Bean
	MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	RabbitTemplate rabbitTemplate(ConnectionFactory factory, MessageConverter converter) {
		RabbitTemplate template = new RabbitTemplate(factory);
		template.setMessageConverter(converter);
		return template;
	}
}
