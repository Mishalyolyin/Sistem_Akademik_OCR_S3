package ac.kampus.pembayaran.report;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Laporan Excel dan kuitansi PDF.
 *
 * <p>Kuitansi adalah bukti bahwa uang diterima, jadi yang paling dijaga di sini:
 * ia tidak boleh terbit untuk pembayaran yang belum diverifikasi. Mencetaknya
 * lebih dulu berarti kampus mengeluarkan bukti untuk uang yang belum tentu
 * masuk.
 */
@ControllerTest(ReportController.class)
class ReportControllerTest extends ControllerTestSupport {

	@MockitoBean
	private PaymentReportExporter exporter;
	@MockitoBean
	private ReceiptGenerator receiptGenerator;
	@MockitoBean
	private OcrDatasetExporter datasetExporter;
	@MockitoBean
	private StudentDataExporter studentExporter;

	@BeforeEach
	void setUp() {
		when(exporter.ledgerMahasiswa(any(PaymentReportExporter.Filter.class)))
				.thenReturn("berkas-excel-palsu".getBytes());
		when(studentExporter.dataMahasiswa(any(), any()))
				.thenReturn("berkas-mahasiswa-palsu".getBytes());
		when(datasetExporter.datasetGambar(anyInt())).thenReturn("PK-palsu".getBytes());
		when(datasetExporter.datasetOcr(anyBoolean()))
				.thenReturn("payment_id,label\n1,VERIFIED\n".getBytes());
	}

	@Test
	@DisplayName("laporan terunduh sebagai lampiran .xlsx bertanggal")
	void laporanExcel() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/reports/pembayaran.xlsx")))
				.andExpect(status().isOk())
				.andExpect(content().contentType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("attachment")))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("laporan-pembayaran-")));
	}

	@Test
	@DisplayName("kuitansi terbit sebagai PDF dengan nama bernomor pembayaran")
	void kuitansiTerbit() throws Exception {
		when(receiptGenerator.generate(7L)).thenReturn("%PDF-palsu".getBytes());

		mockMvc.perform(sebagaiAdmin(get("/reports/kuitansi/7.pdf")))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("kuitansi-7.pdf")));
	}

	@Test
	@DisplayName("kuitansi untuk pembayaran yang belum diverifikasi ditolak 409")
	void kuitansiBelumVerifikasi() throws Exception {
		when(receiptGenerator.generate(7L)).thenThrow(new BusinessRuleException(
				"Kuitansi hanya untuk pembayaran yang sudah diverifikasi."));

		mockMvc.perform(sebagaiAdmin(get("/reports/kuitansi/7.pdf")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("sudah diverifikasi")));
	}

	@Test
	@DisplayName("kuitansi untuk pembayaran yang tidak ada dijawab 404")
	void kuitansiTidakAda() throws Exception {
		when(receiptGenerator.generate(99L)).thenThrow(NotFoundException.of("Pembayaran", 99L));

		mockMvc.perform(sebagaiAdmin(get("/reports/kuitansi/99.pdf")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("nomor pembayaran yang bukan angka ditolak 400, bukan 500")
	void nomorNgawur() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/reports/kuitansi/abc.pdf")))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("dataset terunduh sebagai CSV bertanggal")
	void datasetTerunduh() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/reports/dataset-ocr.csv")))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/csv")))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("attachment")))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("dataset-ocr-")));
	}

	@Test
	@DisplayName("teks mentah tidak ikut kecuali diminta secara eksplisit")
	void tanpaTeksMentah() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/reports/dataset-ocr.csv")))
				.andExpect(status().isOk());

		// Isi struk memuat nama, nomor rekening, dan saldo. Unduhan biasa tidak
		// boleh diam-diam membawanya keluar.
		verify(datasetExporter).datasetOcr(false);
		verify(datasetExporter, never()).datasetOcr(true);
	}

	@Test
	@DisplayName("teks mentah ikut bila sertakanTeks=true")
	void denganTeksMentah() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/reports/dataset-ocr.csv")
						.param("sertakanTeks", "true")))
				.andExpect(status().isOk());

		verify(datasetExporter).datasetOcr(true);
	}

	@Test
	@DisplayName("mahasiswa tidak boleh menarik dataset")
	void datasetMahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/reports/dataset-ocr.csv")))
				.andExpect(status().isForbidden());

		verify(datasetExporter, never()).datasetOcr(anyBoolean());
	}

	@Test
	@DisplayName("dataset tanpa token ditolak 401")
	void datasetTanpaToken() throws Exception {
		mockMvc.perform(get("/reports/dataset-ocr.csv"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("mahasiswa tidak boleh menarik laporan maupun kuitansi siapa pun")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/reports/pembayaran.xlsx")))
				.andExpect(status().isForbidden());

		mockMvc.perform(sebagaiMahasiswa(get("/reports/kuitansi/7.pdf")))
				.andExpect(status().isForbidden());

		verify(exporter, never()).ledgerMahasiswa(any(PaymentReportExporter.Filter.class));
		verify(receiptGenerator, never()).generate(7L);
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get("/reports/pembayaran.xlsx"))
				.andExpect(status().isUnauthorized());
	}
}
