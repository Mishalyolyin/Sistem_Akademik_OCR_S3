package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Membatalkan satu tagihan yang salah dibuat.
 *
 * <h2>Kenapa ini ada</h2>
 *
 * <p>Dua indeks unik di {@code payment_plans} membuat satu kekeliruan jadi
 * permanen. {@code uq_one_time_plan} hanya membolehkan satu tagihan Seminar
 * Proposal seumur studi, dan {@code uq_active_plan} satu tagihan UKT per
 * tahun-term. Keduanya mengecualikan status {@code CANCELLED} — tapi sampai
 * kelas ini ada, tidak satu pun baris kode pernah menuliskan status itu.
 *
 * <p>Akibatnya: tagihan Seminar Proposal yang terlanjur dibuat untuk mahasiswa
 * yang keliru membuat mahasiswa itu tidak akan pernah bisa punya tagihan
 * Seminar Proposal lagi, dan tagihan UKT di tahun yang salah mengunci tahun itu
 * sekaligus ikut memakan jatah 6 semester. Satu-satunya jalan keluar sebelum
 * ini adalah menghapus mahasiswanya, atau menyentuh database langsung.
 *
 * <h2>Yang sengaja TIDAK dilakukan di sini</h2>
 *
 * <p>Pembatalan ini <b>tidak menyentuh uang</b>. Tagihan yang sudah menerima
 * pembayaran sah ditolak mentah-mentah, bukan dibatalkan sambil menarik semua
 * alokasinya sekaligus. Alasannya: menarik uang adalah pernyataan bahwa kampus
 * membatalkan penerimaan sejumlah rupiah, dan tiap penarikan berhak atas
 * alasannya sendiri di jejak audit. Satu tombol yang membalik lima transaksi
 * dengan satu kalimat alasan menghapus justru keterangan yang paling dicari
 * ketika pembukuan diperiksa.
 *
 * <p>Jalannya karena itu dua langkah dan memang disengaja: batalkan dulu tiap
 * keputusan verifikasinya di panel verifikasi — masing-masing dengan alasannya
 * — baru tagihannya bisa dibatalkan.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanCancellationService {

	/** Status yang berarti uangnya diakui masuk. Yang ditolak tidak termasuk. */
	private static final List<PaymentStatus> DIAKUI =
			List.of(PaymentStatus.VERIFIED, PaymentStatus.AUTO_VERIFIED);

	private static final int ALASAN_MINIMAL = 5;

	private final PaymentPlanRepository planRepository;
	private final PaymentRepository paymentRepository;

	@Transactional
	public PaymentPlan batalkan(Long planId, String alasan, Long adminId) {
		if (alasan == null || alasan.trim().length() < ALASAN_MINIMAL) {
			throw new BusinessRuleException(
					"Alasan pembatalan wajib diisi, minimal %d karakter."
							.formatted(ALASAN_MINIMAL));
		}

		PaymentPlan plan = planRepository.findWithInstallmentsById(planId)
				.orElseThrow(() -> NotFoundException.of("Tagihan", planId));

		if (plan.getStatus() == PlanStatus.CANCELLED) {
			throw new BusinessRuleException("Tagihan ini sudah dibatalkan sebelumnya.");
		}

		tolakBilaAdaUangMasuk(plan);

		plan.setStatus(PlanStatus.CANCELLED);
		plan.setCancelledAt(Instant.now());
		plan.setCancelledBy(adminId);
		plan.setCancelReason(alasan.trim());
		planRepository.save(plan);

		log.warn("Tagihan {} ({} {} {}) milik mahasiswa {} dibatalkan oleh admin {}: {}",
				planId, plan.getCategory(), plan.getAcademicYear(), plan.getTerm(),
				plan.getStudent().getId(), adminId, alasan.trim());

		return plan;
	}

	/**
	 * Pesannya sengaja menyebut jumlah dan nominalnya. "Tidak bisa dibatalkan"
	 * saja membuat admin menebak-nebak apa yang menghalangi; menyebut "2
	 * pembayaran senilai Rp 2.400.000" langsung menunjukkan apa yang harus
	 * dibereskan lebih dulu dan di mana.
	 */
	private void tolakBilaAdaUangMasuk(PaymentPlan plan) {
		long jumlah = paymentRepository.countOnPlan(plan.getId(), DIAKUI);
		if (jumlah == 0) {
			return;
		}

		BigDecimal nominal = paymentRepository.sumOnPlan(plan.getId(), DIAKUI);
		throw new BusinessRuleException(
				("Tagihan ini sudah menerima %d pembayaran terverifikasi senilai Rp %s. "
						+ "Batalkan dulu keputusan verifikasinya satu per satu di panel "
						+ "verifikasi, baru tagihannya bisa dibatalkan.")
						.formatted(jumlah, rupiah(nominal)));
	}

	private static String rupiah(BigDecimal nilai) {
		return String.format("%,.0f", nilai).replace(',', '.');
	}
}
