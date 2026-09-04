package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.studyclass.StudyClass;
import ac.kampus.pembayaran.studyclass.StudyClassRepository;
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

	@Transactional
	public void delete(Long id) {
		studentRepository.delete(get(id));
	}
}
