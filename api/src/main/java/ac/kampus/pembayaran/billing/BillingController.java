package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.auth.AuthService;
import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.student.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Tagihan")
public class BillingController {

	private final PaymentGenerationService generationService;
	private final InstallmentBillingService billingService;
	private final PaymentPlanRepository planRepository;
	private final InstallmentAmountChangeRepository changeRepository;
	private final StudentService studentService;
	private final PlanCancellationService cancellationService;
	private final UktAutoService uktAutoService;

	// --- DTO ---

	public record InstallmentResponse(
			Long id,
			int installmentNo,
			LocalDate dueDate,
			BigDecimal amount,
			BigDecimal amountPaid,
			BigDecimal outstanding,
			InstallmentStatus status
	) {
		static InstallmentResponse from(Installment i) {
			return new InstallmentResponse(
					i.getId(), i.getInstallmentNo(), i.getDueDate(),
					i.getAmount(), i.getAmountPaid(), i.outstanding(), i.getStatus());
		}
	}

	public record PlanResponse(
			Long id,
			PaymentCategory category,
			String categoryLabel,
			String academicYear,
			AcademicTerm term,
			Integer semesterNumber,
			BigDecimal baseAmount,
			BigDecimal discountPercent,
			BigDecimal totalAmount,
			BigDecimal amountPaid,
			BigDecimal remaining,
			PlanStatus status,
			/** Terisi hanya bila status CANCELLED; layar memakainya sebagai keterangan. */
			Instant cancelledAt,
			String cancelReason,
			List<InstallmentResponse> installments
	) {
		static PlanResponse from(PaymentPlan plan) {
			return new PlanResponse(
					plan.getId(), plan.getCategory(), plan.getCategory().label(),
					plan.getAcademicYear(), plan.getTerm(), plan.getSemesterNumber(),
					plan.getBaseAmount(), plan.getDiscountPercent(), plan.getTotalAmount(),
					plan.amountPaid(), plan.remaining(), plan.getStatus(),
					plan.getCancelledAt(), plan.getCancelReason(),
					plan.getInstallments().stream().map(InstallmentResponse::from).toList());
		}
	}

	public record CancelPlanRequest(
			@NotNull(message = "Alasan wajib diisi.")
			@Size(min = 5, max = 500, message = "Alasan minimal 5 karakter.")
			String reason
	) {
	}

	public record UpdateAmountRequest(
			@NotNull(message = "Nominal wajib diisi.")
			@DecimalMin(value = "1", message = "Nominal harus lebih besar dari nol.")
			BigDecimal amount,

			@NotNull(message = "Alasan wajib diisi.")
			@Size(min = 5, max = 500, message = "Alasan minimal 5 karakter.")
			String reason
	) {
	}

	public record AmountChangeResponse(
			Long id,
			Long installmentId,
			BigDecimal oldAmount,
			BigDecimal newAmount,
			String reason,
			Long adminId,
			Instant createdAt
	) {
		static AmountChangeResponse from(InstallmentAmountChange change) {
			return new AmountChangeResponse(
					change.getId(), change.getInstallmentId(), change.getOldAmount(),
					change.getNewAmount(), change.getReason(), change.getAdminId(),
					change.getCreatedAt());
		}
	}

	// --- Endpoint ---

	/**
	 * Menjalankan putaran pembuatan tagihan UKT sekarang, tanpa menunggu
	 * jadwalnya. Jalur ini memakai kode yang sama persis dengan penjadwal —
	 * bukan jalan kedua, hanya pemicu lain untuk jalan yang sama.
	 */
	@PostMapping("/tagihan-ukt/jalankan")
	@Operation(summary = "Buat tagihan UKT yang sudah waktunya untuk semua mahasiswa aktif")
	public UktAutoService.Hasil jalankanUktOtomatis() {
		return uktAutoService.jalankan(LocalDate.now());
	}

	@GetMapping("/students/{studentId}/plans")
	@Operation(summary = "Semua tagihan milik satu mahasiswa")
	@Transactional(readOnly = true)
	public List<PlanResponse> listForStudent(@PathVariable Long studentId) {
		return planRepository.findByStudentIdOrderByCategoryAscSemesterNumberAsc(studentId)
				.stream()
				.map(PlanResponse::from)
				.toList();
	}

	@GetMapping("/plans/{planId}")
	@Operation(summary = "Detail satu tagihan beserta cicilannya")
	@Transactional(readOnly = true)
	public PlanResponse get(@PathVariable Long planId) {
		return planRepository.findWithInstallmentsById(planId)
				.map(PlanResponse::from)
				.orElseThrow(() -> NotFoundException.of("Tagihan", planId));
	}

	@PatchMapping("/plans/{planId}/cancel")
	@Operation(summary = "Batalkan satu tagihan yang salah dibuat; alasan wajib. "
			+ "Tagihan yang sudah menerima pembayaran terverifikasi ditolak.")
	public PlanResponse cancel(
			@PathVariable Long planId,
			@Valid @RequestBody CancelPlanRequest request) {

		return PlanResponse.from(cancellationService.batalkan(
				planId, request.reason(), AuthService.currentUserId()));
	}

	@PatchMapping("/installments/{installmentId}/amount")
	@Operation(summary = "Ubah nominal satu cicilan; alasan wajib dan tercatat di audit")
	public AmountChangeResponse updateAmount(
			@PathVariable Long installmentId,
			@Valid @RequestBody UpdateAmountRequest request) {

		return AmountChangeResponse.from(billingService.updateAmount(
				installmentId, request.amount(), request.reason(), AuthService.currentUserId()));
	}

	@GetMapping("/installments/{installmentId}/amount-changes")
	@Operation(summary = "Riwayat perubahan nominal satu cicilan")
	public List<AmountChangeResponse> amountChanges(@PathVariable Long installmentId) {
		return changeRepository.findByInstallmentIdOrderByCreatedAtDesc(installmentId).stream()
				.map(AmountChangeResponse::from)
				.toList();
	}
}
