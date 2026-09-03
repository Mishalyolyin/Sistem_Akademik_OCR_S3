package ac.kampus.pembayaran.studyclass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyClassRepository extends JpaRepository<StudyClass, Long> {

	Optional<StudyClass> findByNameIgnoreCaseAndAcademicYear(String name, String academicYear);

	List<StudyClass> findAllByOrderByAcademicYearDescNameAsc();

	boolean existsByNameIgnoreCaseAndAcademicYear(String name, String academicYear);
}
