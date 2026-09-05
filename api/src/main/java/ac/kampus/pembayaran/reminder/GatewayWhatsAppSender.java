package ac.kampus.pembayaran.reminder;

import ac.kampus.pembayaran.settings.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Locale;

/**
 * Pengirim lewat gateway WhatsApp HTTP — Fonnte, Wablas, dan sejenisnya.
 *
 * <p>Ketiganya menerima POST form biasa berisi tujuan dan isi pesan, dengan
 * token di header {@code Authorization}. Alamat dan tokennya ada di pengaturan
 * sistem, bukan di berkas konfigurasi, supaya bisa diganti tanpa deploy ulang —
 * termasuk saat nomor pengirimnya perlu dipindah karena diblokir.
 *
 * <p>Selama tokennya kosong, pesan hanya dicatat di log dan TIDAK dikirim. Itu
 * bukan mode darurat melainkan cara kerja yang dimaksudkan: seluruh aturan
 * pengingat bisa dijalankan dan diperiksa lebih dulu tanpa mengirim satu pun
 * pesan ke mahasiswa sungguhan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayWhatsAppSender implements WhatsAppSender {

	private final SystemSettingService settings;
	private final RestClient.Builder restClientBuilder;

	@Override
	public boolean siap() {
		return settings.get(ReminderSettings.GATEWAY_TOKEN).isPresent()
				&& settings.get(ReminderSettings.GATEWAY_URL).isPresent();
	}

	@Override
	public void kirim(String nomor, String pesan) {
		String token = settings.get(ReminderSettings.GATEWAY_TOKEN).orElse(null);
		String url = settings.get(ReminderSettings.GATEWAY_URL).orElse(null);

		if (token == null || url == null) {
			log.info("""
					Gateway WhatsApp belum diatur, pesan TIDAK dikirim.
					Tujuan: {}
					Isi: {}""", nomor, pesan);
			return;
		}

		String jawaban;
		try {
			jawaban = restClientBuilder.build()
					.post()
					.uri(url)
					.header("Authorization", token)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body("target=%s&message=%s".formatted(
							urlEncode(nomor), urlEncode(pesan)))
					.retrieve()
					.body(String.class);
		} catch (RestClientException e) {
			throw new WhatsAppException(
					"Gateway WhatsApp menolak atau tidak bisa dihubungi: " + e.getMessage(), e);
		}

		periksaJawaban(jawaban);
	}

	/**
	 * Memeriksa badan jawaban, bukan hanya status HTTP-nya.
	 *
	 * <p>Ini bukan kehati-hatian berlebihan: dicoba sungguhan dengan token yang
	 * salah, Fonnte membalas <b>HTTP 200</b> berisi {@code {"status":false,...}}.
	 * Tanpa pemeriksaan ini, token yang keliru membuat seluruh pengingat tercatat
	 * terkirim padahal tidak satu pun sampai — dan karena satu cicilan hanya
	 * diingatkan sekali, kekeliruan itu permanen.
	 */
	private static void periksaJawaban(String jawaban) {
		if (jawaban == null || jawaban.isBlank()) {
			return;
		}

		String ringkas = jawaban.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
		// Bentuk yang dipakai Fonnte maupun Wablas saat menolak.
		if (ringkas.contains("\"status\":false") || ringkas.contains("\"status\":\"false\"")) {
			throw new WhatsAppException(
					"Gateway menolak pesan: " + potong(jawaban));
		}
	}

	private static String potong(String nilai) {
		return nilai.length() <= 300 ? nilai : nilai.substring(0, 300) + "…";
	}

	private static String urlEncode(String nilai) {
		return java.net.URLEncoder.encode(nilai, java.nio.charset.StandardCharsets.UTF_8);
	}

	/**
	 * Merapikan nomor ke bentuk yang diterima gateway: 62xxx tanpa spasi,
	 * tanda hubung, atau awalan +.
	 *
	 * <p>Nomor mahasiswa diketik manusia saat import, jadi bentuknya bermacam —
	 * 0812…, +62812…, 0812-3456-7890. Mengirimnya apa adanya berarti sebagian
	 * pengingat diam-diam tidak sampai.
	 */
	public static String rapikanNomor(String nomor) {
		if (nomor == null) return null;

		String bersih = nomor.replaceAll("[^0-9+]", "");
		if (bersih.startsWith("+")) bersih = bersih.substring(1);
		if (bersih.startsWith("0")) bersih = "62" + bersih.substring(1);
		if (bersih.startsWith("8")) bersih = "62" + bersih;

		// Terlalu pendek untuk nomor Indonesia mana pun; lebih baik ditolak
		// daripada dikirim ke nomor entah milik siapa.
		return bersih.length() < 10 ? null : bersih.toLowerCase(Locale.ROOT);
	}
}
