package ac.kampus.pembayaran.dashboard;

import ac.kampus.pembayaran.TestcontainersConfiguration;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.report.StudentPhotoExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Query dashboard per kelas dijalankan ke PostgreSQL sungguhan.
 *
 * <p>Query ini memakai CTE bertingkat, {@code COUNT(*) FILTER}, dan cast enum
 * ke teks — semuanya tidak tersentuh test yang me-mock JdbcClient. Yang paling
 * ingin dibuktikan di sini bukan sekadar "query-nya jalan", melainkan bahwa
 * angka uangnya <b>tidak menggelembung</b>: menggabungkan cicilan dan
 * pembayaran dalam satu FROM akan menggandakan tiap cicilan sebanyak pembayaran
 * yang menyentuhnya, dan "tertagih" ikut membesar tanpa satu pun galat yang
 * memberi tahu.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class DashboardQueryIntegrationTest {

	@Autowired
	private DashboardRepository repository;
	@Autowired
	private StudentPhotoExporter photoExporter;
	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void siapkanData() {
		Long adminId = jdbc.sql("""
						INSERT INTO users (name, email, password_hash, role)
						VALUES ('Admin Dash', 'admin.dash@kampus.ac.id', 'x', 'ADMIN')
						RETURNING id
						""")
				.query(Long.class).single();

		Long kelasId = jdbc.sql("""
						INSERT INTO study_classes (name, academic_year)
						VALUES ('Kelas Dash', '2026/2027')
						RETURNING id
						""")
				.query(Long.class).single();

		Long mahasiswaId = jdbc.sql("""
						INSERT INTO students
						    (nim, name, study_class_id, start_term, start_academic_year,
						     profile_picture)
						VALUES ('D9200001', 'Mahasiswa Dash', ?, 'GASAL', '2026/2027',
						        'foto/tidak-ada.jpg')
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
						    (payment_plan_id, installment_no, due_date, amount, amount_paid, status)
						VALUES (?, 1, DATE '2026-09-10', 2000000, 2000000, 'PAID')
						RETURNING id
						""")
				.param(planId)
				.query(Long.class).single();

		// TIGA pembayaran menyentuh SATU cicilan. Inilah bentuk yang membuat
		// JOIN berantai menggandakan angka uangnya jadi tiga kali lipat.
		for (int i = 0; i < 3; i++) {
			jdbc.sql("""
							INSERT INTO payments
							    (student_id, payment_plan_id, installment_id, amount,
							     proof_file_path, status, ocr_data, ocr_confidence,
							     verified_at, verified_by)
							VALUES (?, ?, ?, 2000000, 'bukti/uji.png',
							        CAST(? AS payment_status),
							        CAST('{"confidence": 0.9}' AS JSONB), 0.9000,
							        now(), ?)
							""")
					.params(mahasiswaId, planId, cicilanId,
							i == 0 ? "VERIFIED" : (i == 1 ? "NEEDS_REVIEW" : "FAILED"),
							adminId)
					.update();
		}
	}

	private DashboardController.KelasRingkas kelasDash() {
		List<DashboardController.KelasRingkas> semua = repository.summaryByClass();
		return semua.stream()
				.filter(k -> "Kelas Dash".equals(k.kelas()))
				.findFirst()
				.orElseThrow();
	}

	@Test
	@DisplayName("angka uang tidak menggelembung walau satu cicilan disentuh banyak pembayaran")
	void uangTidakBerlipat() {
		var kelas = kelasDash();

		assertThat(kelas.jumlahMahasiswa()).isEqualTo(1);
		// Satu cicilan Rp 2.000.000, bukan tiga kali lipat.
		assertThat(kelas.tertagih()).isEqualByComparingTo("2000000");
		assertThat(kelas.terkumpul()).isEqualByComparingTo("2000000");
	}

	@Test
	@DisplayName("angka OCR per kelas terhitung dari status pembayarannya")
	void angkaOcr() {
		var kelas = kelasDash();

		assertThat(kelas.buktiDibaca()).isEqualTo(3);
		assertThat(kelas.buktiPerluDitinjau()).isEqualTo(1);
		assertThat(kelas.buktiGagalDibaca()).isEqualTo(1);
		assertThat(kelas.rataKeyakinan()).isEqualByComparingTo(new BigDecimal("0.9000"));
	}

	@Test
	@DisplayName("kelas tanpa pembayaran tetap muncul dengan angka OCR nol, bukan hilang")
	void kelasTanpaPembayaran() {
		jdbc.sql("""
						INSERT INTO study_classes (name, academic_year)
						VALUES ('Kelas Sepi', '2026/2027')
						""")
				.update();
		jdbc.sql("""
						INSERT INTO students (nim, name, study_class_id, start_term,
						                      start_academic_year)
						SELECT 'D9200003', 'Mahasiswa Sepi', id, 'GASAL', '2026/2027'
						FROM study_classes WHERE name = 'Kelas Sepi'
						""")
				.update();

		var sepi = repository.summaryByClass().stream()
				.filter(k -> "Kelas Sepi".equals(k.kelas()))
				.findFirst()
				.orElseThrow();

		// LEFT JOIN yang keliru akan membuat kelas ini hilang sama sekali dari
		// dashboard — dan kelas yang belum menagih apa pun justru yang paling
		// perlu terlihat.
		assertThat(sepi.jumlahMahasiswa()).isEqualTo(1);
		assertThat(sepi.buktiDibaca()).isZero();
		assertThat(sepi.rataKeyakinan()).isNull();
	}

	@Test
	@DisplayName("ekspor foto menolak pilihan yang tidak menghasilkan satu foto pun")
	void fotoKelasKosong() {
		// Berkas foto memang tidak ada di penyimpanan, tapi barisnya ada — jadi
		// yang diuji di sini query-nya, bukan pembacaan berkasnya.
		assertThatThrownBy(() -> photoExporter.fotoKelas(999_999L))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Tidak ada foto");
	}

	@Test
	@DisplayName("ekspor foto tetap menghasilkan ZIP dan mencatat foto yang berkasnya raib")
	void fotoBerkasHilang() throws Exception {
		byte[] zip = photoExporter.fotoKelas(null);

		var isi = new java.util.ArrayList<String>();
		try (var in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
			for (var e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
				isi.add(e.getName());
			}
		}

		// Foto tercatat di database tapi berkasnya tidak ada. Itu tidak boleh
		// hilang diam-diam: yang menerima ZIP perlu tahu siapa yang fotonya
		// belum ada, bukan menghitung sendiri isinya.
		assertThat(isi).contains("FOTO-HILANG.txt");
	}
}
