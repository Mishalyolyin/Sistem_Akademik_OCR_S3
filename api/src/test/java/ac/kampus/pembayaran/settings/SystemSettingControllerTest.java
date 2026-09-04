package ac.kampus.pembayaran.settings;

import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pengaturan sistem.
 *
 * <p>Isinya menentukan keputusan otomatis atas bukti bayar — ambang keyakinan
 * OCR, toleransi selisih, dan rekening tujuan — jadi yang dijaga di sini
 * terutama batas perannya: nilai-nilai ini tidak boleh bisa disentuh siapa pun
 * selain admin.
 */
@ControllerTest(SystemSettingController.class)
class SystemSettingControllerTest extends ControllerTestSupport {

	@MockitoBean
	private SystemSettingService service;

	private static SystemSetting setelan(String key, String value) {
		return new SystemSetting(key, value, "Keterangan " + key, null);
	}

	@Test
	@DisplayName("daftar pengaturan lengkap dengan keterangannya")
	void daftar() throws Exception {
		when(service.all()).thenReturn(List.of(
				setelan("ocr_confidence_threshold", "80"),
				setelan("bank_account_number", "1234567890")));

		mockMvc.perform(sebagaiAdmin(get("/settings")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].key").value("ocr_confidence_threshold"))
				.andExpect(jsonPath("$[0].value").value("80"))
				.andExpect(jsonPath("$[0].description").value("Keterangan ocr_confidence_threshold"));
	}

	@Test
	@DisplayName("nilai tersimpan dan dikembalikan apa adanya")
	void ubahNilai() throws Exception {
		when(service.set("ocr_confidence_threshold", "85"))
				.thenReturn(setelan("ocr_confidence_threshold", "85"));

		mockMvc.perform(sebagaiAdmin(put("/settings/ocr_confidence_threshold"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("value", "85"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.value").value("85"));
	}

	@Test
	@DisplayName("nilai kosong diterima, karena mengosongkan pengaturan itu sah")
	void nilaiKosongBoleh() throws Exception {
		when(service.set(anyString(), any())).thenReturn(setelan("blacklist_keywords", ""));

		mockMvc.perform(sebagaiAdmin(put("/settings/blacklist_keywords"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("value", ""))))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("nilai yang kepanjangan ditolak 400 sebelum tersimpan")
	void nilaiTerlaluPanjang() throws Exception {
		mockMvc.perform(sebagaiAdmin(put("/settings/blacklist_keywords"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("value", "x".repeat(2001)))))
				.andExpect(status().isBadRequest());

		verify(service, never()).set(anyString(), any());
	}

	@Test
	@DisplayName("mahasiswa tidak boleh membaca maupun mengubah pengaturan")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/settings")))
				.andExpect(status().isForbidden());

		mockMvc.perform(sebagaiMahasiswa(put("/settings/ocr_confidence_threshold"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("value", "0"))))
				.andExpect(status().isForbidden());

		verify(service, never()).set(anyString(), any());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get("/settings"))
				.andExpect(status().isUnauthorized());
	}
}
