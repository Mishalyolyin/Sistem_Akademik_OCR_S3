package ac.kampus.pembayaran.student.importer;

import ac.kampus.pembayaran.tuition.DiscountTierRate;
import ac.kampus.pembayaran.tuition.DiscountTierRateRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pemeriksaan golongan potongan pada berkas import.
 *
 * <p>Golongan tidak lagi berupa enum Java, jadi daftar pilihannya tidak bisa
 * ditulis di kode: golongan yang baru ditambah admin harus langsung diterima
 * berkas import, tanpa deploy ulang. Sebaliknya kode yang tidak ada harus
 * ditolak di sini juga — kalau lolos, yang menahannya tinggal kunci asing di
 * database, dan galatnya sampai ke admin sebagai kegagalan teknis tanpa
 * keterangan baris mana yang salah.
 */
class StudentImportServiceTest {

	private ImportBatchRepository batchRepository;
	private StudentImportService.StudentRowWriter rowWriter;
	private DiscountTierRateRepository tierRepository;
	private StudentImportService service;

	@BeforeEach
	void setUp() {
		batchRepository = mock(ImportBatchRepository.class);
		rowWriter = mock(StudentImportService.StudentRowWriter.class);
		tierRepository = mock(DiscountTierRateRepository.class);
		service = new StudentImportService(batchRepository, rowWriter, tierRepository);

		when(batchRepository.save(any(ImportBatch.class))).thenAnswer(inv -> inv.getArgument(0));
		golonganAktif("NON_ALUMNI", "ALUMNI", "MITRA_INSTANSI");
	}

	private void golonganAktif(String... kode) {
		when(tierRepository.findByActiveTrueOrderBySortOrderAscTierAsc()).thenReturn(
				java.util.Arrays.stream(kode)
						.map(k -> DiscountTierRate.builder()
								.tier(k).label(k).percent(BigDecimal.ZERO)
								.active(true).sortOrder(1)
								.build())
						.toList());
	}

	/** Berkas .xlsx berisi baris judul kolom template dan baris-baris di bawahnya. */
	private static MockMultipartFile berkas(List<String>... baris) {
		try (Workbook workbook = new XSSFWorkbook();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			Sheet sheet = workbook.createSheet("Mahasiswa");
			Row header = sheet.createRow(0);
			for (int c = 0; c < StudentExcelTemplate.HEADERS.size(); c++) {
				header.createCell(c).setCellValue(StudentExcelTemplate.HEADERS.get(c));
			}
			for (int r = 0; r < baris.length; r++) {
				Row row = sheet.createRow(r + 1);
				List<String> nilai = baris[r];
				for (int c = 0; c < nilai.size(); c++) {
					row.createCell(c).setCellValue(nilai.get(c));
				}
			}

			workbook.write(out);
			return new MockMultipartFile("file", "import.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					out.toByteArray());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static List<String> baris(String nim, String golongan) {
		return List.of(nim, "Uji Coba", "A", golongan, "GASAL", "2026/2027", "081234567890");
	}

	@Test
	@DisplayName("golongan yang baru ditambah admin langsung diterima berkas import")
	void golonganBaruDiterima() {
		ImportBatch batch = service.importFile(berkas(baris("2612600001", "MITRA_INSTANSI")), 1L);

		assertThat(batch.getSuccessRows()).isEqualTo(1);
		assertThat(batch.getFailedRows()).isZero();

		ArgumentCaptor<StudentImportService.ParsedRow> tertulis =
				ArgumentCaptor.forClass(StudentImportService.ParsedRow.class);
		verify(rowWriter).write(tertulis.capture());
		assertThat(tertulis.getValue().discountTier()).isEqualTo("MITRA_INSTANSI");
	}

	@Test
	@DisplayName("golongan yang tidak ada dilaporkan per baris, tanpa menggagalkan baris lain")
	void golonganTidakDikenal() {
		ImportBatch batch = service.importFile(berkas(
				baris("2612600001", "ALUMNI"),
				baris("2612600002", "MITRA_INSTANS"),
				baris("2612600003", "NON_ALUMNI")), 1L);

		assertThat(batch.getTotalRows()).isEqualTo(3);
		assertThat(batch.getSuccessRows()).isEqualTo(2);
		assertThat(batch.getErrors()).hasSize(1);

		ImportBatch.RowError galat = batch.getErrors().getFirst();
		assertThat(galat.nim()).isEqualTo("2612600002");
		// Baris 1 adalah judul kolom, jadi baris ketiga berkas ini nomor 3.
		assertThat(galat.row()).isEqualTo(3);
		assertThat(galat.message())
				.contains("MITRA_INSTANS")
				.contains("tidak dikenal")
				// Pilihannya ikut disebut supaya admin tidak perlu menebak.
				.contains("NON_ALUMNI, ALUMNI, MITRA_INSTANSI");
	}

	@Test
	@DisplayName("golongan yang sudah dinonaktifkan tidak lagi diterima")
	void golonganNonaktifDitolak() {
		golonganAktif("NON_ALUMNI", "ALUMNI");

		ImportBatch batch = service.importFile(berkas(baris("2612600001", "MITRA_INSTANSI")), 1L);

		assertThat(batch.getSuccessRows()).isZero();
		assertThat(batch.getErrors().getFirst().message()).contains("tidak dikenal");
		verify(rowWriter, times(0)).write(any());
	}

	@Test
	@DisplayName("huruf kecil dan spasi berlebih pada kode golongan dimaafkan")
	void kodeDirapikan() {
		ImportBatch batch = service.importFile(berkas(baris("2612600001", "  alumni ")), 1L);

		assertThat(batch.getSuccessRows()).isEqualTo(1);

		ArgumentCaptor<StudentImportService.ParsedRow> tertulis =
				ArgumentCaptor.forClass(StudentImportService.ParsedRow.class);
		verify(rowWriter).write(tertulis.capture());
		assertThat(tertulis.getValue().discountTier()).isEqualTo("ALUMNI");
	}

	@Test
	@DisplayName("daftar golongan dibaca sekali untuk seluruh berkas, bukan sekali per baris")
	void daftarDibacaSekali() {
		service.importFile(berkas(
				baris("2612600001", "ALUMNI"),
				baris("2612600002", "ALUMNI"),
				baris("2612600003", "ALUMNI")), 1L);

		verify(tierRepository, times(1)).findByActiveTrueOrderBySortOrderAscTierAsc();
	}
}
