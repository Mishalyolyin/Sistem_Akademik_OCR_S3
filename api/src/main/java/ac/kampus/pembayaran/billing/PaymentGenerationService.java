package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.template.InstallmentTemplate;
import ac.kampus.pembayaran.template.InstallmentTemplateItem;
import ac.kampus.pembayaran.template.InstallmentTemplateRepository;
import ac.kampus.pembayaran.tuition.DiscountTierRate;
import ac.kampus.pembayaran.tuition.DiscountTierRateRepository;
import ac.kampus.pembayaran.tuition.TuitionRate;
import ac.kampus.pembayaran.tuition.TuitionRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Membuat tagihan beserta cicilannya.
 *
 * <p>Aturan yang ditegakkan di sini (lihat RENCANA_V2.md bagian Aturan Bisnis):
 * <ul>
 *   <li>Nominal UKT dihitung dari tarif dasar dikali potongan golongan, tidak diketik admin.</li>
 *   <li>Tarif dan persen potongan dibekukan saat plan dibuat.</li>
 *   <li>Sisa pembagian masuk ke cicilan TERAKHIR, supaya jumlahnya persis.</li>
 *   <li>Maksimal 6 semester UKT.</li>
 *   <li>Kategori sekali bayar hanya boleh sekali seumur studi.</li>
 *   <li>Empat tahap ujian harus lunas berurutan.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGenerationService {

	public static final int MAX_SEMESTER_UKT = 6;

	private final PaymentPlanRepository planRepository;
	private final InstallmentTemplateRepository templateRepository;
	private final TuitionRateRepository rateRepository;
	private final DiscountTierRateRepository tierRepository;

	@Transactional
	public PaymentPlan generate(
			Student student, PaymentCategory category, String academicYear, AcademicTerm term) {

		Integer semesterNumber = category == PaymentCategory.UKT
				? nextUktSemester(student)
				: null;

		assertNotDuplicated(student, category, academicYear, term);
		assertExamSequence(student, category);

		TuitionRate rate = rateRepository
				.findByCategoryAndAcademicYearAndActiveTrue(category, academicYear)
				.orElseThrow(() -> new NotFoundException(
						"Tarif %s untuk tahun %s belum diatur."
								.formatted(category.label(), academicYear)));

		InstallmentTemplate template = templateRepository
				.findByCategoryAndTermAndActiveTrue(category, term)
				.orElseThrow(() -> new NotFoundException(
						"Template angsuran %s untuk term %s belum diatur."
								.formatted(category.label(), term)));

		if (template.getItems().isEmpty()) {
			throw new BusinessRuleException(
					"Template \"%s\" belum punya jadwal cicilan.".formatted(template.getName()));
		}

		// Potongan hanya berlaku untuk UKT.
		BigDecimal discountPercent = BigDecimal.ZERO;
		BigDecimal total = rate.getAmount();

		if (category.discountable()) {
			DiscountTierRate tier = tierRepository.findById(student.getDiscountTier())
					.orElseThrow(() -> new NotFoundException(
							"Persentase potongan untuk golongan %s belum diatur."
									.formatted(student.getDiscountTier())));
			discountPercent = tier.getPercent();
			total = tier.applyTo(rate.getAmount());
		}

		PaymentPlan plan = PaymentPlan.builder()
				.student(student)
				.installmentTemplateId(template.getId())
				.category(category)
				.academicYear(academicYear)
				.term(term)
				.semesterNumber(semesterNumber)
				.baseAmount(rate.getAmount())
				.discountPercent(discountPercent)
				.totalAmount(total)
				.status(PlanStatus.ACTIVE)
				.build();

		for (Installment installment : buildInstallments(template, academicYear, term, total)) {
			plan.addInstallment(installment);
		}

		PaymentPlan saved = planRepository.save(plan);
		log.info("Tagihan {} dibuat untuk {} ({}): {} dalam {} cicilan",
				category, student.getNim(), academicYear, total, saved.getInstallments().size());
		return saved;
	}

	/**
	 * Membagi total ke sejumlah cicilan. Pembagian dibulatkan ke bawah dan
	 * SISANYA ditambahkan ke cicilan terakhir, sehingga jumlah seluruh cicilan
	 * selalu sama persis dengan total — tidak ada rupiah yang hilang atau
	 * bertambah karena pembulatan.
	 */
	private List<Installment> buildInstallments(
			InstallmentTemplate template, String academicYear, AcademicTerm term, BigDecimal total) {

		List<InstallmentTemplateItem> items = template.getItems();
		int count = items.size();

		BigDecimal base = total.divideToIntegralValue(BigDecimal.valueOf(count));
		BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(count)));

		LocalDate termStart = termStartDate(academicYear, term);

		return items.stream()
				.map(item -> {
					boolean last = item.getInstallmentNo() == count;
					BigDecimal amount = last ? base.add(remainder) : base;

					return Installment.builder()
							.installmentNo(item.getInstallmentNo())
							.dueDate(termStart
									.plusMonths(item.getMonthOffset())
									.withDayOfMonth(item.getDueDay()))
							.amount(amount)
							.amountPaid(BigDecimal.ZERO)
							.status(InstallmentStatus.UNPAID)
							.build();
				})
				.toList();
	}

	/**
	 * Bulan pertama semester. Gasal mulai September di tahun pertama,
	 * Genap mulai Februari di tahun kedua. Contoh "2026/2027":
	 * Gasal → September 2026, Genap → Februari 2027.
	 */
	static LocalDate termStartDate(String academicYear, AcademicTerm term) {
		String[] parts = academicYear.split("/");
		if (parts.length != 2) {
			throw new BusinessRuleException(
					"Tahun akademik \"%s\" salah format. Contoh: 2026/2027.".formatted(academicYear));
		}

		int year = Integer.parseInt(term == AcademicTerm.GASAL ? parts[0] : parts[1]);
		return LocalDate.of(year, term.startMonth(), 1);
	}

	private Integer nextUktSemester(Student student) {
		long sudahAda = planRepository.countByStudentIdAndCategoryAndStatusNot(
				student.getId(), PaymentCategory.UKT, PlanStatus.CANCELLED);

		if (sudahAda >= MAX_SEMESTER_UKT) {
			throw new BusinessRuleException(
					"%s sudah punya %d tagihan UKT. Program Doktor hanya %d semester."
							.formatted(student.getName(), sudahAda, MAX_SEMESTER_UKT));
		}

		return planRepository.lastSemesterNumber(
				student.getId(), PaymentCategory.UKT, PlanStatus.CANCELLED) + 1;
	}

	private void assertNotDuplicated(
			Student student, PaymentCategory category, String academicYear, AcademicTerm term) {

		if (category == PaymentCategory.UKT) {
			boolean ada = planRepository
					.existsByStudentIdAndCategoryAndAcademicYearAndTermAndStatusNot(
							student.getId(), category, academicYear, term, PlanStatus.CANCELLED);
			if (ada) {
				throw new BusinessRuleException(
						"%s sudah punya tagihan UKT untuk %s %s."
								.formatted(student.getName(), academicYear, term));
			}
			return;
		}

		// Pendaftaran dan keempat tahap ujian hanya sekali seumur studi.
		boolean ada = planRepository.existsByStudentIdAndCategoryAndStatusNot(
				student.getId(), category, PlanStatus.CANCELLED);
		if (ada) {
			throw new BusinessRuleException(
					"%s sudah pernah ditagih %s.".formatted(student.getName(), category.label()));
		}
	}

	/**
	 * Tahap ujian harus lunas berurutan. Pengecekan ini WAJIB di backend,
	 * bukan sekadar menyembunyikan tombol di antarmuka.
	 */
	private void assertExamSequence(Student student, PaymentCategory category) {
		category.previousExamStage().ifPresent(sebelumnya -> {
			PaymentPlan plan = planRepository
					.findByStudentIdAndCategoryAndStatusNot(
							student.getId(), sebelumnya, PlanStatus.CANCELLED)
					.orElseThrow(() -> new BusinessRuleException(
							"%s harus lunas dulu sebelum mendaftar %s."
									.formatted(sebelumnya.label(), category.label())));

			if (!plan.isFullyPaid()) {
				throw new BusinessRuleException(
						"%s belum lunas (sisa %s). Lunasi dulu sebelum mendaftar %s."
								.formatted(sebelumnya.label(), plan.remaining(), category.label()));
			}
		});
	}
}
