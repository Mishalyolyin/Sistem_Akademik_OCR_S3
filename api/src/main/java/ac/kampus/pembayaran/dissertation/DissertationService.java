package ac.kampus.pembayaran.dissertation;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Identitas disertasi: judul, promotor, dan naskahnya.
 *
 * <h2>Kenapa judul dan berkas dipisah</h2>
 *
 * <p>Judul dan kedua promotor ditetapkan jauh sebelum naskahnya jadi — itulah
 * yang dibawa mahasiswa ke Seminar Proposal. Naskah disertasi dan artikel
 * jurnalnya baru ada menjelang Ujian Tertutup dan Terbuka. Memaksa semuanya
 * diisi sekaligus berarti tidak ada seorang pun bisa mendaftar Seminar
 * Proposal sampai disertasinya hampir selesai — persis kebalikan dari urutan
 * yang sebenarnya.
 *
 * <p>Karena itu {@link #simpanIdentitas} dan {@link #unggahBerkas} terpisah,
 * dan hanya yang pertama yang jadi syarat mendaftar tahap ujian.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DissertationService {

	/**
	 * Naskah disertasi lazim berukuran puluhan megabyte. Batas ini sengaja jauh
	 * di atas batas bukti bayar, dan tetap di bawah batas multipart Spring
	 * (25 MB) supaya penolakannya datang sebagai pesan yang bisa dibaca
	 * mahasiswa, bukan sebagai galat Tomcat sebelum controller sempat jalan.
	 */
	static final long MAKS_BERKAS = 20L * 1024 * 1024;

	private static final int JUDUL_MINIMAL = 10;
	private static final int NAMA_MINIMAL = 3;

	private final DissertationRepository repository;
	private final StudentRepository studentRepository;
	private final FileStorageService storage;

	public enum JenisBerkas {
		DISERTASI("disertasi"),
		ARTIKEL("artikel");

		private final String folder;

		JenisBerkas(String folder) {
			this.folder = folder;
		}

		public String folder() {
			return folder;
		}
	}

	@Transactional(readOnly = true)
	public Optional<DissertationDetail> cari(Long studentId) {
		return repository.findById(studentId);
	}

	/** Dipakai gerbang pendaftaran tahap ujian di portal. */
	@Transactional(readOnly = true)
	public boolean sudahDiisi(Long studentId) {
		return repository.existsByStudentId(studentId);
	}

	/**
	 * Menyimpan judul dan kedua promotor. Boleh dipanggil berulang: judul
	 * disertasi memang lazim berubah selama penyusunan, dan mengunci setelah
	 * pengisian pertama akan memaksa mahasiswa meminta bantuan admin untuk
	 * membetulkan salah ketik.
	 */
	@Transactional
	public DissertationDetail simpanIdentitas(
			Long studentId, String judul, String promotor, String copromotor) {

		String judulBersih = wajib(judul, JUDUL_MINIMAL,
				"Judul disertasi wajib diisi, minimal %d karakter.".formatted(JUDUL_MINIMAL));
		String promotorBersih = wajib(promotor, NAMA_MINIMAL, "Nama promotor wajib diisi.");
		String copromotorBersih = wajib(copromotor, NAMA_MINIMAL, "Nama ko-promotor wajib diisi.");

		if (promotorBersih.equalsIgnoreCase(copromotorBersih)) {
			throw new BusinessRuleException(
					"Promotor dan ko-promotor tidak boleh orang yang sama.");
		}

		DissertationDetail detail = repository.findById(studentId).orElseGet(() -> {
			Student student = studentRepository.findById(studentId)
					.orElseThrow(() -> NotFoundException.of("Mahasiswa", studentId));
			return DissertationDetail.builder().student(student).build();
		});

		detail.setTitle(judulBersih);
		detail.setPromotor(promotorBersih);
		detail.setCopromotor(copromotorBersih);

		return repository.save(detail);
	}

	/**
	 * Mengunggah naskah disertasi atau artikel jurnalnya.
	 *
	 * <p>Identitasnya harus sudah diisi lebih dulu. Berkas tanpa judul dan
	 * promotor adalah berkas yatim: ia tersimpan tanpa keterangan siapa
	 * membimbing dan tentang apa isinya.
	 */
	@Transactional
	public DissertationDetail unggahBerkas(
			Long studentId, JenisBerkas jenis, MultipartFile berkas) {

		DissertationDetail detail = repository.findById(studentId)
				.orElseThrow(() -> new BusinessRuleException(
						"Isi dulu judul disertasi dan nama promotor sebelum mengunggah naskah."));

		if (!"pdf".equals(ekstensi(berkas.getOriginalFilename()))) {
			throw new BusinessRuleException(
					"Naskah harus berupa PDF. Berkas gambar atau dokumen Word tidak diterima.");
		}

		String path = storage.store(berkas, "disertasi/" + jenis.folder(), MAKS_BERKAS);

		switch (jenis) {
			case DISERTASI -> detail.setDissertationFilePath(path);
			case ARTIKEL -> detail.setArticleFilePath(path);
		}

		log.info("Naskah {} mahasiswa {} tersimpan di {}", jenis, studentId, path);
		return repository.save(detail);
	}

	private static String wajib(String nilai, int minimal, String pesan) {
		String bersih = nilai == null ? "" : nilai.trim();
		if (bersih.length() < minimal) {
			throw new BusinessRuleException(pesan);
		}
		return bersih;
	}

	private static String ekstensi(String namaBerkas) {
		if (namaBerkas == null) {
			return "";
		}
		int titik = namaBerkas.lastIndexOf('.');
		return titik < 0 ? "" : namaBerkas.substring(titik + 1).toLowerCase(java.util.Locale.ROOT);
	}
}
