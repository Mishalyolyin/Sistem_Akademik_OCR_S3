package ac.kampus.pembayaran.student;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keempat berkas dokumen wajib, beserta cara mengambil path-nya dari mahasiswa.
 *
 * <p>Langkah kelima dokumen wajib — alamat — sengaja tidak ada di sini karena
 * ia teks, bukan berkas. Lihat {@link Student.DocumentStep} untuk urutan
 * pengisiannya.
 */
public enum StudentDocument {

	FOTO(Student::getProfilePicture, "Foto profil"),
	KTP(Student::getKtpFilePath, "KTP"),
	KK(Student::getKkFilePath, "Kartu Keluarga"),
	IJAZAH(Student::getIjazahFilePath, "Ijazah");

	private final Function<Student, String> pengambilPath;
	private final String label;

	StudentDocument(Function<Student, String> pengambilPath, String label) {
		this.pengambilPath = pengambilPath;
		this.label = label;
	}

	/** Path relatif terhadap folder penyimpanan, atau null bila belum diunggah. */
	public String pathOf(Student student) {
		return pengambilPath.apply(student);
	}

	public String label() {
		return label;
	}

	/** Kode yang dipakai di URL, selalu huruf kecil. */
	public String kode() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Jenis dokumen dari potongan alamat.
	 *
	 * <p>Tidak menyerahkannya ke konversi enum bawaan Spring karena alamatnya
	 * ditulis huruf kecil sementara {@code valueOf} peka huruf besar — jadi
	 * {@code /dokumen/ktp} akan ditolak padahal itu bentuk yang benar.
	 */
	public static StudentDocument dariKode(String kode) {
		String bersih = kode == null ? "" : kode.trim().toUpperCase(Locale.ROOT);
		return Arrays.stream(values())
				.filter(jenis -> jenis.name().equals(bersih))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"Jenis dokumen \"%s\" tidak dikenal. Pilihan: %s.".formatted(kode, pilihan())));
	}

	/**
	 * Kode dokumen yang berkasnya benar-benar ada, untuk memberi tahu klien mana
	 * yang boleh dibuka. Path-nya sendiri tidak ikut keluar: itu detail
	 * penyimpanan, dan klien cukup tahu ada atau tidak.
	 */
	public static List<String> tersediaUntuk(Student student) {
		return Arrays.stream(values())
				.filter(jenis -> {
					String path = jenis.pathOf(student);
					return path != null && !path.isBlank();
				})
				.map(StudentDocument::kode)
				.toList();
	}

	private static String pilihan() {
		return Arrays.stream(values())
				.map(StudentDocument::kode)
				.collect(Collectors.joining(", "));
	}
}
