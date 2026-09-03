package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.billing.Installment;
import ac.kampus.pembayaran.billing.InstallmentRepository;
import ac.kampus.pembayaran.billing.InstallmentStatus;
import ac.kampus.pembayaran.billing.PaymentPlan;
import ac.kampus.pembayaran.billing.PaymentPlanRepository;
import ac.kampus.pembayaran.billing.PlanStatus;
import ac.kampus.pembayaran.settings.SystemSettingService;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Aturan pembagian uang ke cicilan. Salah di sini berarti uang mahasiswa
 * nyangkut atau tercatat ganda, jadi tiap cabang keputusan diuji.
 */
class PaymentAllocationServiceTest {

	private PaymentRepository paymentRepository;
	private InstallmentRepository installmentRepository;
	private PaymentPlanRepository planRepository;
	private StudentRepository studentRepository;
	private VerificationLogRepository logRepository;
	private SystemSettingService settings;
	private PaymentAllocationService service;

	private Student student;

	@BeforeEach
	void setUp() {
		paymentRepository = mock(PaymentRepository.class);
		installmentRepository = mock(InstallmentRepository.class);
		planRepository = mock(PaymentPlanRepository.class);
		studentRepository = mock(StudentRepository.class);
		logRepository = mock(VerificationLogRepository.class);
		settings = mock(SystemSettingService.class);

		service = new PaymentAllocationService(
				paymentRepository, installmentRepository, planRepository,
				studentRepository, logRepository, settings);

		student = Student.builder()
				.id(1L).nim("2612600001").name("Uji Coba")
				.walletBalance(BigDecimal.ZERO).active(true)
				.build();

		when(studentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(student));
		when(settings.getAmount(anyString(), any())).thenReturn(BigDecimal.ZERO);
	}

	// --- Pembantu penyusun kondisi awal ---

	private Installment cicilan(long id, int no, String amount, String paid, LocalDate due) {
		Installment i = Installment.builder()
				.id(id).installmentNo(no).dueDate(due)
				.amount(new BigDecimal(amount)).amountPaid(new BigDecimal(paid))
				.build();
		i.refreshStatus();
		when(installmentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(i));
		return i;
	}

	private PaymentPlan plan(Installment... cicilan) {
		PaymentPlan p = PaymentPlan.builder()
				.id(10L).student(student)
				.totalAmount(new BigDecimal("6000000"))
				.status(PlanStatus.ACTIVE)
				.installments(new ArrayList<>(List.of(cicilan)))
				.build();
		p.getInstallments().forEach(i -> i.setPaymentPlan(p));
		when(planRepository.findWithInstallmentsById(10L)).thenReturn(Optional.of(p));
		return p;
	}

	private Payment payment(String amount, Installment target, PaymentPlan plan) {
		Payment p = Payment.builder()
				.id(100L).student(student).paymentPlan(plan).installment(target)
				.amount(new BigDecimal(amount))
				.proofFilePath("bukti/x.png")
				.status(PaymentStatus.AUTO_VERIFIED)
				.build();
		when(paymentRepository.findWithDetailsById(100L)).thenReturn(Optional.of(p));
		return p;
	}

	// --- Test ---

	@Test
	@DisplayName("bayar pas: cicilan lunas, tidak ada sisa ke saldo")
	void bayarPas() {
		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		PaymentPlan plan = plan(c1);
		Payment p = payment("1200000", c1, plan);

		service.allocate(100L);

		assertThat(c1.getAmountPaid()).isEqualByComparingTo("1200000");
		assertThat(c1.getStatus()).isEqualTo(InstallmentStatus.PAID);
		assertThat(student.getWalletBalance()).isEqualByComparingTo("0");
		assertThat(p.getAllocatedAt()).isNotNull();
	}

	@Test
	@DisplayName("bayar kurang: cicilan jadi PARTIAL")
	void bayarKurang() {
		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		payment("500000", c1, plan(c1));

		service.allocate(100L);

		assertThat(c1.getAmountPaid()).isEqualByComparingTo("500000");
		assertThat(c1.getStatus()).isEqualTo(InstallmentStatus.PARTIAL);
		assertThat(c1.outstanding()).isEqualByComparingTo("700000");
		assertThat(student.getWalletBalance()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("bayar lebih: kelebihan mengalir ke cicilan berikutnya, sisanya ke saldo")
	void bayarLebih() {
		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		Installment c2 = cicilan(2, 2, "1200000", "0", LocalDate.of(2026, 10, 10));
		Installment c3 = cicilan(3, 3, "1200000", "0", LocalDate.of(2026, 11, 10));
		payment("3000000", c1, plan(c1, c2, c3));

		service.allocate(100L);

		assertThat(c1.getStatus()).isEqualTo(InstallmentStatus.PAID);
		assertThat(c2.getStatus()).isEqualTo(InstallmentStatus.PAID);
		assertThat(c3.getAmountPaid()).isEqualByComparingTo("600000");
		assertThat(c3.getStatus()).isEqualTo(InstallmentStatus.PARTIAL);
		assertThat(student.getWalletBalance()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("semua cicilan lunas dan masih ada sisa: sisanya masuk saldo mahasiswa")
	void sisaMasukSaldo() {
		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		payment("1500000", c1, plan(c1));

		service.allocate(100L);

		assertThat(c1.getStatus()).isEqualTo(InstallmentStatus.PAID);
		assertThat(student.getWalletBalance()).isEqualByComparingTo("300000");
	}

	@Test
	@DisplayName("sisa kecil dalam toleransi dibuang, tidak dibawa ke cicilan lain atau saldo")
	void sisaDalamToleransiDibuang() {
		// Kode unik bank: mahasiswa transfer 1.200.437 padahal tagihan 1.200.000.
		when(settings.getAmount(anyString(), any())).thenReturn(new BigDecimal("1000"));

		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		Installment c2 = cicilan(2, 2, "1200000", "0", LocalDate.of(2026, 10, 10));
		payment("1200437", c1, plan(c1, c2));

		service.allocate(100L);

		assertThat(c1.getStatus()).isEqualTo(InstallmentStatus.PAID);
		// Sisa 437 tidak nyangkut di cicilan berikutnya maupun di saldo.
		assertThat(c2.getAmountPaid()).isEqualByComparingTo("0");
		assertThat(student.getWalletBalance()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("cicilan tujuan sudah lunas: ditandai perlu ditinjau, uang tidak dialihkan diam-diam")
	void cicilanTujuanSudahLunas() {
		Installment c1 = cicilan(1, 1, "1200000", "1200000", LocalDate.of(2026, 9, 10));
		Installment c2 = cicilan(2, 2, "1200000", "0", LocalDate.of(2026, 10, 10));
		Payment p = payment("1200000", c1, plan(c1, c2));

		service.allocate(100L);

		assertThat(p.getStatus()).isEqualTo(PaymentStatus.NEEDS_REVIEW);
		// Uangnya TIDAK diam-diam pindah ke cicilan berikutnya.
		assertThat(c2.getAmountPaid()).isEqualByComparingTo("0");
		assertThat(student.getWalletBalance()).isEqualByComparingTo("0");
		assertThat(p.getAllocatedAt()).isNull();

		ArgumentCaptor<VerificationLog> log = ArgumentCaptor.forClass(VerificationLog.class);
		verify(logRepository).save(log.capture());
		assertThat(log.getValue().getNote()).contains("sudah lunas");
	}

	@Test
	@DisplayName("tanpa cicilan tujuan: dibagikan FIFO ke jatuh tempo terlama dulu")
	void fifoTanpaTujuan() {
		Installment c3 = cicilan(3, 3, "1200000", "0", LocalDate.of(2026, 11, 10));
		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		Installment c2 = cicilan(2, 2, "1200000", "0", LocalDate.of(2026, 10, 10));

		// Sengaja diberikan dengan urutan acak untuk membuktikan pengurutannya benar.
		payment("1500000", null, plan(c3, c1, c2));

		service.allocate(100L);

		assertThat(c1.getStatus()).isEqualTo(InstallmentStatus.PAID);
		assertThat(c2.getAmountPaid()).isEqualByComparingTo("300000");
		assertThat(c3.getAmountPaid()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("pembayaran yang sudah dialokasikan tidak dihitung dua kali")
	void tidakDialokasikanDuaKali() {
		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		Payment p = payment("1200000", c1, plan(c1));
		p.setAllocatedAt(Instant.now());

		service.allocate(100L);

		assertThat(c1.getAmountPaid()).isEqualByComparingTo("0");
		verify(installmentRepository, never()).save(any());
	}

	@Test
	@DisplayName("pembayaran yang belum diverifikasi tidak dialokasikan")
	void belumDiverifikasi() {
		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		Payment p = payment("1200000", c1, plan(c1));
		p.setStatus(PaymentStatus.NEEDS_REVIEW);

		service.allocate(100L);

		assertThat(c1.getAmountPaid()).isEqualByComparingTo("0");
		assertThat(p.getAllocatedAt()).isNull();
	}

	@Test
	@DisplayName("tagihan menjadi COMPLETED ketika seluruh cicilan lunas")
	void planLunasJadiCompleted() {
		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		Installment c2 = cicilan(2, 2, "1200000", "0", LocalDate.of(2026, 10, 10));
		PaymentPlan plan = plan(c1, c2);
		payment("2400000", c1, plan);

		service.allocate(100L);

		assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
	}

	@Test
	@DisplayName("pengambilan cicilan selalu memakai kunci baris")
	void selaluMemakaiKunciBaris() {
		Installment c1 = cicilan(1, 1, "1200000", "0", LocalDate.of(2026, 9, 10));
		payment("1200000", c1, plan(c1));

		service.allocate(100L);

		verify(installmentRepository).findByIdForUpdate(1L);
		verify(installmentRepository, never()).findById(anyLong());
	}
}
