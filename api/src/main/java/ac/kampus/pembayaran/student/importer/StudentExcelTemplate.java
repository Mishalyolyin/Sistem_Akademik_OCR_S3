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

import ac.kampus.pembayaran.tuition.DiscountTierRate;
import ac.kampus.pembayaran.tuition.DiscountTierRateRepository;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Membuat berkas .xlsx contoh untuk import mahasiswa.
 * Padanan StudentTemplateExport di sistem Laravel lama, tapi tanpa kolom
 * program_type, is_alumni, dan kelas_kerjasama — ketiganya melebur jadi
 * satu kolom discount_tier.
 */
@Component
@RequiredArgsConstructor
public class StudentExcelTemplate {

	static final List<String> HEADERS = List.of(
			"nim", "name", "class", "discount_tier", "start_term", "academic_year", "phone");

	private final DiscountTierRateRepository tierRepository;

	public byte[] build() {
		// Golongan dibaca dari database, bukan didaftar di kode: admin bisa
		// menambah golongan baru, dan template yang menyebut daftar lama akan
		// menuntun orang mengisi kode yang justru ditolak importer.
		List<DiscountTierRate> golongan =
				tierRepository.findByActiveTrueOrderBySortOrderAscTierAsc();

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
			List<List<String>> barisContoh = contoh(golongan);
			for (int r = 0; r < barisContoh.size(); r++) {
				Row row = contoh.createRow(r + 1);
				List<String> values = barisContoh.get(r);
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
			List<String> catatan = new ArrayList<>(List.of(
					"Cara mengisi:",
					"",
					"nim            Nomor induk mahasiswa, wajib, tidak boleh sama dengan yang sudah ada.",
					"name           Nama lengkap, wajib.",
					"class          Nama kelas bebas: A, B, C, atau Kerjasama A. Dibuat otomatis kalau belum ada.",
					"               Nama yang mengandung kata \"Kerjasama\" otomatis ditandai sebagai kelas kerjasama.",
					"discount_tier  Golongan potongan UKT, wajib. Pilihan:"));

			catatan.addAll(pilihanGolongan(golongan));

			catatan.addAll(List.of(
					"start_term     GASAL atau GENAP, wajib.",
					"academic_year  Format 2026/2027, wajib.",
					"phone          Boleh dikosongkan.",
					"",
					"Isi data di sheet Mahasiswa; hanya sheet itu yang dibaca saat diunggah.",
					"Sheet Contoh berisi baris teladan dan boleh dibiarkan apa adanya.",
					"Baris yang gagal akan dilaporkan satu per satu tanpa membatalkan baris lain."));

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

	/**
	 * Baris teladan, kode golongannya diambil dari golongan yang benar-benar
	 * aktif supaya contohnya selalu bisa diunggah apa adanya.
	 */
	private static List<List<String>> contoh(List<DiscountTierRate> golongan) {
		List<List<String>> baris = new ArrayList<>();
		baris.add(List.of("2612600001", "Contoh Mahasiswa", "A",
				kode(golongan, 0), "GASAL", "2026/2027", "081234567890"));
		baris.add(List.of("2612600002", "Contoh Mahasiswa Dua", "B",
				kode(golongan, 1), "GASAL", "2026/2027", "081234567891"));
		baris.add(List.of("2612600003", "Contoh Kelas Kerjasama", "Kerjasama A",
				kode(golongan, golongan.size() - 1), "GENAP", "2026/2027", "081234567892"));
		return baris;
	}

	/** Golongan ke-i, atau yang pertama bila daftarnya lebih pendek dari itu. */
	private static String kode(List<DiscountTierRate> golongan, int index) {
		if (golongan.isEmpty()) return "";
		return golongan.get(Math.min(Math.max(index, 0), golongan.size() - 1)).getTier();
	}

	private static List<String> pilihanGolongan(List<DiscountTierRate> golongan) {
		if (golongan.isEmpty()) {
			return List.of("                 (belum ada golongan aktif — atur dulu di menu "
					+ "Tarif & Potongan sebelum mengimpor)");
		}
		return golongan.stream()
				.map(tier -> "                 %-16s potongan %s%%  %s".formatted(
						tier.getTier(),
						tier.getPercent().stripTrailingZeros().toPlainString(),
						tier.getLabel()))
				.toList();
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
