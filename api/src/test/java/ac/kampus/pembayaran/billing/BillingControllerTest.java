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
	private PlanCancellationService cancellationService;
	@MockitoBean
	private UktAutoService uktAutoService;
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
	@DisplayName("mahasiswa tidak boleh mengubah nominal cicilan")
	void mahasiswaTidakBolehUbahNominal() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(patch("/installments/10/amount"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", 1, "reason", "biar murah"))))
				.andExpect(status().isForbidden());

		verify(billingService, never()).updateAmount(anyLong(), any(), anyString(), anyLong());
	}

	@Test
	@DisplayName("admin bisa menjalankan putaran tagihan UKT sekarang")
	void jalankanUktOtomatis() throws Exception {
		when(uktAutoService.jalankan(any())).thenReturn(
				new UktAutoService.Hasil(30, 12, 18, 0, java.util.List.of()));

		mockMvc.perform(sebagaiAdmin(post("/tagihan-ukt/jalankan")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diperiksa").value(30))
				.andExpect(jsonPath("$.dibuat").value(12))
				.andExpect(jsonPath("$.dilewati").value(18));
	}

	@Test
	@DisplayName("kegagalan per mahasiswa ikut terkirim beserta sebabnya")
	void kegagalanIkutTerkirim() throws Exception {
		when(uktAutoService.jalankan(any())).thenReturn(new UktAutoService.Hasil(
				2, 1, 0, 1,
				java.util.List.of("Gagal (D9600007) semester 1: Tarif UKT belum diatur.")));

		mockMvc.perform(sebagaiAdmin(post("/tagihan-ukt/jalankan")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gagal").value(1))
				.andExpect(jsonPath("$.catatan[0]",
						org.hamcrest.Matchers.containsString("Tarif UKT belum diatur")));
	}

	@Test
	@DisplayName("mahasiswa tidak boleh menjalankan putaran tagihan")
	void mahasiswaTidakBolehMenjalankan() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/tagihan-ukt/jalankan")))
				.andExpect(status().isForbidden());

		verify(uktAutoService, never()).jalankan(any());
	}

	@Test
	@DisplayName("tidak ada lagi jalur membuat tagihan per mahasiswa")
	void jalurManualSudahTidakAda() throws Exception {
		// Tagihan UKT kini hanya lahir dari satu tempat. Route POST-nya sengaja
		// dihapus, bukan sekadar disembunyikan dari layar. Jawabannya 405 dan
		// bukan 404 karena alamat yang sama masih melayani GET — daftar tagihan
		// satu mahasiswa tetap bisa dibaca.
		mockMvc.perform(sebagaiAdmin(post("/students/1/plans"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(java.util.Map.of(
								"category", "UKT",
								"academicYear", "2026/2027",
								"term", "GASAL"))))
				.andExpect(status().isMethodNotAllowed());
	}
}
