package ac.kampus.pembayaran.forensic;

import ac.kampus.pembayaran.payment.Payment;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.payment.PaymentStatus;
import ac.kampus.pembayaran.payment.VerificationLog;
import ac.kampus.pembayaran.payment.VerificationLogRepository;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import ac.kampus.pembayaran.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Forensik OCR.
 *
 * <p>Ini satu-satunya endpoint yang benar-benar milik peran DEVELOPER — sebelum
 * ini ia dikecualikan dari seluruh endpoint admin dan tidak punya apa pun selain
 * membuka gambar bukti. Jadi yang dijaga di sini: DEVELOPER benar-benar masuk,
 * mahasiswa tetap tidak, dan hasil pembacaan mentahnya sampai apa adanya —
 * merapikannya justru menghilangkan yang dicari saat menelusuri.
 */
@ControllerTest(ForensicController.class)
class ForensicControllerTest extends ControllerTestSupport {

	@MockitoBean
	private PaymentRepository paymentRepository;
	@MockitoBean
	private VerificationLogRepository logRepository;

	/** Token peran DEVELOPER; belum ada pembantunya di ControllerTestSupport. */
	private MockHttpServletRequestBuilder sebagaiDeveloper(
			MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION,
				"Bearer " + token(UserRole.DEVELOPER, 11L));
	}

	@BeforeEach
	void setUp() {
		// Tipe disebut jelas: PaymentRepository mewarisi JpaSpecificationExecutor
		// sekaligus mendeklarasikan findAll sendiri, jadi any() saja ambigu.
		when(paymentRepository.findAll(
				ArgumentMatchers.<Specification<Payment>>any(), any(Pageable.class)))
				.thenReturn(halaman(pembayaran()));
		when(paymentRepository.findWithDetailsById(7L)).thenReturn(Optional.of(pembayaran()));
		when(logRepository.findByPaymentIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(
				log(null, PaymentStatus.PENDING, PaymentStatus.NEEDS_REVIEW,
						"Selisih nominal: diklaim 1000000, terbaca 1200000"),
				log(9L, PaymentStatus.NEEDS_REVIEW, PaymentStatus.VERIFIED, "Dicek manual")));
	}

	private static Page<Payment> halaman(Payment... isi) {
		return new PageImpl<>(List.of(isi));
	}

	private static VerificationLog log(Long adminId, PaymentStatus dari, PaymentStatus ke,
			String catatan) {
		return VerificationLog.builder()
				.id(1L).paymentId(7L).adminId(adminId)
				.fromStatus(dari).toStatus(ke).note(catatan)
				.createdAt(Instant.parse("2026-09-04T02:00:00Z"))
				.build();
	}

	private static Payment pembayaran() {
		Map<String, Object> ocr = new LinkedHashMap<>();
		ocr.put("confidence", 0.72);
		ocr.put("amount", 1_200_000);
		ocr.put("bank_name", "BSI");
		// Field yang tidak dikenal kode mana pun; justru ini yang dicari saat
		// menelusuri kenapa keputusannya meleset.
		ocr.put("raw_text", "TRANSFER BERHASIL Rp1.200.000");
		ocr.put("flags", List.of("Selisih nominal: diklaim 1000000, terbaca 1200000"));

		Payment payment = new Payment();
		payment.setId(7L);
		payment.setStudent(Student.builder()
				.id(1L).nim("2612600001").name("Uji Coba")
				.discountTier("NON_ALUMNI").walletBalance(BigDecimal.ZERO).active(true)
				.build());
		payment.setAmount(new BigDecimal("1000000"));
		payment.setStatus(PaymentStatus.NEEDS_REVIEW);
		payment.setOcrConfidence(new BigDecimal("0.7200"));
		payment.setOcrData(ocr);
		payment.setCreatedAt(Instant.parse("2026-09-04T01:00:00Z"));
		return payment;
	}

	// --- Peran ---

	@Test
	@DisplayName("developer boleh masuk; ini satu-satunya halaman miliknya")
	void developerBolehMasuk() throws Exception {
		mockMvc.perform(sebagaiDeveloper(get("/forensik/pembayaran")))
				.andExpect(status().isOk());

		mockMvc.perform(sebagaiDeveloper(get("/forensik/pembayaran/7")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("admin juga boleh, karena ia yang menanggung keputusannya")
	void adminBolehMasuk() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/forensik/pembayaran")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("mahasiswa tidak boleh melihat pembacaan bukti siapa pun")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/forensik/pembayaran")))
				.andExpect(status().isForbidden());

		mockMvc.perform(sebagaiMahasiswa(get("/forensik/pembayaran/7")))
				.andExpect(status().isForbidden());

		verify(paymentRepository, never()).findWithDetailsById(any());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get("/forensik/pembayaran"))
				.andExpect(status().isUnauthorized());
	}

	// --- Isi ---

	@Test
	@DisplayName("hasil OCR sampai apa adanya, termasuk field yang tidak dikenal kode")
	void ocrMentahUtuh() throws Exception {
		mockMvc.perform(sebagaiDeveloper(get("/forensik/pembayaran/7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ocrMentah.confidence").value(0.72))
				.andExpect(jsonPath("$.ocrMentah.bank_name").value("BSI"))
				// Tidak ada di DTO mana pun, tetap harus terkirim.
				.andExpect(jsonPath("$.ocrMentah.raw_text",
						containsString("TRANSFER BERHASIL")));
	}

	@Test
	@DisplayName("catatan mesin diangkat jadi daftar sendiri, tidak terkubur di dalam hasil OCR")
	void catatanTerangkat() throws Exception {
		mockMvc.perform(sebagaiDeveloper(get("/forensik/pembayaran/7")))
				.andExpect(jsonPath("$.catatan[0]", containsString("Selisih nominal")))
				.andExpect(jsonPath("$.nominalDiklaim").value(1000000))
				// Pecahan, bukan persen: yang mengubahnya jadi 72% adalah UI.
				.andExpect(jsonPath("$.keyakinan").value(0.72));
	}

	@Test
	@DisplayName("riwayat membedakan keputusan mesin dari keputusan admin")
	void riwayatMembedakanPengambilKeputusan() throws Exception {
		mockMvc.perform(sebagaiDeveloper(get("/forensik/pembayaran/7")))
				.andExpect(jsonPath("$.riwayat[0].otomatis").value(true))
				.andExpect(jsonPath("$.riwayat[0].adminId").doesNotExist())
				.andExpect(jsonPath("$.riwayat[1].otomatis").value(false))
				.andExpect(jsonPath("$.riwayat[1].adminId").value(9));
	}

	@Test
	@DisplayName("daftar menyebut ada tidaknya hasil OCR dan jumlah catatannya")
	void daftarRingkas() throws Exception {
		mockMvc.perform(sebagaiDeveloper(get("/forensik/pembayaran")))
				.andExpect(jsonPath("$.content[0].paymentId").value(7))
				.andExpect(jsonPath("$.content[0].adaHasilOcr").value(true))
				.andExpect(jsonPath("$.content[0].jumlahCatatan").value(1))
				.andExpect(jsonPath("$.content[0].nim").value("2612600001"));
	}

	@Test
	@DisplayName("pembayaran yang tidak ada dijawab 404")
	void tidakAda() throws Exception {
		when(paymentRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

		mockMvc.perform(sebagaiDeveloper(get("/forensik/pembayaran/99")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("status ngawur ditolak 400 beserta daftar pilihannya, bukan 500")
	void statusNgawur() throws Exception {
		mockMvc.perform(sebagaiDeveloper(get("/forensik/pembayaran").param("status", "SULTAN")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("NEEDS_REVIEW")));
	}
}
