package ac.kampus.pembayaran.student;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Meringkas hasil pembacaan dokumen jadi kesimpulan yang bisa dibaca sekilas.
 *
 * <p>Yang diringkas bukan "dokumen ini asli atau palsu" — Tesseract tidak bisa
 * memeriksa hologram, stempel, atau tanda tangan, dan berpura-pura bisa justru
 * berbahaya. Yang diringkas adalah <b>kecocokan</b>: NIK yang terbaca di KTP
 * sama atau tidak dengan yang diketik mahasiswa, nama di ijazah sama atau tidak
 * dengan namanya di sistem. Ketidakcocokan bukan berarti palsu, tapi itulah
 * dokumen yang layak dilihat manusia lebih dulu.
 */
public final class StudentDocumentCheck {

	private StudentDocumentCheck() {
	}

	/**
	 * @param cocok null berarti tidak bisa disimpulkan — biasanya karena
	 *              tulisannya tidak terbaca. Dibedakan dari {@code false} yang
	 *              berarti terbaca tapi memang berbeda.
	 */
	public record Hasil(
			String jenis,
			boolean adaBerkas,
			boolean sudahDibaca,
			Boolean cocok,
			String keterangan
	) {
	}

	public static List<Hasil> untuk(Student student) {
		List<Hasil> hasil = new ArrayList<>();
		hasil.add(foto(student));
		hasil.add(ktp(student));
		hasil.add(kk(student));
		hasil.add(ijazah(student));
		return hasil;
	}

	private static Hasil foto(Student student) {
		Map<String, Object> ocr = student.getProfilePictureAnalysis();
		boolean adaBerkas = terisi(StudentDocument.FOTO.pathOf(student));

		if (!adaBerkas || ocr == null) {
			return new Hasil("foto", adaBerkas, false, null, belumDibaca(adaBerkas));
		}

		Boolean merah = asBoolean(ocr.get("red_background"));
		if (merah == null) {
			return new Hasil("foto", true, true, null, "Latar foto tidak bisa disimpulkan.");
		}

		return new Hasil("foto", true, true, merah,
				merah ? "Latar foto merah, sesuai ketentuan."
						: "Latar foto tampaknya bukan merah — perlu dilihat.");
	}

	private static Hasil ktp(Student student) {
		Map<String, Object> ocr = student.getKtpOcrData();
		boolean adaBerkas = terisi(StudentDocument.KTP.pathOf(student));

		if (!adaBerkas || ocr == null) {
			return new Hasil("ktp", adaBerkas, false, null, belumDibaca(adaBerkas));
		}

		String terbaca = angkaSaja(asString(ocr.get("nik")));
		String diketik = angkaSaja(student.getNik());

		if (terbaca == null) {
			return new Hasil("ktp", true, true, null,
					"NIK pada gambar tidak terbaca — cocokkan sendiri dengan yang diketik.");
		}
		if (diketik == null) {
			return new Hasil("ktp", true, true, null,
					"NIK terbaca %s, tapi mahasiswa belum mengetik NIK-nya.".formatted(terbaca));
		}

		boolean cocok = terbaca.equals(diketik);
		return new Hasil("ktp", true, true, cocok,
				cocok ? "NIK pada gambar cocok dengan yang diketik."
						: "NIK berbeda: terbaca %s, diketik %s.".formatted(terbaca, diketik));
	}

	private static Hasil kk(Student student) {
		Map<String, Object> ocr = student.getKkOcrData();
		boolean adaBerkas = terisi(StudentDocument.KK.pathOf(student));

		if (!adaBerkas || ocr == null) {
			return new Hasil("kk", adaBerkas, false, null, belumDibaca(adaBerkas));
		}

		String terbaca = angkaSaja(asString(ocr.get("kk_number")));
		String diketik = angkaSaja(student.getKkNumber());
		Boolean namaAda = asBoolean(ocr.get("name_found_in_family"));

		if (terbaca != null && diketik != null && !terbaca.equals(diketik)) {
			return new Hasil("kk", true, true, false,
					"Nomor KK berbeda: terbaca %s, diketik %s.".formatted(terbaca, diketik));
		}
		if (Boolean.FALSE.equals(namaAda)) {
			// Tabel KK berisi banyak nama dan mudah salah baca, jadi ini
			// petunjuk untuk dilihat, bukan tuduhan.
			return new Hasil("kk", true, true, false,
					"Nama mahasiswa tidak ditemukan di daftar anggota keluarga.");
		}
		if (terbaca == null) {
			return new Hasil("kk", true, true, null, "Nomor KK pada gambar tidak terbaca.");
		}

		return new Hasil("kk", true, true, true,
				"Nomor KK cocok, dan nama mahasiswa ada di daftar keluarga.");
	}

	private static Hasil ijazah(Student student) {
		Map<String, Object> ocr = student.getIjazahOcrData();
		boolean adaBerkas = terisi(StudentDocument.IJAZAH.pathOf(student));

		if (!adaBerkas || ocr == null) {
			return new Hasil("ijazah", adaBerkas, false, null, belumDibaca(adaBerkas));
		}

		Boolean namaCocok = asBoolean(ocr.get("name_match"));
		String terbaca = asString(ocr.get("extracted_name"));

		if (namaCocok == null) {
			return new Hasil("ijazah", true, true, null, "Nama pada ijazah tidak terbaca.");
		}
		if (namaCocok) {
			return new Hasil("ijazah", true, true, true,
					"Nama pada ijazah cocok dengan nama mahasiswa.");
		}

		return new Hasil("ijazah", true, true, false,
				terbaca == null
						? "Nama pada ijazah tidak cocok dengan nama mahasiswa."
						: "Nama pada ijazah terbaca \"%s\", berbeda dari \"%s\"."
								.formatted(terbaca, student.getName()));
	}

	private static String belumDibaca(boolean adaBerkas) {
		return adaBerkas
				? "Belum dibaca. Pembacaannya berjalan di belakang layar setelah diunggah."
				: "Belum diunggah.";
	}

	private static boolean terisi(String nilai) {
		return nilai != null && !nilai.isBlank();
	}

	private static String asString(Object nilai) {
		if (nilai == null) return null;
		String teks = String.valueOf(nilai).trim();
		return teks.isEmpty() || "null".equals(teks) ? null : teks;
	}

	/** Membandingkan angkanya saja: pemisah dan spasi berbeda antar pembacaan. */
	private static String angkaSaja(String nilai) {
		if (nilai == null) return null;
		String angka = nilai.replaceAll("\\D", "");
		return angka.isEmpty() ? null : angka;
	}

	private static Boolean asBoolean(Object nilai) {
		if (nilai instanceof Boolean b) return b;
		if (nilai instanceof String teks) {
			if ("true".equalsIgnoreCase(teks)) return true;
			if ("false".equalsIgnoreCase(teks)) return false;
		}
		return null;
	}
}
