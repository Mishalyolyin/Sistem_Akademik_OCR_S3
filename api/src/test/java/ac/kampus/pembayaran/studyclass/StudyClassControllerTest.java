package ac.kampus.pembayaran.studyclass;

import ac.kampus.pembayaran.student.StudentRepository;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pengelolaan kelas.
 *
 * <p>Yang paling perlu dijaga: kelas yang masih berisi mahasiswa tidak boleh
 * terhapus. Kunci asingnya tidak punya {@code ON DELETE}, jadi tanpa penjagaan
 * di controller penolakan datang dari database dan sampai ke admin sebagai 500
 * tanpa keterangan apa pun.
 */
@ControllerTest(StudyClassController.class)
class StudyClassControllerTest extends ControllerTestSupport {

	@MockitoBean
	private StudyClassRepository repository;
	@MockitoBean
	private StudentRepository studentRepository;

	@BeforeEach
	void setUp() {
		when(repository.save(any(StudyClass.class))).thenAnswer(inv -> {
			StudyClass kelas = inv.getArgument(0);
			if (kelas.getId() == null) kelas.setId(1L);
			return kelas;
		});
		when(repository.findByNameIgnoreCaseAndAcademicYear(any(), any()))
				.thenReturn(Optional.empty());
	}

	private static StudyClass kelas(String nama, boolean kerjasama) {
		return StudyClass.builder()
				.id(1L).name(nama).kerjasama(kerjasama)
				.academicYear("2026/2027").active(true)
				.build();
	}

	private static Map<String, Object> permintaan() {
		Map<String, Object> body = new HashMap<>();
		body.put("name", "A");
		body.put("academicYear", "2026/2027");
		body.put("kerjasama", false);
		return body;
	}

	@Test
	@DisplayName("nama tampilan diturunkan, bukan disimpan dua kali")
	void namaTampilan() throws Exception {
		when(repository.findAllByOrderByAcademicYearDescNameAsc())
				.thenReturn(List.of(kelas("A", false), kelas("Kerjasama B", true)));

		mockMvc.perform(sebagaiAdmin(get("/classes")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].displayName").value("Kelas A"))
				.andExpect(jsonPath("$[1].displayName").value("Kelas Kerjasama B"));
	}

	@Test
	@DisplayName("nama kosong ditolak 400 sebelum menyentuh repository")
	void namaWajib() throws Exception {
		Map<String, Object> body = permintaan();
		body.put("name", "  ");

		mockMvc.perform(sebagaiAdmin(post("/classes"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(body)))
				.andExpect(status().isBadRequest());

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("tahun akademik harus berbentuk 2026/2027")
	void tahunAkademikSalah() throws Exception {
		Map<String, Object> body = permintaan();
		body.put("academicYear", "2026");

		mockMvc.perform(sebagaiAdmin(post("/classes"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("2026/2027")));

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("nama kelas yang sudah ada di tahun yang sama ditolak 409")
	void namaGanda() throws Exception {
		// Nama ganda akan mematahkan import: pencarian kelasnya memakai
		// Optional, jadi dua baris yang cocok berujung galat teknis.
		when(repository.existsByNameIgnoreCaseAndAcademicYear("A", "2026/2027"))
				.thenReturn(true);

		mockMvc.perform(sebagaiAdmin(post("/classes"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(permintaan())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("sudah ada")));
	}

	@Test
	@DisplayName("kelas yang masih berisi mahasiswa tidak bisa dihapus")
	void kelasTerisiTidakBisaDihapus() throws Exception {
		when(repository.findById(1L)).thenReturn(Optional.of(kelas("A", false)));
		when(studentRepository.countByStudyClassId(1L)).thenReturn(7L);

		mockMvc.perform(sebagaiAdmin(delete("/classes/1")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("berisi 7 mahasiswa")));

		verify(repository, never()).delete(any());
	}

	@Test
	@DisplayName("kelas kosong boleh dihapus")
	void kelasKosongBolehDihapus() throws Exception {
		when(repository.findById(1L)).thenReturn(Optional.of(kelas("A", false)));
		when(studentRepository.countByStudyClassId(1L)).thenReturn(0L);

		mockMvc.perform(sebagaiAdmin(delete("/classes/1")))
				.andExpect(status().isNoContent());

		verify(repository).delete(any(StudyClass.class));
	}

	@Test
	@DisplayName("kelas yang tidak ada dijawab 404")
	void kelasTidakAda() throws Exception {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		mockMvc.perform(sebagaiAdmin(delete("/classes/99")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("mahasiswa tidak boleh mengelola kelas")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/classes")))
				.andExpect(status().isForbidden());

		mockMvc.perform(sebagaiMahasiswa(delete("/classes/1")))
				.andExpect(status().isForbidden());

		verify(repository, never()).delete(any());
	}
}
