package ac.kampus.pembayaran.payment.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Klien ke service OCR (FastAPI).
 *
 * <p>Menggantikan pemanggilan subprocess Python di sistem Laravel lama. Service
 * OCR hanya membaca berkas dan mengembalikan hasilnya; keputusan diterima atau
 * ditolak tetap diambil di sini, di {@link OcrJobConsumer}.
 */
@Slf4j
@Component
public class OcrClient {

	private final RestClient client;

	public OcrClient(
			@Value("${app.ocr.base-url:http://localhost:8000}") String baseUrl,
			@Value("${app.ocr.timeout-seconds:60}") long timeoutSeconds) {

		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

		this.client = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(factory)
				.build();

		log.info("Service OCR: {} (batas waktu baca {} detik)", baseUrl, timeoutSeconds);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> read(Path file, OcrRequest request) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("file", new FileSystemResource(file));
		form.add("doc_type", request.docType());

		if (request.accounts() != null) form.add("accounts", request.accounts());
		if (request.blacklist() != null) form.add("blacklist", request.blacklist());
		if (request.maxDays() != null) form.add("max_days", request.maxDays());
		if (request.studentName() != null) form.add("student_name", request.studentName());

		return client.post()
				.uri("/ocr")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(form)
				.retrieve()
				.body(Map.class);
	}

	public boolean isHealthy() {
		try {
			Map<?, ?> body = client.get().uri("/health").retrieve().body(Map.class);
			return body != null && "ok".equals(body.get("status"));
		} catch (Exception e) {
			log.warn("Service OCR tidak merespons: {}", e.getMessage());
			return false;
		}
	}

	public record OcrRequest(
			String docType,
			String accounts,
			String blacklist,
			Integer maxDays,
			String studentName
	) {
		public static OcrRequest forPayment(
				String accounts, String blacklist, Integer maxDays, String studentName) {
			return new OcrRequest("payment", accounts, blacklist, maxDays, studentName);
		}

		/**
		 * Dokumen wajib mahasiswa. Rekening tujuan, blacklist, dan batas umur
		 * tanggal tidak dikirim: ketiganya hanya berlaku untuk bukti transfer.
		 * Nama mahasiswa tetap dikirim karena itulah yang dicocokkan dengan nama
		 * di ijazah dan dengan daftar anggota di Kartu Keluarga.
		 */
		public static OcrRequest forDocument(String docType, String studentName) {
			return new OcrRequest(docType, null, null, null, studentName);
		}
	}
}
