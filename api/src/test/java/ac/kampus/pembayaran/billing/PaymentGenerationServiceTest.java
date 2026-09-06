package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.template.InstallmentTemplate;
import ac.kampus.pembayaran.template.InstallmentTemplateItem;
import ac.kampus.pembayaran.template.InstallmentTemplateRepository;
import ac.kampus.pembayaran.tuition.DiscountTierRate;
import ac.kampus.pembayaran.tuition.DiscountTierRateRepository;
import ac.kampus.pembayaran.tuition.TuitionRate;
import ac.kampus.pembayaran.tuition.TuitionRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Aturan hitungan tagihan. Ini bagian uang, jadi tiap aturan di RENCANA_V2.md
 * punya test-nya sendiri.
 */
class PaymentGenerationServiceTest {

	private PaymentPlanRepository planRepository;
	private InstallmentTemplateRepository templateRepository;
	private TuitionRateRepository rateRepository;
	private DiscountTierRateRepository tierRepository;
	private PaymentGenerationService service;

	private static final String TAHUN = "2026/2027";
	private static final BigDecimal UKT_DASAR = new BigDecimal("10000000");

	@BeforeEach
	void setUp() {
		planRepository = mock(PaymentPlanRepository.class);
		templateRepository = mock(InstallmentTemplateRepository.class);
		rateRepository = mock(TuitionRateRepository.class);
		tierRepository = mock(DiscountTierRateRepository.class);

		service = new PaymentGenerationService(
				planRepository, templateRepository, rateRepository, tierRepository);

		// Simpan mengembalikan objek yang sama, supaya isi plan bisa diperiksa.
		when(planRepository.save(any(PaymentPlan.class)))
				.thenAnswer((Answer<PaymentPlan>) invocation -> invocation.getArgument(0));
		when(planRepository.countByStudentIdAndCategoryAndStatusNot(anyLong(), any(), any()))
				.thenReturn(0L);
		when(planRepository.lastSemesterNumber(anyLong(), any(), any())).thenReturn(0);
	}

	/**
	 * Membuat seluruh enam semester UKT tampak sudah ditagihkan dan lunas.
	 *
	 * <p>Dipakai test yang ingin menguji aturan LAIN pada tahap ujian: sejak
	 * syarat lunas UKT ada, gerbang itu berdiri lebih dulu dan akan menjawab
	 * setiap pendaftaran ujian sebelum aturan urutan sempat diperiksa.
	 */
	private void uktLunasEnamSemester() {
		List<PaymentPlan> plans = new ArrayList<>();
		for (int semester = 1; semester <= 6; semester++) {
			PaymentPlan plan = PaymentPlan.builder()
					.id((long) (100 + semester))
					.category(PaymentCategory.UKT)
					.semesterNumber(semester)
					.totalAmount(new BigDecimal("10000000"))
					.status(PlanStatus.ACTIVE)
					.build();
			plan.addInstallment(Installment.builder()
					.installmentNo(1)
					.amount(new BigDecimal("10000000"))
					.amountPaid(new BigDecimal("10000000"))
					.status(InstallmentStatus.PAID)
					.build());
			plans.add(plan);
		}
		when(planRepository.findByStudentIdOrderByCategoryAscSemesterNumberAsc(anyLong()))
				.thenReturn(plans);
	}

	private Student mahasiswa(String tier) {
		return Student.builder()
				.id(1L)
				.nim("2612600001")
				.name("Uji Coba")
				.discountTier(tier)
				.startTerm(AcademicTerm.GASAL)
				.startAcademicYear(TAHUN)
				.walletBalance(BigDecimal.ZERO)
				.active(true)
				.build();
	}

	private void siapkanTarif(PaymentCategory category, BigDecimal amount) {
		when(rateRepository.findByCategoryAndAcademicYearAndActiveTrue(category, TAHUN))
				.thenReturn(Optional.of(TuitionRate.builder()
						.id(1L).category(category).academicYear(TAHUN)
						.amount(amount).active(true).build()));
	}

	private void siapkanPotongan(String tier, String percent) {
		var rate = new DiscountTierRate();
		rate.setTier(tier);
		rate.setLabel(tier);
		rate.setPercent(new BigDecimal(percent));
		when(tierRepository.findById(tier)).thenReturn(Optional.of(rate));
	}

	private void siapkanTemplate(PaymentCategory category, AcademicTerm term, int jumlahCicilan) {
		List<InstallmentTemplateItem> items = new ArrayList<>();
		for (int i = 1; i <= jumlahCicilan; i++) {
			items.add(InstallmentTemplateItem.builder()
					.id((long) i).installmentNo(i).monthOffset(i - 1).dueDay(10).build());
		}

		when(templateRepository.findByCategoryAndTermAndActiveTrue(category, term))
				.thenReturn(Optional.of(InstallmentTemplate.builder()
						.id(1L).name("Uji").category(category).term(term)
						.installmentsCount(jumlahCicilan).active(true).items(items).build()));
	}

	@Nested
	@DisplayName("Nominal UKT per golongan")
	class NominalUkt {

		@ParameterizedTest(name = "{0} potongan {1}% -> {2} per semester, {3} per cicilan")
		@CsvSource({
				"NON_ALUMNI,      0, 10000000, 2000000",
				"KERABAT_ALUMNI, 20,  8000000, 1600000",
				"ALUMNI,         25,  7500000, 1500000",
				"ALUMNI_PASUTRI, 35,  6500000, 1300000",
				"KERJASAMA,      40,  6000000, 1200000",
		})
		void sesuaiBrosur(String tier, String persen, String perSemester, String perCicilan) {
			siapkanTarif(PaymentCategory.UKT, UKT_DASAR);
			siapkanPotongan(tier, persen);
			siapkanTemplate(PaymentCategory.UKT, AcademicTerm.GASAL, 5);

			PaymentPlan plan = service.generate(
					mahasiswa(tier), PaymentCategory.UKT, TAHUN, AcademicTerm.GASAL);

			assertThat(plan.getTotalAmount()).isEqualByComparingTo(perSemester);
			assertThat(plan.getInstallments()).hasSize(5);
			assertThat(plan.getInstallments())
					.allSatisfy(i -> assertThat(i.getAmount()).isEqualByComparingTo(perCicilan));
		}

		@Test
		@DisplayName("jumlah seluruh cicilan sama persis dengan total, tanpa selisih pembulatan")
		void jumlahCicilanSamaDenganTotal() {
			// 10.000.001 tidak habis dibagi 5.
			siapkanTarif(PaymentCategory.UKT, new BigDecimal("10000001"));
			siapkanPotongan("NON_ALUMNI", "0");
			siapkanTemplate(PaymentCategory.UKT, AcademicTerm.GASAL, 5);

			PaymentPlan plan = service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.UKT, TAHUN, AcademicTerm.GASAL);

			BigDecimal jumlah = plan.getInstallments().stream()
					.map(Installment::getAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			assertThat(jumlah).isEqualByComparingTo(plan.getTotalAmount());
			// Sisa pembagian menempel di cicilan TERAKHIR.
			assertThat(plan.getInstallments().get(4).getAmount())
					.isEqualByComparingTo("2000001");
			assertThat(plan.getInstallments().get(0).getAmount())
					.isEqualByComparingTo("2000000");
		}

		@Test
		@DisplayName("tarif dan potongan dibekukan di plan")
		void tarifDibekukan() {
			siapkanTarif(PaymentCategory.UKT, UKT_DASAR);
			siapkanPotongan("ALUMNI", "25");
			siapkanTemplate(PaymentCategory.UKT, AcademicTerm.GASAL, 5);

			PaymentPlan plan = service.generate(
					mahasiswa("ALUMNI"), PaymentCategory.UKT, TAHUN, AcademicTerm.GASAL);

			assertThat(plan.getBaseAmount()).isEqualByComparingTo(UKT_DASAR);
			assertThat(plan.getDiscountPercent()).isEqualByComparingTo("25");
		}
	}

	@Nested
	@DisplayName("Jadwal jatuh tempo")
	class JatuhTempo {

		@Test
		@DisplayName("Gasal: September sampai Januari")
		void gasal() {
			siapkanTarif(PaymentCategory.UKT, UKT_DASAR);
			siapkanPotongan("NON_ALUMNI", "0");
			siapkanTemplate(PaymentCategory.UKT, AcademicTerm.GASAL, 5);

			List<LocalDate> tanggal = service
					.generate(mahasiswa("NON_ALUMNI"), PaymentCategory.UKT, TAHUN, AcademicTerm.GASAL)
					.getInstallments().stream().map(Installment::getDueDate).toList();

			assertThat(tanggal).containsExactly(
					LocalDate.of(2026, Month.SEPTEMBER, 10),
					LocalDate.of(2026, Month.OCTOBER, 10),
					LocalDate.of(2026, Month.NOVEMBER, 10),
					LocalDate.of(2026, Month.DECEMBER, 10),
					LocalDate.of(2027, Month.JANUARY, 10));
		}

		@Test
		@DisplayName("Genap: Februari sampai Juni, memakai tahun kedua")
		void genap() {
			siapkanTarif(PaymentCategory.UKT, UKT_DASAR);
			siapkanPotongan("NON_ALUMNI", "0");
			siapkanTemplate(PaymentCategory.UKT, AcademicTerm.GENAP, 5);

			List<LocalDate> tanggal = service
					.generate(mahasiswa("NON_ALUMNI"), PaymentCategory.UKT, TAHUN, AcademicTerm.GENAP)
					.getInstallments().stream().map(Installment::getDueDate).toList();

			assertThat(tanggal).containsExactly(
					LocalDate.of(2027, Month.FEBRUARY, 10),
					LocalDate.of(2027, Month.MARCH, 10),
					LocalDate.of(2027, Month.APRIL, 10),
					LocalDate.of(2027, Month.MAY, 10),
					LocalDate.of(2027, Month.JUNE, 10));
		}
	}

	@Nested
	@DisplayName("Batas dan larangan")
	class Batasan {

		@Test
		@DisplayName("tagihan UKT ke-7 ditolak")
		void maksimalEnamSemester() {
			when(planRepository.countByStudentIdAndCategoryAndStatusNot(anyLong(), any(), any()))
					.thenReturn(6L);

			assertThatThrownBy(() -> service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.UKT, TAHUN, AcademicTerm.GASAL))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("hanya 6 semester");
		}

		@Test
		@DisplayName("UKT ganda untuk tahun dan term yang sama ditolak")
		void tidakBolehGanda() {
			when(planRepository.existsByStudentIdAndCategoryAndAcademicYearAndTermAndStatusNot(
					anyLong(), any(), any(), any(), any())).thenReturn(true);

			assertThatThrownBy(() -> service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.UKT, TAHUN, AcademicTerm.GASAL))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("sudah punya tagihan UKT");
		}

		@Test
		@DisplayName("Pendaftaran hanya boleh sekali seumur studi")
		void pendaftaranSekaliSaja() {
			when(planRepository.existsByStudentIdAndCategoryAndStatusNot(anyLong(), any(), any()))
					.thenReturn(true);

			assertThatThrownBy(() -> service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.PENDAFTARAN, TAHUN, AcademicTerm.GASAL))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("sudah pernah ditagih");
		}

		@Test
		@DisplayName("biaya ujian tidak kena potongan walau mahasiswanya kerjasama")
		void ujianTanpaPotongan() {
			uktLunasEnamSemester();

			siapkanTarif(PaymentCategory.SEMINAR_PROPOSAL, new BigDecimal("5000000"));
			siapkanTemplate(PaymentCategory.SEMINAR_PROPOSAL, AcademicTerm.GASAL, 1);

			PaymentPlan plan = service.generate(
					mahasiswa("KERJASAMA"), PaymentCategory.SEMINAR_PROPOSAL,
					TAHUN, AcademicTerm.GASAL);

			assertThat(plan.getTotalAmount()).isEqualByComparingTo("5000000");
			assertThat(plan.getDiscountPercent()).isEqualByComparingTo("0");
			assertThat(plan.getSemesterNumber()).isNull();
		}
	}

	@Nested
	@DisplayName("Urutan wajib tahap ujian")
	class UrutanUjian {

		@Test
		@DisplayName("Ujian Kelayakan ditolak bila Seminar Proposal belum pernah ditagih")
		void tahapSebelumnyaBelumAda() {
			uktLunasEnamSemester();

			assertThatThrownBy(() -> service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.UJIAN_KELAYAKAN,
					TAHUN, AcademicTerm.GASAL))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("Seminar Proposal harus lunas dulu");
		}

		@Test
		@DisplayName("Ujian Kelayakan ditolak bila Seminar Proposal belum lunas")
		void tahapSebelumnyaBelumLunas() {
			uktLunasEnamSemester();

			PaymentPlan belumLunas = PaymentPlan.builder()
					.id(9L)
					.category(PaymentCategory.SEMINAR_PROPOSAL)
					.totalAmount(new BigDecimal("5000000"))
					.status(PlanStatus.ACTIVE)
					.build();
			belumLunas.addInstallment(Installment.builder()
					.installmentNo(1)
					.amount(new BigDecimal("5000000"))
					.amountPaid(new BigDecimal("2000000"))
					.status(InstallmentStatus.PARTIAL)
					.build());

			when(planRepository.findByStudentIdAndCategoryAndStatusNot(
					anyLong(), any(), any())).thenReturn(Optional.of(belumLunas));

			assertThatThrownBy(() -> service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.UJIAN_KELAYAKAN,
					TAHUN, AcademicTerm.GASAL))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("belum lunas");
		}

		@Test
		@DisplayName("Ujian Kelayakan diterima bila Seminar Proposal sudah lunas")
		void tahapSebelumnyaLunas() {
			uktLunasEnamSemester();

			PaymentPlan lunas = PaymentPlan.builder()
					.id(9L)
					.category(PaymentCategory.SEMINAR_PROPOSAL)
					.totalAmount(new BigDecimal("5000000"))
					.status(PlanStatus.ACTIVE)
					.build();
			lunas.addInstallment(Installment.builder()
					.installmentNo(1)
					.amount(new BigDecimal("5000000"))
					.amountPaid(new BigDecimal("5000000"))
					.status(InstallmentStatus.PAID)
					.build());

			when(planRepository.findByStudentIdAndCategoryAndStatusNot(
					anyLong(), any(), any())).thenReturn(Optional.of(lunas));
			siapkanTarif(PaymentCategory.UJIAN_KELAYAKAN, new BigDecimal("5000000"));
			siapkanTemplate(PaymentCategory.UJIAN_KELAYAKAN, AcademicTerm.GASAL, 1);

			PaymentPlan plan = service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.UJIAN_KELAYAKAN,
					TAHUN, AcademicTerm.GASAL);

			assertThat(plan.getTotalAmount()).isEqualByComparingTo("5000000");
		}

		@Test
		@DisplayName("Seminar Proposal sebagai tahap pertama tidak butuh prasyarat")
		void tahapPertamaBebas() {
			uktLunasEnamSemester();

			siapkanTarif(PaymentCategory.SEMINAR_PROPOSAL, new BigDecimal("5000000"));
			siapkanTemplate(PaymentCategory.SEMINAR_PROPOSAL, AcademicTerm.GASAL, 1);

			PaymentPlan plan = service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.SEMINAR_PROPOSAL,
					TAHUN, AcademicTerm.GASAL);

			assertThat(plan.getInstallments()).hasSize(1);
		}
	}

	@Nested
	@DisplayName("Syarat lunas UKT sebelum tahap ujian")
	class GerbangUkt {

		private void siapkanUjian() {
			siapkanTarif(PaymentCategory.SEMINAR_PROPOSAL, new BigDecimal("5000000"));
			siapkanPotongan("NON_ALUMNI", "0");
			siapkanTemplate(PaymentCategory.SEMINAR_PROPOSAL, AcademicTerm.GASAL, 1);
		}

		@Test
		@DisplayName("ditolak bila belum satu pun semester UKT ditagihkan")
		void belumAdaUktSamaSekali() {
			siapkanUjian();

			assertThatThrownBy(() -> service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.SEMINAR_PROPOSAL,
					TAHUN, AcademicTerm.GASAL))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("Seluruh UKT harus lunas")
					.hasMessageContaining("6 dari 6 semester belum ditagihkan");
		}

		@Test
		@DisplayName("ditolak bila baru sebagian semester ditagihkan, walau semuanya lunas")
		void baruSebagianDitagihkan() {
			siapkanUjian();

			// Tiga semester lunas semua. Tetap ditolak: menagihkan semester
			// berikutnya adalah pekerjaan admin, dan mahasiswa tidak boleh
			// diuntungkan karena pekerjaan itu belum dilakukan.
			when(planRepository.findByStudentIdOrderByCategoryAscSemesterNumberAsc(anyLong()))
					.thenReturn(uktPlans(3, true));

			assertThatThrownBy(() -> service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.SEMINAR_PROPOSAL,
					TAHUN, AcademicTerm.GASAL))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("3 dari 6 semester belum ditagihkan");
		}

		@Test
		@DisplayName("pesan menyebut semester mana yang belum lunas, bukan sekadar 'belum lunas'")
		void menyebutSemesterYangMenghalangi() {
			siapkanUjian();
			when(planRepository.findByStudentIdOrderByCategoryAscSemesterNumberAsc(anyLong()))
					.thenReturn(uktPlans(6, false));

			assertThatThrownBy(() -> service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.SEMINAR_PROPOSAL,
					TAHUN, AcademicTerm.GASAL))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("belum lunas: semester 1");
		}

		@Test
		@DisplayName("lolos bila keenam semester ditagihkan dan lunas")
		void enamSemesterLunas() {
			siapkanUjian();
			uktLunasEnamSemester();

			PaymentPlan plan = service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.SEMINAR_PROPOSAL,
					TAHUN, AcademicTerm.GASAL);

			assertThat(plan.getCategory()).isEqualTo(PaymentCategory.SEMINAR_PROPOSAL);
		}

		@Test
		@DisplayName("mahasiswa yang dibebaskan lolos walau UKT belum ditagihkan sama sekali")
		void dibebaskan() {
			siapkanUjian();

			Student dibebaskan = mahasiswa("NON_ALUMNI");
			dibebaskan.setUjianExempt(true);

			PaymentPlan plan = service.generate(
					dibebaskan, PaymentCategory.SEMINAR_PROPOSAL, TAHUN, AcademicTerm.GASAL);

			assertThat(plan.getCategory()).isEqualTo(PaymentCategory.SEMINAR_PROPOSAL);
		}

		@Test
		@DisplayName("UKT sendiri tidak kena gerbang ini, kalau tidak tidak ada yang bisa dimulai")
		void uktTidakKenaGerbang() {
			siapkanTarif(PaymentCategory.UKT, UKT_DASAR);
			siapkanPotongan("NON_ALUMNI", "0");
			siapkanTemplate(PaymentCategory.UKT, AcademicTerm.GASAL, 5);

			PaymentPlan plan = service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.UKT, TAHUN, AcademicTerm.GASAL);

			assertThat(plan.getCategory()).isEqualTo(PaymentCategory.UKT);
		}

		@Test
		@DisplayName("Pendaftaran juga tidak kena gerbang ini")
		void pendaftaranTidakKenaGerbang() {
			siapkanTarif(PaymentCategory.PENDAFTARAN, new BigDecimal("1000000"));
			siapkanPotongan("NON_ALUMNI", "0");
			siapkanTemplate(PaymentCategory.PENDAFTARAN, AcademicTerm.GASAL, 1);

			PaymentPlan plan = service.generate(
					mahasiswa("NON_ALUMNI"), PaymentCategory.PENDAFTARAN,
					TAHUN, AcademicTerm.GASAL);

			assertThat(plan.getCategory()).isEqualTo(PaymentCategory.PENDAFTARAN);
		}

		private List<PaymentPlan> uktPlans(int jumlah, boolean lunas) {
			List<PaymentPlan> plans = new ArrayList<>();
			for (int semester = 1; semester <= jumlah; semester++) {
				PaymentPlan plan = PaymentPlan.builder()
						.id((long) (200 + semester))
						.category(PaymentCategory.UKT)
						.semesterNumber(semester)
						.totalAmount(new BigDecimal("10000000"))
						.status(PlanStatus.ACTIVE)
						.build();
				plan.addInstallment(Installment.builder()
						.installmentNo(1)
						.amount(new BigDecimal("10000000"))
						.amountPaid(lunas ? new BigDecimal("10000000") : BigDecimal.ZERO)
						.status(lunas ? InstallmentStatus.PAID : InstallmentStatus.UNPAID)
						.build());
				plans.add(plan);
			}
			return plans;
		}
	}
}
