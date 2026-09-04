package ac.kampus.pembayaran.common;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

/**
 * Menyajikan berkas yang sengaja TIDAK ditaruh di folder publik.
 *
 * <p>Bukti transfer, KTP, dan Kartu Keluarga sama-sama memuat data yang tidak
 * boleh bisa dibuka siapa saja yang menebak URL. Keduanya karena itu lewat
 * endpoint yang memeriksa hak akses dulu, dan bagian penyajiannya dikumpulkan
 * di sini supaya aturan cache dan tipe berkasnya tidak berbeda antar tempat.
 */
public final class StoredFileResponse {

	private StoredFileResponse() {
	}

	/**
	 * @param pesanBilaHilang dipakai saat baris databasenya menunjuk berkas yang
	 *                        tidak ada di disk — kasus nyata setelah pemulihan
	 *                        basis data tanpa folder unggahannya.
	 */
	public static ResponseEntity<Resource> inline(Path file, String pesanBilaHilang) {
		if (!Files.exists(file)) {
			throw new NotFoundException(pesanBilaHilang);
		}

		Resource resource;
		try {
			resource = new UrlResource(file.toUri());
		} catch (IOException e) {
			throw new NotFoundException(pesanBilaHilang);
		}

		return ResponseEntity.ok()
				.contentType(tebakTipe(file))
				// inline supaya bisa langsung tampil di panel, bukan terunduh
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename(file.getFileName().toString())
								.build().toString())
				// Privat: berkasnya milik satu orang, tidak boleh disimpan proxy bersama.
				.cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
				.body(resource);
	}

	public static MediaType tebakTipe(Path file) {
		String nama = file.getFileName().toString().toLowerCase(Locale.ROOT);
		if (nama.endsWith(".png")) return MediaType.IMAGE_PNG;
		if (nama.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
		if (nama.endsWith(".bmp")) return MediaType.parseMediaType("image/bmp");
		if (nama.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
		return MediaType.IMAGE_JPEG;
	}
}
