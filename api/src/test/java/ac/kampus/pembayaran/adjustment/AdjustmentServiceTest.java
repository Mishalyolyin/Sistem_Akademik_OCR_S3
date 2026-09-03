package ac.kampus.pembayaran.adjustment;

import ac.kampus.pembayaran.billing.Installment;
import ac.kampus.pembayaran.billing.InstallmentRepository;
import ac.kampus.pembayaran.billing.InstallmentStatus;
import ac.kampus.pembayaran.billing.PaymentPlan;
import ac.kampus.pembayaran.billing.PaymentPlanRepository;
import ac.kampus.pembayaran.billing.PlanStatus;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Penyesuaian manual atas uang mahasiswa.
 *
 * <p>Ini satu-satunya jalur di mana admin memindahkan uang tanpa ada bukti
 * transfer yang mendasarinya, jadi tiap penolakan diuji: yang lolos begitu saja
 * di sini tidak akan tertangkap di mana pun lagi.
 */
class AdjustmentServiceTest {

	private AdjustmentRepository repository;
	private StudentRepository studentRepository;
	private InstallmentRepository installmentRepository;
	private PaymentPlanRepository planRepository;
	private AdjustmentService service;

	private Student student;

	@BeforeEach
	void setUp() {
		repository = mock(AdjustmentRepository.class);
		studentRepository = mock(StudentRepository.class);
		installmentRepository = mock(InstallmentRepository.class);
		planRepository = mock(PaymentPlanRepository.class);

		service = new AdjustmentService(repository, studentRepository,
				installmentRepository, planRepository);

		student = Student.builder()
				.id(1L).nim("2612600001").name("Uji Coba")
				.walletBalance(new BigDecimal("500000")).active(true)
				.build();

		when(studentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(student));
		when(studentRepository.existsById(1L)).thenReturn(true);
		when(repository.save(any(Adjustment.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	// --- Pembantu penyusun kondisi awal ---

	private PaymentPlan plan(long planId, Student pemilik, Installment... cicilan) {
		PaymentPlan plan = PaymentPlan.builder()
				.id(planId).student(pemilik).status(PlanStatus.ACTIVE)
				.installments(new ArrayList<>(List.of(cicilan)))
				.build();

		for (Installment i : cicilan) {
			i.setPaymentPlan(plan);
		}
		when(planRepository.findWithInstallmentsById(planId)).thenReturn(Optional.of(plan));
		return plan;
	}

	private Installment cicilan(long id, String amount, String paid) {
		Installment i = Installment.builder()
				.id(id).installmentNo(1).dueDate(LocalDate.of(2026, 9, 10))
				.amount(new BigDecimal(amount)).amountPaid(new BigDecimal(paid))
				.build();
		i.refreshStatus();
		when(installmentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(i));
		return i;
	}

	private Adjustment auditTersimpan() {
		ArgumentCaptor<Adjustment> captor = ArgumentCaptor.forClass(Adjustment.class);
		verify(repository).save(captor.capture());
		return captor.getValue();
	}

	@Nested
	@DisplayName("Penyesuaian saldo")
	class Saldo {

		@Test
		@DisplayName("saldo bertambah dan audit mencatat nilai sesudahnya")
		void saldoBertambah() {
			service.sesuaikanSaldo(1L, new BigDecimal("200000"), "kelebihan bayar cicilan 1", 9L);

			assertThat(student.getWalletBalance()).isEqualByComparingTo("700000");

			Adjustment audit = auditTersimpan();
			assertThat(audit.getAmount()).isEqualByComparingTo("200000");
			assertThat(audit.getBalanceAfter()).isEqualByComparingTo("700000");
			assertThat(audit.getInstallmentId()).isNull();
			assertThat(audit.getAdminId()).isEqualTo(9L);
			assertThat(audit.getReason()).isEqualTo("kelebihan bayar cicilan 1");
		}

		@Test
		@DisplayName("saldo berkurang")
		void saldoBerkurang() {
			service.sesuaikanSaldo(1L, new BigDecimal("-150000"), "dipakai untuk cicilan 2", 9L);

			assertThat(student.getWalletBalance()).isEqualByComparingTo("350000");
		}

		@Test
		@DisplayName("saldo tidak boleh jadi minus")
		void saldoTidakBolehMinus() {
			assertThatThrownBy(() ->
					service.sesuaikanSaldo(1L, new BigDecimal("-600000"), "salah hitung", 9L))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("tidak boleh minus");

			assertThat(student.getWalletBalance()).isEqualByComparingTo("500000");
			verify(repository, never()).save(any());
		}

		@Test
		@DisplayName("pengambilan mahasiswa memakai kunci baris, bukan findById biasa")
		void memakaiKunciBaris() {
			service.sesuaikanSaldo(1L, new BigDecimal("1000"), "koreksi kecil", 9L);

			verify(studentRepository).findByIdForUpdate(1L);
			verify(studentRepository, never()).findById(anyLong());
		}

		@Test
		@DisplayName("mahasiswa tidak ditemukan")
		void mahasiswaTidakAda() {
			when(studentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() ->
					service.sesuaikanSaldo(99L, new BigDecimal("1000"), "koreksi kecil", 9L))
					.isInstanceOf(NotFoundException.class);
		}
	}

	@Nested
	@DisplayName("Penyesuaian cicilan")
	class Cicilan {

		@Test
		@DisplayName("uang masuk bertambah dan status dihitung ulang")
		void cicilanJadiLunas() {
			Installment c = cicilan(10L, "1000000", "700000");
			plan(5L, student, c);

			service.sesuaikanCicilan(1L, 10L, new BigDecimal("300000"), "transfer manual", 9L);

			assertThat(c.getAmountPaid()).isEqualByComparingTo("1000000");
			assertThat(c.getStatus()).isEqualTo(InstallmentStatus.PAID);
			assertThat(auditTersimpan().getBalanceAfter()).isEqualByComparingTo("1000000");
		}

		@Test
		@DisplayName("uang masuk berkurang, status turun dari PAID ke PARTIAL")
		void cicilanTurunJadiPartial() {
			Installment c = cicilan(10L, "1000000", "1000000");
			plan(5L, student, c);

			service.sesuaikanCicilan(1L, 10L, new BigDecimal("-400000"), "koreksi salah catat", 9L);

			assertThat(c.getAmountPaid()).isEqualByComparingTo("600000");
			assertThat(c.getStatus()).isEqualTo(InstallmentStatus.PARTIAL);
		}

		@Test
		@DisplayName("uang yang tercatat masuk tidak boleh minus")
		void tidakBolehMinus() {
			Installment c = cicilan(10L, "1000000", "200000");
			plan(5L, student, c);

			assertThatThrownBy(() ->
					service.sesuaikanCicilan(1L, 10L, new BigDecimal("-500000"), "koreksi", 9L))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("tidak boleh minus");

			assertThat(c.getAmountPaid()).isEqualByComparingTo("200000");
		}

		@Test
		@DisplayName("kelebihan tidak boleh menumpuk di cicilan, diarahkan ke saldo")
		void tidakBolehMelebihiTagihan() {
			Installment c = cicilan(10L, "1000000", "900000");
			plan(5L, student, c);

			assertThatThrownBy(() ->
					service.sesuaikanCicilan(1L, 10L, new BigDecimal("200000"), "kelebihan", 9L))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("masukkan ke saldo");

			assertThat(c.getAmountPaid()).isEqualByComparingTo("900000");
			verify(repository, never()).save(any());
		}

		@Test
		@DisplayName("cicilan milik mahasiswa lain ditolak")
		void cicilanMilikOrangLain() {
			Student orangLain = Student.builder()
					.id(2L).nim("2612600002").name("Orang Lain")
					.walletBalance(BigDecimal.ZERO).active(true).build();
			Installment c = cicilan(10L, "1000000", "0");
			plan(5L, orangLain, c);

			assertThatThrownBy(() ->
					service.sesuaikanCicilan(1L, 10L, new BigDecimal("100000"), "salah orang", 9L))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("milik mahasiswa lain");

			assertThat(c.getAmountPaid()).isEqualByComparingTo("0");
		}

		@Test
		@DisplayName("tagihan jadi COMPLETED ketika seluruh cicilannya lunas")
		void planJadiCompleted() {
			Installment satu = cicilan(10L, "500000", "500000");
			Installment dua = cicilan(11L, "500000", "300000");
			PaymentPlan plan = plan(5L, student, satu, dua);

			service.sesuaikanCicilan(1L, 11L, new BigDecimal("200000"), "pelunasan tunai", 9L);

			assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		}

		@Test
		@DisplayName("tagihan yang tadinya lunas dibuka lagi kalau penyesuaian membatalkannya")
		void planDibukaLagi() {
			Installment c = cicilan(10L, "500000", "500000");
			PaymentPlan plan = plan(5L, student, c);
			plan.setStatus(PlanStatus.COMPLETED);

			service.sesuaikanCicilan(1L, 10L, new BigDecimal("-100000"), "koreksi salah catat", 9L);

			assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		}

		@Test
		@DisplayName("tagihan yang dibatalkan tidak diubah statusnya")
		void planCancelledDibiarkan() {
			Installment c = cicilan(10L, "500000", "500000");
			PaymentPlan plan = plan(5L, student, c);
			plan.setStatus(PlanStatus.CANCELLED);

			service.sesuaikanCicilan(1L, 10L, new BigDecimal("-100000"), "koreksi salah catat", 9L);

			assertThat(plan.getStatus()).isEqualTo(PlanStatus.CANCELLED);
		}
	}

	@Nested
	@DisplayName("Aturan yang berlaku untuk keduanya")
	class AturanUmum {

		@Test
		@DisplayName("nominal nol ditolak, supaya audit tidak terisi baris kosong")
		void nominalNolDitolak() {
			assertThatThrownBy(() -> service.sesuaikanSaldo(1L, BigDecimal.ZERO, "koreksi", 9L))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("tidak boleh nol");

			verify(repository, never()).save(any());
		}

		@Test
		@DisplayName("alasan kosong atau terlalu pendek ditolak")
		void alasanPendekDitolak() {
			assertThatThrownBy(() -> service.sesuaikanSaldo(1L, new BigDecimal("1000"), "", 9L))
					.isInstanceOf(BusinessRuleException.class);
			assertThatThrownBy(() -> service.sesuaikanSaldo(1L, new BigDecimal("1000"), "ok", 9L))
					.isInstanceOf(BusinessRuleException.class);
			assertThatThrownBy(() -> service.sesuaikanSaldo(1L, new BigDecimal("1000"), null, 9L))
					.isInstanceOf(BusinessRuleException.class);

			assertThat(student.getWalletBalance()).isEqualByComparingTo("500000");
			verify(repository, never()).save(any());
		}

		@Test
		@DisplayName("alasan disimpan tanpa spasi berlebih di ujung")
		void alasanDirapikan() {
			service.sesuaikanSaldo(1L, new BigDecimal("1000"), "   koreksi kecil   ", 9L);

			assertThat(auditTersimpan().getReason()).isEqualTo("koreksi kecil");
		}

		@Test
		@DisplayName("riwayat mahasiswa yang tidak ada dijawab tidak ditemukan")
		void riwayatMahasiswaTidakAda() {
			when(studentRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.riwayat(99L)).isInstanceOf(NotFoundException.class);
		}
	}
}
