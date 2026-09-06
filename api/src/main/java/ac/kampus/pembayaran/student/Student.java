package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.studyclass.StudyClass;
import ac.kampus.pembayaran.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 30)
	private String nim;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 25)
	private String phone;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "study_class_id")
	private StudyClass studyClass;

	/**
	 * Kode golongan potongan, menunjuk ke {@code discount_tier_rates.tier}.
	 *
	 * <p>Bukan enum Java: golongan bisa ditambah admin tanpa deploy ulang, jadi
	 * daftarnya tidak bisa dikunci saat kompilasi. Kunci asing di database yang
	 * menjaga nilainya tetap sah.
	 */
	@Column(name = "discount_tier", nullable = false, length = 40)
	private String discountTier;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "start_term", nullable = false, columnDefinition = "academic_term")
	private AcademicTerm startTerm;

	@Column(name = "start_academic_year", nullable = false, length = 9)
	private String startAcademicYear;

	// --- Dokumen wajib, diisi berurutan oleh mahasiswa sendiri ---

	@Column(name = "profile_picture", length = 255)
	private String profilePicture;

	@Column(length = 20)
	private String nik;

	@Column(name = "ktp_file_path", length = 255)
	private String ktpFilePath;

	@Column(name = "kk_number", length = 20)
	private String kkNumber;

	@Column(name = "kk_file_path", length = 255)
	private String kkFilePath;

	@Column(name = "ijazah_file_path", length = 255)
	private String ijazahFilePath;

	@Column(columnDefinition = "text")
	private String address;

	// --- Hasil pembacaan OCR atas dokumen di atas ---
	//
	// Disimpan apa adanya, sama seperti payments.ocr_data: bentuk jawaban
	// service OCR bisa berubah, dan yang berguna saat menelusuri justru field
	// yang tidak diduga ada.

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ktp_ocr_data", columnDefinition = "jsonb")
	private Map<String, Object> ktpOcrData;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "kk_ocr_data", columnDefinition = "jsonb")
	private Map<String, Object> kkOcrData;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ijazah_ocr_data", columnDefinition = "jsonb")
	private Map<String, Object> ijazahOcrData;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "profile_picture_analysis", columnDefinition = "jsonb")
	private Map<String, Object> profilePictureAnalysis;

	/** Keduanya datang dari pembacaan ijazah; tidak ada isian manualnya. */
	@Column(name = "birth_place", length = 100)
	private String birthPlace;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	// --- Keuangan ---

	@Column(name = "wallet_balance", nullable = false, precision = 15, scale = 2)
	private BigDecimal walletBalance;

	@Column(name = "pendaftaran_exempt", nullable = false)
	private boolean pendaftaranExempt;

	/** Membebaskan dari syarat lunas seluruh UKT sebelum mendaftar tahap ujian. */
	@Column(name = "ujian_exempt", nullable = false)
	private boolean ujianExempt;

	@Column(name = "import_batch_id")
	private Long importBatchId;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	/**
	 * Dokumen wajib diisi berurutan: Foto, No.KTP + KTP, No.KK + KK, Ijazah, Alamat.
	 * Mengembalikan langkah pertama yang belum lengkap, atau kosong bila sudah semua.
	 */
	public DocumentStep nextIncompleteDocumentStep() {
		if (isBlank(profilePicture)) return DocumentStep.PHOTO;
		if (isBlank(nik) || isBlank(ktpFilePath)) return DocumentStep.KTP;
		if (isBlank(kkNumber) || isBlank(kkFilePath)) return DocumentStep.KK;
		if (isBlank(ijazahFilePath)) return DocumentStep.IJAZAH;
		if (isBlank(address)) return DocumentStep.ADDRESS;
		return null;
	}

	public boolean hasCompletedDocuments() {
		return nextIncompleteDocumentStep() == null;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	public enum DocumentStep {
		PHOTO, KTP, KK, IJAZAH, ADDRESS
	}
}
