package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Penurunan tahun akademik dan term tiap semester UKT.
 *
 * <p>Hitungan ini menggantikan angka yang sebelumnya diketik admin per tagihan.
 * Salah di sini berarti seluruh tagihan satu angkatan jatuh di tahun yang
 * keliru sekaligus — tanpa satu pun galat, karena tahun akademik apa pun tetap
 * tersimpan dengan sah.
 */
class SemesterUktTest {

	@Nested
	@DisplayName("Angkatan yang masuk Gasal")
	class MulaiGasal {

		@ParameterizedTest(name = "semester {0} jatuh di {1} {2}")
		@CsvSource({
				"1, 2026/2027, GASAL",
				"2, 2026/2027, GENAP",
				"3, 2027/2028, GASAL",
				"4, 2027/2028, GENAP",
				"5, 2028/2029, GASAL",
				"6, 2028/2029, GENAP",
		})
		void enamSemesterBerurutan(int semester, String tahun, AcademicTerm term) {
			SemesterUkt hasil = SemesterUkt.hitung("2026/2027", AcademicTerm.GASAL, semester);

			assertThat(hasil.academicYear()).isEqualTo(tahun);
			assertThat(hasil.term()).isEqualTo(term);
		}
	}

	@Nested
	@DisplayName("Angkatan yang masuk Genap")
	class MulaiGenap {

		@ParameterizedTest(name = "semester {0} jatuh di {1} {2}")
		@CsvSource({
				"1, 2026/2027, GENAP",
				"2, 2027/2028, GASAL",
				"3, 2027/2028, GENAP",
				"4, 2028/2029, GASAL",
				"5, 2028/2029, GENAP",
				"6, 2029/2030, GASAL",
		})
		void bergeserSetengahTahun(int semester, String tahun, AcademicTerm term) {
			// Angkatan Genap menghabiskan tiga tahun akademik yang berbeda dari
			// angkatan Gasal. Menurunkan tahunnya dari tanggal hari ini akan
			// salah untuk salah satu dari keduanya, apa pun pilihannya.
			SemesterUkt hasil = SemesterUkt.hitung("2026/2027", AcademicTerm.GENAP, semester);

			assertThat(hasil.academicYear()).isEqualTo(tahun);
			assertThat(hasil.term()).isEqualTo(term);
		}
	}

	@Nested
	@DisplayName("Sudah waktunya ditagihkan")
	class SudahWaktunya {

		private final SemesterUkt gasal2627 = new SemesterUkt("2026/2027", AcademicTerm.GASAL);
		private final SemesterUkt genap2627 = new SemesterUkt("2026/2027", AcademicTerm.GENAP);
		private final SemesterUkt gasal2728 = new SemesterUkt("2027/2028", AcademicTerm.GASAL);

		@Test
		@DisplayName("semester yang sedang berjalan sudah waktunya")
		void semesterBerjalan() {
			assertThat(gasal2627.sudahWaktunya(gasal2627)).isTrue();
			assertThat(genap2627.sudahWaktunya(genap2627)).isTrue();
		}

		@Test
		@DisplayName("semester yang sudah lewat juga, supaya yang tertinggal terkejar")
		void semesterLewat() {
			// Mahasiswa yang diimpor di tengah Genap melewatkan putaran Gasal.
			// Tanpa ini, semester pertamanya tidak akan pernah terbentuk.
			assertThat(gasal2627.sudahWaktunya(genap2627)).isTrue();
			assertThat(genap2627.sudahWaktunya(gasal2728)).isTrue();
			assertThat(gasal2627.sudahWaktunya(gasal2728)).isTrue();
		}

		@Test
		@DisplayName("semester yang belum tiba tidak ditagihkan lebih dulu")
		void semesterBelumTiba() {
			// Inilah yang menjaga aturan snapshot tarif: semester kelima tidak
			// boleh dibuat hari ini, kalau tidak tarif hari ini ikut terkunci
			// untuk yang baru dibayar dua tahun lagi.
			assertThat(genap2627.sudahWaktunya(gasal2627)).isFalse();
			assertThat(gasal2728.sudahWaktunya(genap2627)).isFalse();
			assertThat(gasal2728.sudahWaktunya(gasal2627)).isFalse();
		}
	}

	@Test
	@DisplayName("tahun akademik yang salah format ditolak dengan contoh yang benar")
	void formatSalah() {
		assertThatThrownBy(() -> SemesterUkt.hitung("2026", AcademicTerm.GASAL, 1))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("2026/2027");

		assertThatThrownBy(() -> SemesterUkt.hitung("dua ribu/dua ribu satu", AcademicTerm.GASAL, 1))
				.isInstanceOf(BusinessRuleException.class);

		assertThatThrownBy(() -> SemesterUkt.hitung(null, AcademicTerm.GASAL, 1))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("semester nol atau negatif ditolak sebagai kekeliruan pemrograman")
	void semesterTidakMasukAkal() {
		assertThatThrownBy(() -> SemesterUkt.hitung("2026/2027", AcademicTerm.GASAL, 0))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
