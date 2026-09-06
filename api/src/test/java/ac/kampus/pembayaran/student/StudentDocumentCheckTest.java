package ac.kampus.pembayaran.student;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ringkasan hasil pembacaan dokumen.
 *
 * <p>Yang diringkas bukan asli atau palsu — Tesseract tidak bisa memeriksa
 * hologram maupun tanda tangan. Yang diringkas adalah kecocokan antara yang
 * terbaca dan yang diketik, dan bedanya penting: <b>tidak cocok</b> berarti
 * layak dilihat manusia, sementara <b>tidak bisa disimpulkan</b> berarti
 * mesinnya yang tidak berhasil membaca. Menyamakan keduanya membuat admin
 * curiga pada dokumen yang sebenarnya baik-baik saja.
 */
class StudentDocumentCheckTest {

	private static Student mahasiswa() {
		return Student.builder()
				.id(1L).nim("2612600001").name("Budi Santoso")
				.discountTier("NON_ALUMNI").walletBalance(BigDecimal.ZERO).active(true)
				.build();
	}

	private static Map<String, Object> ocr(Object... pasangan) {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < pasangan.length; i += 2) {
			map.put(String.valueOf(pasangan[i]), pasangan[i + 1]);
		}
		return map;
	}

	private static StudentDocumentCheck.Hasil ambil(Student student, String jenis) {
		return StudentDocumentCheck.untuk(student).stream()
				.filter(h -> h.jenis().equals(jenis))
				.findFirst()
				.orElseThrow();
	}

	@Test
	@DisplayName("keempat dokumen selalu dilaporkan, termasuk yang belum diunggah")
	void keempatnyaSelaluAda() {
		List<StudentDocumentCheck.Hasil> hasil = StudentDocumentCheck.untuk(mahasiswa());

		assertThat(hasil).extracting(StudentDocumentCheck.Hasil::jenis)
				.containsExactly("foto", "ktp", "kk", "ijazah");
		assertThat(hasil).allMatch(h -> !h.adaBerkas() && !h.sudahDibaca());
		assertThat(hasil).allMatch(h -> h.keterangan().contains("Belum diunggah"));
	}

	@Test
	@DisplayName("berkas yang ada tapi belum terbaca dibedakan dari yang belum diunggah")
	void sudahDiunggahBelumDibaca() {
		Student student = mahasiswa();
		student.setKtpFilePath("dokumen/ktp/a.png");

		StudentDocumentCheck.Hasil hasil = ambil(student, "ktp");

		assertThat(hasil.adaBerkas()).isTrue();
		assertThat(hasil.sudahDibaca()).isFalse();
		assertThat(hasil.cocok()).isNull();
		assertThat(hasil.keterangan()).contains("Belum dibaca");
	}

	@Nested
	@DisplayName("KTP")
	class Ktp {

		@Test
		@DisplayName("NIK yang sama dianggap cocok walau pemisahnya berbeda")
		void nikCocok() {
			Student student = mahasiswa();
			student.setKtpFilePath("dokumen/ktp/a.png");
			student.setNik("3201010101010001");
			student.setKtpOcrData(ocr("nik", "3201 0101 0101 0001"));

			StudentDocumentCheck.Hasil hasil = ambil(student, "ktp");

			assertThat(hasil.cocok()).isTrue();
			assertThat(hasil.keterangan()).contains("cocok");
		}

		@Test
		@DisplayName("NIK yang berbeda menyebutkan kedua angkanya sekaligus")
		void nikBeda() {
			Student student = mahasiswa();
			student.setKtpFilePath("dokumen/ktp/a.png");
			student.setNik("3201010101010001");
			student.setKtpOcrData(ocr("nik", "3201010101019999"));

			StudentDocumentCheck.Hasil hasil = ambil(student, "ktp");

			assertThat(hasil.cocok()).isFalse();
			assertThat(hasil.keterangan())
					.contains("3201010101019999")
					.contains("3201010101010001");
		}

		@Test
		@DisplayName("NIK yang tidak terbaca bukan berarti tidak cocok")
		void nikTidakTerbaca() {
			Student student = mahasiswa();
			student.setKtpFilePath("dokumen/ktp/a.png");
			student.setNik("3201010101010001");
			student.setKtpOcrData(ocr("nik", null));

			StudentDocumentCheck.Hasil hasil = ambil(student, "ktp");

			// null, bukan false: mesinnya yang gagal membaca, bukan dokumennya
			// yang mencurigakan.
			assertThat(hasil.cocok()).isNull();
			assertThat(hasil.sudahDibaca()).isTrue();
		}
	}

	@Nested
	@DisplayName("Kartu Keluarga")
	class KartuKeluarga {

		@Test
		@DisplayName("nomor cocok dan nama ada di daftar keluarga")
		void semuaCocok() {
			Student student = mahasiswa();
			student.setKkFilePath("dokumen/kk/a.png");
			student.setKkNumber("3201010101010002");
			student.setKkOcrData(ocr(
					"kk_number", "3201010101010002",
					"name_found_in_family", true));

			assertThat(ambil(student, "kk").cocok()).isTrue();
		}

		@Test
		@DisplayName("nama yang tidak ada di daftar keluarga ditandai untuk dilihat")
		void namaTidakAda() {
			Student student = mahasiswa();
			student.setKkFilePath("dokumen/kk/a.png");
			student.setKkNumber("3201010101010002");
			student.setKkOcrData(ocr(
					"kk_number", "3201010101010002",
					"name_found_in_family", false));

			StudentDocumentCheck.Hasil hasil = ambil(student, "kk");

			assertThat(hasil.cocok()).isFalse();
			assertThat(hasil.keterangan()).contains("daftar anggota keluarga");
		}

		@Test
		@DisplayName("nomor berbeda lebih menentukan daripada nama yang ketemu")
		void nomorBedaMenang() {
			Student student = mahasiswa();
			student.setKkFilePath("dokumen/kk/a.png");
			student.setKkNumber("3201010101010002");
			student.setKkOcrData(ocr(
					"kk_number", "9999999999999999",
					"name_found_in_family", true));

			StudentDocumentCheck.Hasil hasil = ambil(student, "kk");

			assertThat(hasil.cocok()).isFalse();
			assertThat(hasil.keterangan()).contains("Nomor KK berbeda");
		}
	}

	@Nested
	@DisplayName("Ijazah")
	class Ijazah {

		@Test
		@DisplayName("nama yang cocok disebut cocok")
		void namaCocok() {
			Student student = mahasiswa();
			student.setIjazahFilePath("dokumen/ijazah/a.png");
			student.setIjazahOcrData(ocr("name_match", true, "extracted_name", "Budi Santoso"));

			assertThat(ambil(student, "ijazah").cocok()).isTrue();
		}

		@Test
		@DisplayName("nama yang berbeda menyebutkan keduanya, supaya salah ketik kelihatan")
		void namaBeda() {
			Student student = mahasiswa();
			student.setIjazahFilePath("dokumen/ijazah/a.png");
			student.setIjazahOcrData(ocr("name_match", false, "extracted_name", "Budi Santosa"));

			StudentDocumentCheck.Hasil hasil = ambil(student, "ijazah");

			assertThat(hasil.cocok()).isFalse();
			assertThat(hasil.keterangan())
					.contains("Budi Santosa")
					.contains("Budi Santoso");
		}
	}

	@Nested
	@DisplayName("Foto")
	class Foto {

		@Test
		@DisplayName("latar merah sesuai ketentuan")
		void latarMerah() {
			Student student = mahasiswa();
			student.setProfilePicture("dokumen/foto/a.png");
			student.setProfilePictureAnalysis(ocr("red_background", true, "red_ratio", 0.82));

			assertThat(ambil(student, "foto").cocok()).isTrue();
		}

		@Test
		@DisplayName("latar bukan merah ditandai untuk dilihat, bukan ditolak")
		void latarBukanMerah() {
			Student student = mahasiswa();
			student.setProfilePicture("dokumen/foto/a.png");
			student.setProfilePictureAnalysis(ocr("red_background", false, "red_ratio", 0.04));

			StudentDocumentCheck.Hasil hasil = ambil(student, "foto");

			assertThat(hasil.cocok()).isFalse();
			assertThat(hasil.keterangan()).contains("perlu dilihat");
		}
	}
}
