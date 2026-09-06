package ac.kampus.pembayaran.report;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Ekspor data mahasiswa ke Excel.
 *
 * <p>Sistem ini sejak awal bisa <i>menerima</i> daftar mahasiswa lewat import
 * Excel, tapi tidak pernah bisa <i>mengeluarkannya</i> lagi. Akibatnya data
 * yang sudah masuk — termasuk yang dilengkapi mahasiswa sendiri lewat portal,
 * seperti NIK, nomor KK, tempat dan tanggal lahir hasil pembacaan ijazah —
 * hanya bisa dilihat satu per satu di layar.
 *
 * <p>Kolomnya sengaja memuat <b>hasil pemeriksaan dokumen</b>, bukan sekadar
 * "sudah unggah atau belum". Yang dicari admin saat membuka berkas ini biasanya
 * bukan siapa yang sudah mengunggah, melainkan berkas siapa yang perlu dilihat
 * manusia karena isinya tidak cocok dengan yang diketik.
 */
@Component
@RequiredArgsConstructor
public class StudentDataExporter {

	private final JdbcClient jdbc;

	/**
	 * @param classId batasi ke satu kelas; kosong berarti semua
	 * @param tier    batasi ke satu golongan potongan; kosong berarti semua
	 */
	@Transactional(readOnly = true)
	public byte[] dataMahasiswa(Long classId, String tier) {
		try (Workbook workbook = new XSSFWorkbook();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			Sheet sheet = workbook.createSheet("Data Mahasiswa");
			CellStyle header = headerStyle(workbook);

			tulisHeader(sheet, header,
					"NIM", "Nama", "Kelas", "Golongan", "Angkatan", "Term Masuk", "No HP",
					"Status", "Dokumen Lengkap", "Langkah Berikutnya",
					"NIK", "KTP NIK Cocok",
					"No. KK", "KK Nomor Cocok",
					"Ijazah Nama Cocok", "Tempat Lahir", "Tanggal Lahir",
					"Foto Latar Merah", "Alamat",
					"Bebas Pendaftaran", "Bebas Syarat Ujian", "Saldo");

			List<Object[]> rows = jdbc.sql(SQL)
					.params(classId, classId, tier, tier)
					.query((rs, n) -> new Object[] {
							rs.getString("nim"), rs.getString("name"), rs.getString("kelas"),
							rs.getString("golongan"), rs.getString("start_academic_year"),
							rs.getString("start_term"), rs.getString("phone"),
							rs.getBoolean("active") ? "Aktif" : "Nonaktif",
							rs.getString("dokumen_lengkap"), rs.getString("langkah_berikutnya"),
							rs.getString("nik"), rs.getString("ktp_cocok"),
							rs.getString("kk_number"), rs.getString("kk_cocok"),
							rs.getString("ijazah_cocok"), rs.getString("birth_place"),
							rs.getString("birth_date"), rs.getString("foto_merah"),
							rs.getString("address"),
							rs.getBoolean("pendaftaran_exempt") ? "Ya" : "-",
							rs.getBoolean("ujian_exempt") ? "Ya" : "-",
							rs.getBigDecimal("wallet_balance") })
					.list();

			int r = 1;
			for (Object[] row : rows) {
				Row baris = sheet.createRow(r++);
				for (int i = 0; i < row.length; i++) {
					baris.createCell(i).setCellValue(
							row[i] == null ? "-" : String.valueOf(row[i]));
				}
			}

			for (int i = 0; i < 22; i++) {
				sheet.autoSizeColumn(i);
			}

			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Gagal menyusun ekspor mahasiswa.", e);
		}
	}

	private void tulisHeader(Sheet sheet, CellStyle style, String... judul) {
		Row row = sheet.createRow(0);
		for (int i = 0; i < judul.length; i++) {
			var cell = row.createCell(i);
			cell.setCellValue(judul[i]);
			cell.setCellStyle(style);
		}
		sheet.createFreezePane(0, 1);
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

	/**
	 * Kolom kecocokan dibedakan tiga keadaan, bukan ya/tidak: berkasnya belum
	 * ada ("-"), sudah ada tapi belum sempat dibaca OCR ("Belum terbaca"), atau
	 * sudah dibaca dan hasilnya cocok atau tidak. Menyamakan "belum terbaca"
	 * dengan "tidak cocok" akan mengirim admin memeriksa berkas yang sebenarnya
	 * tidak bermasalah.
	 */
	private static final String SQL = """
			SELECT s.nim,
			       s.name,
			       COALESCE(c.name, '-')                       AS kelas,
			       COALESCE(d.label, s.discount_tier)          AS golongan,
			       s.start_academic_year,
			       s.start_term::text                          AS start_term,
			       COALESCE(s.phone, '-')                      AS phone,
			       s.active,
			       s.pendaftaran_exempt,
			       s.ujian_exempt,
			       s.wallet_balance,
			       COALESCE(s.nik, '-')                        AS nik,
			       COALESCE(s.kk_number, '-')                  AS kk_number,
			       COALESCE(s.birth_place, '-')                AS birth_place,
			       COALESCE(to_char(s.birth_date, 'YYYY-MM-DD'), '-') AS birth_date,
			       COALESCE(s.address, '-')                    AS address,

			       CASE WHEN s.profile_picture IS NOT NULL
			             AND s.nik IS NOT NULL AND s.ktp_file_path IS NOT NULL
			             AND s.kk_number IS NOT NULL AND s.kk_file_path IS NOT NULL
			             AND s.ijazah_file_path IS NOT NULL
			             AND s.address IS NOT NULL
			            THEN 'Ya' ELSE 'Belum' END             AS dokumen_lengkap,

			       CASE WHEN s.profile_picture IS NULL                     THEN 'Foto'
			            WHEN s.nik IS NULL OR s.ktp_file_path IS NULL      THEN 'KTP'
			            WHEN s.kk_number IS NULL OR s.kk_file_path IS NULL THEN 'Kartu Keluarga'
			            WHEN s.ijazah_file_path IS NULL                    THEN 'Ijazah'
			            WHEN s.address IS NULL                             THEN 'Alamat'
			            ELSE '-' END                           AS langkah_berikutnya,

			       CASE WHEN s.ktp_file_path IS NULL THEN '-'
			            WHEN s.ktp_ocr_data IS NULL   THEN 'Belum terbaca'
			            WHEN s.nik IS NOT NULL
			             AND s.ktp_ocr_data ->> 'nik' = s.nik THEN 'Cocok'
			            ELSE 'Tidak cocok' END                 AS ktp_cocok,

			       CASE WHEN s.kk_file_path IS NULL THEN '-'
			            WHEN s.kk_ocr_data IS NULL   THEN 'Belum terbaca'
			            WHEN s.kk_number IS NOT NULL
			             AND s.kk_ocr_data ->> 'kk_number' = s.kk_number THEN 'Cocok'
			            ELSE 'Tidak cocok' END                 AS kk_cocok,

			       CASE WHEN s.ijazah_file_path IS NULL THEN '-'
			            WHEN s.ijazah_ocr_data IS NULL   THEN 'Belum terbaca'
			            WHEN (s.ijazah_ocr_data ->> 'name_match')::boolean THEN 'Cocok'
			            ELSE 'Tidak cocok' END                 AS ijazah_cocok,

			       CASE WHEN s.profile_picture IS NULL          THEN '-'
			            WHEN s.profile_picture_analysis IS NULL THEN 'Belum terbaca'
			            WHEN (s.profile_picture_analysis ->> 'red_background')::boolean THEN 'Ya'
			            ELSE 'Tidak' END                        AS foto_merah

			FROM students s
			LEFT JOIN study_classes c        ON c.id = s.study_class_id
			LEFT JOIN discount_tier_rates d  ON d.tier = s.discount_tier
			WHERE (CAST(? AS BIGINT) IS NULL OR s.study_class_id = CAST(? AS BIGINT))
			  AND (CAST(? AS TEXT) IS NULL OR s.discount_tier = CAST(? AS TEXT))
			ORDER BY c.name NULLS LAST, s.name
			""";
}
