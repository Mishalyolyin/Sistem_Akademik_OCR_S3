package ac.kampus.pembayaran.report;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.payment.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Foto profil mahasiswa satu kelas, dibungkus jadi satu ZIP.
 *
 * <p>Dipakai untuk mencetak kartu mahasiswa dan berkas wisuda. Tanpa ini
 * satu-satunya cara mengumpulkannya adalah membuka halaman detail mahasiswa
 * satu per satu dan menyimpan gambarnya dengan klik kanan.
 *
 * <p>Berkasnya dinamai <b>NIM_Nama</b>, bukan nama acak dari penyimpanan.
 * Nama acak memaksa siapa pun yang menerima ZIP ini mencocokkan gambar dengan
 * daftar mahasiswa secara manual — pekerjaan yang justru ingin dihindari
 * dengan mengunduhnya sekaligus.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentPhotoExporter {

	private final JdbcClient jdbc;
	private final FileStorageService storage;

	/**
	 * @param classId batasi ke satu kelas; kosong berarti seluruh mahasiswa aktif
	 */
	@Transactional(readOnly = true)
	public byte[] fotoKelas(Long classId) {
		List<Object[]> mahasiswa = jdbc.sql("""
						SELECT s.nim, s.name, s.profile_picture,
						       COALESCE(c.name, 'Tanpa kelas') AS kelas
						FROM students s
						LEFT JOIN study_classes c ON c.id = s.study_class_id
						WHERE s.active
						  AND s.profile_picture IS NOT NULL
						  AND (CAST(? AS BIGINT) IS NULL OR s.study_class_id = CAST(? AS BIGINT))
						ORDER BY c.name NULLS LAST, s.name
						""")
				.params(classId, classId)
				.query((rs, n) -> new Object[] {
						rs.getString("nim"), rs.getString("name"),
						rs.getString("profile_picture"), rs.getString("kelas") })
				.list();

		if (mahasiswa.isEmpty()) {
			// Sengaja ditolak, bukan menghasilkan ZIP kosong: yang meminta ini
			// hampir pasti salah memilih kelas, dan berkas kosong membuatnya
			// baru sadar setelah membukanya.
			throw new BusinessRuleException(
					"Tidak ada foto yang bisa diunduh untuk pilihan ini. "
							+ "Mahasiswanya belum mengunggah foto, atau kelasnya salah pilih.");
		}

		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
			 ZipOutputStream zip = new ZipOutputStream(out)) {

			int dimasukkan = 0;
			StringBuilder hilang = new StringBuilder();

			for (Object[] row : mahasiswa) {
				String nim = (String) row[0];
				String nama = (String) row[1];
				String path = (String) row[2];

				byte[] isi = baca(path);
				if (isi == null) {
					hilang.append(nim).append(' ').append(nama).append('\n');
					continue;
				}

				// Dikelompokkan per folder kelas supaya ZIP "semua kelas" tetap
				// bisa dipilah tanpa membaca nama berkas satu per satu.
				String entri = "%s/%s_%s.%s".formatted(
						aman((String) row[3]), nim, aman(nama), ekstensi(path));

				zip.putNextEntry(new ZipEntry(entri));
				zip.write(isi);
				zip.closeEntry();
				dimasukkan++;
			}

			// Foto yang tercatat di database tapi berkasnya raib tidak boleh
			// hilang diam-diam: yang menerima ZIP ini perlu tahu siapa yang
			// fotonya belum ada, bukan menghitung sendiri isinya.
			if (!hilang.isEmpty()) {
				zip.putNextEntry(new ZipEntry("FOTO-HILANG.txt"));
				zip.write(("Foto berikut tercatat di database tapi berkasnya tidak ada "
						+ "di penyimpanan:\n\n" + hilang).getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
				log.warn("{} foto mahasiswa tidak ditemukan di penyimpanan saat ekspor.",
						mahasiswa.size() - dimasukkan);
			}

			zip.finish();
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Gagal menyusun ZIP foto.", e);
		}
	}

	private byte[] baca(String path) {
		try {
			return storage.exists(path) ? Files.readAllBytes(storage.resolve(path)) : null;
		} catch (Exception e) {
			return null;
		}
	}

	/** Membuang karakter yang bisa merusak nama entri ZIP atau nama berkas. */
	private static String aman(String teks) {
		return teks.trim().replaceAll("[^A-Za-z0-9 ._-]", "").replace(' ', '_');
	}

	private static String ekstensi(String path) {
		int titik = path.lastIndexOf('.');
		return titik < 0 ? "jpg" : path.substring(titik + 1).toLowerCase(Locale.ROOT);
	}
}
