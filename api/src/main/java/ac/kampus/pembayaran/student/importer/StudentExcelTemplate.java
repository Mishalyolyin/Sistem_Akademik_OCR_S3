package ac.kampus.pembayaran.student.importer;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Membuat berkas .xlsx contoh untuk import mahasiswa.
 * Padanan StudentTemplateExport di sistem Laravel lama, tapi tanpa kolom
 * program_type, is_alumni, dan kelas_kerjasama — ketiganya melebur jadi
 * satu kolom discount_tier.
 */
@Component
public class StudentExcelTemplate {

	static final List<String> HEADERS = List.of(
			"nim", "name", "class", "discount_tier", "start_term", "academic_year", "phone");

	private static final List<List<String>> CONTOH = List.of(
			List.of("2612600001", "Contoh Mahasiswa", "A", "NON_ALUMNI", "GASAL", "2026/2027", "081234567890"),
			List.of("2612600002", "Contoh Alumni", "B", "ALUMNI", "GASAL", "2026/2027", "081234567891"),
			List.of("2612600003", "Contoh Kerjasama", "Kerjasama A", "KERJASAMA", "GENAP", "2026/2027", "081234567892"));

	public byte[] build() {
		try (Workbook workbook = new XSSFWorkbook();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			Sheet sheet = workbook.createSheet("Mahasiswa");
			CellStyle headerStyle = headerStyle(workbook);

			Row header = sheet.createRow(0);
			for (int i = 0; i < HEADERS.size(); i++) {
				var cell = header.createCell(i);
				cell.setCellValue(HEADERS.get(i));
				cell.setCellStyle(headerStyle);
			}

			for (int i = 0; i < HEADERS.size(); i++) {
				sheet.autoSizeColumn(i);
			}

			// Contoh pengisian ditaruh di sheet sendiri, bukan di sheet data.
			// Yang diunggah hanya sheet pertama, jadi contoh yang lupa dihapus
			// tidak bisa lagi ikut masuk sebagai mahasiswa sungguhan.
			Sheet contoh = workbook.createSheet("Contoh");
			Row contohHeader = contoh.createRow(0);
			for (int i = 0; i < HEADERS.size(); i++) {
				var cell = contohHeader.createCell(i);
				cell.setCellValue(HEADERS.get(i));
				cell.setCellStyle(headerStyle);
			}
			for (int r = 0; r < CONTOH.size(); r++) {
				Row row = contoh.createRow(r + 1);
				List<String> values = CONTOH.get(r);
				for (int c = 0; c < values.size(); c++) {
					row.createCell(c).setCellValue(values.get(c));
				}
			}
			for (int i = 0; i < HEADERS.size(); i++) {
				contoh.autoSizeColumn(i);
			}

			// Petunjuk juga di sheet terpisah. Kalau ditaruh di sheet data,
			// barisnya ikut terbaca sebagai baris mahasiswa saat diunggah.
			Sheet petunjuk = workbook.createSheet("Petunjuk");
			List<String> catatan = List.of(
					"Cara mengisi:",
					"",
					"nim            Nomor induk mahasiswa, wajib, tidak boleh sama dengan yang sudah ada.",
					"name           Nama lengkap, wajib.",
					"class          Nama kelas bebas: A, B, C, atau Kerjasama A. Dibuat otomatis kalau belum ada.",
					"               Nama yang mengandung kata \"Kerjasama\" otomatis ditandai sebagai kelas kerjasama.",
					"discount_tier  Golongan potongan UKT, wajib. Pilihan:",
					"                 NON_ALUMNI      potongan 0%   UKT 10.000.000 per semester",
					"                 KERABAT_ALUMNI  potongan 20%  UKT  8.000.000 per semester",
					"                 ALUMNI          potongan 25%  UKT  7.500.000 per semester",
					"                 ALUMNI_PASUTRI  potongan 35%  UKT  6.500.000 per semester",
					"                 KERJASAMA       potongan 40%  UKT  6.000.000 per semester",
					"start_term     GASAL atau GENAP, wajib.",
					"academic_year  Format 2026/2027, wajib.",
					"phone          Boleh dikosongkan.",
					"",
					"Isi data di sheet Mahasiswa; hanya sheet itu yang dibaca saat diunggah.",
					"Sheet Contoh berisi tiga baris teladan dan boleh dibiarkan apa adanya.",
					"Baris yang gagal akan dilaporkan satu per satu tanpa membatalkan baris lain.");

			for (int i = 0; i < catatan.size(); i++) {
				petunjuk.createRow(i).createCell(0).setCellValue(catatan.get(i));
			}
			petunjuk.setColumnWidth(0, 100 * 256);

			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Gagal membuat template Excel.", e);
		}
	}

	private CellStyle headerStyle(Workbook workbook) {
		Font font = workbook.createFont();
		font.setBold(true);
		font.setColor(IndexedColors.WHITE.getIndex());

		CellStyle style = workbook.createCellStyle();
		style.setFont(font);
		style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setBorderBottom(BorderStyle.THIN);
		return style;
	}
}
