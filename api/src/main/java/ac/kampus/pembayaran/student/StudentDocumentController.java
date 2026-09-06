package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.StoredFileResponse;
import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.student.self.StudentSelfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Menyajikan dokumen wajib mahasiswa: foto, KTP, Kartu Keluarga, dan ijazah.
 *
 * <p>Sebelumnya keempat berkas ini hanya punya jalur unggah. Berkasnya tersimpan
 * di disk tapi tidak pernah bisa dibuka kembali oleh siapa pun — sehingga gate
 * dokumen wajib berjalan tanpa ada seorang pun yang benar-benar bisa memeriksa
 * apa yang diunggah.
 *
 * <p>Berkasnya TIDAK ditaruh di folder publik: KTP dan Kartu Keluarga memuat
 * NIK, nama ibu kandung, dan alamat, jadi aksesnya harus melewati pemeriksaan
 * hak akses seperti bukti bayar.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Mahasiswa")
public class StudentDocumentController {

	private final StudentService service;
	private final StudentSelfService selfService;
	private final FileStorageService storage;

	/**
	 * Ringkasan pembacaan keempat dokumen: cocok, tidak, atau belum bisa
	 * disimpulkan. Dipisah dari daftar mahasiswa supaya halaman daftar tidak ikut
	 * memuat hasil OCR yang hanya dipakai di halaman detail.
	 */
	@GetMapping("/students/{id}/dokumen")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Hasil pembacaan dokumen wajib satu mahasiswa")
	public List<StudentDocumentCheck.Hasil> pemeriksaan(@PathVariable Long id) {
		return StudentDocumentCheck.untuk(service.get(id));
	}

	@GetMapping("/students/{id}/dokumen/{jenis}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Buka dokumen wajib satu mahasiswa")
	public ResponseEntity<Resource> dokumenMahasiswa(
			@PathVariable Long id, @PathVariable String jenis) {

		return sajikan(service.get(id), StudentDocument.dariKode(jenis));
	}

	/**
	 * Mahasiswa membuka dokumennya sendiri. Id-nya tidak pernah datang dari
	 * klien, jadi tidak ada yang bisa ditebak untuk membuka milik orang lain.
	 */
	@GetMapping("/me/dokumen/{jenis}")
	@PreAuthorize("hasRole('MAHASISWA')")
	@Operation(summary = "Buka dokumen wajib milik sendiri")
	public ResponseEntity<Resource> dokumenSendiri(@PathVariable String jenis) {
		return sajikan(selfService.current(), StudentDocument.dariKode(jenis));
	}

	private ResponseEntity<Resource> sajikan(Student student, StudentDocument jenis) {
		String path = jenis.pathOf(student);
		if (path == null || path.isBlank()) {
			// Belum diunggah bukan berarti alamatnya salah, jadi bukan 404:
			// bedanya penting supaya UI bisa bilang "belum diunggah" alih-alih
			// "berkas hilang".
			throw new BusinessRuleException(
					"%s milik %s belum diunggah.".formatted(jenis.label(), student.getName()));
		}

		return StoredFileResponse.inline(
				storage.resolve(path),
				"Berkas %s milik %s tidak ditemukan di penyimpanan."
						.formatted(jenis.label(), student.getName()));
	}
}
