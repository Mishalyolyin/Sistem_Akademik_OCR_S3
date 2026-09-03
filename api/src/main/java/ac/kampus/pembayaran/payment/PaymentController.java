package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.auth.AuthService;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.common.PaymentCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Pembayaran")
public class PaymentController {

	private final PaymentService service;
	private final PaymentRepository repository;
	private final VerificationLogRepository logRepository;

	public record PaymentResponse(
			Long id,
			Long studentId,
			String studentName,
			String studentNim,
			String className,
			Long installmentId,
			Integer installmentNo,
			String categoryLabel,
			Integer semesterNumber,
			BigDecimal amount,
			String proofFilePath,
			String bankName,
			LocalDate paymentProofDate,
			PaymentStatus status,
			BigDecimal ocrConfidence,
			Map<String, Object> ocrData,
			String rejectReason,
			Instant verifiedAt,
			Instant createdAt
	) {
		static PaymentResponse from(Payment p) {
			var installment = p.getInstallment();
			var plan = p.getPaymentPlan();
			var student = p.getStudent();

			return new PaymentResponse(
					p.getId(),
					student.getId(),
					student.getName(),
					student.getNim(),
					student.getStudyClass() == null ? null : student.getStudyClass().displayName(),
					installment == null ? null : installment.getId(),
					installment == null ? null : installment.getInstallmentNo(),
					plan == null ? null : plan.getCategory().label(),
					plan == null ? null : plan.getSemesterNumber(),
					p.getAmount(),
					p.getProofFilePath(),
					p.getBankName(),
					p.getPaymentProofDate(),
					p.getStatus(),
					p.getOcrConfidence(),
					p.getOcrData(),
					p.getRejectReason(),
					p.getVerifiedAt(),
					p.getCreatedAt());
		}
	}

	public record DecideRequest(
			@NotNull(message = "Keputusan wajib diisi.")
			Boolean approve,

			@Size(max = 500, message = "Catatan maksimal 500 karakter.")
			String note
	) {
	}

	public record LogResponse(
			Long id,
			PaymentStatus fromStatus,
			PaymentStatus toStatus,
			Long adminId,
			String note,
			Instant createdAt
	) {
		static LogResponse from(VerificationLog log) {
			return new LogResponse(log.getId(), log.getFromStatus(), log.getToStatus(),
					log.getAdminId(), log.getNote(), log.getCreatedAt());
		}
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Unggah bukti bayar; pembacaan OCR berjalan di belakang layar")
	public PaymentResponse upload(
			@RequestParam Long studentId,
			@RequestParam(required = false) Long installmentId,
			@RequestParam BigDecimal amount,
			@RequestParam("file") MultipartFile file) {

		return PaymentResponse.from(service.upload(studentId, installmentId, amount, file));
	}

	@GetMapping
	@Operation(summary = "Daftar pembayaran, bisa disaring per kategori, status, dan mahasiswa")
	@Transactional(readOnly = true)
	public Page<PaymentResponse> list(
			@RequestParam(required = false) PaymentCategory category,
			@RequestParam(required = false) List<PaymentStatus> status,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Long studentId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		Specification<Payment> spec = Specification.allOf(
				PaymentSpecifications.inCategory(category),
				PaymentSpecifications.hasStatus(status),
				PaymentSpecifications.studentMatches(search),
				PaymentSpecifications.forStudent(studentId));

		var pageable = PageRequest.of(page, Math.min(size, 100),
				Sort.by(Sort.Direction.DESC, "createdAt"));

		return repository.findAll(spec, pageable).map(PaymentResponse::from);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Detail satu pembayaran beserta hasil OCR mentahnya")
	@Transactional(readOnly = true)
	public PaymentResponse get(@PathVariable Long id) {
		return repository.findWithDetailsById(id)
				.map(PaymentResponse::from)
				.orElseThrow(() -> NotFoundException.of("Pembayaran", id));
	}

	@PostMapping("/{id}/decide")
	@Operation(summary = "Verifikasi atau tolak secara manual")
	public PaymentResponse decide(
			@PathVariable Long id, @Valid @RequestBody DecideRequest request) {

		return PaymentResponse.from(service.decide(
				id, request.approve(), request.note(), AuthService.currentUserId()));
	}

	@PostMapping("/{id}/requeue")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(summary = "Baca ulang bukti bayar dengan OCR")
	public void requeue(@PathVariable Long id) {
		service.requeue(id);
	}

	@GetMapping("/{id}/logs")
	@Operation(summary = "Riwayat perpindahan status satu pembayaran")
	public List<LogResponse> logs(@PathVariable Long id) {
		return logRepository.findByPaymentIdOrderByCreatedAtAsc(id).stream()
				.map(LogResponse::from)
				.toList();
	}
}
