package ac.kampus.pembayaran.reminder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Pengingat jatuh tempo: riwayat kiriman dan tombol jalankan sekarang.
 *
 * <p>Tombolnya bukan kemewahan. Penjadwal berjalan sekali sehari, jadi tanpa
 * cara menjalankannya sendiri admin baru tahu pengaturannya salah keesokan
 * harinya — atau tidak tahu sama sekali, karena yang gagal hanya tercatat di
 * log server.
 */
@RestController
@RequestMapping("/pengingat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Pengingat")
public class ReminderController {

	private final ReminderService service;
	private final ReminderLogRepository logRepository;
	private final WhatsAppSender sender;

	public record StatusResponse(
			/** Gateway sudah diatur, atau pesan hanya dicatat di log. */
			boolean gatewaySiap,
			List<RiwayatItem> riwayat
	) {
	}

	public record RiwayatItem(
			Long id,
			Long installmentId,
			Long studentId,
			ReminderKind jenis,
			String nomor,
			ReminderStatus status,
			String galat,
			Instant waktu
	) {
		static RiwayatItem from(ReminderLog log) {
			return new RiwayatItem(
					log.getId(), log.getInstallmentId(), log.getStudentId(),
					log.getKind(), log.getPhone(), log.getStatus(),
					log.getError(), log.getCreatedAt());
		}
	}

	@GetMapping
	@Operation(summary = "Kesiapan gateway dan 50 pengingat terakhir")
	public StatusResponse status() {
		return new StatusResponse(
				sender.siap(),
				logRepository.findTop50ByOrderByCreatedAtDesc().stream()
						.map(RiwayatItem::from)
						.toList());
	}

	@PostMapping("/jalankan")
	@Operation(summary = "Jalankan putaran pengingat sekarang, tanpa menunggu jadwalnya")
	public ReminderService.Hasil jalankanSekarang() {
		return service.jalankan(LocalDate.now());
	}
}
