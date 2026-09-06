package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.settings.SystemSettingService;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Membuat tagihan UKT sendiri, satu semester pada waktunya.
 *
 * <h2>Kenapa tahun akademiknya tidak diturunkan dari tanggal hari ini</h2>
 *
 * <p>Tiap angkatan punya jalurnya sendiri. Mahasiswa yang masuk Genap
 * 2026/2027 berada di semester keduanya ketika mahasiswa angkatan Gasal
 * 2027/2028 baru memulai semester pertamanya — pada bulan kalender yang sama.
 * Menurunkan tahun akademik dari tanggal berarti salah untuk salah satu dari
 * keduanya, apa pun pilihannya.
 *
 * <p>Karena itu yang dipakai adalah {@code start_academic_year} dan
 * {@code start_term} milik mahasiswa itu sendiri, keduanya diisi admin lewat
 * berkas import. Kuasa menentukannya tetap di tangan admin; yang hilang hanya
 * pengetikan ulang yang cepat atau lambat salah.
 *
 * <h2>Mengejar yang tertinggal</h2>
 *
 * <p>Putaran ini membuat SEMUA semester yang waktunya sudah tiba, bukan hanya
 * satu. Mahasiswa yang diimpor di tengah Genap melewatkan putaran September,
 * dan tanpa pengejaran itu semester pertamanya tidak akan pernah terbentuk —
 * sementara jalur manual sudah tidak ada lagi.
 *
 * <p>Yang belum tiba waktunya sengaja TIDAK dibuat lebih dulu. Tarif dibekukan
 * saat tagihan dibuat, jadi membuat semester kelima hari ini berarti mengunci
 * tarif hari ini untuk yang baru dibayar dua tahun lagi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UktAutoService {

	public static final String ENABLED = "ukt_auto_enabled";
	public static final String LAST_RUN = "ukt_auto_last_run";

	private final StudentRepository studentRepository;
	private final UktAutoPerMahasiswa perMahasiswa;
	private final SystemSettingService settings;

	/**
	 * @param diperiksa mahasiswa aktif yang ditinjau
	 * @param dibuat    tagihan yang terbentuk
	 * @param dilewati  tidak ada yang perlu dibuat — sudah lengkap atau belum waktunya
	 * @param gagal     ditolak aturan bisnis, misalnya tarifnya belum diatur
	 * @param catatan   sebab kegagalan, satu baris per mahasiswa
	 */
	public record Hasil(
			int diperiksa,
			int dibuat,
			int dilewati,
			int gagal,
			List<String> catatan
	) {
	}

	/**
	 * Menjalankan satu putaran untuk tanggal yang diberikan.
	 *
	 * <p>Tanggalnya parameter, bukan {@code LocalDate.now()} di dalam sini —
	 * supaya aturannya bisa diuji untuk hari mana pun tanpa memutar jam mesin.
	 */
	public Hasil jalankan(LocalDate hariIni) {
		if (!"true".equalsIgnoreCase(settings.getOrDefault(ENABLED, "true"))) {
			log.info("Pembuatan tagihan UKT otomatis sedang dimatikan.");
			return new Hasil(0, 0, 0, 0, List.of());
		}

		SemesterUkt sekarang = semesterKalender(hariIni);
		List<Student> mahasiswa = studentRepository.findByActiveTrueOrderByIdAsc();

		int dibuat = 0;
		int dilewati = 0;
		int gagal = 0;
		List<String> catatan = new ArrayList<>();

		for (Student student : mahasiswa) {
			Hasil satu = perMahasiswa.kerjakan(student, sekarang);
			dibuat += satu.dibuat();
			dilewati += satu.dilewati();
			gagal += satu.gagal();
			catatan.addAll(satu.catatan());
		}

		Hasil hasil = new Hasil(mahasiswa.size(), dibuat, dilewati, gagal, catatan);
		catatKapanTerakhir(hariIni, hasil);

		log.info("Putaran tagihan UKT {}: {} mahasiswa diperiksa, {} tagihan dibuat, "
						+ "{} dilewati, {} gagal.",
				hariIni, hasil.diperiksa(), dibuat, dilewati, gagal);
		return hasil;
	}

	/**
	 * Semester kalender yang sedang berjalan.
	 *
	 * <p>Ini SATU-SATUNYA tempat tanggal ikut menentukan sesuatu, dan yang
	 * ditentukannya bukan tahun akademik mahasiswa melainkan batas "sampai mana
	 * yang sudah boleh ditagihkan". Juli dan Agustus tidak punya angsuran; di
	 * dua bulan itu batasnya adalah Genap yang baru saja lewat, bukan Gasal yang
	 * belum dimulai — kalau tidak, tagihan semester berikutnya terbit sebulan
	 * lebih awal beserta tarif yang ikut terkunci lebih awal.
	 */
	static SemesterUkt semesterKalender(LocalDate hariIni) {
		int bulan = hariIni.getMonthValue();
		int tahun = hariIni.getYear();

		if (bulan >= 9) {
			return new SemesterUkt("%d/%d".formatted(tahun, tahun + 1), AcademicTerm.GASAL);
		}
		if (bulan == 1) {
			return new SemesterUkt("%d/%d".formatted(tahun - 1, tahun), AcademicTerm.GASAL);
		}
		// Februari–Agustus: Genap dari tahun akademik yang dimulai tahun lalu.
		return new SemesterUkt("%d/%d".formatted(tahun - 1, tahun), AcademicTerm.GENAP);
	}

	private void catatKapanTerakhir(LocalDate hariIni, Hasil hasil) {
		settings.set(LAST_RUN, "%s — %d diperiksa, %d dibuat, %d gagal"
				.formatted(hariIni, hasil.diperiksa(), hasil.dibuat(), hasil.gagal()));
	}
}
