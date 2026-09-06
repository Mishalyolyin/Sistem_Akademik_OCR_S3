package ac.kampus.pembayaran.report;

import ac.kampus.pembayaran.TestcontainersConfiguration;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kedua ekspor Excel dijalankan ke PostgreSQL sungguhan.
 *
 * <p>Query-nya memakai hal yang hanya ada di Postgres: {@code FILTER}, operator
 * {@code jsonb}, cast enum ke teks, dan penyaring opsional lewat
 * {@code CAST(? AS BIGINT) IS NULL}. Semua itu tidak tersentuh oleh test yang
 * me-mock JdbcClient — query yang salah ketik lolos mulus di sana dan baru
 * meledak sebagai 500 di layar admin. Pelajaran itu sudah dibayar sekali:
 * statistik OCR sempat lolos seluruh test satuan padahal kolom {@code status}-nya
 * ambigu dan PostgreSQL menolaknya.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ExcelExportIntegrationTest {

	@Autowired
	private PaymentReportExporter paymentExporter;
	@Autowired
	private StudentDataExporter studentExporter;
	@Autowired
	private JdbcClient jdbc;

	private Long kelasId;

	@BeforeEach
	void siapkanData() {
		Long adminId = jdbc.sql("""
						INSERT INTO users (name, email, password_hash, role)
						VALUES ('Admin Ekspor', 'admin.ekspor@kampus.ac.id', 'x', 'ADMIN')
						RETURNING id
						""")
				.query(Long.class).single();

		kelasId = jdbc.sql("""
						INSERT INTO study_classes (name, academic_year)
						VALUES ('Kelas Uji Ekspor', '2026/2027')
						RETURNING id
						""")
				.query(Long.class).single();

		Long mahasiswaId = jdbc.sql("""
						INSERT INTO students
						    (nim, name, study_class_id, start_term, start_academic_year,
						     nik, ktp_file_path, ktp_ocr_data, ijazah_file_path, ijazah_ocr_data)
						VALUES ('D9100001', 'Mahasiswa Ekspor', ?, 'GASAL', '2026/2027',
						        '3374010101900001', 'dok/ktp.png',
						        CAST('{"nik": "3374010101900001"}' AS JSONB),
						        'dok/ijazah.png',
						        CAST('{"name_match": false}' AS JSONB))
						RETURNING id
						""")
				.param(kelasId)
				.query(Long.class).single();

		// Satu semester UKT dengan lima angsuran; angsuran 1 dan 2 lunas.
		Long planId = jdbc.sql("""
						INSERT INTO payment_plans
						    (student_id, category, academic_year, term, semester_number,
						     base_amount, total_amount)
						VALUES (?, 'UKT', '2026/2027', 'GASAL', 1, 10000000, 10000000)
						RETURNING id
						""")
				.param(mahasiswaId)
				.query(Long.class).single();

		List<Long> cicilan = new ArrayList<>();
		for (int no = 1; no <= 5; no++) {
			cicilan.add(jdbc.sql("""
							INSERT INTO installments
							    (payment_plan_id, installment_no, due_date, amount, amount_paid, status)
							VALUES (?, ?, DATE '2026-09-10' + (? * INTERVAL '1 month'),
							        2000000, CAST(? AS NUMERIC), CAST(? AS installment_status))
							RETURNING id
							""")
					.params(planId, no, no - 1,
							no <= 2 ? "2000000" : "0",
							no <= 2 ? "PAID" : "UNPAID")
					.query(Long.class).single());
		}

		for (int i = 0; i < 2; i++) {
			jdbc.sql("""
							INSERT INTO payments
							    (student_id, payment_plan_id, installment_id, amount,
							     proof_file_path, status, verified_at, verified_by)
							VALUES (?, ?, ?, 2000000, 'bukti/uji.png', 'VERIFIED', now(), ?)
							""")
					.params(mahasiswaId, planId, cicilan.get(i), adminId)
					.update();
		}
	}

	private List<String> namaLembar(byte[] xlsx) throws Exception {
		try (var wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
			List<String> nama = new ArrayList<>();
			for (int i = 0; i < wb.getNumberOfSheets(); i++) {
				nama.add(wb.getSheetName(i));
			}
			return nama;
		}
	}

	@Test
	@DisplayName("format transaksi menghasilkan tiga lembar rekap")
	void formatTransaksi() throws Exception {
		byte[] xlsx = paymentExporter.ledgerMahasiswa(PaymentReportExporter.Filter.SEMUA);

		assertThat(namaLembar(xlsx)).containsExactly(
				"Ringkasan per Kelas", "Tagihan per Mahasiswa", "Riwayat Pembayaran");
	}

	@Test
	@DisplayName("format termin menghasilkan satu lembar per semester UKT yang ada")
	void formatTermin() throws Exception {
		byte[] xlsx = paymentExporter.ledgerMahasiswa(new PaymentReportExporter.Filter(
				null, null, PaymentReportExporter.Format.TERMIN));

		// Hanya semester 1 yang punya tagihan, jadi hanya satu lembar.
		assertThat(namaLembar(xlsx)).containsExactly("UKT Semester 1");
	}

	@Test
	@DisplayName("ledger termin mengisi kolom termin yang lunas dan mengosongkan sisanya")
	void terminTerisi() throws Exception {
		byte[] xlsx = paymentExporter.ledgerMahasiswa(new PaymentReportExporter.Filter(
				kelasId, null, PaymentReportExporter.Format.TERMIN));

		try (var wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
			Sheet sheet = wb.getSheet("UKT Semester 1");
			Row baris = sheet.getRow(1);

			assertThat(baris.getCell(1).getStringCellValue()).isEqualTo("D9100001");
			assertThat(baris.getCell(5).getNumericCellValue()).isEqualTo(10_000_000d);

			// Termin 1 dan 2 terisi (kolom jumlah di 7 dan 9).
			assertThat(baris.getCell(7).getNumericCellValue()).isEqualTo(2_000_000d);
			assertThat(baris.getCell(9).getNumericCellValue()).isEqualTo(2_000_000d);

			// Termin 3 dibiarkan KOSONG, bukan nol: nol di kolom uang terbaca
			// sebagai "sudah dicatat, nihil".
			assertThat(baris.getCell(11).getStringCellValue()).isEmpty();

			int akhir = 6 + 5 * 2;
			assertThat(baris.getCell(akhir).getNumericCellValue()).isEqualTo(4_000_000d);
			assertThat(baris.getCell(akhir + 1).getNumericCellValue()).isEqualTo(6_000_000d);
			assertThat(baris.getCell(akhir + 2).getStringCellValue()).isEqualTo("BELUM LUNAS");
		}
	}

	@Test
	@DisplayName("penyaring kelas yang tidak cocok menghasilkan lembar kosong yang menjelaskan diri")
	void terminKelasKosong() throws Exception {
		byte[] xlsx = paymentExporter.ledgerMahasiswa(new PaymentReportExporter.Filter(
				999_999L, null, PaymentReportExporter.Format.TERMIN));

		// Workbook tanpa satu lembar pun tidak bisa ditulis POI, dan berkas
		// rusak lebih membingungkan daripada berkas kosong yang menerangkan diri.
		assertThat(namaLembar(xlsx)).containsExactly("Kosong");
	}

	@Test
	@DisplayName("penyaring status menyaring lembar riwayat pembayaran")
	void penyaringStatus() throws Exception {
		byte[] semua = paymentExporter.ledgerMahasiswa(
				new PaymentReportExporter.Filter(null, null, null));
		byte[] ditolak = paymentExporter.ledgerMahasiswa(
				new PaymentReportExporter.Filter(null, "REJECTED", null));

		assertThat(barisRiwayat(semua)).isEqualTo(2);
		assertThat(barisRiwayat(ditolak)).isZero();
	}

	private int barisRiwayat(byte[] xlsx) throws Exception {
		try (var wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
			return wb.getSheet("Riwayat Pembayaran").getLastRowNum();
		}
	}

	@Test
	@DisplayName("ekspor mahasiswa membedakan cocok, tidak cocok, dan belum terbaca")
	void eksporMahasiswa() throws Exception {
		byte[] xlsx = studentExporter.dataMahasiswa(kelasId, null);

		try (var wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
			Sheet sheet = wb.getSheet("Data Mahasiswa");
			Row baris = sheet.getRow(1);

			assertThat(baris.getCell(0).getStringCellValue()).isEqualTo("D9100001");
			assertThat(baris.getCell(2).getStringCellValue()).isEqualTo("Kelas Uji Ekspor");
			// KTP terbaca dan NIK-nya sama.
			assertThat(baris.getCell(11).getStringCellValue()).isEqualTo("Cocok");
			// Ijazah terbaca tapi namanya tidak cocok.
			assertThat(baris.getCell(14).getStringCellValue()).isEqualTo("Tidak cocok");
			// KK belum diunggah sama sekali — dibedakan dari "belum terbaca".
			assertThat(baris.getCell(13).getStringCellValue()).isEqualTo("-");
			// Dokumen belum lengkap: foto, KK, dan alamat belum ada.
			assertThat(baris.getCell(8).getStringCellValue()).isEqualTo("Belum");
			assertThat(baris.getCell(9).getStringCellValue()).isEqualTo("Foto");
		}
	}

	@Test
	@DisplayName("penyaring golongan yang tidak cocok menghasilkan lembar tanpa baris data")
	void eksporMahasiswaGolonganKosong() throws Exception {
		byte[] xlsx = studentExporter.dataMahasiswa(null, "KERJASAMA");

		try (var wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
			assertThat(wb.getSheet("Data Mahasiswa").getLastRowNum()).isZero();
		}
	}
}
