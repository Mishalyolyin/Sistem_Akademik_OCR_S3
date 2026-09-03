package ac.kampus.pembayaran.adjustment;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lapisan HTTP penyesuaian.
 *
 * <p>Yang diuji di sini bukan aturan uangnya — itu sudah dikunci
 * {@link AdjustmentServiceTest} — melainkan hal-hal yang hanya ada di lapisan
 * ini: siapa yang boleh memanggil, ke mana permintaan diarahkan, dari mana id
 * admin diambil, dan bagaimana penolakan sampai ke layar.
 */
@ControllerTest(AdjustmentController.class)
class AdjustmentControllerTest extends ControllerTestSupport {

	private static final String URL = "/students/1/adjustments";

	@MockitoBean
	private AdjustmentService service;

	private static Adjustment contoh(Long installmentId) {
		return Adjustment.builder()
				.id(50L).studentId(1L).installmentId(installmentId)
				.amount(new BigDecimal("200000")).balanceAfter(new BigDecimal("700000"))
				.reason("kelebihan bayar cicilan 1").adminId(ID_ADMIN)
				.createdAt(Instant.parse("2026-09-03T10:00:00Z"))
				.build();
	}

	// --- Ke mana permintaan diarahkan ---

	@Test
	@DisplayName("tanpa installmentId diarahkan ke penyesuaian saldo")
	void tanpaCicilanKeSaldo() throws Exception {
		when(service.sesuaikanSaldo(anyLong(), any(), anyString(), anyLong()))
				.thenReturn(contoh(null));

		mockMvc.perform(sebagaiAdmin(post(URL))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", 200000, "reason", "kelebihan bayar"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.target").value("SALDO"))
				.andExpect(jsonPath("$.balanceAfter").value("700000"));

		verify(service, never())
				.sesuaikanCicilan(anyLong(), anyLong(), any(), anyString(), anyLong());
	}

	@Test
	@DisplayName("dengan installmentId diarahkan ke penyesuaian cicilan")
	void denganCicilan() throws Exception {
		when(service.sesuaikanCicilan(anyLong(), anyLong(), any(), anyString(), anyLong()))
				.thenReturn(contoh(10L));

		mockMvc.perform(sebagaiAdmin(post(URL))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("installmentId", 10, "amount", 200000,
								"reason", "transfer manual"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.target").value("CICILAN"))
				.andExpect(jsonPath("$.installmentId").value(10));

		verify(service, never()).sesuaikanSaldo(anyLong(), any(), anyString(), anyLong());
	}

	@Test
	@DisplayName("id mahasiswa diambil dari URL, id admin dari token")
	void identitasTidakDatangDariBadanPermintaan() throws Exception {
		when(service.sesuaikanSaldo(anyLong(), any(), anyString(), anyLong()))
				.thenReturn(contoh(null));

		// Badan permintaan sengaja menyelipkan studentId dan adminId lain.
		mockMvc.perform(sebagaiAdmin(post("/students/1/adjustments"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", 1000, "reason", "coba curang",
								"studentId", 999, "adminId", 999))))
				.andExpect(status().isCreated());

		verify(service).sesuaikanSaldo(eq(1L), any(), anyString(), eq(ID_ADMIN));
	}

	// --- Validasi masukan ---

	@Test
	@DisplayName("alasan terlalu pendek ditolak 400 dengan keterangan yang bisa dibaca")
	void alasanPendek() throws Exception {
		mockMvc.perform(sebagaiAdmin(post(URL))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", 1000, "reason", "ok"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("minimal 5 karakter")));

		verify(service, never()).sesuaikanSaldo(anyLong(), any(), anyString(), anyLong());
	}

	@Test
	@DisplayName("nominal tidak diisi ditolak 400")
	void nominalKosong() throws Exception {
		mockMvc.perform(sebagaiAdmin(post(URL))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("reason", "alasan cukup panjang"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").isNotEmpty());

		verify(service, never()).sesuaikanSaldo(anyLong(), any(), anyString(), anyLong());
	}

	@Test
	@DisplayName("badan permintaan bukan JSON yang benar ditolak 400, bukan 500")
	void badanRusak() throws Exception {
		mockMvc.perform(sebagaiAdmin(post(URL))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{bukan json"))
				.andExpect(status().isBadRequest());
	}

	// --- Penolakan aturan bisnis sampai ke klien ---

	@Test
	@DisplayName("aturan bisnis yang dilanggar dijawab 409 beserta alasannya")
	void aturanBisnisJadi409() throws Exception {
		when(service.sesuaikanSaldo(anyLong(), any(), anyString(), anyLong()))
				.thenThrow(new BusinessRuleException("Saldo tidak boleh minus."));

		mockMvc.perform(sebagaiAdmin(post(URL))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", -900000, "reason", "salah hitung"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Saldo tidak boleh minus."));
	}

	@Test
	@DisplayName("mahasiswa tidak ditemukan dijawab 404")
	void tidakDitemukan() throws Exception {
		when(service.riwayat(anyLong())).thenThrow(NotFoundException.of("Mahasiswa", 1L));

		mockMvc.perform(sebagaiAdmin(get(URL)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	// --- Siapa yang boleh masuk ---

	@Test
	@DisplayName("mahasiswa tidak boleh membuat penyesuaian untuk dirinya sendiri")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post(URL))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", 1000000, "reason", "menambah saldo sendiri"))))
				.andExpect(status().isForbidden());

		verify(service, never()).sesuaikanSaldo(anyLong(), any(), anyString(), anyLong());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
	}

	// --- Riwayat ---

	@Test
	@DisplayName("riwayat dikembalikan beserta penanda sasarannya")
	void riwayat() throws Exception {
		when(service.riwayat(1L)).thenReturn(List.of(contoh(null), contoh(10L)));

		mockMvc.perform(sebagaiAdmin(get(URL)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].target").value("SALDO"))
				.andExpect(jsonPath("$[1].target").value("CICILAN"));
	}
}
