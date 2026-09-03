package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lapisan HTTP verifikasi pembayaran.
 *
 * <p>Keputusan di sini memindahkan uang ke cicilan, jadi yang dijaga bukan
 * cuma peran: keputusan wajib eksplisit diterima atau ditolak, dan id admin
 * yang tercatat di jejak audit harus datang dari token — bukan dari angka yang
 * bisa diketik siapa pun di badan permintaan.
 */
@ControllerTest(PaymentController.class)
class PaymentControllerTest extends ControllerTestSupport {

	@MockitoBean
	private PaymentService service;
	@MockitoBean
	private PaymentRepository repository;
	@MockitoBean
	private VerificationLogRepository logRepository;

	private static Payment contoh() {
		return Payment.builder()
				.id(7L)
				.student(Student.builder().id(1L).nim("2612600001").name("Uji Coba").build())
				.amount(new BigDecimal("1200000"))
				.proofFilePath("bukti.png")
				.status(PaymentStatus.VERIFIED)
				.verifiedAt(Instant.parse("2026-09-03T10:00:00Z"))
				.build();
	}

	// --- Keputusan manual ---

	@Test
	@DisplayName("keputusan wajib eksplisit; tanpa approve ditolak 400")
	void keputusanWajib() throws Exception {
		mockMvc.perform(sebagaiAdmin(post("/payments/7/decide"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("note", "kelihatan benar"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").isNotEmpty());

		verify(service, never()).decide(anyLong(), anyBoolean(), any(), anyLong());
	}

	@Test
	@DisplayName("id admin yang tercatat diambil dari token, bukan dari badan permintaan")
	void adminDariToken() throws Exception {
		when(service.decide(anyLong(), anyBoolean(), any(), anyLong())).thenReturn(contoh());

		Map<String, Object> body = new HashMap<>();
		body.put("approve", true);
		body.put("note", "bukti cocok");
		body.put("adminId", 999);

		mockMvc.perform(sebagaiAdmin(post("/payments/7/decide"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(body)))
				.andExpect(status().isOk());

		verify(service).decide(eq(7L), eq(true), eq("bukti cocok"), eq(ID_ADMIN));
	}

	@Test
	@DisplayName("penolakan tanpa alasan diteruskan sebagai 409 dari service")
	void penolakanTanpaAlasan() throws Exception {
		when(service.decide(anyLong(), anyBoolean(), any(), anyLong())).thenThrow(
				new BusinessRuleException("Alasan penolakan wajib diisi, minimal 5 karakter."));

		Map<String, Object> body = new HashMap<>();
		body.put("approve", false);

		mockMvc.perform(sebagaiAdmin(post("/payments/7/decide"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(body)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail")
						.value("Alasan penolakan wajib diisi, minimal 5 karakter."));
	}

	@Test
	@DisplayName("catatan yang kepanjangan ditolak 400")
	void catatanKepanjangan() throws Exception {
		mockMvc.perform(sebagaiAdmin(post("/payments/7/decide"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("approve", false, "note", "x".repeat(501)))))
				.andExpect(status().isBadRequest());

		verify(service, never()).decide(anyLong(), anyBoolean(), any(), anyLong());
	}

	// --- Baca ulang ---

	@Test
	@DisplayName("baca ulang dijawab 202, karena hasilnya belum ada saat itu juga")
	void bacaUlangDijawab202() throws Exception {
		mockMvc.perform(sebagaiAdmin(post("/payments/7/requeue")))
				.andExpect(status().isAccepted());

		verify(service).requeue(7L);
	}

	@Test
	@DisplayName("baca ulang pembayaran yang sudah diverifikasi dijawab 409")
	void bacaUlangYangSudahDiverifikasi() throws Exception {
		org.mockito.Mockito.doThrow(new BusinessRuleException(
						"Pembayaran ini sudah diverifikasi, tidak perlu dibaca ulang."))
				.when(service).requeue(anyLong());

		mockMvc.perform(sebagaiAdmin(post("/payments/7/requeue")))
				.andExpect(status().isConflict());
	}

	// --- Pengambilan data ---

	@Test
	@DisplayName("pembayaran yang tidak ada dijawab 404, bukan badan kosong")
	void tidakDitemukan() throws Exception {
		when(repository.findWithDetailsById(7L)).thenReturn(java.util.Optional.empty());

		mockMvc.perform(sebagaiAdmin(get("/payments/7")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	@DisplayName("status yang tidak dikenal pada penyaring dijawab 400 beserta pilihannya")
	void saringanStatusSalah() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/payments").param("status", "SULTAN")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail",
						org.hamcrest.Matchers.containsString("NEEDS_REVIEW")));
	}

	// --- Peran ---

	@Test
	@DisplayName("mahasiswa tidak boleh memverifikasi pembayaran")
	void mahasiswaTidakBolehMemutuskan() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/payments/7/decide"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("approve", true))))
				.andExpect(status().isForbidden());

		verify(service, never()).decide(anyLong(), anyBoolean(), any(), anyLong());
	}

	@Test
	@DisplayName("mahasiswa tidak boleh melihat daftar seluruh pembayaran")
	void mahasiswaTidakBolehMelihatSemua() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/payments")))
				.andExpect(status().isForbidden());
	}
}
