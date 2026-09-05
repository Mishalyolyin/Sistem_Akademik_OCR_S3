package ac.kampus.pembayaran.reminder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Menjalankan pengingat sekali sehari.
 *
 * <p>Jamnya diatur lewat properti, bawaannya 08.00 waktu server. Pengingat
 * tengah malam sampai ke ponsel mahasiswa tengah malam juga.
 *
 * <p>Penjadwal ini sengaja tipis: seluruh keputusannya ada di
 * {@link ReminderService}, yang menerima tanggal sebagai parameter supaya bisa
 * diuji tanpa menunggu jam berganti.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

	private final ReminderService service;

	@Scheduled(cron = "${app.reminder.cron:0 0 8 * * *}", zone = "${app.reminder.zone:Asia/Jakarta}")
	public void jalankanHarian() {
		try {
			service.jalankan(LocalDate.now());
		} catch (RuntimeException e) {
			// Penjadwal yang melempar akan diam selamanya di sebagian penjadwal
			// Spring; ditangkap di sini supaya putaran besok tetap berjalan.
			log.error("Putaran pengingat gagal seluruhnya", e);
		}
	}
}
