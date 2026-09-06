package ac.kampus.pembayaran.student.self;

import ac.kampus.pembayaran.billing.PaymentGenerationService;
import ac.kampus.pembayaran.billing.PaymentPlan;
import ac.kampus.pembayaran.billing.PaymentPlanRepository;
import ac.kampus.pembayaran.billing.PlanStatus;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.payment.Payment;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.payment.PaymentService;
import ac.kampus.pembayaran.tuition.DiscountTierRate;
import ac.kampus.pembayaran.tuition.DiscountTierRateRepository;
import ac.kampus.pembayaran.payment.PaymentSpecifications;
import ac.kampus.pembayaran.payment.PaymentStatus;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Endpoint untuk mahasiswa mengurus akunnya sendiri.
 *
 * <p>Tidak ada satu pun endpoint di sini yang menerima id mahasiswa dari klien;
 * semuanya bekerja pada pemilik token yang sedang dipakai.
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAHASISWA')")
@Tag(name = "Mahasiswa (akun sendiri)")
public class StudentSelfController {

	private final StudentSelfService selfService;
	private final PaymentGenerationService generationService;
	private final PaymentPlanRepository planRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentService paymentService;
	private final DiscountTierRateRepository tierRepository;
	private final ac.kampus.pembayaran.dissertation.DissertationService dissertationService;

	// --- DTO ---

	public record ProfilResponse(
			Long id,
			String nim,
			String nama,
			String email,
			String kelas,
			String golongan,
			/** Nama golongan yang bisa dibaca; portal tidak boleh memanggil endpoint admin. */
			String golonganLabel,
			String telepon,
			String alamat,
			boolean dokumenLengkap,
			String langkahDokumenBerikutnya,
			String namaLangkahBerikutnya,
			boolean pendaftaranLunas,
			BigDecimal saldo,
			String nik,
			String nomorKk,
			/** Kode dokumen yang sudah diunggah, supaya portal bisa menampilkannya kembali. */
			List<String> dokumenTersedia
	) {
	}

	public record KtpRequest(@NotBlank(message = "Nomor KTP wajib diisi.") String nik) {
	}

	public record AlamatRequest(
			@NotBlank(message = "Alamat wajib diisi.")
			@Size(min = 10, max = 500, message = "Alamat minimal 10 karakter.")
			String alamat,

			String telepon
	) {
	}

	public record DaftarTagihanRequest(
			@NotNull(message = "Kategori wajib dipilih.")
			PaymentCategory category
	) {
	}

	public record CicilanRingkas(
			Long id,
			int nomor,
			LocalDate jatuhTempo,
			BigDecimal nominal,
			BigDecimal dibayar,
			BigDecimal sisa,
			String status
	) {
	}

	public record TagihanResponse(
			Long id,
			String kategori,
			Integer semester,
			String tahunAkademik,
			BigDecimal total,
			BigDecimal dibayar,
			BigDecimal sisa,
			String status,
			List<CicilanRingkas> cicilan
	) {
	}

	public record RiwayatItem(
			Long id,
			BigDecimal nominal,
			String kategori,
			Integer cicilanKe,
			String status,
			String bank,
			LocalDate tanggalBukti,
			String alasanDitolak,
			Instant diunggah
	) {
	}

	// --- Profil dan dokumen ---

	@GetMapping("/profil")
	@Operation(summary = "Profil dan status kelengkapan dokumen")
	@Transactional(readOnly = true)
	public ProfilResponse profil() {
		return toProfil(selfService.current());
	}

	@PostMapping(value = "/dokumen/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Unggah foto profil")
	public ProfilResponse unggahFoto(@RequestParam("file") MultipartFile file) {
		return toProfil(selfService.simpanFoto(file));
	}

	@PostMapping(value = "/dokumen/ktp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Unggah KTP beserta nomornya")
	public ProfilResponse unggahKtp(
			@RequestParam String nik, @RequestParam("file") MultipartFile file) {
		return toProfil(selfService.simpanKtp(nik, file));
	}

	@PostMapping(value = "/dokumen/kk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Unggah Kartu Keluarga beserta nomornya")
	public ProfilResponse unggahKk(
			@RequestParam String nomorKk, @RequestParam("file") MultipartFile file) {
		return toProfil(selfService.simpanKk(nomorKk, file));
	}

	@PostMapping(value = "/dokumen/ijazah", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Unggah ijazah terakhir")
	public ProfilResponse unggahIjazah(@RequestParam("file") MultipartFile file) {
		return toProfil(selfService.simpanIjazah(file));
	}

	@PostMapping("/dokumen/alamat")
	@Operation(summary = "Simpan alamat, langkah terakhir dokumen wajib")
	public ProfilResponse simpanAlamat(@Valid @RequestBody AlamatRequest request) {
		return toProfil(selfService.simpanAlamat(request.alamat(), request.telepon()));
	}

	// --- Tagihan ---

	@GetMapping("/tagihan")
	@Operation(summary = "Semua tagihan milik saya")
	@Transactional(readOnly = true)
	public List<TagihanResponse> tagihan() {
		Student student = selfService.current();
		wajibDokumenLengkap(student);

		return planRepository
				.findByStudentIdOrderByCategoryAscSemesterNumberAsc(student.getId())
				.stream()
				.map(StudentSelfController::toTagihan)
				.toList();
	}

	@PostMapping("/tagihan")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Daftarkan tagihan Pendaftaran atau tahap ujian")
	public TagihanResponse daftarTagihan(@Valid @RequestBody DaftarTagihanRequest request) {
		Student student = selfService.current();
		wajibDokumenLengkap(student);

		if (!StudentSelfService.bolehDaftarSendiri(request.category())) {
			throw new BusinessRuleException(
					"Tagihan %s dibuat oleh admin, tidak bisa didaftarkan sendiri."
							.formatted(request.category().label()));
		}

		// Selain Pendaftaran, semua tagihan menunggu Pendaftaran lunas dulu.
		if (request.category() != PaymentCategory.PENDAFTARAN) {
			wajibPendaftaranLunas(student);
		}

		// Keempat tahap ujian menguji satu disertasi yang sama, jadi identitasnya
		// harus sudah ada sebelum tahap pertamanya didaftarkan.
		if (PaymentCategory.examSequence().contains(request.category())) {
			wajibDisertasiTerisi(student);
		}

		return toTagihan(generationService.generate(
				student,
				request.category(),
				student.getStartAcademicYear(),
				student.getStartTerm()));
	}

	// --- Pembayaran ---

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/pembayaran")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Unggah bukti bayar untuk salah satu cicilan saya")
	public RiwayatItem unggahBukti(
			@RequestParam Long installmentId,
			@RequestParam BigDecimal amount,
			@RequestParam("file") MultipartFile file) {

		Student student = selfService.current();
		wajibDokumenLengkap(student);

		// PaymentService sudah memeriksa bahwa cicilannya benar milik mahasiswa ini.
		return toRiwayat(paymentService.upload(student.getId(), installmentId, amount, file));
	}

	@GetMapping("/pembayaran")
	@Operation(summary = "Riwayat pembayaran saya")
	@Transactional(readOnly = true)
	public List<RiwayatItem> riwayat() {
		Student student = selfService.current();

		Specification<Payment> spec = Specification.allOf(
				PaymentSpecifications.forStudent(student.getId()));

		return paymentRepository
				.findAll(spec, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")))
				.map(StudentSelfController::toRiwayat)
				.getContent();
	}

	// --- Gate ---

	private void wajibDokumenLengkap(Student student) {
		var langkah = student.nextIncompleteDocumentStep();
		if (langkah != null) {
			throw new BusinessRuleException(
					"Lengkapi dulu dokumen wajib. Langkah berikutnya: %s."
							.formatted(StudentSelfService.namaLangkah(langkah)));
		}
	}

	/**
	 * Tahap ujian butuh disertasi yang sudah punya identitas.
	 *
	 * <p>Gerbang ini sengaja di portal, bukan di
	 * {@code PaymentGenerationService} bersama urutan tahap dan syarat lunas
	 * UKT. Alasannya: admin kadang membuatkan tagihan dari formulir kertas yang
	 * sudah ditandatangani promotor, dan menahannya di sana berarti pekerjaan
	 * admin ikut terhenti karena mahasiswa belum sempat mengetik judulnya.
	 * Yang dijaga di sini adalah pendaftaran mandiri — satu-satunya jalur di
	 * mana tidak ada manusia lain yang memeriksa.
	 */
	private void wajibDisertasiTerisi(Student student) {
		if (!dissertationService.sudahDiisi(student.getId())) {
			throw new BusinessRuleException(
					"Isi dulu judul disertasi dan nama kedua promotor di halaman Disertasi "
							+ "sebelum mendaftar tahap ujian.");
		}
	}

	private void wajibPendaftaranLunas(Student student) {
		if (student.isPendaftaranExempt()) {
			return;
		}

		boolean lunas = planRepository
				.findByStudentIdAndCategoryAndStatusNot(
						student.getId(), PaymentCategory.PENDAFTARAN, PlanStatus.CANCELLED)
				.map(PaymentPlan::isFullyPaid)
				.orElse(false);

		if (!lunas) {
			throw new BusinessRuleException(
					"Lunasi biaya Pendaftaran dulu sebelum mengakses tagihan lain.");
		}
	}

	// --- Pemetaan ---

	private ProfilResponse toProfil(Student student) {
		var langkah = student.nextIncompleteDocumentStep();
		boolean pendaftaranLunas = student.isPendaftaranExempt() || planRepository
				.findByStudentIdAndCategoryAndStatusNot(
						student.getId(), PaymentCategory.PENDAFTARAN, PlanStatus.CANCELLED)
				.map(PaymentPlan::isFullyPaid)
				.orElse(false);

		return new ProfilResponse(
				student.getId(),
				student.getNim(),
				student.getName(),
				student.getUser() == null ? null : student.getUser().getEmail(),
				student.getStudyClass() == null ? null : student.getStudyClass().displayName(),
				student.getDiscountTier(),
				tierRepository.findById(student.getDiscountTier())
						.map(DiscountTierRate::getLabel)
						.orElse(student.getDiscountTier()),
				student.getPhone(),
				student.getAddress(),
				langkah == null,
				langkah == null ? null : langkah.name(),
				langkah == null ? null : StudentSelfService.namaLangkah(langkah),
				pendaftaranLunas,
				student.getWalletBalance(),
				student.getNik(),
				student.getKkNumber(),
				StudentDocument.tersediaUntuk(student));
	}

	private static TagihanResponse toTagihan(PaymentPlan plan) {
		return new TagihanResponse(
				plan.getId(),
				plan.getCategory().label(),
				plan.getSemesterNumber(),
				plan.getAcademicYear() + " " + plan.getTerm(),
				plan.getTotalAmount(),
				plan.amountPaid(),
				plan.remaining(),
				plan.getStatus().name(),
				plan.getInstallments().stream()
						.map(i -> new CicilanRingkas(
								i.getId(), i.getInstallmentNo(), i.getDueDate(),
								i.getAmount(), i.getAmountPaid(), i.outstanding(),
								i.getStatus().name()))
						.toList());
	}

	private static RiwayatItem toRiwayat(Payment payment) {
		var plan = payment.getPaymentPlan();
		var installment = payment.getInstallment();

		return new RiwayatItem(
				payment.getId(),
				payment.getAmount(),
				plan == null ? null : plan.getCategory().label(),
				installment == null ? null : installment.getInstallmentNo(),
				payment.getStatus().name(),
				payment.getBankName(),
				payment.getPaymentProofDate(),
				// Alasan penolakan sengaja ditampilkan agar mahasiswa tahu
				// apa yang harus diperbaiki sebelum mengunggah ulang.
				payment.getStatus() == PaymentStatus.REJECTED ? payment.getRejectReason() : null,
				payment.getCreatedAt());
	}
}
