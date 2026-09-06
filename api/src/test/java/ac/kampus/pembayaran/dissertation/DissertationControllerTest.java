package ac.kampus.pembayaran.dissertation;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentService;
import ac.kampus.pembayaran.student.self.StudentSelfService;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Identitas disertasi dari sisi HTTP.
 *
 * <p>Yang dijaga di sini terutama batas wewenangnya. Mahasiswa hanya boleh
 * menyentuh disertasinya sendiri, dan id-nya tidak pernah datang dari klien —
 * jadi tidak ada angka di URL yang bisa ditukar untuk membuka naskah orang
 * lain. Sebaliknya admin boleh membaca milik siapa pun, karena itulah yang
 * membuatnya tahu disertasi mana yang sedang diuji saat memverifikasi bukti
 * bayar tahap ujian.
 */
@ControllerTest(DissertationController.class)
class DissertationControllerTest extends ControllerTestSupport {

	@MockitoBean
	private DissertationService service;
	@MockitoBean
	private StudentSelfService selfService;
	@MockitoBean
	private StudentService studentService;
	@MockitoBean
	private FileStorageService storage;

	@TempDir
	Path folderUji;

	private Path naskah;

	private static final String JUDUL =
			"Implementasi Nilai Pendidikan Agama Islam di Pesantren Modern";

	@BeforeEach
	void setUp() throws IOException {
		naskah = folderUji.resolve("naskah.pdf");
		Files.writeString(naskah, "%PDF-palsu");

		when(selfService.current()).thenReturn(
				Student.builder().id(3L).nim("D9000001").name("Uji Coba").build());
		when(studentService.get(anyLong())).thenReturn(
				Student.builder().id(3L).nim("D9000001").name("Uji Coba").build());
		when(storage.resolve(anyString())).thenReturn(naskah);
		when(service.simpanIdentitas(anyLong(), anyString(), anyString(), anyString()))
				.thenReturn(detail());
	}

	private DissertationDetail detail() {
		return DissertationDetail.builder()
				.studentId(3L)
				.title(JUDUL)
				.promotor("Prof. Ahmad")
				.copromotor("Dr. Siti")
				.dissertationFilePath("disertasi/disertasi/x.pdf")
				.build();
	}

	private Map<String, String> identitasSah() {
		return Map.of("title", JUDUL, "promotor", "Prof. Ahmad", "copromotor", "Dr. Siti");
	}

	@Test
	@DisplayName("mahasiswa menyimpan identitas disertasinya sendiri")
	void simpanIdentitas() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(put("/me/disertasi"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(identitasSah())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value(JUDUL))
				.andExpect(jsonPath("$.promotor").value("Prof. Ahmad"))
				.andExpect(jsonPath("$.adaNaskah").value(true))
				.andExpect(jsonPath("$.adaArtikel").value(false));
	}

	@Test
	@DisplayName("judul yang terlalu pendek ditolak 400 sebelum menyentuh service")
	void judulPendek() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(put("/me/disertasi"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("title", "Disertasi",
								"promotor", "Prof. Ahmad", "copromotor", "Dr. Siti"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("10 karakter")));

		verify(service, never()).simpanIdentitas(anyLong(), anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("promotor kosong ditolak 400")
	void promotorKosong() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(put("/me/disertasi"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("title", JUDUL,
								"promotor", "", "copromotor", "Dr. Siti"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("disertasi yang belum diisi dijawab kosong, bukan 404")
	void belumDiisi() throws Exception {
		when(service.cari(3L)).thenReturn(Optional.empty());

		// Bedanya penting: layar harus bisa menampilkan formulir kosong, bukan
		// halaman galat, untuk mahasiswa yang memang belum pernah mengisinya.
		mockMvc.perform(sebagaiMahasiswa(get("/me/disertasi")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("mahasiswa mengunggah naskah PDF miliknya sendiri")
	void unggahNaskah() throws Exception {
		when(service.unggahBerkas(anyLong(), any(), any())).thenReturn(detail());

		mockMvc.perform(sebagaiMahasiswa(multipart("/me/disertasi/berkas")
						.file(new MockMultipartFile(
								"file", "naskah.pdf", "application/pdf", "%PDF".getBytes()))
						.param("jenis", "DISERTASI")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.adaNaskah").value(true));
	}

	@Test
	@DisplayName("naskah yang belum diunggah dijawab 409, bukan 404")
	void naskahBelumAda() throws Exception {
		when(service.cari(3L)).thenReturn(Optional.of(DissertationDetail.builder()
				.studentId(3L).title(JUDUL).promotor("Prof. Ahmad").copromotor("Dr. Siti")
				.build()));

		// "Belum diunggah" bukan "alamatnya salah". Layar perlu membedakannya
		// supaya bisa bilang yang benar kepada mahasiswa.
		mockMvc.perform(sebagaiMahasiswa(get("/me/disertasi/berkas/DISERTASI")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("belum diunggah")));
	}

	@Test
	@DisplayName("admin membaca disertasi mahasiswa mana pun")
	void adminMembaca() throws Exception {
		when(service.cari(7L)).thenReturn(Optional.of(detail()));

		mockMvc.perform(sebagaiAdmin(get("/students/7/disertasi")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value(JUDUL));
	}

	@Test
	@DisplayName("admin membuka naskah mahasiswa")
	void adminMembukaNaskah() throws Exception {
		when(service.cari(7L)).thenReturn(Optional.of(detail()));

		mockMvc.perform(sebagaiAdmin(get("/students/7/disertasi/berkas/DISERTASI")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("mahasiswa tidak boleh membuka jalur admin")
	void mahasiswaDitolakDiJalurAdmin() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/students/7/disertasi")))
				.andExpect(status().isForbidden());

		mockMvc.perform(sebagaiMahasiswa(get("/students/7/disertasi/berkas/DISERTASI")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("admin tidak boleh memakai jalur mahasiswa")
	void adminDitolakDiJalurMahasiswa() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/me/disertasi")))
				.andExpect(status().isForbidden());

		mockMvc.perform(sebagaiAdmin(put("/me/disertasi"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(identitasSah())))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get("/me/disertasi")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/students/7/disertasi")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("aturan bisnis dari service diteruskan sebagai 409 yang bisa dibaca")
	void galatServiceDiteruskan() throws Exception {
		when(service.unggahBerkas(anyLong(), any(), any())).thenThrow(
				new BusinessRuleException("Isi dulu judul disertasi dan nama promotor."));

		mockMvc.perform(sebagaiMahasiswa(multipart("/me/disertasi/berkas")
						.file(new MockMultipartFile(
								"file", "naskah.pdf", "application/pdf", "%PDF".getBytes()))
						.param("jenis", "DISERTASI")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("Isi dulu judul")));
	}
}
