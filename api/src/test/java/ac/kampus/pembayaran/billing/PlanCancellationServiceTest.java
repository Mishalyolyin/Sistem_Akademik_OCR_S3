package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.payment.PaymentStatus;
import ac.kampus.pembayaran.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

/**
 * Pembatalan tagihan yang salah dibuat.
 *
 * <p>Fitur ini menutup satu-satunya jalan buntu yang tersisa di sistem: dua
 * indeks unik membuat tagihan yang keliru mengunci slotnya selamanya, dan
 * sampai kelas ini ada, satu-satunya jalan keluar adalah menghapus mahasiswanya.
 *
 * <p>Yang paling dijaga di sini justru <b>penolakannya</b>. Tagihan yang sudah
 * menerima uang tidak boleh bisa dicoret begitu saja: kalau bisa, uang yang
 * sudah masuk berhenti punya tagihan yang menerangkannya, dan pembukuan tidak
 * lagi bisa dijelaskan.
 */
class PlanCancellationServiceTest {

	private PaymentPlanRepository planRepository;
	private PaymentRepository paymentRepository;
	private PlanCancellationService service;

	private static final Long ADMIN = 7L;

	@BeforeEach
	void setUp() {
		planRepository = mock(PaymentPlanRepository.class);
		paymentRepository = mock(PaymentRepository.class);
		service = new PlanCancellationService(planRepository, paymentRepository);

		when(planRepository.save(any(PaymentPlan.class)))
				.thenAnswer((Answer<PaymentPlan>) inv -> inv.getArgument(0));
		when(paymentRepository.countOnPlan(anyLong(), any())).thenReturn(0L);
	}

	private PaymentPlan plan(PlanStatus status) {
		PaymentPlan plan = PaymentPlan.builder()
				.id(5L)
				.student(Student.builder().id(3L).name("Uji Coba").build())
				.category(PaymentCategory.SEMINAR_PROPOSAL)
				.academicYear("2026/2027")
				.totalAmount(new BigDecimal("5000000"))
				.status(status)
				.build();
		when(planRepository.findWithInstallmentsById(5L)).thenReturn(Optional.of(plan));
		return plan;
	}

	@Test
	@DisplayName("tagihan tanpa pembayaran bisa dibatalkan, dan jejaknya terisi lengkap")
	void batalBerhasil() {
		plan(PlanStatus.ACTIVE);

		PaymentPlan hasil = service.batalkan(5L, "salah pilih mahasiswa", ADMIN);

		assertThat(hasil.getStatus()).isEqualTo(PlanStatus.CANCELLED);
		assertThat(hasil.getCancelReason()).isEqualTo("salah pilih mahasiswa");
		assertThat(hasil.getCancelledBy()).isEqualTo(ADMIN);
		// Ketiganya harus terisi bersamaan: CHECK di migrasi V12 menolak
		// status CANCELLED yang alasannya kosong.
		assertThat(hasil.getCancelledAt()).isNotNull();
	}

	@Test
	@DisplayName("tagihan yang sudah menerima pembayaran terverifikasi ditolak")
	void adaUangMasuk() {
		plan(PlanStatus.ACTIVE);
		when(paymentRepository.countOnPlan(5L, java.util.List.of(
				PaymentStatus.VERIFIED, PaymentStatus.AUTO_VERIFIED))).thenReturn(2L);
		when(paymentRepository.sumOnPlan(anyLong(), any()))
				.thenReturn(new BigDecimal("2400000"));

		assertThatThrownBy(() -> service.batalkan(5L, "salah pilih mahasiswa", ADMIN))
				.isInstanceOf(BusinessRuleException.class)
				// Pesannya menyebut jumlah DAN nominalnya: "tidak bisa dibatalkan"
				// saja memaksa admin menebak apa yang menghalangi.
				.hasMessageContaining("2 pembayaran terverifikasi")
				.hasMessageContaining("2.400.000")
				.hasMessageContaining("panel verifikasi");

		verify(planRepository, never()).save(any());
	}

	@Test
	@DisplayName("pembayaran yang ditolak tidak menghalangi pembatalan")
	void pembayaranDitolakTidakMenghalangi() {
		plan(PlanStatus.ACTIVE);
		// countOnPlan hanya menghitung VERIFIED dan AUTO_VERIFIED; yang REJECTED
		// memang tidak pernah diakui masuk, jadi tidak ada uang yang perlu
		// dijelaskan lebih dulu.
		when(paymentRepository.countOnPlan(anyLong(), any())).thenReturn(0L);

		PaymentPlan hasil = service.batalkan(5L, "mahasiswa mengundurkan diri", ADMIN);

		assertThat(hasil.getStatus()).isEqualTo(PlanStatus.CANCELLED);
	}

	@Test
	@DisplayName("alasan wajib diisi, minimal lima karakter")
	void alasanWajib() {
		assertThatThrownBy(() -> service.batalkan(5L, "  ", ADMIN))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Alasan pembatalan wajib diisi");

		assertThatThrownBy(() -> service.batalkan(5L, "oops", ADMIN))
				.isInstanceOf(BusinessRuleException.class);

		assertThatThrownBy(() -> service.batalkan(5L, null, ADMIN))
				.isInstanceOf(BusinessRuleException.class);

		// Tagihannya bahkan tidak sempat dibaca — alasan diperiksa lebih dulu.
		verify(planRepository, never()).findWithInstallmentsById(anyLong());
	}

	@Test
	@DisplayName("tagihan yang sudah dibatalkan tidak bisa dibatalkan dua kali")
	void sudahDibatalkan() {
		plan(PlanStatus.CANCELLED);

		assertThatThrownBy(() -> service.batalkan(5L, "salah pilih mahasiswa", ADMIN))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("sudah dibatalkan");
	}

	@Test
	@DisplayName("tagihan yang tidak ada dijawab 404, bukan diam-diam berhasil")
	void tidakDitemukan() {
		when(planRepository.findWithInstallmentsById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.batalkan(99L, "salah pilih mahasiswa", ADMIN))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	@DisplayName("tagihan yang sudah lunas pun bisa dibatalkan bila uangnya sudah ditarik")
	void planLunasTanpaPembayaranTersisa() {
		// Keadaan ini nyata: admin membatalkan tiap keputusan verifikasinya
		// lebih dulu di panel verifikasi — uangnya tertarik, statusnya kembali
		// NEEDS_REVIEW — lalu barulah tagihannya dibatalkan.
		plan(PlanStatus.COMPLETED);

		PaymentPlan hasil = service.batalkan(5L, "tagihan ganda, dibereskan", ADMIN);

		assertThat(hasil.getStatus()).isEqualTo(PlanStatus.CANCELLED);
	}
}
