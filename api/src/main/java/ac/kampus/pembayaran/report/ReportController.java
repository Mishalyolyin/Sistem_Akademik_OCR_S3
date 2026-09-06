package ac.kampus.pembayaran.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Laporan")
public class ReportController {

	private static final String XLSX =
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final PaymentReportExporter exporter;
	private final ReceiptGenerator receiptGenerator;
	private final OcrDatasetExporter datasetExporter;
	private final StudentDataExporter studentExporter;
	private final StudentPhotoExporter photoExporter;

	@GetMapping("/pembayaran.xlsx")
	@Operation(summary = "Laporan Excel pembayaran, bisa disaring per kelas dan status. "
			+ "Format TRANSAKSI memberi tiga lembar rekap; TERMIN memberi ledger "
			+ "per semester dengan kolom tiap angsuran.")
	public ResponseEntity<Resource> laporanPembayaran(
			@RequestParam(required = false) Long classId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) PaymentReportExporter.Format format) {

		var filter = new PaymentReportExporter.Filter(classId, status, format);
		byte[] bytes = exporter.ledgerMahasiswa(filter);
		String nama = "laporan-pembayaran-%s-%s.xlsx"
				.formatted(filter.keterangan(), LocalDate.now());

		return unduh(bytes, nama, MediaType.parseMediaType(XLSX));
	}

	@GetMapping("/mahasiswa.xlsx")
	@Operation(summary = "Ekspor data mahasiswa beserta hasil pemeriksaan dokumennya")
	public ResponseEntity<Resource> dataMahasiswa(
			@RequestParam(required = false) Long classId,
			@RequestParam(required = false) String tier) {

		byte[] bytes = studentExporter.dataMahasiswa(classId, tier);
		String nama = "data-mahasiswa-%s.xlsx".formatted(LocalDate.now());

		return unduh(bytes, nama, MediaType.parseMediaType(XLSX));
	}

	@GetMapping("/foto-mahasiswa.zip")
	@Operation(summary = "Foto profil mahasiswa satu kelas dalam satu ZIP, "
			+ "dinamai NIM_Nama dan dikelompokkan per folder kelas")
	public ResponseEntity<Resource> fotoMahasiswa(
			@RequestParam(required = false) Long classId) {

		byte[] bytes = photoExporter.fotoKelas(classId);
		String nama = "foto-mahasiswa-%s.zip".formatted(LocalDate.now());

		return unduh(bytes, nama, MediaType.parseMediaType("application/zip"));
	}

	@GetMapping("/dataset-ocr.zip")
	@Operation(summary = "Dataset gambar bukti bayar: gambar hasil praproses + label.csv. "
			+ "Hanya bukti berlabel admin yang gambarnya sudah tersimpan.")
	public ResponseEntity<Resource> datasetGambar(
			@RequestParam(defaultValue = "1000") int batas) {

		byte[] bytes = datasetExporter.datasetGambar(Math.clamp(batas, 1, 5000));
		String nama = "dataset-ocr-gambar-%s.zip".formatted(LocalDate.now());

		return unduh(bytes, nama, MediaType.parseMediaType("application/zip"));
	}

	@GetMapping("/statistik-ocr")
	@Operation(summary = "Berapa banyak bahan belajar yang sudah terkumpul, dan "
			+ "seberapa sering keputusan mesin sepakat dengan admin")
	public OcrDatasetExporter.Statistik statistikOcr() {
		return datasetExporter.statistik();
	}

	@GetMapping("/kuitansi/{paymentId}.pdf")
	@Operation(summary = "Kuitansi PDF satu pembayaran yang sudah diverifikasi")
	public ResponseEntity<Resource> kuitansi(@PathVariable Long paymentId) {
		byte[] bytes = receiptGenerator.generate(paymentId);
		String nama = "kuitansi-%d.pdf".formatted(paymentId);

		return unduh(bytes, nama, MediaType.APPLICATION_PDF);
	}

	@GetMapping("/dataset-ocr.csv")
	@Operation(summary = "Dataset CSV pembacaan bukti bayar yang sudah diputuskan admin, "
			+ "untuk melatih model. Hanya baris berlabel manusia yang ikut.")
	public ResponseEntity<Resource> datasetOcr(
			// Teks mentah memuat nama, nomor rekening, dan saldo, jadi ia harus
			// diminta secara sadar — bukan ikut terbawa oleh unduhan biasa.
			@RequestParam(defaultValue = "false") boolean sertakanTeks) {
		byte[] bytes = datasetExporter.datasetOcr(sertakanTeks);
		String nama = "dataset-ocr-%s.csv".formatted(LocalDate.now());

		return unduh(bytes, nama, new MediaType("text", "csv", StandardCharsets.UTF_8));
	}

	private ResponseEntity<Resource> unduh(byte[] bytes, String namaBerkas, MediaType tipe) {
		return ResponseEntity.ok()
				.contentType(tipe)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(namaBerkas).build().toString())
				.contentLength(bytes.length)
				.body(new ByteArrayResource(bytes));
	}
}
