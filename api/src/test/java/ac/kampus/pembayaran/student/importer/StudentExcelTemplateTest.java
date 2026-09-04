package ac.kampus.pembayaran.student.importer;

import ac.kampus.pembayaran.tuition.DiscountTierRate;
import ac.kampus.pembayaran.tuition.DiscountTierRateRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bentuk berkas template import.
 *
 * <p>Dua hal yang dijaga di sini. Pertama, sheet data tidak boleh berisi baris
 * contoh: importer hanya membaca sheet pertama, jadi contoh yang tertinggal di
 * sana akan masuk sebagai mahasiswa sungguhan — dan karena datanya valid, tidak
 * ada galat apa pun yang memberi tahu bahwa itu keliru. Kedua, daftar golongan
 * di petunjuk harus ikut golongan yang ada di database, bukan daftar tetap:
 * template yang menyebut golongan lama menuntun orang mengisi kode yang justru
 * ditolak importer.
 */
class StudentExcelTemplateTest {

	private final DiscountTierRateRepository tierRepository = mock(DiscountTierRateRepository.class);

	private Workbook workbook;

	@BeforeEach
	void setUp() throws IOException {
		workbook = bangun(
				golongan("NON_ALUMNI", "Non alumni", "0", 1),
				golongan("ALUMNI", "Alumni", "25", 2),
				golongan("KERJASAMA", "Kerjasama", "40", 3));
	}

	private Workbook bangun(DiscountTierRate... golongan) throws IOException {
		when(tierRepository.findByActiveTrueOrderBySortOrderAscTierAsc())
				.thenReturn(List.of(golongan));
		return new XSSFWorkbook(
				new ByteArrayInputStream(new StudentExcelTemplate(tierRepository).build()));
	}

	private static DiscountTierRate golongan(
			String tier, String label, String percent, int urutan) {
		return DiscountTierRate.builder()
				.tier(tier).label(label).percent(new BigDecimal(percent))
				.active(true).sortOrder(urutan)
				.build();
	}

	/** Seluruh isi sheet Petunjuk sebagai satu teks, untuk pemeriksaan isi. */
	private String petunjuk() {
		Sheet sheet = workbook.getSheet("Petunjuk");
		StringBuilder teks = new StringBuilder();
		for (Row row : sheet) {
			Cell cell = row.getCell(0);
			teks.append(cell == null ? "" : cell.getStringCellValue()).append(System.lineSeparator());
		}
		return teks.toString();
	}

	private static List<String> nilaiBaris(Row row) {
		List<String> nilai = new ArrayList<>();
		for (int c = 0; c < row.getLastCellNum(); c++) {
			nilai.add(row.getCell(c) == null ? "" : row.getCell(c).getStringCellValue());
		}
		return nilai;
	}

	@Test
	@DisplayName("sheet pertama adalah sheet data, karena hanya itu yang dibaca importer")
	void sheetPertamaAdalahSheetData() {
		assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("Mahasiswa");
	}

	@Test
	@DisplayName("sheet data hanya berisi baris judul kolom, tanpa satu pun baris contoh")
	void sheetDataKosong() {
		Sheet data = workbook.getSheetAt(0);

		assertThat(data.getPhysicalNumberOfRows()).isEqualTo(1);
		assertThat(nilaiBaris(data.getRow(0))).isEqualTo(StudentExcelTemplate.HEADERS);
	}

	@Test
	@DisplayName("contoh pengisian tetap disediakan, tapi di sheet yang tidak ikut dibaca")
	void contohAdaDiSheetTerpisah() {
		Sheet contoh = workbook.getSheet("Contoh");

		assertThat(contoh).isNotNull();
		assertThat(contoh.getPhysicalNumberOfRows()).isGreaterThan(1);
		assertThat(nilaiBaris(contoh.getRow(0))).isEqualTo(StudentExcelTemplate.HEADERS);
		assertThat(workbook.getSheetIndex(contoh)).isNotZero();
	}

	@Test
	@DisplayName("petunjuk pengisian ikut disertakan")
	void petunjukAda() {
		assertThat(workbook.getSheet("Petunjuk")).isNotNull();
	}

	@Test
	@DisplayName("petunjuk mendaftar golongan yang ada di database, termasuk yang baru ditambah")
	void petunjukMengikutiGolonganDiDatabase() throws Exception {
		workbook = bangun(
				golongan("NON_ALUMNI", "Non alumni", "0", 1),
				golongan("MITRA_INSTANSI", "Mitra instansi", "30", 2));

		assertThat(petunjuk())
				.contains("NON_ALUMNI")
				.contains("MITRA_INSTANSI")
				.contains("potongan 30%")
				.contains("Mitra instansi")
				// Golongan yang sudah tidak ada tidak boleh ikut ditawarkan.
				.doesNotContain("KERJASAMA");
	}

	@Test
	@DisplayName("baris contoh memakai kode golongan yang benar-benar aktif")
	void contohMemakaiGolonganAktif() throws Exception {
		workbook = bangun(golongan("MITRA_INSTANSI", "Mitra instansi", "30", 1));

		Sheet contoh = workbook.getSheet("Contoh");
		int kolomGolongan = StudentExcelTemplate.HEADERS.indexOf("discount_tier");

		for (int r = 1; r < contoh.getPhysicalNumberOfRows(); r++) {
			assertThat(contoh.getRow(r).getCell(kolomGolongan).getStringCellValue())
					.isEqualTo("MITRA_INSTANSI");
		}
	}

	@Test
	@DisplayName("tanpa golongan aktif, template tetap terbentuk dan bilang apa yang harus diatur")
	void tanpaGolonganAktif() throws Exception {
		workbook = bangun();

		assertThat(petunjuk()).contains("belum ada golongan aktif");
		assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isEqualTo(1);
	}
}
