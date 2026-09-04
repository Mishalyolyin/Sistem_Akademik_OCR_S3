package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.studyclass.StudyClass;
import ac.kampus.pembayaran.studyclass.StudyClassRepository;
import ac.kampus.pembayaran.adjustment.AdjustmentRepository;
import ac.kampus.pembayaran.billing.PaymentPlanRepository;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.tuition.DiscountTierRate;
import ac.kampus.pembayaran.tuition.DiscountTierRateRepository;
import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

	private final StudentRepository studentRepository;
	private final StudyClassRepository studyClassRepository;
	private final StudentTierLockPolicy tierLockPolicy;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final DiscountTierRateRepository tierRepository;
	private final PaymentPlanRepository planRepository;
	private final PaymentRepository paymentRepository;
	private final AdjustmentRepository adjustmentRepository;

	@Transactional(readOnly = true)
	public Page<Student> search(
			String search, Long classId, String tier, Boolean active, Pageable pageable) {

		Specification<Student> spec = Specification
				.allOf(StudentSpecifications.nameOrNimContains(search),
						StudentSpecifications.inClass(classId),
						StudentSpecifications.hasTier(tier),
						StudentSpecifications.isActive(active));

		return studentRepository.findAll(spec, pageable);
	}

	@Transactional(readOnly = true)
	public Student get(Long id) {
		return studentRepository.findWithClassById(id)
				.orElseThrow(() -> NotFoundException.of("Mahasiswa", id));
	}

	@Transactional
	public Student update(
			Long id,
			String name,
			String phone,
			Long studyClassId,
			AcademicTerm startTerm,
			String startAcademicYear,
			Boolean pendaftaranExempt,
			Boolean active) {

		Student student = get(id);

		if (name != null) student.setName(name.trim());
		if (phone != null) student.setPhone(phone.isBlank() ? null : phone.trim());
		if (startTerm != null) student.setStartTerm(startTerm);
		if (startAcademicYear != null) student.setStartAcademicYear(startAcademicYear);
		if (pendaftaranExempt != null) student.setPendaftaranExempt(pendaftaranExempt);
		if (active != null) student.setActive(active);

		if (studyClassId != null) {
			StudyClass studyClass = studyClassRepository.findById(studyClassId)
					.orElseThrow(() -> NotFoundException.of("Kelas", studyClassId));
			student.setStudyClass(studyClass);
		}

		return studentRepository.save(student);
	}

	/**
	 * Golongan potongan hanya boleh diubah selama mahasiswa belum pernah
	 * mengunggah bukti bayar. Lihat {@link StudentTierLockPolicy}.
	 */
	@Transactional
	public Student changeDiscountTier(Long id, String tier) {
		Student student = get(id);
		tierLockPolicy.assertTierChangeAllowed(student);

		// Golongan bukan lagi enum, jadi kodenya tidak tersaring saat kompilasi.
		// Tanpa pemeriksaan ini, salah ketik menghasilkan mahasiswa yang
		// tarifnya tidak bisa dihitung sama sekali.
		DiscountTierRate golongan = tierRepository.findById(tier)
				.orElseThrow(() -> new BusinessRuleException(
						"Golongan potongan \"%s\" tidak dikenal. Pilihan: %s."
								.formatted(tier, kodeGolonganAktif())));
		if (!golongan.isActive()) {
			throw new BusinessRuleException(
					"Golongan %s sudah tidak aktif, tidak bisa dipakai lagi."
							.formatted(golongan.getLabel()));
		}

		student.setDiscountTier(golongan.getTier());
		return studentRepository.save(student);
	}

	/**
	 * Mengembalikan kata sandi mahasiswa ke NIM-nya sendiri.
	 *
	 * <p>Mahasiswa tidak mengelola kata sandinya sendiri di sistem ini; kalau
	 * lupa, ia datang ke bagian keuangan dan admin mengembalikannya ke NIM.
	 * Karena NIM diketahui banyak orang, seluruh sesi yang sedang berjalan ikut
	 * dicabut: membiarkannya hidup berarti sesi lama tetap bisa dipakai oleh
	 * siapa pun yang sempat masuk sebelumnya.
	 *
	 * @return NIM, yang sekaligus menjadi kata sandi barunya
	 */
	@Transactional
	public String resetKataSandi(Long id) {
		Student student = get(id);
		User user = student.getUser();

		if (user == null) {
			throw new BusinessRuleException(
					"Mahasiswa ini belum punya akun untuk masuk, jadi tidak ada kata sandi "
							+ "yang bisa dikembalikan.");
		}

		user.setPasswordHash(passwordEncoder.encode(student.getNim()));
		user.setTokensValidFrom(Instant.now());
		userRepository.save(user);

		log.info("Kata sandi mahasiswa {} dikembalikan ke NIM; sesi lama dicabut.",
				student.getNim());
		return student.getNim();
	}

	/** Daftar kode golongan aktif, untuk pesan galat yang menuntun. */
	private String kodeGolonganAktif() {
		return tierRepository.findByActiveTrueOrderBySortOrderAscTierAsc().stream()
				.map(DiscountTierRate::getTier)
				.collect(java.util.stream.Collectors.joining(", "));
	}

	@Transactional(readOnly = true)
	public boolean tierLocked(Student student) {
		return tierLockPolicy.hasUploadedProof(student.getId());
	}

	/**
	 * Menghapus mahasiswa yang belum punya riwayat keuangan apa pun.
	 *
	 * <p>Penjagaan di bawah bukan formalitas: {@code payment_plans},
	 * {@code payments}, dan {@code adjustments} semuanya
	 * {@code ON DELETE CASCADE} ke mahasiswa. Tanpa penjagaan ini, menghapus satu
	 * mahasiswa ikut menghapus seluruh tagihan, pembayaran yang sudah
	 * diverifikasi, dan jejak audit penyesuaiannya — diam-diam, dalam satu
	 * perintah, tanpa satu pun galat.
	 *
	 * <p>Jadi penghapusan hanya untuk salah entri saat import. Mahasiswa yang
	 * sudah berjalan cukup dinonaktifkan.
	 */
	@Transactional
	public void delete(Long id) {
		Student student = get(id);
		String penghalang = penghalangPenghapusan(student.getId());
		if (penghalang != null) {
			throw new BusinessRuleException(
					("Mahasiswa %s sudah punya %s, jadi tidak bisa dihapus — riwayat uangnya "
							+ "akan ikut terhapus. Nonaktifkan saja lewat Ubah data.")
							.formatted(student.getName(), penghalang));
		}

		User akun = student.getUser();
		studentRepository.delete(student);

		// Akun tanpa data mahasiswa masih bisa masuk, tapi tiap halaman portal
		// menjawab "tidak terhubung ke data mahasiswa". Ikut dihapus supaya tidak
		// ada pintu masuk yang menuju ke mana-mana.
		if (akun != null) {
			userRepository.delete(akun);
		}
	}

	/** Hasil satu baris pada penghapusan massal. */
	public record HasilHapus(Long id, String nim, String nama, boolean berhasil, String alasan) {
	}

	/**
	 * Penghapusan massal, dilaporkan per baris.
	 *
	 * <p>Satu mahasiswa yang ditolak tidak boleh membatalkan penghapusan yang
	 * lain — kebiasaan yang sama dengan import Excel, karena keduanya menangani
	 * sekumpulan baris yang nasibnya berdiri sendiri-sendiri.
	 */
	@Transactional
	public List<HasilHapus> deleteMassal(List<Long> ids) {
		List<HasilHapus> hasil = new ArrayList<>();

		for (Long id : ids.stream().distinct().toList()) {
			Student student = studentRepository.findWithClassById(id).orElse(null);
			if (student == null) {
				hasil.add(new HasilHapus(id, null, null, false, "Mahasiswa tidak ditemukan."));
				continue;
			}

			String penghalang = penghalangPenghapusan(id);
			if (penghalang != null) {
				hasil.add(new HasilHapus(id, student.getNim(), student.getName(), false,
						"Sudah punya %s.".formatted(penghalang)));
				continue;
			}

			User akun = student.getUser();
			studentRepository.delete(student);
			if (akun != null) {
				userRepository.delete(akun);
			}
			hasil.add(new HasilHapus(id, student.getNim(), student.getName(), true, null));
		}

		return hasil;
	}

	/** Sebutan riwayat yang menghalangi penghapusan, atau null bila aman dihapus. */
	private String penghalangPenghapusan(Long studentId) {
		if (paymentRepository.existsByStudentId(studentId)) return "bukti pembayaran";
		if (planRepository.existsByStudentId(studentId)) return "tagihan";
		if (adjustmentRepository.existsByStudentId(studentId)) return "penyesuaian saldo";
		return null;
	}
}
