package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Aturan ubah nominal cicilan dan jejak auditnya. */
class InstallmentBillingServiceTest {

	private InstallmentRepository installmentRepository;
	private PaymentPlanRepository planRepository;
	private InstallmentAmountChangeRepository changeRepository;
	private InstallmentBillingService service;

	private static final Long ADMIN = 99L;

	@BeforeEach
	void setUp() {
		installmentRepository = mock(InstallmentRepository.class);
		planRepository = mock(PaymentPlanRepository.class);
		changeRepository = mock(InstallmentAmountChangeRepository.class);

		service = new InstallmentBillingService(
				installmentRepository, planRepository, changeRepository);

		when(changeRepository.save(any(InstallmentAmountChange.class)))
				.thenAnswer((Answer<InstallmentAmountChange>) i -> i.getArgument(0));
	}

	private Installment cicilan(String amount, String amountPaid) {
		PaymentPlan plan = PaymentPlan.builder()
				.id(10L)
				.totalAmount(new BigDecimal(amount))
				.status(PlanStatus.ACTIVE)
				.build();

		Installment installment = Installment.builder()
				.id(1L)
				.paymentPlan(plan)
				.installmentNo(1)
				.amount(new BigDecimal(amount))
				.amountPaid(new BigDecimal(amountPaid))
				.build();
		installment.refreshStatus();

		when(installmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(installment));
		return installment;
	}

	@Test
	@DisplayName("nominal berubah, status dihitung ulang, audit tercatat")
	void ubahNominalNormal() {
		Installment installment = cicilan("2000000", "0");
		when(installmentRepository.sumAmountByPlan(10L)).thenReturn(new BigDecimal("1200000"));

		InstallmentAmountChange change = service.updateAmount(
				1L, new BigDecimal("1200000"), "Potongan kerjasama instansi", ADMIN);

		assertThat(installment.getAmount()).isEqualByComparingTo("1200000");
		assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.UNPAID);
		assertThat(change.getOldAmount()).isEqualByComparingTo("2000000");
		assertThat(change.getNewAmount()).isEqualByComparingTo("1200000");
		assertThat(change.getAdminId()).isEqualTo(ADMIN);

		// total_amount plan dihitung ulang dari jumlah seluruh cicilan.
		ArgumentCaptor<PaymentPlan> plan = ArgumentCaptor.forClass(PaymentPlan.class);
		verify(planRepository).save(plan.capture());
		assertThat(plan.getValue().getTotalAmount()).isEqualByComparingTo("1200000");
	}

	@Test
	@DisplayName("nominal turun di bawah yang sudah dibayar: jadi PAID, amount_paid tidak disentuh")
	void turunDiBawahYangSudahDibayar() {
		Installment installment = cicilan("2000000", "1500000");
		when(installmentRepository.sumAmountByPlan(10L)).thenReturn(new BigDecimal("1000000"));

		service.updateAmount(1L, new BigDecimal("1000000"), "Koreksi tagihan", ADMIN);

		assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PAID);
		// TIDAK ada refund otomatis — kelebihan diurus lewat fitur Penyesuaian.
		assertThat(installment.getAmountPaid()).isEqualByComparingTo("1500000");
	}

	@Test
	@DisplayName("bayar sebagian lalu nominal dinaikkan: status jadi PARTIAL")
	void naikkanNominalJadiPartial() {
		Installment installment = cicilan("2000000", "2000000");
		assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PAID);
		when(installmentRepository.sumAmountByPlan(10L)).thenReturn(new BigDecimal("3000000"));

		service.updateAmount(1L, new BigDecimal("3000000"), "Tambahan biaya sesuai MoU", ADMIN);

		assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PARTIAL);
		assertThat(installment.outstanding()).isEqualByComparingTo("1000000");
	}

	@Test
	@DisplayName("alasan kosong ditolak dan tidak ada yang tersimpan")
	void alasanWajib() {
		cicilan("2000000", "0");

		assertThatThrownBy(() -> service.updateAmount(1L, new BigDecimal("1000000"), "  ", ADMIN))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Alasan wajib diisi");

		verify(changeRepository, never()).save(any());
		verify(installmentRepository, never()).save(any());
	}

	@Test
	@DisplayName("alasan kurang dari 5 karakter ditolak")
	void alasanTerlaluPendek() {
		cicilan("2000000", "0");

		assertThatThrownBy(() -> service.updateAmount(1L, new BigDecimal("1000000"), "abc", ADMIN))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("minimal 5 karakter");
	}

	@Test
	@DisplayName("nominal nol atau negatif ditolak")
	void nominalHarusPositif() {
		cicilan("2000000", "0");

		assertThatThrownBy(() -> service.updateAmount(1L, BigDecimal.ZERO, "Alasan cukup", ADMIN))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("lebih besar dari nol");

		assertThatThrownBy(() ->
				service.updateAmount(1L, new BigDecimal("-100"), "Alasan cukup", ADMIN))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("nominal sama dengan sekarang ditolak, supaya audit tidak terisi sampah")
	void nominalTidakBerubah() {
		cicilan("2000000", "0");

		assertThatThrownBy(() ->
				service.updateAmount(1L, new BigDecimal("2000000"), "Tidak berubah", ADMIN))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("sama dengan yang sekarang");

		verify(changeRepository, never()).save(any());
	}

	@Test
	@DisplayName("cicilan tidak ditemukan")
	void cicilanTidakAda() {
		when(installmentRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() ->
				service.updateAmount(404L, new BigDecimal("1000000"), "Alasan cukup", ADMIN))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	@DisplayName("pengambilan cicilan memakai kunci baris, bukan findById biasa")
	void memakaiPessimisticLock() {
		cicilan("2000000", "0");
		when(installmentRepository.sumAmountByPlan(10L)).thenReturn(new BigDecimal("1000000"));

		service.updateAmount(1L, new BigDecimal("1000000"), "Alasan cukup", ADMIN);

		verify(installmentRepository).findByIdForUpdate(1L);
		verify(installmentRepository, never()).findById(anyLong());
	}
}
