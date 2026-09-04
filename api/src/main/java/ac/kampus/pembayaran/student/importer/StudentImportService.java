package ac.kampus.pembayaran.student.importer;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.tuition.DiscountTierRate;
import ac.kampus.pembayaran.tuition.DiscountTierRateRepository;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import ac.kampus.pembayaran.studyclass.StudyClass;
import ac.kampus.pembayaran.studyclass.StudyClassRepository;
import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import ac.kampus.pembayaran.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Import mahasiswa dari Excel.
 *
 * <p>Setiap baris diproses dalam transaksi sendiri, sehingga satu baris yang
 * gagal tidak menggagalkan seluruh berkas — persis seperti perilaku
 * StudentsImport di sistem Laravel lama.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentImportService {

	private final ImportBatchRepository importBatchRepository;
	private final StudentRowWriter rowWriter;
	private final DiscountTierRateRepository tierRepository;

	private static final DataFormatter FORMATTER = new DataFormatter();

	// Sengaja TIDAK @Transactional: tiap baris punya transaksinya sendiri,
	// jadi baris yang berhasil tetap tersimpan walau baris lain gagal.
	public ImportBatch importFile(MultipartFile file, Long adminId) {
		String filename = file.getOriginalFilename() == null ? "import.xlsx" : file.getOriginalFilename();
		if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
			throw new BusinessRuleException("Berkas harus berformat .xlsx.");
		}

		List<ImportBatch.RowError> errors = new ArrayList<>();
		int total = 0;
		int success = 0;

		try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
			Sheet sheet = workbook.getSheetAt(0);
			Row headerRow = sheet.getRow(sheet.getFirstRowNum());
			if (headerRow == null) {
				throw new BusinessRuleException("Berkas kosong, baris judul kolom tidak ditemukan.");
			}

			Map<String, Integer> columns = readHeader(headerRow);
			for (String required : StudentExcelTemplate.HEADERS) {
				// phone boleh tidak ada; sisanya wajib.
				if (!"phone".equals(required) && !columns.containsKey(required)) {
					throw new BusinessRuleException(
							"Kolom \"%s\" tidak ada di berkas. Unduh template terbaru.".formatted(required));
				}
			}

			// Dibaca sekali untuk seluruh berkas, bukan sekali per baris.
			List<String> golongan = tierRepository
					.findByActiveTrueOrderBySortOrderAscTierAsc()
					.stream()
					.map(DiscountTierRate::getTier)
					.toList();

			for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if (isEmpty(row, columns)) continue;

				total++;
				int excelRowNumber = r + 1;
				String nim = value(row, columns, "nim");

				try {
					rowWriter.write(parse(row, columns, excelRowNumber, golongan));
					success++;
				} catch (Exception e) {
					errors.add(new ImportBatch.RowError(excelRowNumber, nim, pesan(e)));
				}
			}
		} catch (IOException e) {
			throw new BusinessRuleException("Berkas tidak bisa dibaca: " + e.getMessage());
		}

		ImportBatch batch = ImportBatch.builder()
				.filename(filename)
				.totalRows(total)
				.successRows(success)
				.failedRows(errors.size())
				.errors(errors)
				.adminId(adminId)
				.build();

		log.info("Import {}: {} baris, {} berhasil, {} gagal", filename, total, success, errors.size());
		return importBatchRepository.save(batch);
	}

	private ParsedRow parse(Row row, Map<String, Integer> columns, int excelRowNumber,
			List<String> golonganAktif) {
		String nim = require(row, columns, "nim");
		String name = require(row, columns, "name");
		String className = require(row, columns, "class");
		String tierRaw = require(row, columns, "discount_tier");
		String termRaw = require(row, columns, "start_term");
		String academicYear = require(row, columns, "academic_year");
		String phone = value(row, columns, "phone");

		// Golongan diperiksa terhadap daftar yang benar-benar ada di database,
		// bukan daftar tetap di kode: admin bisa menambah golongan baru, dan
		// berkas import harus langsung menerimanya tanpa deploy ulang.
		String tier = tierRaw.trim().toUpperCase(Locale.ROOT);
		if (!golonganAktif.contains(tier)) {
			throw new IllegalArgumentException(
					"Golongan \"%s\" tidak dikenal. Pilihan: %s."
							.formatted(tierRaw, String.join(", ", golonganAktif)));
		}

		AcademicTerm term;
		try {
			term = AcademicTerm.valueOf(termRaw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"Term \"%s\" tidak dikenal. Isi GASAL atau GENAP.".formatted(termRaw));
		}

		if (!academicYear.matches("\\d{4}/\\d{4}")) {
			throw new IllegalArgumentException(
					"Tahun akademik \"%s\" salah format. Contoh: 2026/2027.".formatted(academicYear));
		}

		return new ParsedRow(excelRowNumber, nim.trim(), name.trim(), className.trim(),
				tier, term, academicYear.trim(), phone == null || phone.isBlank() ? null : phone.trim());
	}

	private Map<String, Integer> readHeader(Row headerRow) {
		Map<String, Integer> columns = new HashMap<>();
		for (Cell cell : headerRow) {
			String key = FORMATTER.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
			if (!key.isEmpty()) columns.putIfAbsent(key, cell.getColumnIndex());
		}
		return columns;
	}

	private boolean isEmpty(Row row, Map<String, Integer> columns) {
		if (row == null) return true;
		String nim = value(row, columns, "nim");
		String name = value(row, columns, "name");
		return (nim == null || nim.isBlank()) && (name == null || name.isBlank());
	}

	private String value(Row row, Map<String, Integer> columns, String column) {
		Integer index = columns.get(column);
		if (index == null) return null;
		Cell cell = row.getCell(index);
		return cell == null ? null : FORMATTER.formatCellValue(cell).trim();
	}

	private String require(Row row, Map<String, Integer> columns, String column) {
		String value = value(row, columns, column);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Kolom \"%s\" wajib diisi.".formatted(column));
		}
		return value;
	}

	private String pesan(Exception e) {
		String message = e.getMessage();
		return (message == null || message.isBlank()) ? e.getClass().getSimpleName() : message;
	}

	record ParsedRow(
			int excelRow,
			String nim,
			String name,
			String className,
			String discountTier,
			AcademicTerm startTerm,
			String academicYear,
			String phone
	) {
	}

	/**
	 * Penulis satu baris. Dipisah ke bean sendiri supaya {@code REQUIRES_NEW}
	 * benar-benar membuka transaksi baru — panggilan antar-method di kelas yang
	 * sama tidak melewati proxy Spring, jadi anotasinya akan diabaikan.
	 */
	@Service
	@RequiredArgsConstructor
	static class StudentRowWriter {

		private final StudentRepository studentRepository;
		private final StudyClassRepository studyClassRepository;
		private final UserRepository userRepository;
		private final PasswordEncoder passwordEncoder;

		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void write(ParsedRow row) {
			if (studentRepository.existsByNim(row.nim())) {
				throw new IllegalArgumentException("NIM %s sudah terdaftar.".formatted(row.nim()));
			}

			StudyClass studyClass = studyClassRepository
					.findByNameIgnoreCaseAndAcademicYear(row.className(), row.academicYear())
					.orElseGet(() -> studyClassRepository.save(StudyClass.builder()
							.name(row.className())
							.kerjasama(StudyClass.looksLikeKerjasama(row.className()))
							.academicYear(row.academicYear())
							.active(true)
							.build()));

			// Email dan kata sandi awal diturunkan dari NIM. Mahasiswa memang
			// tidak mengganti kata sandinya sendiri; kalau lupa, admin
			// mengembalikannya ke NIM lewat halaman detail mahasiswa.
			String email = row.nim() + "@student.kampus.ac.id";
			if (userRepository.existsByEmailIgnoreCase(email)) {
				throw new IllegalArgumentException("Akun untuk NIM %s sudah ada.".formatted(row.nim()));
			}

			User user = userRepository.save(User.builder()
					.name(row.name())
					.email(email)
					.passwordHash(passwordEncoder.encode(row.nim()))
					.role(UserRole.MAHASISWA)
					.active(true)
					.build());

			studentRepository.save(Student.builder()
					.user(user)
					.nim(row.nim())
					.name(row.name())
					.phone(row.phone())
					.studyClass(studyClass)
					.discountTier(row.discountTier())
					.startTerm(row.startTerm())
					.startAcademicYear(row.academicYear())
					.walletBalance(BigDecimal.ZERO)
					.pendaftaranExempt(false)
					.active(true)
					.build());
		}
	}
}
