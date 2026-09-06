package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.common.BusinessRuleException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Penyimpanan berkas bukti bayar dan dokumen mahasiswa. */
@Slf4j
@Service
public class FileStorageService {

	private static final Set<String> EKSTENSI_DIIZINKAN =
			Set.of("jpg", "jpeg", "png", "webp", "bmp", "pdf");

	private static final long MAKS_BYTE = 10L * 1024 * 1024;

	private final Path root;

	public FileStorageService(@Value("${app.storage.root:./storage}") String root) {
		this.root = Path.of(root).toAbsolutePath().normalize();
	}

	@PostConstruct
	void init() {
		try {
			Files.createDirectories(root);
			log.info("Berkas unggahan disimpan di {}", root);
		} catch (IOException e) {
			throw new UncheckedIOException("Tidak bisa membuat folder penyimpanan: " + root, e);
		}
	}

	/**
	 * @return path relatif terhadap folder penyimpanan, itu yang dicatat di database.
	 */
	public String store(MultipartFile file, String folder) {
		return store(file, folder, MAKS_BYTE);
	}

	/**
	 * Menyimpan unggahan dengan batas ukuran yang ditentukan pemanggil.
	 *
	 * <p>Batasnya tidak sama untuk semua jenis berkas, dan itu disengaja. Bukti
	 * transfer adalah tangkapan layar — sepuluh megabyte sudah kelewat longgar,
	 * dan melonggarkannya lagi hanya membuka pintu untuk unggahan yang tidak
	 * ada gunanya. Naskah disertasi memang besar, dan menolaknya di angka yang
	 * sama akan membuat fitur itu tidak terpakai sama sekali.
	 */
	public String store(MultipartFile file, String folder, long maksByte) {
		if (file.isEmpty()) {
			throw new BusinessRuleException("Berkas kosong.");
		}
		if (file.getSize() > maksByte) {
			throw new BusinessRuleException(
					"Berkas terlalu besar. Maksimal %d MB.".formatted(maksByte / (1024 * 1024)));
		}

		String ekstensi = extensionOf(file.getOriginalFilename());
		if (!EKSTENSI_DIIZINKAN.contains(ekstensi)) {
			throw new BusinessRuleException(
					"Format .%s tidak didukung. Gunakan JPG, PNG, WEBP, BMP, atau PDF."
							.formatted(ekstensi));
		}

		// Dikelompokkan per tanggal supaya satu folder tidak menampung ribuan berkas.
		LocalDate today = LocalDate.now();
		Path folderTujuan = root.resolve(folder).resolve(today.toString());
		String namaBerkas = UUID.randomUUID() + "." + ekstensi;

		try {
			Files.createDirectories(folderTujuan);
			try (var in = file.getInputStream()) {
				Files.copy(in, folderTujuan.resolve(namaBerkas), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Gagal menyimpan berkas.", e);
		}

		return "%s/%s/%s".formatted(folder, today, namaBerkas);
	}

	/**
	 * Menyimpan berkas yang dihasilkan sistem sendiri, bukan unggahan pengguna.
	 *
	 * <p>Dipakai untuk gambar hasil praproses OCR. Tidak melewati pemeriksaan
	 * ekstensi dan ukuran seperti {@link #store}: isinya bukan berasal dari
	 * luar, dan kegagalan menyimpannya tidak boleh menggagalkan pembacaan yang
	 * sudah berhasil — karena itu galatnya dikembalikan sebagai kosong, bukan
	 * dilempar.
	 *
	 * @return path relatif, atau kosong bila gagal disimpan.
	 */
	public Optional<String> storeGenerated(byte[] isi, String folder, String ekstensi) {
		if (isi == null || isi.length == 0) {
			return Optional.empty();
		}

		LocalDate today = LocalDate.now();
		Path folderTujuan = root.resolve(folder).resolve(today.toString());
		String namaBerkas = UUID.randomUUID() + "." + ekstensi;

		try {
			Files.createDirectories(folderTujuan);
			Files.write(folderTujuan.resolve(namaBerkas), isi);
			return Optional.of("%s/%s/%s".formatted(folder, today, namaBerkas));
		} catch (IOException e) {
			log.warn("Gagal menyimpan berkas hasil sistem di {}: {}", folderTujuan, e.getMessage());
			return Optional.empty();
		}
	}

	public Path resolve(String relativePath) {
		Path resolved = root.resolve(relativePath).normalize();
		// Cegah path traversal: hasilnya harus tetap di dalam folder penyimpanan.
		if (!resolved.startsWith(root)) {
			throw new BusinessRuleException("Path berkas tidak valid.");
		}
		return resolved;
	}

	public boolean exists(String relativePath) {
		return Files.exists(resolve(relativePath));
	}

	private String extensionOf(String filename) {
		if (filename == null) return "";
		int dot = filename.lastIndexOf('.');
		return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}
