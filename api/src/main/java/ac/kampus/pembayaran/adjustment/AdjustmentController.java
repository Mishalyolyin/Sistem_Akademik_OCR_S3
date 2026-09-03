package ac.kampus.pembayaran.adjustment;

import ac.kampus.pembayaran.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Penyesuaian saldo dan cicilan, selalu dalam konteks satu mahasiswa.
 *
 * <p>Sengaja bersarang di bawah {@code /students/{studentId}} supaya pemiliknya
 * tidak pernah datang dari badan permintaan: cicilan yang disesuaikan wajib
 * benar-benar milik mahasiswa di jalur URL.
 */
@RestController
@RequestMapping("/students/{studentId}/adjustments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Penyesuaian")
public class AdjustmentController {

	private final AdjustmentService service;

	// --- DTO ---

	public record AdjustmentRequest(
			/** Kosong berarti penyesuaian mengenai saldo, bukan cicilan tertentu. */
			Long installmentId,

			@NotNull(message = "Nominal wajib diisi.")
			BigDecimal amount,

			@NotNull(message = "Alasan wajib diisi.")
			@Size(min = 5, max = 500, message = "Alasan minimal 5 karakter.")
			String reason
	) {
	}

	public record AdjustmentResponse(
			Long id,
			Long installmentId,
			BigDecimal amount,
			BigDecimal balanceAfter,
			String reason,
			Long adminId,
			Instant createdAt,
			String target
	) {
		static AdjustmentResponse from(Adjustment a) {
			return new AdjustmentResponse(
					a.getId(), a.getInstallmentId(), a.getAmount(), a.getBalanceAfter(),
					a.getReason(), a.getAdminId(), a.getCreatedAt(),
					a.mengenaiCicilan() ? "CICILAN" : "SALDO");
		}
	}

	// --- Endpoint ---

	@GetMapping
	@Operation(summary = "Riwayat penyesuaian seorang mahasiswa")
	public List<AdjustmentResponse> riwayat(@PathVariable Long studentId) {
		return service.riwayat(studentId).stream().map(AdjustmentResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Buat penyesuaian saldo atau cicilan")
	public AdjustmentResponse buat(@PathVariable Long studentId,
			@Valid @RequestBody AdjustmentRequest request) {
		Long adminId = AuthService.currentUserId();

		Adjustment hasil = request.installmentId() == null
				? service.sesuaikanSaldo(studentId, request.amount(), request.reason(), adminId)
				: service.sesuaikanCicilan(studentId, request.installmentId(),
						request.amount(), request.reason(), adminId);

		return AdjustmentResponse.from(hasil);
	}
}
