package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.billing.InstallmentRepository;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.payment.ocr.OcrJobPublisher;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pembatalan keputusan verifikasi.
 *
 * <p>Ini jalan keluar dari kekeliruan yang paling mahal di sistem ini: bukti
 * palsu yang telanjur diverifikasi, atau tombol yang salah pencet. Sebelum ada
 * jalur ini, satu-satunya cara membetulkannya adalah menyentuh database
 * langsung — sementara barisnya tetap berbunyi VERIFIED dan kuitansinya tetap
 * bisa dicetak.
 */
class PaymentServiceBatalTest {

	private PaymentRepository paymentRepository;
	private VerificationLogRepository logRepository;
	private PaymentAllocationService allocationService;
	private PaymentService service;

	private Payment payment;

	@BeforeEach
	void setUp() {
		paymentRepository = mock(PaymentRepository.class);
		logRepository = mock(VerificationLogRepository.class);
		allocationService = mock(PaymentAllocationService.class);

		service = new PaymentService(
				paymentRepository,
				logRepository,
				mock(StudentRepository.class),
				mock(InstallmentRepository.class),
				mock(FileStorageService.class),
				mock(OcrJobPublisher.class),
				allocationService);

		payment = Payment.builder()
				.id(100L)
				.student(Student.builder()
						.id(1L).nim("2612600001").name("Uji Coba")
						.walletBalance(BigDecimal.ZERO).active(true)
						.build())
				.amount(new BigDecimal("1200000"))
				.proofFilePath("bukti/x.png")
				.status(PaymentStatus.VERIFIED)
				.verifiedAt(Instant.parse("2026-09-04T02:00:00Z"))
				.verifiedBy(9L)
				.allocatedAt(Instant.parse("2026-09-04T02:00:01Z"))
				.build();

		when(paymentRepository.findWithDetailsById(100L)).thenReturn(Optional.of(payment));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	@Test
	@DisplayName("keputusan dibatalkan, jejak verifikasinya ikut dibersihkan")
	void keputusanDibatalkan() {
		service.batalkanKeputusan(100L, "Bukti ternyata milik orang lain", 9L);

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.NEEDS_REVIEW);
		assertThat(payment.getVerifiedAt()).isNull();
		assertThat(payment.getVerifiedBy()).isNull();
		assertThat(payment.getRejectReason()).isNull();
		verify(allocationService).reverse(100L);
	}

	@Test
	@DisplayName("uangnya ditarik LEBIH DULU, sebelum statusnya diubah")
	void uangDitarikLebihDulu() {
		service.batalkanKeputusan(100L, "Salah pencet tombol verifikasi", 9L);

		// Urutannya bukan selera: kalau statusnya berubah lebih dulu lalu
		// penarikannya gagal, pembayaran berakhir NEEDS_REVIEW sementara uangnya
		// tetap tercatat masuk cicilan.
		InOrder urutan = inOrder(allocationService, paymentRepository);
		urutan.verify(allocationService).reverse(100L);
		urutan.verify(paymentRepository).save(any(Payment.class));
	}

	@Test
	@DisplayName("penarikan yang ditolak membatalkan seluruhnya, status tidak ikut berubah")
	void penarikanDitolak() {
		doThrow(new BusinessRuleException("Kelebihan sudah terpakai"))
				.when(allocationService).reverse(100L);

		assertThatThrownBy(() -> service.batalkanKeputusan(100L, "Alasan yang cukup panjang", 9L))
				.isInstanceOf(BusinessRuleException.class);

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	@DisplayName("alasan wajib diisi, minimal lima karakter")
	void alasanWajib() {
		for (String alasan : new String[] { null, "", "   ", "oops" }) {
			assertThatThrownBy(() -> service.batalkanKeputusan(100L, alasan, 9L))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("minimal 5 karakter");
		}

		verify(allocationService, never()).reverse(anyLong());
	}

	@Test
	@DisplayName("alasan tercatat di jejak audit beserta status asalnya")
	void alasanTercatat() {
		service.batalkanKeputusan(100L, "Bukti ganda, sudah dibayar lewat bukti lain", 9L);

		ArgumentCaptor<VerificationLog> captor = ArgumentCaptor.forClass(VerificationLog.class);
		verify(logRepository).save(captor.capture());

		VerificationLog jejak = captor.getValue();
		assertThat(jejak.getFromStatus()).isEqualTo(PaymentStatus.VERIFIED);
		assertThat(jejak.getToStatus()).isEqualTo(PaymentStatus.NEEDS_REVIEW);
		assertThat(jejak.getAdminId()).isEqualTo(9L);
		assertThat(jejak.getNote()).contains("Bukti ganda");
	}

	@Test
	@DisplayName("yang ditolak juga bisa dibatalkan; penolakan pun bisa keliru")
	void penolakanJugaBisaDibatalkan() {
		payment.setStatus(PaymentStatus.REJECTED);
		payment.setRejectReason("Gambar tidak terbaca");
		payment.setAllocatedAt(null);

		service.batalkanKeputusan(100L, "Mahasiswa mengirim ulang gambar yang jelas", 9L);

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.NEEDS_REVIEW);
		assertThat(payment.getRejectReason()).isNull();
	}

	@Test
	@DisplayName("yang belum diputuskan ditolak, dan diarahkan ke Baca ulang")
	void belumDiputuskan() {
		for (PaymentStatus status : new PaymentStatus[] {
				PaymentStatus.PENDING, PaymentStatus.NEEDS_REVIEW, PaymentStatus.FAILED }) {

			payment.setStatus(status);

			assertThatThrownBy(() -> service.batalkanKeputusan(100L, "Alasan cukup panjang", 9L))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("Baca ulang");
		}

		verify(allocationService, never()).reverse(anyLong());
	}
}
