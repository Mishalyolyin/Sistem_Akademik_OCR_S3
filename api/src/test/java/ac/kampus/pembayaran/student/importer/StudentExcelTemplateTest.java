package ac.kampus.pembayaran.student.importer;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bentuk berkas template import.
 *
 * <p>Yang dijaga di sini satu hal: sheet data tidak boleh berisi baris contoh.
 * Importer hanya membaca sheet pertama, jadi contoh yang tertinggal di sana akan
 * masuk sebagai mahasiswa sungguhan — dan karena datanya valid, tidak ada galat
 * apa pun yang memberi tahu bahwa itu keliru.
 */
class StudentExcelTemplateTest {

	private Workbook workbook;

	@BeforeEach
	void setUp() throws IOException {
		byte[] berkas = new StudentExcelTemplate().build();
		workbook = new XSSFWorkbook(new ByteArrayInputStream(berkas));
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
}
