package ac.kampus.pembayaran.report;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Dataset pembacaan bukti bayar untuk melatih model.
 *
 * <p>Satu baris = satu bukti bayar yang <b>sudah diputuskan manusia</b>.
 * Isinya sepasang: apa yang dibaca mesin (keyakinan, nominal, tanggal, flag)
 * dan apa yang akhirnya diputuskan admin. Pasangan itulah yang bisa dipelajari
 * — ambang keyakinan hari ini ditebak, bukan diukur, dan data ini yang nanti
 * mengukurnya.
 *
 * <h2>Kenapa hanya keputusan manusia</h2>
 *
 * <p>Barisnya disaring ke {@code verified_by IS NOT NULL}. Pembayaran yang
 * berstatus {@code AUTO_VERIFIED} adalah tebakan mesin itu sendiri, dan
 * memasukkannya sebagai label berarti melatih model dari jawabannya sendiri:
 * kesalahan yang sudah ada justru dikukuhkan, bukan diperbaiki. Verifikasi yang
 * dibatalkan admin juga otomatis keluar dari dataset, karena pembatalan
 * mengosongkan {@code verified_by} — labelnya memang sudah tidak berlaku.
 *
 * <h2>Teks mentah</h2>
 *
 * <p>{@code raw_text} adalah fitur paling berguna sekaligus paling sensitif:
 * di dalamnya ada nama, nomor rekening, dan saldo. Karena itu ia hanya ikut
 * bila diminta secara eksplisit, dan nama mahasiswa tidak pernah ikut sebagai
 * kolom tersendiri — yang ada hanya {@code student_id}.
 */
@Component
@RequiredArgsConstructor
public class OcrDatasetExporter {

	/**
	 * Kolom dataset. Urutannya: penanda baris, lalu yang dibaca mesin, lalu
	 * label yang diputuskan manusia. Sengaja label ada di belakang supaya
	 * mudah dipisah sebagai target saat dimuat.
	 */
	private static final List<String> KOLOM = List.of(
			"payment_id", "student_id", "kategori", "kelas",
			"nominal_diklaim", "nominal_tagihan",
			"ocr_status", "ocr_keyakinan", "ocr_nominal", "ocr_tanggal", "ocr_bank",
			"ocr_panjang_teks", "ocr_jumlah_flag",
			"ocr_rekening_cocok", "ocr_nama_cocok", "ocr_flags",
			"selisih_nominal",
			"keputusan_mesin", "mesin_sepakat",
			"label", "diputuskan_pada", "alasan_tolak");

	private static final String KOLOM_TEKS = "ocr_teks_mentah";

	private final JdbcClient jdbc;
	private final ac.kampus.pembayaran.payment.FileStorageService storage;

	/**
	 * @param sertakanTeks ikut menyertakan hasil OCR mentah. Mati secara bawaan:
	 *                     berkasnya membengkak dan isinya data pribadi.
	 */
	@Transactional(readOnly = true)
	public byte[] datasetOcr(boolean sertakanTeks) {
		StringBuilder csv = new StringBuilder();
		tulisBaris(csv, sertakanTeks
				? gabung(KOLOM, KOLOM_TEKS)
				: KOLOM);

		List<Object[]> baris = jdbc.sql(SQL)
				.query((rs, n) -> new Object[] {
						rs.getLong("payment_id"),
						rs.getLong("student_id"),
						rs.getString("kategori"),
						rs.getString("kelas"),
						rs.getBigDecimal("nominal_diklaim"),
						rs.getBigDecimal("nominal_tagihan"),
						rs.getString("ocr_status"),
						rs.getBigDecimal("ocr_keyakinan"),
						rs.getString("ocr_nominal"),
						rs.getString("ocr_tanggal"),
						rs.getString("ocr_bank"),
						rs.getInt("ocr_panjang_teks"),
						rs.getInt("ocr_jumlah_flag"),
						rs.getBoolean("ocr_rekening_cocok"),
						rs.getBoolean("ocr_nama_cocok"),
						rs.getString("ocr_flags"),
						rs.getBigDecimal("selisih_nominal"),
						rs.getString("keputusan_mesin"),
						rs.getObject("mesin_sepakat"),
						rs.getString("label"),
						rs.getString("diputuskan_pada"),
						rs.getString("alasan_tolak"),
						rs.getString("ocr_teks_mentah") })
				.list();

		int kolomTerpakai = sertakanTeks ? KOLOM.size() + 1 : KOLOM.size();
		for (Object[] row : baris) {
			tulisBarisNilai(csv, row, kolomTerpakai);
		}

		// Tanpa BOM. Dataset ini dibaca pandas, bukan Excel, dan BOM di depan
		// membuat nama kolom pertama terbaca ikut membawa karakter itu.
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	// --- Dataset gambar ---

	/**
	 * Ringkasan berapa banyak bahan belajar yang sudah terkumpul.
	 *
	 * <p>Angka yang paling menentukan bukan {@code total}, melainkan
	 * {@code mesinKeliru}: berapa kali admin memutuskan berbeda dari usul mesin.
	 * Itulah satu-satunya ukuran yang menjawab "ambang keyakinan kita kekencangan
	 * atau kekendoran", dan tanpanya angka ambang di halaman pengaturan hanyalah
	 * tebakan yang tidak pernah diperiksa.
	 */
	public record Statistik(
			long berlabel,
			long diterima,
			long ditolak,
			long adaGambar,
			long sudahDibaca,
			long belumDiputuskan,
			long mesinSepakat,
			long mesinKeliru,
			BigDecimal rataKeyakinan
	) {
		/** Persentase kesepakatan mesin dengan admin; kosong bila belum ada bandingannya. */
		public BigDecimal akurasiMesin() {
			long dibandingkan = mesinSepakat + mesinKeliru;
			if (dibandingkan == 0) {
				return null;
			}
			return BigDecimal.valueOf(mesinSepakat)
					.multiply(BigDecimal.valueOf(100))
					.divide(BigDecimal.valueOf(dibandingkan), 1, java.math.RoundingMode.HALF_UP);
		}
	}

	@Transactional(readOnly = true)
	public Statistik statistik() {
		return jdbc.sql(SQL_STATISTIK)
				.query((rs, n) -> new Statistik(
						rs.getLong("berlabel"),
						rs.getLong("diterima"),
						rs.getLong("ditolak"),
						rs.getLong("ada_gambar"),
						rs.getLong("sudah_dibaca"),
						rs.getLong("belum_diputuskan"),
						rs.getLong("mesin_sepakat"),
						rs.getLong("mesin_keliru"),
						rs.getBigDecimal("rata_keyakinan")))
				.single();
	}

	/**
	 * Dataset gambar: satu ZIP berisi {@code gambar/} dan {@code label.csv}.
	 *
	 * <p>Yang dimasukkan adalah gambar hasil praproses — yang benar-benar dibaca
	 * Tesseract — bukan berkas asli unggahan mahasiswa. Itu yang membuat dataset
	 * ini konsisten: tiap gambar sudah melewati pipeline yang sama, jadi model
	 * yang dilatih darinya melihat masukan sebentuk dengan yang akan dilihatnya
	 * saat bekerja.
	 *
	 * <p>Bukti yang gambarnya belum tersimpan dilewati tanpa suara. Kolom
	 * {@code processed_file_path} baru ada sejak migrasi V12, jadi seluruh bukti
	 * yang masuk sebelum itu memang tidak punya gambarnya — dan tidak akan pernah
	 * punya, karena gambar yang tidak disimpan tidak bisa dipulihkan.
	 */
	@Transactional(readOnly = true)
	public byte[] datasetGambar(int batas) {
		List<Object[]> baris = jdbc.sql(SQL_GAMBAR)
				.param(batas)
				.query((rs, n) -> new Object[] {
						rs.getLong("payment_id"),
						rs.getString("processed_file_path"),
						rs.getString("label"),
						rs.getBigDecimal("nominal_diklaim"),
						rs.getString("ocr_nominal"),
						rs.getBigDecimal("ocr_keyakinan"),
						rs.getString("kategori") })
				.list();

		StringBuilder label = new StringBuilder(
				"berkas,payment_id,label,nominal_diklaim,ocr_nominal,ocr_keyakinan,kategori\n");

		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
			 java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {

			int dimasukkan = 0;
			for (Object[] row : baris) {
				String path = (String) row[1];
				byte[] isi = bacaGambar(path);
				if (isi == null) {
					continue;
				}

				String nama = "%d.png".formatted((Long) row[0]);
				zip.putNextEntry(new java.util.zip.ZipEntry("gambar/" + nama));
				zip.write(isi);
				zip.closeEntry();
				dimasukkan++;

				label.append(kutip(nama)).append(',')
						.append(row[0]).append(',')
						.append(kutip(String.valueOf(row[2]))).append(',')
						.append(row[3] == null ? "" : row[3]).append(',')
						.append(row[4] == null ? "" : row[4]).append(',')
						.append(row[5] == null ? "" : row[5]).append(',')
						.append(kutip(String.valueOf(row[6]))).append('\n');
			}

			zip.putNextEntry(new java.util.zip.ZipEntry("label.csv"));
			zip.write(label.toString().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			zip.putNextEntry(new java.util.zip.ZipEntry("BACA-DULU.txt"));
			zip.write(keterangan(dimasukkan).getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			zip.finish();
			return out.toByteArray();
		} catch (java.io.IOException e) {
			throw new java.io.UncheckedIOException("Gagal menyusun dataset gambar.", e);
		}
	}

	/** Gambar yang hilang dari penyimpanan tidak boleh menggagalkan seluruh ekspor. */
	private byte[] bacaGambar(String path) {
		try {
			if (path == null || !storage.exists(path)) {
				return null;
			}
			return java.nio.file.Files.readAllBytes(storage.resolve(path));
		} catch (Exception e) {
			return null;
		}
	}

	private static String keterangan(int jumlah) {
		return """
				Dataset gambar bukti bayar Program Doktor PAI
				=============================================

				Isi:
				  gambar/<payment_id>.png  Gambar hasil praproses OpenCV, yaitu gambar yang
				                           benar-benar dibaca Tesseract. Bukan berkas asli
				                           yang diunggah mahasiswa.
				  label.csv                Label dan sedikit konteks untuk tiap gambar.

				Jumlah gambar: %d

				Label diambil dari keputusan ADMIN, bukan dari tebakan mesin. Pembayaran
				yang berstatus AUTO_VERIFIED tanpa pernah disentuh admin sengaja tidak
				ikut: melatih model dari jawabannya sendiri hanya mengukuhkan kesalahan
				yang sudah ada.

				Peringatan: gambar ini memuat data pribadi — nama, nomor rekening, dan
				saldo. Jangan disebar, dan jangan diunggah ke layanan pihak ketiga tanpa
				izin.
				""".formatted(jumlah);
	}

	private static List<String> gabung(List<String> awal, String tambahan) {
		List<String> semua = new java.util.ArrayList<>(awal);
		semua.add(tambahan);
		return semua;
	}

	private static void tulisBaris(StringBuilder csv, List<String> nilai) {
		for (int i = 0; i < nilai.size(); i++) {
			if (i > 0) csv.append(',');
			csv.append(kutip(nilai.get(i)));
		}
		csv.append('\n');
	}

	private static void tulisBarisNilai(StringBuilder csv, Object[] row, int sampai) {
		for (int i = 0; i < sampai; i++) {
			if (i > 0) csv.append(',');
			csv.append(kutip(row[i] == null ? "" : String.valueOf(row[i])));
		}
		csv.append('\n');
	}

	/**
	 * Mengutip satu sel CSV. Wajib untuk kolom flag dan teks mentah: keduanya
	 * memuat koma dan baris baru, dan tanpa kutipan satu bukti bayar akan
	 * terbaca sebagai puluhan baris rusak.
	 */
	static String kutip(String nilai) {
		boolean perlu = nilai.indexOf(',') >= 0
				|| nilai.indexOf('"') >= 0
				|| nilai.indexOf('\n') >= 0
				|| nilai.indexOf('\r') >= 0;
		if (!perlu) {
			return nilai;
		}
		return '"' + nilai.replace("\"", "\"\"") + '"';
	}

	/**
	 * Flag ditelusuri lewat {@code jsonb_array_elements_text}, dan tiap
	 * pemakaiannya dijaga {@code jsonb_typeof(...) = 'array'} — bukti lama
	 * bisa saja tidak punya kunci {@code flags} sama sekali, dan fungsi itu
	 * melempar galat kalau yang diberikan bukan array.
	 */
	private static final String SQL = """
			SELECT p.id                                        AS payment_id,
			       p.student_id                                AS student_id,
			       COALESCE(pl.category::text, '')             AS kategori,
			       COALESCE(c.name, '')                        AS kelas,
			       p.amount                                    AS nominal_diklaim,
			       i.amount                                    AS nominal_tagihan,

			       COALESCE(p.ocr_data ->> 'verification_status', '') AS ocr_status,
			       p.ocr_confidence                            AS ocr_keyakinan,
			       p.ocr_data ->> 'extracted_amount'           AS ocr_nominal,
			       p.ocr_data ->> 'extracted_date'             AS ocr_tanggal,
			       COALESCE(p.ocr_data ->> 'bank_name', '')    AS ocr_bank,
			       length(COALESCE(p.ocr_data ->> 'raw_text', '')) AS ocr_panjang_teks,

			       CASE WHEN jsonb_typeof(p.ocr_data -> 'flags') = 'array'
			            THEN jsonb_array_length(p.ocr_data -> 'flags')
			            ELSE 0 END                             AS ocr_jumlah_flag,
			       CASE WHEN jsonb_typeof(p.ocr_data -> 'flags') = 'array'
			            THEN EXISTS (SELECT 1
			                         FROM jsonb_array_elements_text(p.ocr_data -> 'flags') f
			                         WHERE f LIKE 'Destination account matched%')
			            ELSE FALSE END                         AS ocr_rekening_cocok,
			       CASE WHEN jsonb_typeof(p.ocr_data -> 'flags') = 'array'
			            THEN EXISTS (SELECT 1
			                         FROM jsonb_array_elements_text(p.ocr_data -> 'flags') f
			                         WHERE f LIKE 'Student name found%')
			            ELSE FALSE END                         AS ocr_nama_cocok,
			       COALESCE((SELECT string_agg(f, ' | ')
			                 FROM jsonb_array_elements_text(
			                          CASE WHEN jsonb_typeof(p.ocr_data -> 'flags') = 'array'
			                               THEN p.ocr_data -> 'flags'
			                               ELSE '[]'::jsonb END) f), '') AS ocr_flags,

			       CASE WHEN (p.ocr_data ->> 'extracted_amount') ~ '^[0-9]+(\\.[0-9]+)?$'
			            THEN p.amount - (p.ocr_data ->> 'extracted_amount')::numeric
			            END                                    AS selisih_nominal,

			       COALESCE(mesin.status, '')                  AS keputusan_mesin,
			       CASE WHEN mesin.status IS NULL THEN NULL
			            WHEN mesin.status = 'AUTO_VERIFIED' THEN p.status::text = 'VERIFIED'
			            WHEN mesin.status = 'REJECTED'      THEN p.status::text = 'REJECTED'
			            END                                    AS mesin_sepakat,

			       p.status::text                              AS label,
			       to_char(p.verified_at AT TIME ZONE 'UTC',
			               'YYYY-MM-DD"T"HH24:MI:SS"Z"')       AS diputuskan_pada,
			       COALESCE(p.reject_reason, '')               AS alasan_tolak,
			       COALESCE(p.ocr_data ->> 'raw_text', '')     AS ocr_teks_mentah

			FROM payments p
			JOIN students s            ON s.id = p.student_id
			LEFT JOIN study_classes c  ON c.id = s.study_class_id
			LEFT JOIN payment_plans pl ON pl.id = p.payment_plan_id
			LEFT JOIN installments i   ON i.id = p.installment_id
			LEFT JOIN LATERAL (
			    SELECT vl.to_status::text AS status
			    FROM verification_logs vl
			    WHERE vl.payment_id = p.id
			      AND vl.admin_id IS NULL
			    ORDER BY vl.created_at, vl.id
			    LIMIT 1
			) mesin ON TRUE
			WHERE p.verified_by IS NOT NULL
			  AND p.status IN ('VERIFIED', 'REJECTED')
			ORDER BY p.id
			""";

	/**
	 * Tiap {@code status} di sini WAJIB berawalan {@code p.}: subquery lateral
	 * di bawah juga punya kolom bernama status, dan {@code status} telanjang
	 * membuat PostgreSQL menolak seluruh query sebagai ambigu.
	 */
	private static final String SQL_STATISTIK = """
			SELECT
			    COUNT(*) FILTER (WHERE p.verified_by IS NOT NULL
			                       AND p.status IN ('VERIFIED', 'REJECTED'))   AS berlabel,
			    COUNT(*) FILTER (WHERE p.verified_by IS NOT NULL
			                       AND p.status = 'VERIFIED')                  AS diterima,
			    COUNT(*) FILTER (WHERE p.verified_by IS NOT NULL
			                       AND p.status = 'REJECTED')                  AS ditolak,
			    COUNT(*) FILTER (WHERE p.verified_by IS NOT NULL
			                       AND p.processed_file_path IS NOT NULL)      AS ada_gambar,
			    COUNT(*) FILTER (WHERE p.ocr_data IS NOT NULL)                 AS sudah_dibaca,
			    COUNT(*) FILTER (WHERE p.status IN ('PENDING', 'NEEDS_REVIEW')) AS belum_diputuskan,

			    -- Mesin dianggap sepakat bila usul otomatisnya sama dengan
			    -- keputusan admin. Yang dibandingkan hanya bukti yang memang
			    -- pernah diusulkan mesin; sisanya tidak punya pembanding.
			    COUNT(*) FILTER (
			        WHERE p.verified_by IS NOT NULL AND mesin.status IS NOT NULL
			          AND ((mesin.status = 'AUTO_VERIFIED' AND p.status::text = 'VERIFIED')
			            OR (mesin.status = 'REJECTED'      AND p.status::text = 'REJECTED'))
			    )                                                              AS mesin_sepakat,
			    COUNT(*) FILTER (
			        WHERE p.verified_by IS NOT NULL AND mesin.status IS NOT NULL
			          AND NOT ((mesin.status = 'AUTO_VERIFIED' AND p.status::text = 'VERIFIED')
			                OR (mesin.status = 'REJECTED'      AND p.status::text = 'REJECTED'))
			    )                                                              AS mesin_keliru,

			    ROUND(AVG(p.ocr_confidence) FILTER (WHERE p.ocr_confidence IS NOT NULL), 4)
			                                                                   AS rata_keyakinan
			FROM payments p
			LEFT JOIN LATERAL (
			    SELECT vl.to_status::text AS status
			    FROM verification_logs vl
			    WHERE vl.payment_id = p.id AND vl.admin_id IS NULL
			      AND vl.to_status IN ('AUTO_VERIFIED', 'REJECTED')
			    ORDER BY vl.created_at, vl.id
			    LIMIT 1
			) mesin ON TRUE
			""";

	private static final String SQL_GAMBAR = """
			SELECT p.id                             AS payment_id,
			       p.processed_file_path,
			       p.status::text                   AS label,
			       p.amount                         AS nominal_diklaim,
			       p.ocr_data ->> 'extracted_amount' AS ocr_nominal,
			       p.ocr_confidence                 AS ocr_keyakinan,
			       COALESCE(pl.category::text, '')  AS kategori
			FROM payments p
			LEFT JOIN payment_plans pl ON pl.id = p.payment_plan_id
			WHERE p.verified_by IS NOT NULL
			  AND p.status IN ('VERIFIED', 'REJECTED')
			  AND p.processed_file_path IS NOT NULL
			ORDER BY p.id
			LIMIT CAST(? AS INTEGER)
			""";
}
