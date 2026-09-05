package ac.kampus.pembayaran.reminder;

import ac.kampus.pembayaran.settings.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pengiriman ke gateway WhatsApp.
 *
 * <p>Yang paling penting di sini ditemukan saat mencobanya sungguhan: dengan
 * token yang salah, Fonnte membalas <b>HTTP 200</b> berisi
 * {@code {"status":false}}. Kalau kegagalan itu tidak dibaca dari badan
 * jawaban, seluruh pengingat tercatat terkirim padahal tidak satu pun sampai —
 * dan karena satu cicilan hanya diingatkan sekali, kekeliruannya permanen.
 */
class GatewayWhatsAppSenderTest {

	private static final String URL = "https://gateway.uji/send";

	private SystemSettingService settings;
	private MockRestServiceServer server;
	private GatewayWhatsAppSender sender;

	@BeforeEach
	void setUp() {
		settings = mock(SystemSettingService.class);
		when(settings.get(ReminderSettings.GATEWAY_URL)).thenReturn(Optional.of(URL));
		when(settings.get(ReminderSettings.GATEWAY_TOKEN)).thenReturn(Optional.of("token-uji"));

		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		sender = new GatewayWhatsAppSender(settings, builder);
	}

	@Test
	@DisplayName("mengirim tujuan dan isi pesan beserta token di header")
	void mengirimDenganToken() {
		server.expect(requestTo(URL))
				.andExpect(header("Authorization", "token-uji"))
				.andExpect(content().string("target=6281234567890&message=Halo+Uji"))
				.andRespond(withSuccess("{\"status\":true,\"id\":[\"1\"]}",
						MediaType.APPLICATION_JSON));

		assertThatCode(() -> sender.kirim("6281234567890", "Halo Uji"))
				.doesNotThrowAnyException();

		server.verify();
	}

	@Test
	@DisplayName("HTTP 200 yang badannya status:false tetap dianggap GAGAL")
	void suksesPalsu() {
		server.expect(requestTo(URL)).andRespond(withSuccess(
				"{\"status\":false,\"reason\":\"token invalid\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> sender.kirim("6281234567890", "Halo"))
				.isInstanceOf(WhatsAppSender.WhatsAppException.class)
				.hasMessageContaining("token invalid");
	}

	@Test
	@DisplayName("status:\"false\" sebagai teks juga tertangkap")
	void suksesPalsuBerupaTeks() {
		server.expect(requestTo(URL)).andRespond(withSuccess(
				"{\"status\": \"false\", \"message\": \"device not found\"}",
				MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> sender.kirim("6281234567890", "Halo"))
				.isInstanceOf(WhatsAppSender.WhatsAppException.class)
				.hasMessageContaining("device not found");
	}

	@Test
	@DisplayName("galat HTTP dibungkus jadi WhatsAppException, bukan bocor apa adanya")
	void galatHttp() {
		server.expect(requestTo(URL)).andRespond(withServerError());

		assertThatThrownBy(() -> sender.kirim("6281234567890", "Halo"))
				.isInstanceOf(WhatsAppSender.WhatsAppException.class)
				.hasMessageContaining("tidak bisa dihubungi");
	}

	@Test
	@DisplayName("jawaban kosong tidak dianggap gagal; sebagian gateway memang begitu")
	void jawabanKosong() {
		server.expect(requestTo(URL))
				.andRespond(withSuccess().contentType(MediaType.TEXT_PLAIN));

		assertThatCode(() -> sender.kirim("6281234567890", "Halo"))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("tanpa token, pesan tidak dikirim ke mana pun")
	void tanpaToken() {
		when(settings.get(ReminderSettings.GATEWAY_TOKEN)).thenReturn(Optional.empty());

		assertThat(sender.siap()).isFalse();
		assertThatCode(() -> sender.kirim("6281234567890", "Halo"))
				.doesNotThrowAnyException();

		// Tidak ada permintaan yang diharapkan, jadi verify gagal kalau ada yang terkirim.
		server.verify();
	}

	@Test
	@DisplayName("siap() hanya benar bila alamat dan token dua-duanya terisi")
	void kesiapan() {
		assertThat(sender.siap()).isTrue();

		when(settings.get(ReminderSettings.GATEWAY_URL)).thenReturn(Optional.empty());
		assertThat(sender.siap()).isFalse();
	}

	@Test
	@DisplayName("status HTTP selain 2xx juga gagal walau badannya rapi")
	void statusBukanDuaRatus() {
		server.expect(requestTo(URL)).andRespond(
				withStatus(HttpStatus.UNAUTHORIZED)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"status\":true}"));

		assertThatThrownBy(() -> sender.kirim("6281234567890", "Halo"))
				.isInstanceOf(WhatsAppSender.WhatsAppException.class);
	}
}
