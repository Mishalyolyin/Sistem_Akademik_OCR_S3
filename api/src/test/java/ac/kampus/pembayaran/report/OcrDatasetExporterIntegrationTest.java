package ac.kampus.pembayaran.report;

import ac.kampus.pembayaran.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Query dataset dijalankan ke PostgreSQL sungguhan.
 *
 * <p>Isinya bertumpu pada hal-hal yang hanya ada di Postgres — operator
 * {@code jsonb}, {@code jsonb_array_elements_text}, {@code LATERAL}, cast enum
 * ke teks. Semua itu tidak pernah tersentuh oleh test yang me-mock JdbcClient:
 * query yang salah ketik akan lolos mulus di sana dan baru meledak di layar
 * admin. Karena itu di sini databasenya asli.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class OcrDatasetExporterIntegrationTest {

	@Autowired
	private OcrDatasetExporter exporter;
	@Autowired
	private JdbcClient jdbc;

	private long idDiverifikasi;
	private long idDitolak;
	private long idOtomatis;
	private long idMenunggu;

	@BeforeEach
	void siapkanData() {
		Long adminId = jdbc.sql("""
						INSERT INTO users (name, email, password_hash, role)
						VALUES ('Admin Uji', 'admin.dataset@kampus.ac.id', 'x', 'ADMIN')
						RETURNING id
						""")
				.query(Long.class).single();

		Long kelasId = jdbc.sql("""
						INSERT INTO study_classes (name, academic_year)
						VALUES ('Kelas Uji Dataset', '2026/2027')
						RETURNING id
						""")
				.query(Long.class).single();

		Long mahasiswaId = jdbc.sql("""
						INSERT INTO students
						    (nim, name, study_class_id, start_term, start_academic_year)
						VALUES ('D9000001', 'Mahasiswa Uji', ?, 'GASAL', '2026/2027')
						RETURNING id
						""")
				.param(kelasId)
				.query(Long.class).single();

		Long planId = jdbc.sql("""
						INSERT INTO payment_plans
						    (student_id, category, academic_year, term, semester_number,
						     base_amount, total_amount)
						VALUES (?, 'UKT', '2026/2027', 'GASAL', 1, 10000000, 10000000)
						RETURNING id
						""")
				.param(mahasiswaId)
				.query(Long.class).single();

		Long cicilanId = jdbc.sql("""
						INSERT INTO installments
						    (payment_plan_id, installment_no, due_date, amount)
						VALUES (?, 1, DATE '2026-09-10', 2000000)
						RETURNING id
						""")
				.param(planId)
				.query(Long.class).single();

		// Diputuskan admin. Flag lengkap, dan nominal yang terbaca berbeda dari
		// yang diklaim — persis kasus yang paling ingin dipelajari model.
		idDiverifikasi = simpanPembayaran(mahasiswaId, planId, cicilanId, adminId,
				"VERIFIED", "1000000.00", """
						{
						  "raw_text": "TRANSFER BERHASIL\\nRp 1.200.000\\n7 1234 5678 90",
						  "extracted_amount": 1200000,
						  "extracted_date": "2026-09-08",
						  "bank_name": "BSI",
						  "verification_status": "needs_review",
						  "flags": ["Destination account matched: 7 1234 5678 90",
						            "Student name found: Mahasiswa Uji",
						            "Selisih nominal: diklaim 1000000.00, terbaca 1200000.00"]
						}
						""", null);

		// Ditolak admin, dan ocr_data-nya TIDAK punya kunci flags sama sekali —
		// bentuk yang mungkin datang dari bukti lama atau pembacaan yang gagal.
		idDitolak = simpanPembayaran(mahasiswaId, planId, cicilanId, adminId,
				"REJECTED", "500000.00", """
						{"raw_text": "gambar buram", "verification_status": "needs_review"}
						""", "Bukti tidak terbaca.");

		// Keputusan mesin murni: tidak boleh masuk dataset.
		idOtomatis = simpanPembayaran(mahasiswaId, planId, cicilanId, null,
				"AUTO_VERIFIED", "2000000.00", """
						{"extracted_amount": 2000000, "flags": []}
						""", null);

		// Belum diputuskan siapa pun: juga tidak boleh masuk.
		idMenunggu = simpanPembayaran(mahasiswaId, planId, cicilanId, null,
				"PENDING", "2000000.00", null, null);

		// Mesin sempat mengusulkan AUTO_VERIFIED untuk keduanya. Admin setuju
		// pada yang pertama dan tidak setuju pada yang kedua.
		catatanMesin(idDiverifikasi);
		catatanMesin(idDitolak);
	}

	private long simpanPembayaran(Long mahasiswaId, Long planId, Long cicilanId, Long adminId,
			String status, String nominal, String ocrData, String alasanTolak) {
		return jdbc.sql("""
						INSERT INTO payments
						    (student_id, payment_plan_id, installment_id, amount,
						     proof_file_path, status, ocr_data, ocr_confidence,
						     verified_at, verified_by, reject_reason)
						VALUES (?, ?, ?, CAST(? AS NUMERIC), 'bukti/uji.png',
						        CAST(? AS payment_status), CAST(? AS JSONB), 0.8500,
						        CASE WHEN CAST(? AS BIGINT) IS NULL THEN NULL ELSE now() END,
						        CAST(? AS BIGINT), CAST(? AS VARCHAR))
						RETURNING id
						""")
				.params(mahasiswaId, planId, cicilanId, nominal, status, ocrData,
						adminId, adminId, alasanTolak)
				.query(Long.class).single();
	}

	/** Baris log tanpa admin_id: itulah penanda keputusan otomatis. */
	private void catatanMesin(long paymentId) {
		jdbc.sql("""
						INSERT INTO verification_logs (payment_id, to_status, admin_id, note)
						VALUES (?, 'AUTO_VERIFIED', NULL, 'Keyakinan 85%')
						""")
				.param(paymentId)
				.update();
	}

	private List<String> barisDataset(boolean sertakanTeks) {
		String csv = new String(exporter.datasetOcr(sertakanTeks), StandardCharsets.UTF_8);
		return List.of(csv.split("\n", -1));
	}

	@Test
	@DisplayName("hanya bukti yang diputuskan manusia yang masuk dataset")
	void hanyaKeputusanManusia() {
		List<String> baris = barisDataset(false);

		// Header + dua baris + baris kosong penutup.
		assertThat(baris).hasSize(4);
		assertThat(baris.get(1)).startsWith(idDiverifikasi + ",");
		assertThat(baris.get(2)).startsWith(idDitolak + ",");

		String semua = String.join("\n", baris);
		// Tebakan mesin dan bukti yang belum diputuskan tidak boleh jadi label.
		assertThat(semua).doesNotContain("\n" + idOtomatis + ",");
		assertThat(semua).doesNotContain("\n" + idMenunggu + ",");
	}

	@Test
	@DisplayName("kolom turunan dari flags terisi benar")
	void kolomTurunan() {
		String baris = barisDataset(false).get(1);

		assertThat(baris).contains("UKT").contains("Kelas Uji Dataset");
		// Rekening dan nama sama-sama ketemu di bukti.
		assertThat(baris).contains(",true,true,");
		// Diklaim 1.000.000, terbaca 1.200.000.
		assertThat(baris).contains("-200000.00");
		// Mesin mengusulkan AUTO_VERIFIED dan admin memang memverifikasi.
		assertThat(baris).contains("AUTO_VERIFIED,true,VERIFIED");
	}

	@Test
	@DisplayName("ocr_data tanpa kunci flags tidak menggagalkan query")
	void tanpaKunciFlags() {
		String baris = barisDataset(false).get(2);

		// Nol flag, tidak ada yang cocok, dan tetap satu baris utuh — bukan galat.
		assertThat(baris).contains(",0,false,false,");
		assertThat(baris).endsWith(",Bukti tidak terbaca.");
	}

	@Test
	@DisplayName("ketidaksepakatan admin dengan mesin ikut tercatat")
	void mesinTidakSepakat() {
		String baris = barisDataset(false).get(2);

		// Mesin mengusulkan AUTO_VERIFIED, admin menolak.
		assertThat(baris).contains("AUTO_VERIFIED,false,REJECTED");
	}

	@Test
	@DisplayName("teks mentah tidak ikut secara bawaan")
	void teksMentahTidakIkut() {
		List<String> baris = barisDataset(false);

		assertThat(baris.getFirst()).doesNotContain("ocr_teks_mentah");
		assertThat(String.join("\n", baris)).doesNotContain("TRANSFER BERHASIL");
	}

	@Test
	@DisplayName("teks mentah ikut bila diminta, dan barisnya tidak pecah")
	void teksMentahIkut() {
		String csv = new String(exporter.datasetOcr(true), StandardCharsets.UTF_8);

		assertThat(csv.lines().findFirst().orElseThrow()).endsWith("ocr_teks_mentah");
		assertThat(csv).contains("TRANSFER BERHASIL");
		// Teks strukturnya berbaris-baris; kalau lupa dikutip, satu bukti akan
		// pecah jadi beberapa baris CSV dan dataset ikut rusak.
		assertThat(csv).contains("\"TRANSFER BERHASIL\nRp 1.200.000\n7 1234 5678 90\"");
	}
}
