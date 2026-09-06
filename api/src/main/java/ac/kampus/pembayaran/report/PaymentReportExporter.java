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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Laporan Excel. Padanan kelas-kelas Export di Laravel, tapi datanya diambil
 * lewat SQL agregat supaya tidak memuat ribuan entitas ke memori hanya untuk
 * ditulis baris demi baris.
 */
@Component
@RequiredArgsConstructor
public class PaymentReportExporter {

	private final JdbcClient jdbc;

	/**
	 * Penyaring laporan. Nilai kosong berarti "semua" — sengaja begitu supaya
	 * laporan tanpa penyaring apa pun tetap satu jalur kode yang sama, bukan
	 * cabang tersendiri yang bisa menyimpang diam-diam dari yang tersaring.
	 *
	 * @param classId  batasi ke satu kelas
	 * @param status   batasi riwayat pembayaran ke satu status
	 * @param format   bentuk laporannya
	 */
	public record Filter(Long classId, String status, Format format) {

		public static final Filter SEMUA = new Filter(null, null, Format.TRANSAKSI);

		public Filter {
			format = format == null ? Format.TRANSAKSI : format;
			status = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
		}

		/** Potongan judul berkas yang menerangkan penyaringnya. */
		public String keterangan() {
			StringBuilder sb = new StringBuilder(format.name().toLowerCase());
			if (classId != null) sb.append("-kelas").append(classId);
			if (status != null) sb.append('-').append(status.toLowerCase());
			return sb.toString();
		}
	}

	public enum Format {
		/** Tiga lembar: ringkasan kelas, tagihan per kategori, riwayat pembayaran. */
		TRANSAKSI,
		/**
		 * Ledger termin: satu lembar per semester UKT, satu baris per mahasiswa,
		 * dengan sepasang kolom tanggal dan jumlah untuk tiap termin. Bentuk ini
		 * mengikuti ledger sistem S2 yang sudah dipakai bagian keuangan — di
		 * sana enam termin, di sini lima, sesuai angsuran UKT Program Doktor.
		 */
		TERMIN
	}

	/** Rekap tagihan seluruh mahasiswa: satu baris per mahasiswa per kategori. */
	@Transactional(readOnly = true)
	public byte[] ledgerMahasiswa() {
		return ledgerMahasiswa(Filter.SEMUA);
	}

	@Transactional(readOnly = true)
	public byte[] ledgerMahasiswa(Filter filter) {
		try (Workbook workbook = new XSSFWorkbook();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			if (filter.format() == Format.TERMIN) {
				tulisLedgerTermin(workbook, filter);
			} else {
				tulisRingkasan(workbook, filter);
				tulisTagihan(workbook, filter);
				tulisPembayaran(workbook, filter);
			}

			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Gagal menyusun laporan Excel.", e);
		}
	}

	private void tulisRingkasan(Workbook workbook, Filter filter) {
		Sheet sheet = workbook.createSheet("Ringkasan per Kelas");
		CellStyle header = headerStyle(workbook);
		CellStyle uang = uangStyle(workbook);

		tulisHeader(sheet, header,
				"Kelas", "Jumlah Mahasiswa", "Tertagih", "Terkumpul", "Sisa", "Persen Lunas");

		List<Object[]> rows = jdbc.sql("""
						SELECT COALESCE(c.name, 'Tanpa kelas')  AS kelas,
						       COUNT(DISTINCT s.id)             AS mahasiswa,
						       COALESCE(SUM(i.amount), 0)       AS tertagih,
						       COALESCE(SUM(i.amount_paid), 0)  AS terkumpul
						FROM students s
						LEFT JOIN study_classes c ON c.id = s.study_class_id
						LEFT JOIN payment_plans p ON p.student_id = s.id AND p.status <> 'CANCELLED'
						LEFT JOIN installments i ON i.payment_plan_id = p.id
						WHERE s.active
						  AND (CAST(? AS BIGINT) IS NULL OR s.study_class_id = CAST(? AS BIGINT))
						GROUP BY c.name
						ORDER BY kelas
						""")
				.params(filter.classId(), filter.classId())
				.query((rs, n) -> new Object[] {
						rs.getString("kelas"), rs.getLong("mahasiswa"),
						rs.getBigDecimal("tertagih"), rs.getBigDecimal("terkumpul") })
				.list();

		int r = 1;
		for (Object[] row : rows) {
			BigDecimal tertagih = (BigDecimal) row[2];
			BigDecimal terkumpul = (BigDecimal) row[3];

			Row baris = sheet.createRow(r++);
			baris.createCell(0).setCellValue((String) row[0]);
			baris.createCell(1).setCellValue((Long) row[1]);
			isiUang(baris, 2, tertagih, uang);
			isiUang(baris, 3, terkumpul, uang);
			isiUang(baris, 4, tertagih.subtract(terkumpul), uang);
			baris.createCell(5).setCellValue(persen(terkumpul, tertagih));
		}

		autoSize(sheet, 6);
	}

	private void tulisTagihan(Workbook workbook, Filter filter) {
		Sheet sheet = workbook.createSheet("Tagihan per Mahasiswa");
		CellStyle header = headerStyle(workbook);
		CellStyle uang = uangStyle(workbook);

		tulisHeader(sheet, header,
				"NIM", "Nama", "Kelas", "Golongan", "Kategori", "Semester",
				"Tahun Akademik", "Tarif Dasar", "Potongan %", "Total Tagihan",
				"Dibayar", "Sisa", "Status");

		List<Object[]> rows = jdbc.sql("""
						SELECT s.nim, s.name,
						       COALESCE(c.name, '-')            AS kelas,
						       s.discount_tier::text            AS golongan,
						       p.category::text                 AS kategori,
						       p.semester_number,
						       p.academic_year,
						       p.base_amount,
						       p.discount_percent,
						       COALESCE(SUM(i.amount), 0)       AS total,
						       COALESCE(SUM(i.amount_paid), 0)  AS dibayar,
						       p.status::text                   AS status
						FROM payment_plans p
						JOIN students s ON s.id = p.student_id
						LEFT JOIN study_classes c ON c.id = s.study_class_id
						LEFT JOIN installments i ON i.payment_plan_id = p.id
						WHERE p.status <> 'CANCELLED'
						  AND (CAST(? AS BIGINT) IS NULL OR s.study_class_id = CAST(? AS BIGINT))
						GROUP BY s.nim, s.name, c.name, s.discount_tier, p.id
						ORDER BY s.nim, p.category, p.semester_number
						""")
				.params(filter.classId(), filter.classId())
				.query((rs, n) -> new Object[] {
						rs.getString("nim"), rs.getString("name"), rs.getString("kelas"),
						rs.getString("golongan"), rs.getString("kategori"),
						rs.getObject("semester_number"), rs.getString("academic_year"),
						rs.getBigDecimal("base_amount"), rs.getBigDecimal("discount_percent"),
						rs.getBigDecimal("total"), rs.getBigDecimal("dibayar"),
						rs.getString("status") })
				.list();

		int r = 1;
		for (Object[] row : rows) {
			BigDecimal total = (BigDecimal) row[9];
			BigDecimal dibayar = (BigDecimal) row[10];

			Row baris = sheet.createRow(r++);
			baris.createCell(0).setCellValue((String) row[0]);
			baris.createCell(1).setCellValue((String) row[1]);
			baris.createCell(2).setCellValue((String) row[2]);
			baris.createCell(3).setCellValue((String) row[3]);
			baris.createCell(4).setCellValue((String) row[4]);
			if (row[5] != null) baris.createCell(5).setCellValue(((Number) row[5]).intValue());
			baris.createCell(6).setCellValue((String) row[6]);
			isiUang(baris, 7, (BigDecimal) row[7], uang);
			baris.createCell(8).setCellValue(((BigDecimal) row[8]).doubleValue());
			isiUang(baris, 9, total, uang);
			isiUang(baris, 10, dibayar, uang);
			isiUang(baris, 11, total.subtract(dibayar), uang);
			baris.createCell(12).setCellValue((String) row[11]);
		}

		autoSize(sheet, 13);
	}

	private void tulisPembayaran(Workbook workbook, Filter filter) {
		Sheet sheet = workbook.createSheet("Riwayat Pembayaran");
		CellStyle header = headerStyle(workbook);
		CellStyle uang = uangStyle(workbook);

		tulisHeader(sheet, header,
				"ID", "NIM", "Nama", "Kategori", "Cicilan", "Nominal", "Bank",
				"Tanggal Bukti", "Status", "Keyakinan OCR", "Diunggah");

		List<Object[]> rows = jdbc.sql("""
						SELECT pay.id, s.nim, s.name,
						       COALESCE(pl.category::text, '-')  AS kategori,
						       i.installment_no,
						       pay.amount,
						       COALESCE(pay.bank_name, '-')      AS bank,
						       pay.payment_proof_date,
						       pay.status::text                  AS status,
						       pay.ocr_confidence,
						       pay.created_at
						FROM payments pay
						JOIN students s ON s.id = pay.student_id
						LEFT JOIN payment_plans pl ON pl.id = pay.payment_plan_id
						LEFT JOIN installments i ON i.id = pay.installment_id
						WHERE (CAST(? AS BIGINT) IS NULL OR s.study_class_id = CAST(? AS BIGINT))
						  AND (CAST(? AS TEXT) IS NULL OR pay.status::text = CAST(? AS TEXT))
						ORDER BY pay.created_at DESC
						""")
				.params(filter.classId(), filter.classId(), filter.status(), filter.status())
				.query((rs, n) -> new Object[] {
						rs.getLong("id"), rs.getString("nim"), rs.getString("name"),
						rs.getString("kategori"), rs.getObject("installment_no"),
						rs.getBigDecimal("amount"), rs.getString("bank"),
						rs.getDate("payment_proof_date"), rs.getString("status"),
						rs.getBigDecimal("ocr_confidence"), rs.getTimestamp("created_at") })
				.list();

		int r = 1;
		for (Object[] row : rows) {
			Row baris = sheet.createRow(r++);
			baris.createCell(0).setCellValue((Long) row[0]);
			baris.createCell(1).setCellValue((String) row[1]);
			baris.createCell(2).setCellValue((String) row[2]);
			baris.createCell(3).setCellValue((String) row[3]);
			if (row[4] != null) baris.createCell(4).setCellValue(((Number) row[4]).intValue());
			isiUang(baris, 5, (BigDecimal) row[5], uang);
			baris.createCell(6).setCellValue((String) row[6]);
			baris.createCell(7).setCellValue(row[7] == null ? "-" : row[7].toString());
			baris.createCell(8).setCellValue((String) row[8]);
			if (row[9] != null) {
				baris.createCell(9).setCellValue(((BigDecimal) row[9]).doubleValue());
			}
			baris.createCell(10).setCellValue(row[10] == null ? "-" : row[10].toString());
		}

		autoSize(sheet, 11);
	}

	// --- Ledger termin ---

	/**
	 * Jumlah angsuran UKT per semester di Program Doktor. Ledger S2 memakai
	 * enam kolom termin karena di sana angsurannya empat kali per tahun; di
	 * sini lima, sesuai jadwal bulanan September–Januari dan Februari–Juni.
	 */
	private static final int TERMIN_PER_SEMESTER = 5;

	/** Batas semester UKT, sama dengan PaymentGenerationService.MAX_SEMESTER_UKT. */
	private static final int MAKS_SEMESTER = 6;

	/**
	 * Satu lembar per semester UKT: satu baris per mahasiswa, sepasang kolom
	 * tanggal dan jumlah untuk tiap termin.
	 *
	 * <p>Yang dihitung sebagai "dibayar" hanya pembayaran yang sudah
	 * diverifikasi. Bukti yang masih menunggu tidak boleh muncul sebagai uang
	 * masuk di ledger — ini lembar yang dipakai bagian keuangan untuk
	 * mencocokkan dengan rekening koran.
	 */
	private void tulisLedgerTermin(Workbook workbook, Filter filter) {
		CellStyle header = headerStyle(workbook);
		CellStyle uang = uangStyle(workbook);
		boolean adaIsi = false;

		for (int semester = 1; semester <= MAKS_SEMESTER; semester++) {
			List<Object[]> rows = barisLedger(filter, semester);
			if (rows.isEmpty()) {
				continue;
			}
			adaIsi = true;
			tulisLembarSemester(workbook, header, uang, semester, rows);
		}

		// Workbook tanpa satu lembar pun tidak bisa ditulis POI, dan berkas
		// rusak lebih membingungkan daripada berkas kosong yang menjelaskan diri.
		if (!adaIsi) {
			Sheet sheet = workbook.createSheet("Kosong");
			sheet.createRow(0).createCell(0).setCellValue(
					"Belum ada tagihan UKT yang cocok dengan penyaring ini.");
			sheet.autoSizeColumn(0);
		}
	}

	private void tulisLembarSemester(
			Workbook workbook, CellStyle header, CellStyle uang,
			int semester, List<Object[]> rows) {

		Sheet sheet = workbook.createSheet("UKT Semester " + semester);

		List<String> judul = new java.util.ArrayList<>(List.of(
				"No.", "NIM", "Nama", "Kelas", "Golongan", "Total UKT"));
		for (int t = 1; t <= TERMIN_PER_SEMESTER; t++) {
			judul.add("Tgl Termin " + t);
			judul.add("Jumlah Termin " + t);
		}
		judul.addAll(List.of("Dibayar", "Sisa", "Status"));
		tulisHeader(sheet, header, judul.toArray(String[]::new));

		int r = 1;
		for (Object[] row : rows) {
			Row baris = sheet.createRow(r);
			baris.createCell(0).setCellValue(r);
			baris.createCell(1).setCellValue((String) row[0]);
			baris.createCell(2).setCellValue((String) row[1]);
			baris.createCell(3).setCellValue((String) row[2]);
			baris.createCell(4).setCellValue((String) row[3]);

			BigDecimal total = (BigDecimal) row[4];
			isiUang(baris, 5, total, uang);

			@SuppressWarnings("unchecked")
			Map<Integer, Object[]> termin = (Map<Integer, Object[]>) row[5];

			BigDecimal dibayar = BigDecimal.ZERO;
			for (int t = 1; t <= TERMIN_PER_SEMESTER; t++) {
				int kolom = 6 + (t - 1) * 2;
				Object[] isi = termin.get(t);

				// Termin yang belum dibayar dibiarkan kosong, bukan diisi nol —
				// nol di kolom uang terbaca sebagai "sudah dicatat, nihil".
				if (isi == null || isi[1] == null) {
					baris.createCell(kolom).setCellValue("");
					baris.createCell(kolom + 1).setCellValue("");
					continue;
				}

				baris.createCell(kolom).setCellValue(isi[0] == null ? "-" : isi[0].toString());
				BigDecimal nominal = (BigDecimal) isi[1];
				isiUang(baris, kolom + 1, nominal, uang);
				dibayar = dibayar.add(nominal);
			}

			int kolomAkhir = 6 + TERMIN_PER_SEMESTER * 2;
			isiUang(baris, kolomAkhir, dibayar, uang);
			isiUang(baris, kolomAkhir + 1, total.subtract(dibayar), uang);
			baris.createCell(kolomAkhir + 2).setCellValue(
					dibayar.compareTo(total) >= 0 ? "LUNAS" : "BELUM LUNAS");
			r++;
		}

		autoSize(sheet, 6 + TERMIN_PER_SEMESTER * 2 + 3);
	}

	/**
	 * Satu baris per mahasiswa yang punya tagihan UKT di semester itu, beserta
	 * peta termin ke pasangan (tanggal bayar terakhir, jumlah terverifikasi).
	 */
	private List<Object[]> barisLedger(Filter filter, int semester) {
		Map<String, Object[]> perMahasiswa = new java.util.LinkedHashMap<>();

		jdbc.sql("""
						SELECT s.nim, s.name,
						       COALESCE(c.name, '-')          AS kelas,
						       s.discount_tier::text          AS golongan,
						       pl.total_amount,
						       i.installment_no,
						       COALESCE(SUM(pay.amount), 0)   AS dibayar,
						       MAX(pay.verified_at)           AS terakhir
						FROM payment_plans pl
						JOIN students s ON s.id = pl.student_id
						LEFT JOIN study_classes c ON c.id = s.study_class_id
						JOIN installments i ON i.payment_plan_id = pl.id
						LEFT JOIN payments pay
						       ON pay.installment_id = i.id
						      AND pay.status IN ('VERIFIED', 'AUTO_VERIFIED')
						WHERE pl.category = 'UKT'
						  AND pl.status <> 'CANCELLED'
						  AND pl.semester_number = CAST(? AS INTEGER)
						  AND (CAST(? AS BIGINT) IS NULL OR s.study_class_id = CAST(? AS BIGINT))
						GROUP BY s.nim, s.name, c.name, s.discount_tier,
						         pl.total_amount, i.installment_no
						ORDER BY s.name, i.installment_no
						""")
				.params(semester, filter.classId(), filter.classId())
				.query((rs, n) -> {
					String nim = rs.getString("nim");
					Object[] mahasiswa = perMahasiswa.computeIfAbsent(nim, k -> {
						try {
							return new Object[] {
									nim, rs.getString("name"), rs.getString("kelas"),
									rs.getString("golongan"), rs.getBigDecimal("total_amount"),
									new java.util.HashMap<Integer, Object[]>() };
						} catch (java.sql.SQLException e) {
							throw new IllegalStateException(e);
						}
					});

					BigDecimal dibayar = rs.getBigDecimal("dibayar");
					if (dibayar != null && dibayar.signum() > 0) {
						@SuppressWarnings("unchecked")
						Map<Integer, Object[]> termin = (Map<Integer, Object[]>) mahasiswa[5];
						var terakhir = rs.getTimestamp("terakhir");
						termin.put(rs.getInt("installment_no"), new Object[] {
								terakhir == null ? null : terakhir.toLocalDateTime().toLocalDate(),
								dibayar });
					}
					return nim;
				})
				.list();

		return List.copyOf(perMahasiswa.values());
	}

	// --- Pembantu format ---

	private void tulisHeader(Sheet sheet, CellStyle style, String... judul) {
		Row row = sheet.createRow(0);
		for (int i = 0; i < judul.length; i++) {
			var cell = row.createCell(i);
			cell.setCellValue(judul[i]);
			cell.setCellStyle(style);
		}
		sheet.createFreezePane(0, 1);
	}

	private void isiUang(Row row, int kolom, BigDecimal nilai, CellStyle style) {
		var cell = row.createCell(kolom);
		cell.setCellValue(nilai == null ? 0 : nilai.doubleValue());
		cell.setCellStyle(style);
	}

	private double persen(BigDecimal bagian, BigDecimal total) {
		if (total == null || total.signum() == 0) return 0;
		return bagian.doubleValue() / total.doubleValue() * 100;
	}

	private void autoSize(Sheet sheet, int kolom) {
		for (int i = 0; i < kolom; i++) {
			sheet.autoSizeColumn(i);
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

	private CellStyle uangStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
		return style;
	}
}
