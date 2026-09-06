package ac.kampus.pembayaran.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Dashboard")
public class DashboardController {

	private final DashboardRepository repository;

	/** Jumlah bukti bayar per status, untuk kartu ringkasan yang bisa diklik. */
	public record RingkasanStatus(
			long menungguDibaca,
			long perluDitinjau,
			long ditolak,
			long terverifikasi,
			long gagalDibaca
	) {
	}

	public record Summary(
			long mahasiswaAktif,
			long perluDitinjau,
			long gagalDibaca,
			RingkasanStatus status,
			BigDecimal totalTertagih,
			BigDecimal totalTerkumpul,
			BigDecimal totalSaldoMahasiswa,
			List<KategoriRingkas> perKategori,
			List<KelasRingkas> perKelas,
			List<AntreanItem> antrean
	) {
	}

	public record KategoriRingkas(
			String kategori,
			long jumlahTagihan,
			BigDecimal tertagih,
			BigDecimal terkumpul
	) {
	}

	/**
	 * Rekap satu kelas: uangnya sekaligus keadaan pembacaan buktinya.
	 *
	 * <p>Angka OCR ikut per kelas karena masalah pembacaan hampir selalu
	 * berkelompok — satu kelas yang diajari cara memfoto struk dengan cara yang
	 * sama akan menghasilkan bukti yang sama sulitnya dibaca. Melihatnya hanya
	 * sebagai satu angka global menyembunyikan justru kelas yang perlu dibantu.
	 */
	public record KelasRingkas(
			String kelas,
			long jumlahMahasiswa,
			BigDecimal tertagih,
			BigDecimal terkumpul,
			/** Bukti bayar yang sudah pernah dibaca OCR di kelas ini. */
			long buktiDibaca,
			/** Yang masih menunggu keputusan admin. */
			long buktiPerluDitinjau,
			/** Yang pembacaannya gagal setelah semua percobaan habis. */
			long buktiGagalDibaca,
			/** Rata-rata keyakinan pembacaan; kosong bila belum ada yang dibaca. */
			BigDecimal rataKeyakinan
	) {
	}

	public record AntreanItem(
			Long paymentId,
			String namaMahasiswa,
			String nim,
			String kategori,
			BigDecimal nominal,
			String status,
			BigDecimal keyakinan,
			Instant diunggah
	) {
	}

	@GetMapping("/summary")
	@Operation(summary = "Angka ringkasan untuk dashboard admin")
	@Transactional(readOnly = true)
	public Summary summary() {
		RingkasanStatus status = repository.countPaymentsByStatus();

		return new Summary(
				repository.countActiveStudents(),
				// Dipertahankan supaya klien lama tetap terbaca; sumbernya sama.
				status.perluDitinjau(),
				status.gagalDibaca(),
				status,
				repository.totalBilled(),
				repository.totalCollected(),
				repository.totalWalletBalance(),
				repository.summaryByCategory(),
				repository.summaryByClass(),
				repository.verificationQueue());
	}
}
