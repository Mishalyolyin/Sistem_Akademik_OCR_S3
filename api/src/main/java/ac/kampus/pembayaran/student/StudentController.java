package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.AcademicTerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Mahasiswa")
public class StudentController {

	private final StudentService service;

	public record StudentSummary(
			Long id,
			String nim,
			String name,
			String phone,
			Long studyClassId,
			String className,
			String discountTier,
			AcademicTerm startTerm,
			String startAcademicYear,
			BigDecimal walletBalance,
			boolean pendaftaranExempt,
			boolean active,
			/** Langkah dokumen pertama yang belum lengkap, null bila sudah lengkap. */
			Student.DocumentStep nextDocumentStep,
			boolean documentsComplete,
			/** Golongan terkunci karena mahasiswa sudah pernah mengunggah bukti bayar. */
			boolean discountTierLocked
	) {
	}

	public record UpdateStudentRequest(
			@Size(max = 150, message = "Nama maksimal 150 karakter.")
			String name,

			@Size(max = 25, message = "Nomor telepon maksimal 25 karakter.")
			String phone,

			Long studyClassId,
			AcademicTerm startTerm,

			@Pattern(regexp = "\\d{4}/\\d{4}", message = "Format tahun akademik harus 2026/2027.")
			String startAcademicYear,

			Boolean pendaftaranExempt,
			Boolean active
	) {
	}

	public record ChangeTierRequest(
			// Kodenya diperiksa di service terhadap daftar golongan yang ada,
			// karena golongan bisa ditambah admin dan tidak lagi berupa enum.
			@NotBlank(message = "Golongan wajib dipilih.")
			String discountTier
	) {
	}

	@GetMapping
	@Operation(summary = "Daftar mahasiswa, bisa dicari dan disaring")
	public Page<StudentSummary> list(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Long classId,
			@RequestParam(required = false) String tier,
			@RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("nim").ascending());
		return service.search(search, classId, tier, active, pageable).map(this::toSummary);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Detail satu mahasiswa")
	public StudentSummary get(@PathVariable Long id) {
		return toSummary(service.get(id));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Ubah data mahasiswa")
	public StudentSummary update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateStudentRequest request) {

		return toSummary(service.update(
				id,
				request.name(),
				request.phone(),
				request.studyClassId(),
				request.startTerm(),
				request.startAcademicYear(),
				request.pendaftaranExempt(),
				request.active()));
	}

	@PatchMapping("/{id}/discount-tier")
	@Operation(summary = "Ubah golongan potongan; ditolak bila sudah pernah upload bukti bayar")
	public StudentSummary changeTier(
			@PathVariable Long id,
			@Valid @RequestBody ChangeTierRequest request) {

		return toSummary(service.changeDiscountTier(id, request.discountTier()));
	}

	public record ResetSandiResponse(String kataSandiBaru) {
	}

	@PostMapping("/{id}/reset-kata-sandi")
	@Operation(summary = "Kembalikan kata sandi mahasiswa ke NIM-nya; sesi lama dicabut")
	public ResetSandiResponse resetKataSandi(@PathVariable Long id) {
		return new ResetSandiResponse(service.resetKataSandi(id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Hapus mahasiswa")
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}

	private StudentSummary toSummary(Student student) {
		var studyClass = student.getStudyClass();
		return new StudentSummary(
				student.getId(),
				student.getNim(),
				student.getName(),
				student.getPhone(),
				studyClass == null ? null : studyClass.getId(),
				studyClass == null ? null : studyClass.displayName(),
				student.getDiscountTier(),
				student.getStartTerm(),
				student.getStartAcademicYear(),
				student.getWalletBalance(),
				student.isPendaftaranExempt(),
				student.isActive(),
				student.nextIncompleteDocumentStep(),
				student.hasCompletedDocuments(),
				service.tierLocked(student));
	}
}
