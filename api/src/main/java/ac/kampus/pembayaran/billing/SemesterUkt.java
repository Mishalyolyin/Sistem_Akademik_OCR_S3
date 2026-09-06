package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.BusinessRuleException;

/**
 * Menghitung tahun akademik dan term untuk semester UKT ke berapa pun.
 *
 * <h2>Kenapa dihitung dari mahasiswanya, bukan dari tanggal hari ini</h2>
 *
 * <p>Tiap mahasiswa punya titik mulainya sendiri —
 * {@code students.start_academic_year} dan {@code start_term}, keduanya diisi
 * admin lewat berkas import. Dari situ seluruh enam semesternya bisa
 * diturunkan tanpa satu angka pun perlu diketik ulang.
 *
 * <p>Mahasiswa yang masuk Gasal 2026/2027 berjalan
 * Gasal 26/27 → Genap 26/27 → Gasal 27/28 → Genap 27/28 → Gasal 28/29 →
 * Genap 28/29. Yang masuk Genap 2026/2027 bergeser setengah tahun:
 * Genap 26/27 → Gasal 27/28 → Genap 27/28 → dan seterusnya.
 *
 * <p>Menurunkannya dari tanggal hari ini akan salah untuk siapa pun yang
 * angkatannya berbeda, dan mengetiknya per tagihan — cara sebelumnya —
 * membuat satu salah ketik jadi tagihan di tahun yang keliru, yang baru
 * ketahuan setelah mahasiswa membayarnya.
 */
public record SemesterUkt(String academicYear, AcademicTerm term) {

	/**
	 * @param semesterNumber 1 sampai {@link PaymentGenerationService#MAX_SEMESTER_UKT}
	 */
	public static SemesterUkt hitung(
			String startAcademicYear, AcademicTerm startTerm, int semesterNumber) {

		if (semesterNumber < 1) {
			throw new IllegalArgumentException("Semester dimulai dari 1, bukan " + semesterNumber);
		}

		int tahunAwal = tahunPertama(startAcademicYear);

		// Tiap semester maju setengah tahun. Gasal ada di paruh pertama tahun
		// akademik, Genap di paruh kedua — jadi hitungannya sekadar menambahkan
		// langkah setengah tahun lalu membaginya kembali.
		int langkah = semesterNumber - 1 + (startTerm == AcademicTerm.GENAP ? 1 : 0);
		int tahunBerjalan = tahunAwal + langkah / 2;
		AcademicTerm term = langkah % 2 == 0 ? AcademicTerm.GASAL : AcademicTerm.GENAP;

		return new SemesterUkt("%d/%d".formatted(tahunBerjalan, tahunBerjalan + 1), term);
	}

	/**
	 * Apakah semester ini sudah waktunya ditagihkan pada tahun akademik dan term
	 * yang sedang berjalan.
	 *
	 * <p>Dipakai penjadwal untuk mengejar ketertinggalan: mahasiswa yang
	 * diimpor di tengah semester melewatkan putaran pembuatan tagihan bulan itu,
	 * dan tanpa pemeriksaan ini semester pertamanya tidak akan pernah terbentuk.
	 */
	public boolean sudahWaktunya(SemesterUkt sekarang) {
		int selisihTahun = tahunPertama(academicYear) - tahunPertama(sekarang.academicYear());
		if (selisihTahun != 0) {
			return selisihTahun < 0;
		}
		// Tahun sama: Gasal lebih dulu dari Genap.
		return term == AcademicTerm.GASAL || sekarang.term() == AcademicTerm.GENAP;
	}

	private static int tahunPertama(String academicYear) {
		String[] bagian = academicYear == null ? new String[0] : academicYear.split("/");
		if (bagian.length != 2) {
			throw new BusinessRuleException(
					"Tahun akademik \"%s\" salah format. Contoh: 2026/2027.".formatted(academicYear));
		}
		try {
			return Integer.parseInt(bagian[0].trim());
		} catch (NumberFormatException e) {
			throw new BusinessRuleException(
					"Tahun akademik \"%s\" salah format. Contoh: 2026/2027.".formatted(academicYear));
		}
	}
}
