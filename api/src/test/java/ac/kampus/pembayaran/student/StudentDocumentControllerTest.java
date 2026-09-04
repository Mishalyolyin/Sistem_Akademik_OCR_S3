package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.student.self.StudentSelfService;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pembukaan dokumen wajib mahasiswa.
 *
 * <p>Keempat berkas ini sebelumnya hanya punya jalur unggah, jadi yang dijaga di
 * sini bukan cuma "berkasnya keluar", tapi juga batasnya: KTP dan Kartu Keluarga
 * memuat NIK dan alamat, dan mahasiswa tidak boleh bisa membuka milik orang lain
 * hanya dengan mengganti angka di URL.
 */
@ControllerTest(StudentDocumentController.class)
class StudentDocumentControllerTest extends ControllerTestSupport {

	@MockitoBean
	private StudentService service;
	@MockitoBean
	private StudentSelfService selfService;
	@MockitoBean
	private FileStorageService storage;

	@TempDir
	Path folderUji;

	private Path berkasKtp;

	@BeforeEach
	void setUp() throws IOException {
		berkasKtp = folderUji.resolve("ktp.png");
		Files.writeString(berkasKtp, "isi-berkas-ktp");

		when(service.get(1L)).thenReturn(mahasiswa());
		when(selfService.current()).thenReturn(mahasiswa());
		when(storage.resolve(anyString())).thenReturn(berkasKtp);
	}

	/** Mahasiswa dengan foto dan KTP terunggah, tapi KK dan ijazah belum. */
	private static Student mahasiswa() {
		return Student.builder()
				.id(1L).nim("2612600001").name("Uji Coba")
				.discountTier("NON_ALUMNI")
				.walletBalance(BigDecimal.ZERO).active(true)
				.profilePicture("dokumen/foto/2026-09-04/a.png")
				.nik("3201010101010001")
				.ktpFilePath("dokumen/ktp/2026-09-04/b.png")
				.build();
	}

	// --- Admin ---

	@Test
	@DisplayName("admin bisa membuka KTP mahasiswa")
	void adminBisaMembuka() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/students/1/dokumen/ktp")))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG))
				.andExpect(content().string("isi-berkas-ktp"));
	}

	@Test
	@DisplayName("alamat ditulis huruf kecil, tapi huruf besar tetap diterima")
	void kodeTidakPekaHurufBesar() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/students/1/dokumen/KTP")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("jenis dokumen yang tidak dikenal dijawab 400 beserta pilihannya")
	void jenisNgawur() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/students/1/dokumen/rapor")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("foto, ktp, kk, ijazah")));
	}

	@Test
	@DisplayName("dokumen yang belum diunggah dibedakan dari berkas yang hilang")
	void belumDiunggah() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/students/1/dokumen/ijazah")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("belum diunggah")));
	}

	@Test
	@DisplayName("baris yang menunjuk berkas tidak ada di disk dijawab 404, bukan 500")
	void berkasHilangDiDisk() throws Exception {
		when(storage.resolve(anyString())).thenReturn(folderUji.resolve("tidak-ada.png"));

		mockMvc.perform(sebagaiAdmin(get("/students/1/dokumen/ktp")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail", containsString("tidak ditemukan")));
	}

	@Test
	@DisplayName("mahasiswa yang tidak ada dijawab 404")
	void mahasiswaTidakAda() throws Exception {
		when(service.get(99L)).thenThrow(NotFoundException.of("Mahasiswa", 99L));

		mockMvc.perform(sebagaiAdmin(get("/students/99/dokumen/ktp")))
				.andExpect(status().isNotFound());
	}

	// --- Batas wewenang ---

	@Test
	@DisplayName("mahasiswa membuka dokumennya sendiri lewat jalur /me")
	void mahasiswaBukaMilikSendiri() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/me/dokumen/ktp")))
				.andExpect(status().isOk())
				.andExpect(content().string("isi-berkas-ktp"));
	}

	@Test
	@DisplayName("mahasiswa tidak boleh membuka dokumen lewat jalur admin")
	void mahasiswaDitolakDiJalurAdmin() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/students/1/dokumen/ktp")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("admin tidak memakai jalur /me, itu khusus mahasiswa")
	void adminDitolakDiJalurPortal() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/me/dokumen/ktp")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get("/students/1/dokumen/ktp"))
				.andExpect(status().isUnauthorized());
	}
}
