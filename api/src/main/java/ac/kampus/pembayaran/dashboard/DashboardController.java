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

	public record Summary(
			long mahasiswaAktif,
			long perluDitinjau,
			long gagalDibaca,
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

	public record KelasRingkas(
			String kelas,
			long jumlahMahasiswa,
			BigDecimal tertagih,
			BigDecimal terkumpul
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
		return new Summary(
				repository.countActiveStudents(),
				repository.countPaymentsNeedingReview(),
				repository.countFailedPayments(),
				repository.totalBilled(),
				repository.totalCollected(),
				repository.totalWalletBalance(),
				repository.summaryByCategory(),
				repository.summaryByClass(),
				repository.verificationQueue());
	}
}
