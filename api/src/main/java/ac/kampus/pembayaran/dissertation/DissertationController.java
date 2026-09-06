package ac.kampus.pembayaran.dissertation;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.common.StoredFileResponse;
import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.student.StudentService;
import ac.kampus.pembayaran.student.self.StudentSelfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Identitas disertasi, dari sisi mahasiswa maupun admin.
 *
 * <p>Mahasiswa mengisi dan mengunggah lewat {@code /me/disertasi}; id-nya tidak
 * pernah datang dari klien, jadi tidak ada yang bisa ditebak untuk menyentuh
 * milik orang lain. Admin membacanya lewat {@code /students/{id}/disertasi}
 * saat memverifikasi bukti bayar tahap ujian — tanpa itu ia hanya melihat
 * nominal, tanpa tahu disertasi mana yang sedang diuji.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Disertasi")
public class DissertationController {

	private final DissertationService service;
	private final StudentSelfService selfService;
	private final StudentService studentService;
	private final FileStorageService storage;

	// --- DTO ---

	public record IdentitasRequest(
			@NotBlank(message = "Judul disertasi wajib diisi.")
			@Size(min = 10, max = 300, message = "Judul disertasi minimal 10 karakter.")
			String title,

			@NotBlank(message = "Nama promotor wajib diisi.")
			@Size(min = 3, max = 150, message = "Nama promotor minimal 3 karakter.")
			String promotor,

			@NotBlank(message = "Nama ko-promotor wajib diisi.")
			@Size(min = 3, max = 150, message = "Nama ko-promotor minimal 3 karakter.")
			String copromotor
	) {
	}

	public record DisertasiResponse(
			String title,
			String promotor,
			String copromotor,
			boolean adaNaskah,
			boolean adaArtikel
	) {
		static DisertasiResponse from(DissertationDetail detail) {
			return new DisertasiResponse(
					detail.getTitle(),
					detail.getPromotor(),
					detail.getCopromotor(),
					terisi(detail.getDissertationFilePath()),
					terisi(detail.getArticleFilePath()));
		}

		private static boolean terisi(String path) {
			return path != null && !path.isBlank();
		}
	}

	// --- Mahasiswa ---

	@GetMapping("/me/disertasi")
	@PreAuthorize("hasRole('MAHASISWA')")
	@Operation(summary = "Identitas disertasi milik sendiri; kosong bila belum diisi")
	public DisertasiResponse milikSendiri() {
		return service.cari(selfService.current().getId())
				.map(DisertasiResponse::from)
				.orElse(null);
	}

	@PutMapping("/me/disertasi")
	@PreAuthorize("hasRole('MAHASISWA')")
	@Operation(summary = "Simpan judul disertasi dan kedua promotor")
	public DisertasiResponse simpanIdentitas(@Valid @RequestBody IdentitasRequest request) {
		return DisertasiResponse.from(service.simpanIdentitas(
				selfService.current().getId(),
				request.title(), request.promotor(), request.copromotor()));
	}

	@PostMapping(value = "/me/disertasi/berkas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('MAHASISWA')")
	@Operation(summary = "Unggah naskah disertasi atau artikel jurnal, berupa PDF")
	public DisertasiResponse unggahBerkas(
			@RequestParam DissertationService.JenisBerkas jenis,
			@RequestParam("file") MultipartFile file) {

		return DisertasiResponse.from(service.unggahBerkas(
				selfService.current().getId(), jenis, file));
	}

	@GetMapping("/me/disertasi/berkas/{jenis}")
	@PreAuthorize("hasRole('MAHASISWA')")
	@Operation(summary = "Buka naskah milik sendiri")
	public ResponseEntity<Resource> berkasSendiri(
			@PathVariable DissertationService.JenisBerkas jenis) {

		return sajikan(selfService.current().getId(), jenis);
	}

	// --- Admin ---

	@GetMapping("/students/{id}/disertasi")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Identitas disertasi satu mahasiswa; kosong bila belum diisi")
	public DisertasiResponse milikMahasiswa(@PathVariable Long id) {
		// Memastikan mahasiswanya memang ada, supaya id ngawur dijawab 404 dan
		// bukan "belum diisi" yang menyesatkan.
		studentService.get(id);

		return service.cari(id).map(DisertasiResponse::from).orElse(null);
	}

	@GetMapping("/students/{id}/disertasi/berkas/{jenis}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Buka naskah disertasi satu mahasiswa")
	public ResponseEntity<Resource> berkasMahasiswa(
			@PathVariable Long id,
			@PathVariable DissertationService.JenisBerkas jenis) {

		return sajikan(id, jenis);
	}

	private ResponseEntity<Resource> sajikan(Long studentId, DissertationService.JenisBerkas jenis) {
		DissertationDetail detail = service.cari(studentId)
				.orElseThrow(() -> NotFoundException.of("Disertasi", studentId));

		String path = jenis == DissertationService.JenisBerkas.DISERTASI
				? detail.getDissertationFilePath()
				: detail.getArticleFilePath();

		if (path == null || path.isBlank()) {
			// Belum diunggah bukan berarti alamatnya salah, jadi bukan 404:
			// bedanya penting supaya layar bisa bilang "belum diunggah" alih-alih
			// "berkas hilang".
			throw new BusinessRuleException(
					"Naskah %s belum diunggah.".formatted(jenis.folder()));
		}

		return StoredFileResponse.inline(
				storage.resolve(path),
				"Naskah %s tidak ditemukan di penyimpanan.".formatted(jenis.folder()));
	}
}
