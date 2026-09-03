package ac.kampus.pembayaran.student.importer;

import ac.kampus.pembayaran.auth.AuthService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/students/import")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Import Mahasiswa")
public class StudentImportController {

	private static final String XLSX =
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final StudentImportService importService;
	private final StudentExcelTemplate template;
	private final ImportBatchRepository batchRepository;

	public record ImportResult(
			Long batchId,
			String filename,
			int totalRows,
			int successRows,
			int failedRows,
			List<ImportBatch.RowError> errors
	) {
		static ImportResult from(ImportBatch batch) {
			return new ImportResult(
					batch.getId(),
					batch.getFilename(),
					batch.getTotalRows(),
					batch.getSuccessRows(),
					batch.getFailedRows(),
					batch.getErrors());
		}
	}

	@GetMapping("/template")
	@Operation(summary = "Unduh template Excel import mahasiswa")
	public ResponseEntity<Resource> downloadTemplate() {
		byte[] bytes = template.build();

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(XLSX))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename("template-import-mahasiswa.xlsx")
						.build()
						.toString())
				.contentLength(bytes.length)
				.body(new ByteArrayResource(bytes));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Unggah Excel mahasiswa; baris gagal dilaporkan tanpa membatalkan yang lain")
	public ImportResult upload(@RequestParam("file") MultipartFile file) {
		return ImportResult.from(importService.importFile(file, AuthService.currentUserId()));
	}

	@GetMapping("/history")
	@Operation(summary = "Riwayat 20 import terakhir")
	public List<ImportResult> history() {
		return batchRepository.findTop20ByOrderByCreatedAtDesc().stream()
				.map(ImportResult::from)
				.toList();
	}
}
