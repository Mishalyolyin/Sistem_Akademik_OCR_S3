package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.common.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

/**
 * Menyajikan gambar bukti bayar.
 *
 * <p>Berkas TIDAK ditaruh di folder publik. Aksesnya lewat endpoint ini supaya
 * tetap melewati pemeriksaan hak akses — bukti transfer memuat nama, nominal,
 * dan nomor rekening yang tidak boleh bisa dibuka siapa saja yang menebak URL.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'MAHASISWA')")
@Tag(name = "Pembayaran")
public class ProofFileController {

	private final PaymentRepository repository;
	private final FileStorageService storage;
	private final ac.kampus.pembayaran.student.StudentRepository studentRepository;

	@GetMapping("/{id}/proof")
	@Operation(summary = "Ambil gambar bukti bayar")
	public ResponseEntity<Resource> proof(@PathVariable Long id) {
		Payment payment = repository.findById(id)
				.orElseThrow(() -> NotFoundException.of("Pembayaran", id));

		assertBolehMelihat(payment);

		Path file = storage.resolve(payment.getProofFilePath());
		if (!Files.exists(file)) {
			throw new NotFoundException("Berkas bukti pembayaran %d tidak ditemukan.".formatted(id));
		}

		Resource resource;
		try {
			resource = new UrlResource(file.toUri());
		} catch (IOException e) {
			throw new NotFoundException("Berkas bukti tidak bisa dibaca.");
		}

		return ResponseEntity.ok()
				.contentType(tebakTipe(file))
				// inline supaya bisa langsung tampil di panel verifikasi
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename(file.getFileName().toString())
								.build().toString())
				.cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
				.body(resource);
	}

	/**
	 * Mahasiswa hanya boleh membuka bukti bayarnya sendiri. Tanpa pemeriksaan
	 * ini, siapa pun yang login bisa melihat bukti transfer orang lain hanya
	 * dengan mengganti angka di URL.
	 */
	private void assertBolehMelihat(Payment payment) {
		var auth = org.springframework.security.core.context.SecurityContextHolder
				.getContext().getAuthentication();

		boolean mahasiswa = auth.getAuthorities().stream()
				.anyMatch(a -> "ROLE_MAHASISWA".equals(a.getAuthority()));
		if (!mahasiswa) {
			return;
		}

		Long userId = ac.kampus.pembayaran.auth.AuthService.currentUserId();
		Long pemilik = studentRepository.findByUserId(userId)
				.map(ac.kampus.pembayaran.student.Student::getId)
				.orElse(null);

		if (pemilik == null || !pemilik.equals(payment.getStudent().getId())) {
			throw new org.springframework.security.access.AccessDeniedException(
					"Bukti bayar ini bukan milik Anda.");
		}
	}

	private MediaType tebakTipe(Path file) {
		String nama = file.getFileName().toString().toLowerCase(Locale.ROOT);
		if (nama.endsWith(".png")) return MediaType.IMAGE_PNG;
		if (nama.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
		if (nama.endsWith(".bmp")) return MediaType.parseMediaType("image/bmp");
		if (nama.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
		return MediaType.IMAGE_JPEG;
	}
}
