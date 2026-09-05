package ac.kampus.pembayaran.reminder;

import ac.kampus.pembayaran.billing.Installment;
import ac.kampus.pembayaran.billing.InstallmentRepository;
import ac.kampus.pembayaran.billing.InstallmentStatus;
import ac.kampus.pembayaran.billing.PaymentPlan;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.settings.SystemSettingService;
import ac.kampus.pembayaran.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Aturan pengingat jatuh tempo.
 *
 * <p>Yang dijaga di sini bukan pengirimannya melainkan keputusannya, karena di
 * situlah kerugiannya nyata dua arah: pengingat yang tidak terkirim membuat
 * cicilan lewat tanpa ada yang tahu, sementara pengingat yang terkirim
 * berulang-ulang membuat mahasiswa memblokir nomornya — dan setelah diblokir,
 * pengingat berikutnya tidak akan pernah sampai lagi.
 */
class ReminderServiceTest {

	private static final LocalDate HARI_INI = LocalDate.of(2026, 9, 10);

	private InstallmentRepository installmentRepository;
	private ReminderLogRepository logRepository;
	private SystemSettingService settings;
	private WhatsAppSender sender;
	private ReminderService service;

	@BeforeEach
	void setUp() {
		installmentRepository = mock(InstallmentRepository.class);
		logRepository = mock(ReminderLogRepository.class);
		settings = mock(SystemSettingService.class);
		sender = mock(WhatsAppSender.class);
		service = new ReminderService(installmentRepository, logRepository, settings, sender);

		// Bawaan: pengingat menyala, tiga hari sebelum jatuh tempo.
		when(settings.get(ReminderSettings.ENABLED)).thenReturn(Optional.of("true"));
		when(settings.get(ReminderSettings.OVERDUE_ENABLED)).thenReturn(Optional.of("true"));
		when(settings.getInteger(ReminderSettings.DAYS_BEFORE)).thenReturn(Optional.of(3));
		when(settings.getOrDefault(eq(ReminderSettings.MESSAGE_TEMPLATE), anyString()))
				.thenReturn("Halo {nama}, cicilan {kategori} ke-{cicilan} {nominal} "
						+ "jatuh tempo {tanggal}. Sisa {sisa}.");
		when(logRepository.save(any(ReminderLog.class))).thenAnswer(inv -> inv.getArgument(0));
		// Bawaan: gateway sudah diatur. Yang belum diatur diuji tersendiri.
		when(sender.siap()).thenReturn(true);
	}

	private static Student mahasiswa(String telepon) {
		return Student.builder()
				.id(1L).nim("2612600001").name("Uji Coba").phone(telepon)
				.discountTier("NON_ALUMNI").walletBalance(BigDecimal.ZERO).active(true)
				.build();
	}

	private static Installment cicilan(Long id, LocalDate jatuhTempo, String telepon) {
		PaymentPlan plan = PaymentPlan.builder()
				.id(1L)
				.student(mahasiswa(telepon))
				.category(PaymentCategory.UKT)
				.academicYear("2026/2027")
				.build();

		Installment cicilan = new Installment();
		cicilan.setId(id);
		cicilan.setPaymentPlan(plan);
		cicilan.setInstallmentNo(2);
		cicilan.setDueDate(jatuhTempo);
		cicilan.setAmount(new BigDecimal("2000000"));
		cicilan.setAmountPaid(BigDecimal.ZERO);
		cicilan.setStatus(InstallmentStatus.UNPAID);
		return cicilan;
	}

	private void kandidat(Installment... isi) {
		when(installmentRepository.findPerluDiingatkan(any(), any(), any()))
				.thenReturn(List.of(isi));
	}

	private ReminderLog jejakTersimpan() {
		ArgumentCaptor<ReminderLog> captor = ArgumentCaptor.forClass(ReminderLog.class);
		verify(logRepository).save(captor.capture());
		return captor.getValue();
	}

	@Nested
	@DisplayName("Siapa yang diingatkan")
	class Sasaran {

		@Test
		@DisplayName("cicilan yang jatuh tempo tepat H-3 diingatkan")
		void tepatHMinusTiga() {
			kandidat(cicilan(10L, HARI_INI.plusDays(3), "081234567890"));

			var hasil = service.jalankan(HARI_INI);

			assertThat(hasil.terkirim()).isEqualTo(1);
			verify(sender).kirim(eq("6281234567890"), anyString());
		}

		@Test
		@DisplayName("yang jatuh tempo H-1 dan H-2 belum diingatkan, supaya tidak tiga kali")
		void bukanSetiapHariDalamRentang() {
			kandidat(
					cicilan(10L, HARI_INI.plusDays(1), "081234567890"),
					cicilan(11L, HARI_INI.plusDays(2), "081234567890"));

			var hasil = service.jalankan(HARI_INI);

			assertThat(hasil.terkirim()).isZero();
			verify(sender, never()).kirim(anyString(), anyString());
		}

		@Test
		@DisplayName("yang sudah lewat jatuh tempo diingatkan sebagai LEWAT_TEMPO")
		void lewatTempo() {
			kandidat(cicilan(10L, HARI_INI.minusDays(5), "081234567890"));

			var hasil = service.jalankan(HARI_INI);

			assertThat(hasil.terkirim()).isEqualTo(1);
			assertThat(jejakTersimpan().getKind()).isEqualTo(ReminderKind.LEWAT_TEMPO);
		}

		@Test
		@DisplayName("jatuh tempo hari ini dihitung menjelang, bukan lewat")
		void jatuhTempoHariIni() {
			when(settings.getInteger(ReminderSettings.DAYS_BEFORE)).thenReturn(Optional.of(0));
			kandidat(cicilan(10L, HARI_INI, "081234567890"));

			service.jalankan(HARI_INI);

			assertThat(jejakTersimpan().getKind()).isEqualTo(ReminderKind.JATUH_TEMPO);
		}

		@Test
		@DisplayName("cicilan lunas tidak pernah ikut, disaring di kueri")
		void lunasTidakIkut() {
			kandidat();

			service.jalankan(HARI_INI);

			verify(installmentRepository).findPerluDiingatkan(
					any(), any(), eq(InstallmentStatus.PAID));
			verify(sender, never()).kirim(anyString(), anyString());
		}
	}

	@Nested
	@DisplayName("Tidak mengganggu")
	class TidakMengganggu {

		@Test
		@DisplayName("yang sudah pernah terkirim tidak dikirimi lagi, selamanya")
		void tidakKirimDuaKali() {
			kandidat(cicilan(10L, HARI_INI.minusDays(5), "081234567890"));
			when(logRepository.existsByInstallmentIdAndKindAndStatus(
					10L, ReminderKind.LEWAT_TEMPO, ReminderStatus.SENT)).thenReturn(true);

			var hasil = service.jalankan(HARI_INI);

			assertThat(hasil.dilewati()).isEqualTo(1);
			verify(sender, never()).kirim(anyString(), anyString());
			// Tidak menumpuk baris jejak untuk sesuatu yang tidak dikerjakan.
			verify(logRepository, never()).save(any());
		}

		@Test
		@DisplayName("yang pernah GAGAL boleh dicoba lagi")
		void yangGagalDicobaLagi() {
			kandidat(cicilan(10L, HARI_INI.minusDays(5), "081234567890"));
			// Hanya status SENT yang menahan; FAILED tidak.
			when(logRepository.existsByInstallmentIdAndKindAndStatus(
					10L, ReminderKind.LEWAT_TEMPO, ReminderStatus.SENT)).thenReturn(false);

			var hasil = service.jalankan(HARI_INI);

			assertThat(hasil.terkirim()).isEqualTo(1);
		}

		@Test
		@DisplayName("pengingat yang dimatikan admin benar-benar tidak mengirim apa pun")
		void dimatikan() {
			when(settings.get(ReminderSettings.ENABLED)).thenReturn(Optional.of("false"));

			var hasil = service.jalankan(HARI_INI);

			assertThat(hasil.diperiksa()).isZero();
			verify(installmentRepository, never()).findPerluDiingatkan(any(), any(), any());
			verify(sender, never()).kirim(anyString(), anyString());
		}

		@Test
		@DisplayName("bawaannya mati: pengaturan yang belum pernah diisi tidak mengirim")
		void bawaanMati() {
			when(settings.get(ReminderSettings.ENABLED)).thenReturn(Optional.empty());

			service.jalankan(HARI_INI);

			verify(sender, never()).kirim(anyString(), anyString());
		}
	}

	@Nested
	@DisplayName("Kegagalan satu tidak menjatuhkan yang lain")
	class Kegagalan {

		@Test
		@DisplayName("mahasiswa tanpa nomor dicatat SKIPPED, bukan didiamkan")
		void tanpaNomor() {
			kandidat(cicilan(10L, HARI_INI.minusDays(1), null));

			var hasil = service.jalankan(HARI_INI);

			assertThat(hasil.dilewati()).isEqualTo(1);
			ReminderLog jejak = jejakTersimpan();
			assertThat(jejak.getStatus()).isEqualTo(ReminderStatus.SKIPPED);
			assertThat(jejak.getError()).contains("Nomor telepon");
			verify(sender, never()).kirim(anyString(), anyString());
		}

		@Test
		@DisplayName("gateway yang belum diatur dicatat SKIPPED, bukan SENT")
		void gatewayBelumDiatur() {
			when(sender.siap()).thenReturn(false);
			kandidat(cicilan(10L, HARI_INI.minusDays(1), "081234567890"));

			var hasil = service.jalankan(HARI_INI);

			assertThat(hasil.dilewati()).isEqualTo(1);
			verify(sender, never()).kirim(anyString(), anyString());

			// Ini yang penting: kalau dicatat SENT, satu putaran uji coba
			// menghabiskan jatah pengingat yang memang cuma sekali — dan begitu
			// gateway dipasang, mahasiswa itu tidak akan pernah diingatkan.
			ReminderLog jejak = jejakTersimpan();
			assertThat(jejak.getStatus()).isEqualTo(ReminderStatus.SKIPPED);
			assertThat(jejak.getError()).contains("belum diatur");
		}

		@Test
		@DisplayName("gateway yang menolak dicatat FAILED, dan putarannya tetap jalan")
		void gatewayMenolak() {
			kandidat(
					cicilan(10L, HARI_INI.minusDays(1), "081234567890"),
					cicilan(11L, HARI_INI.minusDays(2), "081298765432"));
			doThrow(new WhatsAppSender.WhatsAppException("Token ditolak"))
					.when(sender).kirim(eq("6281234567890"), anyString());

			var hasil = service.jalankan(HARI_INI);

			assertThat(hasil.gagal()).isEqualTo(1);
			// Yang kedua tetap terkirim walau yang pertama gagal.
			assertThat(hasil.terkirim()).isEqualTo(1);
			verify(sender).kirim(eq("6281298765432"), anyString());
		}
	}

	@Nested
	@DisplayName("Isi pesan")
	class IsiPesan {

		@Test
		@DisplayName("penanda diganti nilai sungguhan, nominal berformat rupiah")
		void penandaDiganti() {
			Installment cicilan = cicilan(10L, LocalDate.of(2026, 9, 15), "081234567890");
			cicilan.setAmountPaid(new BigDecimal("500000"));

			String pesan = service.susunPesan(cicilan, cicilan.getPaymentPlan().getStudent());

			assertThat(pesan)
					.contains("Uji Coba")
					.contains("UKT")
					.contains("ke-2")
					.contains("Rp 2.000.000")
					// Sisa, bukan nominal penuh: 2.000.000 dikurangi yang sudah dibayar.
					.contains("Rp 1.500.000")
					.contains("15 September 2026")
					.doesNotContain("{");
		}
	}

	@Nested
	@DisplayName("Perapian nomor telepon")
	class NomorTelepon {

		@Test
		@DisplayName("bentuk yang diketik manusia dirapikan ke 62xxx")
		void bentukBermacam() {
			assertThat(GatewayWhatsAppSender.rapikanNomor("081234567890")).isEqualTo("6281234567890");
			assertThat(GatewayWhatsAppSender.rapikanNomor("+6281234567890")).isEqualTo("6281234567890");
			assertThat(GatewayWhatsAppSender.rapikanNomor("0812-3456-7890")).isEqualTo("6281234567890");
			assertThat(GatewayWhatsAppSender.rapikanNomor("0812 3456 7890")).isEqualTo("6281234567890");
			assertThat(GatewayWhatsAppSender.rapikanNomor("81234567890")).isEqualTo("6281234567890");
		}

		@Test
		@DisplayName("yang kosong atau terlalu pendek ditolak, bukan dikirim ke nomor entah siapa")
		void tidakMasukAkal() {
			assertThat(GatewayWhatsAppSender.rapikanNomor(null)).isNull();
			assertThat(GatewayWhatsAppSender.rapikanNomor("")).isNull();
			assertThat(GatewayWhatsAppSender.rapikanNomor("0812")).isNull();
			assertThat(GatewayWhatsAppSender.rapikanNomor("-")).isNull();
		}
	}
}
