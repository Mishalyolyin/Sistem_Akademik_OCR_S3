package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.student.StudentService;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lapisan HTTP tagihan.
 *
 * <p>Aturan hitungannya sudah dikunci di test satuan. Yang tersisa di sini
 * adalah saringan masukan: nilai yang tidak masuk akal harus tertahan di pintu
 * dengan jawaban yang bisa dibaca, bukan lolos ke service lalu meledak jadi 500.
 */
@ControllerTest(BillingController.class)
class BillingControllerTest extends ControllerTestSupport {

	@MockitoBean
	private PaymentGenerationService generationService;
	@MockitoBean
	private InstallmentBillingService billingService;
	@MockitoBean
	private PaymentPlanRepository planRepository;
	@MockitoBean
	private InstallmentAmountChangeRepository changeRepository;
	@MockitoBean
	private StudentService studentService;

	private static Map<String, Object> permintaanPlan() {
		Map<String, Object> body = new HashMap<>();
		body.put("category", "UKT");
		body.put("academicYear", "2026/2027");
		body.put("term", "GASAL");
		return body;
	}

	// --- Buat tagihan ---

	@Test
	@DisplayName("kategori yang tidak dikenal ditolak 400 beserta daftar pilihannya")
	void kategoriTidakDikenal() throws Exception {
		Map<String, Object> body = permintaanPlan();
		body.put("category", "SULTAN");

		mockMvc.perform(sebagaiAdmin(post("/students/1/plans"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("UKT")));

		verify(generationService, never()).generate(any(), any(), anyString(), any());
	}

	@Test
	@DisplayName("format tahun akademik yang salah ditolak 400")
	void tahunAkademikSalah() throws Exception {
		Map<String, Object> body = permintaanPlan();
		body.put("academicYear", "2026");

		mockMvc.perform(sebagaiAdmin(post("/students/1/plans"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("2026/2027")));
	}

	@Test
	@DisplayName("kategori tidak diisi ditolak 400")
	void kategoriKosong() throws Exception {
		Map<String, Object> body = permintaanPlan();
		body.remove("category");

		mockMvc.perform(sebagaiAdmin(post("/students/1/plans"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(body)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("aturan bisnis dari service diteruskan sebagai 409, bukan 500")
	void aturanBisnisJadi409() throws Exception {
		when(studentService.get(anyLong())).thenThrow(
				new BusinessRuleException("Tagihan UKT ke-7 tidak diperbolehkan."));

		mockMvc.perform(sebagaiAdmin(post("/students/1/plans"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(permintaanPlan())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Tagihan UKT ke-7 tidak diperbolehkan."));
	}

	// --- Ubah nominal cicilan ---

	@Test
	@DisplayName("nominal nol atau negatif ditolak 400")
	void nominalTidakMasukAkal() throws Exception {
		for (int nominal : new int[] { 0, -5000 }) {
			mockMvc.perform(sebagaiAdmin(patch("/installments/10/amount"))
							.contentType(MediaType.APPLICATION_JSON)
							.content(json(Map.of("amount", nominal, "reason", "koreksi tarif"))))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.detail", containsString("lebih besar dari nol")));
		}

		verify(billingService, never()).updateAmount(anyLong(), any(), anyString(), anyLong());
	}

	@Test
	@DisplayName("alasan wajib diisi, tidak boleh kurang dari lima karakter")
	void alasanWajib() throws Exception {
		mockMvc.perform(sebagaiAdmin(patch("/installments/10/amount"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", 500000, "reason", "ok"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("minimal 5 karakter")));

		verify(billingService, never()).updateAmount(anyLong(), any(), anyString(), anyLong());
	}

	@Test
	@DisplayName("id admin diambil dari token, bukan dari badan permintaan")
	void adminDariToken() throws Exception {
		when(billingService.updateAmount(anyLong(), any(), anyString(), anyLong()))
				.thenReturn(InstallmentAmountChange.builder()
						.id(1L).installmentId(10L)
						.oldAmount(new BigDecimal("1000000"))
						.newAmount(new BigDecimal("500000"))
						.reason("koreksi tarif").adminId(ID_ADMIN)
						.build());

		mockMvc.perform(sebagaiAdmin(patch("/installments/10/amount"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", 500000, "reason", "koreksi tarif",
								"adminId", 999))))
				.andExpect(status().isOk());

		verify(billingService).updateAmount(
				org.mockito.ArgumentMatchers.eq(10L), any(), anyString(),
				org.mockito.ArgumentMatchers.eq(ID_ADMIN));
	}

	// --- Peran ---

	@Test
	@DisplayName("mahasiswa tidak boleh membuat tagihan untuk dirinya lewat jalur admin")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/students/1/plans"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(permintaanPlan())))
				.andExpect(status().isForbidden());

		verify(generationService, never()).generate(any(), any(), anyString(), any());
	}

	@Test
	@DisplayName("mahasiswa tidak boleh mengubah nominal cicilan")
	void mahasiswaTidakBolehUbahNominal() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(patch("/installments/10/amount"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", 1, "reason", "biar murah"))))
				.andExpect(status().isForbidden());

		verify(billingService, never()).updateAmount(anyLong(), any(), anyString(), anyLong());
	}
}
