package ac.kampus.pembayaran.tuition;

import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.student.DiscountTier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/tuition")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Tarif & Potongan")
public class TuitionController {

	private final TuitionRateRepository rateRepository;
	private final DiscountTierRateRepository tierRepository;

	public record RateRequest(
			@NotNull(message = "Kategori wajib dipilih.")
			PaymentCategory category,

			@NotNull(message = "Tahun akademik wajib diisi.")
			@Pattern(regexp = "\\d{4}/\\d{4}", message = "Format tahun akademik harus 2026/2027.")
			String academicYear,

			@NotNull(message = "Nominal wajib diisi.")
			@DecimalMin(value = "1", message = "Nominal harus lebih besar dari nol.")
			BigDecimal amount,

			Boolean active
	) {
	}

	public record RateResponse(
			Long id,
			PaymentCategory category,
			String categoryLabel,
			String academicYear,
			BigDecimal amount,
			boolean active
	) {
		static RateResponse from(TuitionRate rate) {
			return new RateResponse(
					rate.getId(),
					rate.getCategory(),
					rate.getCategory().label(),
					rate.getAcademicYear(),
					rate.getAmount(),
					rate.isActive());
		}
	}

	public record TierResponse(
			DiscountTier tier,
			String label,
			BigDecimal percent,
			/** UKT satu semester setelah potongan, memakai tarif dasar tahun yang diminta. */
			BigDecimal uktPerSemester,
			BigDecimal uktPerInstallment
	) {
	}

	public record TierRequest(
			@NotNull(message = "Persentase wajib diisi.")
			@DecimalMin(value = "0", message = "Persentase tidak boleh negatif.")
			BigDecimal percent
	) {
	}

	@GetMapping("/rates")
	@Operation(summary = "Daftar tarif dasar per kategori")
	public List<RateResponse> rates() {
		return rateRepository.findAllByOrderByAcademicYearDescCategoryAsc().stream()
				.map(RateResponse::from)
				.toList();
	}

	@PostMapping("/rates")
	@Operation(summary = "Tambah tarif dasar")
	@Transactional
	public RateResponse createRate(@Valid @RequestBody RateRequest request) {
		TuitionRate rate = TuitionRate.builder()
				.category(request.category())
				.academicYear(request.academicYear())
				.amount(request.amount())
				.active(request.active() == null || request.active())
				.build();
		return RateResponse.from(rateRepository.save(rate));
	}

	@PutMapping("/rates/{id}")
	@Operation(summary = "Ubah tarif dasar")
	@Transactional
	public RateResponse updateRate(@PathVariable Long id, @Valid @RequestBody RateRequest request) {
		TuitionRate rate = rateRepository.findById(id)
				.orElseThrow(() -> NotFoundException.of("Tarif", id));

		rate.setCategory(request.category());
		rate.setAcademicYear(request.academicYear());
		rate.setAmount(request.amount());
		if (request.active() != null) rate.setActive(request.active());

		return RateResponse.from(rateRepository.save(rate));
	}

	@GetMapping("/tiers")
	@Operation(summary = "Daftar golongan potongan beserta hitungan UKT-nya")
	public List<TierResponse> tiers(
			@RequestParam(defaultValue = "2026/2027") String academicYear) {

		BigDecimal baseUkt = rateRepository
				.findByCategoryAndAcademicYearAndActiveTrue(PaymentCategory.UKT, academicYear)
				.map(TuitionRate::getAmount)
				.orElseThrow(() -> new NotFoundException(
						"Tarif UKT untuk tahun %s belum diatur.".formatted(academicYear)));

		return tierRepository.findAllByOrderByPercentAsc().stream()
				.map(tier -> {
					BigDecimal perSemester = tier.applyTo(baseUkt);
					return new TierResponse(
							tier.getTier(),
							tier.getLabel(),
							tier.getPercent(),
							perSemester,
							perSemester.divide(BigDecimal.valueOf(5), 0, java.math.RoundingMode.DOWN));
				})
				.toList();
	}

	@PutMapping("/tiers/{tier}")
	@Operation(summary = "Ubah persentase potongan satu golongan")
	@Transactional
	public TierResponse updateTier(
			@PathVariable DiscountTier tier,
			@Valid @RequestBody TierRequest request) {

		DiscountTierRate entity = tierRepository.findById(tier)
				.orElseThrow(() -> NotFoundException.of("Golongan", tier));

		entity.setPercent(request.percent());
		tierRepository.save(entity);

		return tiers("2026/2027").stream()
				.filter(response -> response.tier() == tier)
				.findFirst()
				.orElseThrow(() -> NotFoundException.of("Golongan", tier));
	}
}
