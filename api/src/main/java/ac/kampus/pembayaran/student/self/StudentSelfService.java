package ac.kampus.pembayaran.student.self;

import ac.kampus.pembayaran.auth.AuthService;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.payment.ocr.OcrJobPublisher;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentDocument;
import ac.kampus.pembayaran.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Layanan untuk mahasiswa mengurus akunnya sendiri.
 *
 * <p>Semua method di sini bekerja pada mahasiswa yang sedang login, tidak
 * pernah menerima id mahasiswa dari luar — supaya tidak mungkin seorang
 * mahasiswa mengubah data mahasiswa lain dengan menebak id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentSelfService {

	/** Kategori yang boleh didaftarkan sendiri oleh mahasiswa. UKT dibuat admin. */
	private static final Set<PaymentCategory> BOLEH_DAFTAR_SENDIRI = Set.of(
			PaymentCategory.PENDAFTARAN,
			PaymentCategory.SEMINAR_PROPOSAL,
			PaymentCategory.UJIAN_KELAYAKAN,
			PaymentCategory.UJIAN_TERTUTUP,
			PaymentCategory.UJIAN_TERBUKA);

	private final StudentRepository studentRepository;
	private final FileStorageService storage;
	private final OcrJobPublisher ocrPublisher;

	@Transactional(readOnly = true)
	public Student current() {
		Long userId = AuthService.currentUserId();
		return studentRepository.findWithClassByUserId(userId)
				.orElseThrow(() -> new NotFoundException(
						"Akun ini tidak terhubung ke data mahasiswa. Hubungi admin."));
	}

	public static boolean bolehDaftarSendiri(PaymentCategory category) {
		return BOLEH_DAFTAR_SENDIRI.contains(category);
	}

	// --- Dokumen wajib, diisi berurutan ---

	@Transactional
	public Student simpanFoto(MultipartFile berkas) {
		Student student = current();
		student.setProfilePicture(storage.store(berkas, "dokumen/foto"));
		return simpanLaluBaca(student, StudentDocument.FOTO);
	}

	@Transactional
	public Student simpanKtp(String nik, MultipartFile berkas) {
		Student student = current();
		wajibSudahSampai(student, Student.DocumentStep.KTP);

		String bersih = nik == null ? "" : nik.replaceAll("\\D", "");
		if (bersih.length() != 16) {
			throw new BusinessRuleException("Nomor KTP harus 16 digit angka.");
		}

		student.setNik(bersih);
		student.setKtpFilePath(storage.store(berkas, "dokumen/ktp"));
		// Hasil pembacaan sebelumnya dibuang: berkasnya sudah berganti, dan
		// hasil lama yang tertinggal akan dibaca admin sebagai hasil yang baru.
		student.setKtpOcrData(null);
		return simpanLaluBaca(student, StudentDocument.KTP);
	}

	@Transactional
	public Student simpanKk(String nomorKk, MultipartFile berkas) {
		Student student = current();
		wajibSudahSampai(student, Student.DocumentStep.KK);

		String bersih = nomorKk == null ? "" : nomorKk.replaceAll("\\D", "");
		if (bersih.length() != 16) {
			throw new BusinessRuleException("Nomor Kartu Keluarga harus 16 digit angka.");
		}

		student.setKkNumber(bersih);
		student.setKkFilePath(storage.store(berkas, "dokumen/kk"));
		student.setKkOcrData(null);
		return simpanLaluBaca(student, StudentDocument.KK);
	}

	@Transactional
	public Student simpanIjazah(MultipartFile berkas) {
		Student student = current();
		wajibSudahSampai(student, Student.DocumentStep.IJAZAH);

		student.setIjazahFilePath(storage.store(berkas, "dokumen/ijazah"));
		student.setIjazahOcrData(null);
		return simpanLaluBaca(student, StudentDocument.IJAZAH);
	}

	/**
	 * Menyimpan mahasiswa lalu mengantrekan pembacaan dokumennya.
	 *
	 * <p>Pembacaannya berjalan di belakang layar dan TIDAK menahan unggahan:
	 * mahasiswa yang sedang mengisi dokumen tidak boleh menunggu Tesseract, dan
	 * service OCR yang sedang mati tidak boleh membuat unggahannya gagal.
	 * Hasilnya menyusul, atau tidak sama sekali — gate dokumen tidak
	 * bergantung padanya.
	 */
	private Student simpanLaluBaca(Student student, StudentDocument jenis) {
		Student tersimpan = studentRepository.save(student);
		ocrPublisher.publishDocumentAfterCommit(tersimpan.getId(), jenis);
		return tersimpan;
	}

	@Transactional
	public Student simpanAlamat(String alamat, String telepon) {
		Student student = current();
		wajibSudahSampai(student, Student.DocumentStep.ADDRESS);

		if (alamat == null || alamat.trim().length() < 10) {
			throw new BusinessRuleException("Alamat wajib diisi, minimal 10 karakter.");
		}

		student.setAddress(alamat.trim());
		if (telepon != null && !telepon.isBlank()) {
			student.setPhone(telepon.trim());
		}
		return studentRepository.save(student);
	}

	/**
	 * Dokumen harus diisi berurutan. Menolak unggahan langkah yang belum
	 * gilirannya, supaya urutannya tidak bisa dilompati lewat pemanggilan API
	 * langsung — bukan hanya disembunyikan di antarmuka.
	 */
	private void wajibSudahSampai(Student student, Student.DocumentStep langkah) {
		Student.DocumentStep sekarang = student.nextIncompleteDocumentStep();
		if (sekarang == null) {
			return;
		}
		if (sekarang.ordinal() < langkah.ordinal()) {
			throw new BusinessRuleException(
					"Lengkapi dulu %s sebelum mengisi %s."
							.formatted(namaLangkah(sekarang), namaLangkah(langkah)));
		}
	}

	public static String namaLangkah(Student.DocumentStep langkah) {
		return switch (langkah) {
			case PHOTO -> "Foto profil";
			case KTP -> "KTP";
			case KK -> "Kartu Keluarga";
			case IJAZAH -> "Ijazah";
			case ADDRESS -> "Alamat";
		};
	}
}
