package ac.kampus.pembayaran.tuition;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.common.PaymentCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ac.kampus.pembayaran.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
	private final StudentRepository studentRepository;

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
			String tier,
			String label,
			BigDecimal percent,
			boolean active,
			int sortOrder,
			/** UKT satu semester setelah potongan, memakai tarif dasar tahun yang diminta. */
			BigDecimal uktPerSemester,
			BigDecimal uktPerInstallment
	) {
	}

	public record TierRequest(
			@NotNull(message = "Persentase wajib diisi.")
			@DecimalMin(value = "0", message = "Persentase tidak boleh negatif.")
			@DecimalMax(value = "100", message = "Persentase tidak boleh lebih dari 100.")
			BigDecimal percent,

			@Size(max = 60, message = "Nama golongan maksimal 60 karakter.")
			String label,

			Boolean active,
			Integer sortOrder
	) {
	}

	/** Golongan baru; kodenya ikut dikirim karena belum ada di jalur URL. */
	public record TierBaruRequest(
			@NotBlank(message = "Kode golongan wajib diisi.")
			@Pattern(
					regexp = "^[A-Z][A-Z0-9_]*$",
					message = "Kode hanya boleh huruf kapital, angka, dan garis bawah, "
							+ "contoh: MITRA_INSTANSI.")
			@Size(max = 40, message = "Kode golongan maksimal 40 karakter.")
			String tier,

			@NotBlank(message = "Nama golongan wajib diisi.")
			@Size(max = 60, message = "Nama golongan maksimal 60 karakter.")
			String label,

			@NotNull(message = "Persentase wajib diisi.")
			@DecimalMin(value = "0", message = "Persentase tidak boleh negatif.")
			@DecimalMax(value = "100", message = "Persentase tidak boleh lebih dari 100.")
			BigDecimal percent,

			Integer sortOrder
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

		return tierRepository.findAllByOrderBySortOrderAscTierAsc().stream()
				.map(tier -> {
					BigDecimal perSemester = tier.applyTo(baseUkt);
					return new TierResponse(
							tier.getTier(),
							tier.getLabel(),
							tier.getPercent(),
							tier.isActive(),
							tier.getSortOrder(),
							perSemester,
							perSemester.divide(BigDecimal.valueOf(5), 0, java.math.RoundingMode.DOWN));
				})
				.toList();
	}

	@PostMapping("/tiers")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Tambah golongan potongan baru")
	@Transactional
	public TierResponse createTier(@Valid @RequestBody TierBaruRequest request) {
		if (tierRepository.existsById(request.tier())) {
			throw new BusinessRuleException(
					"Golongan dengan kode %s sudah ada.".formatted(request.tier()));
		}

		tierRepository.save(DiscountTierRate.builder()
				.tier(request.tier())
				.label(request.label().trim())
				.percent(request.percent())
				.active(true)
				.sortOrder(request.sortOrder() == null ? urutanBerikutnya() : request.sortOrder())
				.build());

		return cariTier(request.tier());
	}

	@PutMapping("/tiers/{tier}")
	@Operation(summary = "Ubah golongan potongan")
	@Transactional
	public TierResponse updateTier(
			@PathVariable String tier,
			@Valid @RequestBody TierRequest request) {

		DiscountTierRate entity = tierRepository.findById(tier)
				.orElseThrow(() -> NotFoundException.of("Golongan", tier));

		entity.setPercent(request.percent());
		if (request.label() != null && !request.label().isBlank()) {
			entity.setLabel(request.label().trim());
		}
		if (request.sortOrder() != null) {
			entity.setSortOrder(request.sortOrder());
		}
		if (request.active() != null) {
			// Golongan yang masih dipakai mahasiswa tidak boleh dinonaktifkan:
			// tagihan berikutnya untuk mereka tidak akan bisa dihitung.
			if (!request.active()) {
				long dipakai = studentRepository.countByDiscountTier(tier);
				if (dipakai > 0) {
					throw new BusinessRuleException(
							("Golongan ini masih dipakai %d mahasiswa. Pindahkan mereka dulu "
									+ "sebelum menonaktifkannya.").formatted(dipakai));
				}
			}
			entity.setActive(request.active());
		}
		tierRepository.save(entity);

		return cariTier(tier);
	}

	private TierResponse cariTier(String tier) {
		return tiers("2026/2027").stream()
				.filter(response -> response.tier().equals(tier))
				.findFirst()
				.orElseThrow(() -> NotFoundException.of("Golongan", tier));
	}

	private int urutanBerikutnya() {
		return tierRepository.findAll().stream()
				.mapToInt(DiscountTierRate::getSortOrder)
				.max()
				.orElse(0) + 1;
	}
}
