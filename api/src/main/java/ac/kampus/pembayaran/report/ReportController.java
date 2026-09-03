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
import org.springframework.web.bind.annotation.RestController;

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

	@GetMapping("/pembayaran.xlsx")
	@Operation(summary = "Laporan Excel: ringkasan kelas, tagihan, dan riwayat pembayaran")
	public ResponseEntity<Resource> laporanPembayaran() {
		byte[] bytes = exporter.ledgerMahasiswa();
		String nama = "laporan-pembayaran-%s.xlsx".formatted(LocalDate.now());

		return unduh(bytes, nama, MediaType.parseMediaType(XLSX));
	}

	@GetMapping("/kuitansi/{paymentId}.pdf")
	@Operation(summary = "Kuitansi PDF satu pembayaran yang sudah diverifikasi")
	public ResponseEntity<Resource> kuitansi(@PathVariable Long paymentId) {
		byte[] bytes = receiptGenerator.generate(paymentId);
		String nama = "kuitansi-%d.pdf".formatted(paymentId);

		return unduh(bytes, nama, MediaType.APPLICATION_PDF);
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
