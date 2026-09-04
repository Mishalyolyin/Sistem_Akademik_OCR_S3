package ac.kampus.pembayaran.studyclass;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Kelas")
public class StudyClassController {

	private final StudyClassRepository repository;
	private final ac.kampus.pembayaran.student.StudentRepository studentRepository;

	public record ClassRequest(
			@NotBlank(message = "Nama kelas wajib diisi.")
			String name,

			Boolean kerjasama,

			@NotBlank(message = "Tahun akademik wajib diisi.")
			@Pattern(regexp = "\\d{4}/\\d{4}", message = "Format tahun akademik harus 2026/2027.")
			String academicYear,

			Boolean active
	) {
	}

	public record ClassResponse(
			Long id,
			String name,
			String displayName,
			boolean kerjasama,
			String academicYear,
			boolean active
	) {
		static ClassResponse from(StudyClass entity) {
			return new ClassResponse(
					entity.getId(),
					entity.getName(),
					entity.displayName(),
					entity.isKerjasama(),
					entity.getAcademicYear(),
					entity.isActive());
		}
	}

	@GetMapping
	@Operation(summary = "Daftar semua kelas")
	public List<ClassResponse> list() {
		return repository.findAllByOrderByAcademicYearDescNameAsc().stream()
				.map(ClassResponse::from)
				.toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Tambah kelas")
	@Transactional
	public ClassResponse create(@Valid @RequestBody ClassRequest request) {
		String name = request.name().trim();

		if (repository.existsByNameIgnoreCaseAndAcademicYear(name, request.academicYear())) {
			throw new BusinessRuleException(
					"Kelas \"%s\" untuk tahun %s sudah ada.".formatted(name, request.academicYear()));
		}

		StudyClass entity = StudyClass.builder()
				.name(name)
				// Nama yang mengandung "kerjasama" ditandai otomatis; admin bisa mengoreksi.
				.kerjasama(request.kerjasama() != null
						? request.kerjasama()
						: StudyClass.looksLikeKerjasama(name))
				.academicYear(request.academicYear())
				.active(request.active() == null || request.active())
				.build();

		return ClassResponse.from(repository.save(entity));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Ubah kelas")
	@Transactional
	public ClassResponse update(@PathVariable Long id, @Valid @RequestBody ClassRequest request) {
		StudyClass entity = repository.findById(id)
				.orElseThrow(() -> NotFoundException.of("Kelas", id));

		String name = request.name().trim();
		repository.findByNameIgnoreCaseAndAcademicYear(name, request.academicYear())
				.filter(other -> !other.getId().equals(id))
				.ifPresent(other -> {
					throw new BusinessRuleException(
							"Kelas \"%s\" untuk tahun %s sudah ada.".formatted(name, request.academicYear()));
				});

		entity.setName(name);
		entity.setAcademicYear(request.academicYear());
		if (request.kerjasama() != null) entity.setKerjasama(request.kerjasama());
		if (request.active() != null) entity.setActive(request.active());

		return ClassResponse.from(repository.save(entity));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Hapus kelas; ditolak bila masih ada mahasiswa di dalamnya")
	@Transactional
	public void delete(@PathVariable Long id) {
		StudyClass entity = repository.findById(id)
				.orElseThrow(() -> NotFoundException.of("Kelas", id));

		// Tanpa penjagaan ini, kunci asing di database yang menolak — dan
		// penolakannya sampai ke admin sebagai 500 tanpa keterangan apa pun.
		long penghuni = studentRepository.countByStudyClassId(id);
		if (penghuni > 0) {
			throw new BusinessRuleException(
					("Kelas %s masih berisi %d mahasiswa. Pindahkan mereka dulu, atau "
							+ "nonaktifkan kelasnya saja supaya riwayatnya tetap utuh.")
							.formatted(entity.displayName(), penghuni));
		}

		repository.delete(entity);
	}
}
