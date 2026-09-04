package ac.kampus.pembayaran.student.importer;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import mahasiswa dari Excel.
 *
 * <p>Ini satu-satunya jalan masuk data mahasiswa ke sistem — tidak ada endpoint
 * "buat mahasiswa" — jadi yang dijaga di sini: hasil per baris benar-benar
 * sampai ke pemanggil, dan pintunya tertutup untuk selain admin.
 */
@ControllerTest(StudentImportController.class)
class StudentImportControllerTest extends ControllerTestSupport {

	@MockitoBean
	private StudentImportService importService;
	@MockitoBean
	private StudentExcelTemplate template;
	@MockitoBean
	private ImportBatchRepository batchRepository;

	@BeforeEach
	void setUp() {
		when(template.build()).thenReturn("berkas-xlsx-palsu".getBytes());
	}

	private static ImportBatch batch() {
		return ImportBatch.builder()
				.id(5L).filename("mahasiswa.xlsx")
				.totalRows(3).successRows(2).failedRows(1)
				.errors(List.of(new ImportBatch.RowError(
						4, "2612600009", "Golongan \"SULTAN\" tidak dikenal.")))
				.build();
	}

	private static MockMultipartFile berkas(String nama) {
		return new MockMultipartFile("file", nama,
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"isi".getBytes());
	}

	@Test
	@DisplayName("template terunduh sebagai lampiran .xlsx, bukan tampil di peramban")
	void unduhTemplate() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/students/import/template")))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("attachment")))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("template-import-mahasiswa.xlsx")));
	}

	@Test
	@DisplayName("baris yang gagal ikut dilaporkan, tidak diringkas jadi satu angka")
	void hasilPerBaris() throws Exception {
		when(importService.importFile(any(), anyLong())).thenReturn(batch());

		mockMvc.perform(sebagaiAdmin(multipart("/students/import").file(berkas("mahasiswa.xlsx"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalRows").value(3))
				.andExpect(jsonPath("$.successRows").value(2))
				.andExpect(jsonPath("$.failedRows").value(1))
				.andExpect(jsonPath("$.errors[0].row").value(4))
				.andExpect(jsonPath("$.errors[0].nim").value("2612600009"))
				.andExpect(jsonPath("$.errors[0].message", containsString("SULTAN")));
	}

	@Test
	@DisplayName("berkas selain .xlsx ditolak 409 dengan keterangan, bukan galat teknis")
	void bukanXlsx() throws Exception {
		when(importService.importFile(any(), anyLong()))
				.thenThrow(new BusinessRuleException("Berkas harus berformat .xlsx."));

		mockMvc.perform(sebagaiAdmin(multipart("/students/import").file(berkas("mahasiswa.csv"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString(".xlsx")));
	}

	@Test
	@DisplayName("unggahan tanpa berkas ditolak 400 sebelum menyentuh service")
	void tanpaBerkas() throws Exception {
		mockMvc.perform(sebagaiAdmin(multipart("/students/import")))
				.andExpect(status().isBadRequest());

		verify(importService, never()).importFile(any(), anyLong());
	}

	@Test
	@DisplayName("riwayat import terbaca")
	void riwayat() throws Exception {
		when(batchRepository.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of(batch()));

		mockMvc.perform(sebagaiAdmin(get("/students/import/history")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].batchId").value(5))
				.andExpect(jsonPath("$[0].filename").value("mahasiswa.xlsx"));
	}

	@Test
	@DisplayName("mahasiswa tidak boleh mengimpor siapa pun, termasuk mengunduh templatenya")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/students/import/template")))
				.andExpect(status().isForbidden());

		mockMvc.perform(sebagaiMahasiswa(multipart("/students/import").file(berkas("a.xlsx"))))
				.andExpect(status().isForbidden());

		verify(importService, never()).importFile(any(), anyLong());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get("/students/import/history"))
				.andExpect(status().isUnauthorized());
	}
}
