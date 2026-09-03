package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.studyclass.StudyClass;
import ac.kampus.pembayaran.studyclass.StudyClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

	private final StudentRepository studentRepository;
	private final StudyClassRepository studyClassRepository;
	private final StudentTierLockPolicy tierLockPolicy;

	@Transactional(readOnly = true)
	public Page<Student> search(
			String search, Long classId, DiscountTier tier, Boolean active, Pageable pageable) {

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
	public Student changeDiscountTier(Long id, DiscountTier tier) {
		Student student = get(id);
		tierLockPolicy.assertTierChangeAllowed(student);
		student.setDiscountTier(tier);
		return studentRepository.save(student);
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
