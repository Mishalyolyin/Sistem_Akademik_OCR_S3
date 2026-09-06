package ac.kampus.pembayaran.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pengutipan sel CSV dataset OCR.
 *
 * <p>Yang diuji di sini kelihatan sepele tapi justru bagian yang paling mungkin
 * merusak dataset tanpa bersuara: kolom {@code ocr_flags} dan
 * {@code ocr_teks_mentah} berisi teks bebas hasil pembacaan struk — penuh koma,
 * tanda kutip, dan baris baru. Satu sel yang lupa dikutip tidak melempar galat;
 * ia hanya membuat satu bukti bayar terbaca sebagai belasan baris rusak, dan
 * baru ketahuan jauh kemudian saat model dilatih dari data yang salah.
 */
class OcrDatasetExporterTest {

	@Test
	@DisplayName("teks biasa dibiarkan apa adanya, tanpa kutipan yang tak perlu")
	void teksBiasa() {
		assertThat(OcrDatasetExporter.kutip("VERIFIED")).isEqualTo("VERIFIED");
		assertThat(OcrDatasetExporter.kutip("")).isEmpty();
		assertThat(OcrDatasetExporter.kutip("Kelas Kerjasama A")).isEqualTo("Kelas Kerjasama A");
	}

	@Test
	@DisplayName("koma dikutip, supaya satu sel tidak pecah jadi banyak kolom")
	void koma() {
		assertThat(OcrDatasetExporter.kutip("judi, slot, togel"))
				.isEqualTo("\"judi, slot, togel\"");
	}

	@Test
	@DisplayName("tanda kutip digandakan sesuai aturan CSV")
	void tandaKutip() {
		assertThat(OcrDatasetExporter.kutip("nama \"terbaca\""))
				.isEqualTo("\"nama \"\"terbaca\"\"\"");
	}

	@Test
	@DisplayName("baris baru dikutip, supaya satu struk tidak jadi banyak baris")
	void barisBaru() {
		String struk = "TRANSFER BERHASIL\nRp 1.200.000\r\n7 1234 5678 90";

		String hasil = OcrDatasetExporter.kutip(struk);

		assertThat(hasil).startsWith("\"").endsWith("\"");
		// Isinya tetap utuh — pengutipan tidak boleh ikut membuang barisnya.
		assertThat(hasil).contains("TRANSFER BERHASIL").contains("7 1234 5678 90");
	}

	@Test
	@DisplayName("koma dan kutip sekaligus tetap terbaca sebagai satu sel")
	void gabungan() {
		String flags = "Destination account matched: 7 1234 5678 90 | "
				+ "Selisih nominal: diklaim 1000000.00, terbaca 1200000.00";

		String hasil = OcrDatasetExporter.kutip(flags);

		assertThat(hasil).startsWith("\"").endsWith("\"");
		// Satu-satunya koma yang tersisa adalah koma asli di dalam kutipan.
		assertThat(hasil.substring(1, hasil.length() - 1)).isEqualTo(flags);
	}
}
