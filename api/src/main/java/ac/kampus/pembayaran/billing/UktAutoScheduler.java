package ac.kampus.pembayaran.billing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Menjalankan pembuatan tagihan UKT tiap awal semester.
 *
 * <p>Jadwalnya dua kali setahun — 1 September untuk Gasal dan 1 Februari untuk
 * Genap — tapi putarannya sendiri aman dijalankan berapa kali pun: tagihan yang
 * sudah ada tidak dibuat ulang, dan yang belum tiba waktunya tidak dibuat lebih
 * dulu. Karena itu bawaannya justru harian, supaya mahasiswa yang diimpor di
 * tengah semester tidak perlu menunggu enam bulan sampai tagihannya terbit.
 *
 * <p>Penjadwal ini sengaja tipis: seluruh keputusannya ada di
 * {@link UktAutoService}, yang menerima tanggal sebagai parameter supaya bisa
 * diuji tanpa menunggu tanggal berganti.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UktAutoScheduler {

	private final UktAutoService service;

	@Scheduled(cron = "${app.ukt-auto.cron:0 30 6 * * *}", zone = "${app.ukt-auto.zone:Asia/Jakarta}")
	public void jalankanHarian() {
		try {
			service.jalankan(LocalDate.now());
		} catch (RuntimeException e) {
			// Penjadwal yang melempar akan diam selamanya di sebagian penjadwal
			// Spring; ditangkap di sini supaya putaran besok tetap berjalan.
			log.error("Putaran tagihan UKT gagal seluruhnya", e);
		}
	}
}
