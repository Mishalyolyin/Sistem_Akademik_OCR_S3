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
		if (file.isEmpty()) {
			throw new BusinessRuleException("Berkas kosong.");
		}
		if (file.getSize() > MAKS_BYTE) {
			throw new BusinessRuleException("Berkas terlalu besar. Maksimal 10 MB.");
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
