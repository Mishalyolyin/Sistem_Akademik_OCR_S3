package ac.kampus.pembayaran.reminder;

import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Halaman pengingat: kesiapan gateway, riwayat kiriman, dan tombol jalankan.
 *
 * <p>Tombol "jalankan sekarang" mengirim pesan WhatsApp sungguhan ke mahasiswa
 * sungguhan, jadi batas perannya bukan formalitas — ini satu-satunya endpoint di
 * sistem yang akibatnya keluar dari aplikasi dan sampai ke ponsel orang.
 */
@ControllerTest(ReminderController.class)
class ReminderControllerTest extends ControllerTestSupport {

	@MockitoBean
	private ReminderService service;
	@MockitoBean
	private ReminderLogRepository logRepository;
	@MockitoBean
	private WhatsAppSender sender;

	@BeforeEach
	void setUp() {
		when(sender.siap()).thenReturn(true);
		when(logRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(
				ReminderLog.builder()
						.id(1L).installmentId(10L).studentId(5L)
						.kind(ReminderKind.LEWAT_TEMPO)
						.phone("6281234567890")
						.status(ReminderStatus.SENT)
						.createdAt(Instant.parse("2026-09-10T01:00:00Z"))
						.build(),
				ReminderLog.builder()
						.id(2L).installmentId(11L).studentId(6L)
						.kind(ReminderKind.JATUH_TEMPO)
						.status(ReminderStatus.SKIPPED)
						.error("Nomor telepon belum diisi.")
						.createdAt(Instant.parse("2026-09-10T01:00:01Z"))
						.build()));
	}

	@Test
	@DisplayName("riwayat menyebut status tiap kiriman beserta alasan gagalnya")
	void riwayat() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/pengingat")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gatewaySiap").value(true))
				.andExpect(jsonPath("$.riwayat[0].status").value("SENT"))
				.andExpect(jsonPath("$.riwayat[0].jenis").value("LEWAT_TEMPO"))
				.andExpect(jsonPath("$.riwayat[1].status").value("SKIPPED"))
				.andExpect(jsonPath("$.riwayat[1].galat", containsString("Nomor telepon")));
	}

	@Test
	@DisplayName("gateway yang belum diatur dilaporkan apa adanya, bukan disembunyikan")
	void gatewayBelumSiap() throws Exception {
		when(sender.siap()).thenReturn(false);

		mockMvc.perform(sebagaiAdmin(get("/pengingat")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gatewaySiap").value(false));
	}

	@Test
	@DisplayName("jalankan sekarang mengembalikan ringkasan putarannya")
	void jalankanSekarang() throws Exception {
		when(service.jalankan(any())).thenReturn(new ReminderService.Hasil(9, 4, 3, 2));

		mockMvc.perform(sebagaiAdmin(post("/pengingat/jalankan")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diperiksa").value(9))
				.andExpect(jsonPath("$.terkirim").value(4))
				.andExpect(jsonPath("$.dilewati").value(3))
				.andExpect(jsonPath("$.gagal").value(2));
	}

	@Test
	@DisplayName("mahasiswa tidak boleh memicu kiriman ke siapa pun")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/pengingat/jalankan")))
				.andExpect(status().isForbidden());

		mockMvc.perform(sebagaiMahasiswa(get("/pengingat")))
				.andExpect(status().isForbidden());

		verify(service, never()).jalankan(any());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(post("/pengingat/jalankan"))
				.andExpect(status().isUnauthorized());

		verify(service, never()).jalankan(any());
	}
}
