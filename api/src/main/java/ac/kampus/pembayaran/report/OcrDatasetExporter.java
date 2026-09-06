package ac.kampus.pembayaran.report;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
}
