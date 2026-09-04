package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lapisan HTTP data mahasiswa.
 *
 * <p>Golongan potongan menentukan nominal UKT, jadi perubahannya diperlakukan
 * seperti perubahan uang: nilai yang tidak dikenal harus tertahan di pintu, dan
 * penolakan karena golongan sudah terkunci harus sampai ke layar sebagai
 * penjelasan, bukan sebagai galat server.
 */
@ControllerTest(StudentController.class)
class StudentControllerTest extends ControllerTestSupport {

	@MockitoBean
	private StudentService service;

	// --- Ubah golongan potongan ---

	@Test
	@DisplayName("golongan yang tidak dikenal ditolak 400 beserta daftar pilihannya")
	void golonganTidakDikenal() throws Exception {
		// Golongan bisa ditambah admin, jadi daftarnya tidak lagi tetap di kode
		// dan pemeriksaannya pindah dari lapisan HTTP ke service. Yang penting
		// pesannya tetap menyebut pilihan yang sah.
		when(service.changeDiscountTier(anyLong(), any())).thenThrow(new BusinessRuleException(
				"Golongan potongan \"SULTAN\" tidak dikenal. Pilihan: NON_ALUMNI, KERJASAMA."));

		mockMvc.perform(sebagaiAdmin(patch("/students/1/discount-tier"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("discountTier", "SULTAN"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("KERJASAMA")));
	}

	@Test
	@DisplayName("golongan kosong ditolak 400 sebelum menyentuh service")
	void golonganKosongDitolakDiPintu() throws Exception {
		mockMvc.perform(sebagaiAdmin(patch("/students/1/discount-tier"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("discountTier", "   "))))
				.andExpect(status().isBadRequest());

		verify(service, never()).changeDiscountTier(anyLong(), any());
	}

	@Test
	@DisplayName("golongan tidak diisi ditolak 400")
	void golonganKosong() throws Exception {
		mockMvc.perform(sebagaiAdmin(patch("/students/1/discount-tier"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		verify(service, never()).changeDiscountTier(anyLong(), any());
	}

	@Test
	@DisplayName("golongan yang sudah terkunci dijawab 409 beserta alasannya")
	void golonganTerkunci() throws Exception {
		when(service.changeDiscountTier(anyLong(), any())).thenThrow(new BusinessRuleException(
				"Golongan tidak bisa diubah, mahasiswa sudah pernah mengunggah bukti bayar."));

		mockMvc.perform(sebagaiAdmin(patch("/students/1/discount-tier"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("discountTier", "ALUMNI"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("sudah pernah mengunggah")));
	}

	// --- Ubah data ---

	@Test
	@DisplayName("format tahun akademik yang salah ditolak 400")
	void tahunAkademikSalah() throws Exception {
		mockMvc.perform(sebagaiAdmin(put("/students/1"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("startAcademicYear", "2026"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("2026/2027")));

		verify(service, never()).update(anyLong(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("nama yang kepanjangan ditolak 400")
	void namaKepanjangan() throws Exception {
		mockMvc.perform(sebagaiAdmin(put("/students/1"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("name", "a".repeat(151)))))
				.andExpect(status().isBadRequest());
	}

	// --- Pengambilan data ---

	@Test
	@DisplayName("mahasiswa yang tidak ada dijawab 404 beserta keterangan")
	void tidakDitemukan() throws Exception {
		when(service.get(1L)).thenThrow(NotFoundException.of("Mahasiswa", 1L));

		mockMvc.perform(sebagaiAdmin(get("/students/1")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	// --- Reset kata sandi ---

	@Test
	@DisplayName("admin mengembalikan kata sandi, jawabannya menyebut NIM sebagai sandi barunya")
	void resetMengembalikanNim() throws Exception {
		when(service.resetKataSandi(1L)).thenReturn("2612600001");

		mockMvc.perform(sebagaiAdmin(post("/students/1/reset-kata-sandi")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.kataSandiBaru").value("2612600001"));
	}

	@Test
	@DisplayName("mahasiswa tidak boleh mengembalikan kata sandi siapa pun, termasuk dirinya")
	void mahasiswaTidakBolehReset() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/students/1/reset-kata-sandi")))
				.andExpect(status().isForbidden());

		verify(service, never()).resetKataSandi(anyLong());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void resetTanpaToken() throws Exception {
		mockMvc.perform(post("/students/1/reset-kata-sandi"))
				.andExpect(status().isUnauthorized());

		verify(service, never()).resetKataSandi(anyLong());
	}

	// --- Peran ---

	@Test
	@DisplayName("mahasiswa tidak boleh mengubah golongan potongannya sendiri")
	void mahasiswaTidakBolehUbahGolongan() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(patch("/students/1/discount-tier"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("discountTier", "KERJASAMA"))))
				.andExpect(status().isForbidden());

		verify(service, never()).changeDiscountTier(anyLong(), any());
	}

	@Test
	@DisplayName("mahasiswa tidak boleh menghapus data mahasiswa")
	void mahasiswaTidakBolehMenghapus() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(delete("/students/1")))
				.andExpect(status().isForbidden());

		verify(service, never()).delete(anyLong());
	}

	@Test
	@DisplayName("mahasiswa tidak boleh membuka daftar seluruh mahasiswa")
	void mahasiswaTidakBolehMelihatDaftar() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/students")))
				.andExpect(status().isForbidden());
	}
}
