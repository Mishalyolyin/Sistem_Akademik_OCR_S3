package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.settings.SystemSettingService;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Putaran pembuatan tagihan UKT otomatis.
 *
 * <p>Ini menggantikan pengetikan tahun akademik oleh admin, jadi yang paling
 * dijaga di sini adalah tahun yang dihasilkannya: satu kekeliruan tidak lagi
 * mengenai satu tagihan melainkan seluruh angkatan sekaligus, dan tahun
 * akademik apa pun tetap tersimpan dengan sah tanpa ada galat yang menegur.
 */
class UktAutoServiceTest {

	private StudentRepository studentRepository;
	private PaymentPlanRepository planRepository;
	private PaymentGenerationService generationService;
	private SystemSettingService settings;
	private UktAutoService service;

	@BeforeEach
	void setUp() {
		studentRepository = mock(StudentRepository.class);
		planRepository = mock(PaymentPlanRepository.class);
		generationService = mock(PaymentGenerationService.class);
		settings = mock(SystemSettingService.class);

		service = new UktAutoService(
				studentRepository,
				new UktAutoPerMahasiswa(planRepository, generationService),
				settings);

		when(settings.getOrDefault(eq(UktAutoService.ENABLED), anyString())).thenReturn("true");
		when(planRepository.countByStudentIdAndCategoryAndStatusNot(anyLong(), any(), any()))
				.thenReturn(0L);
	}

	private Student mahasiswa(long id, String tahun, AcademicTerm term) {
		Student student = Student.builder()
				.id(id)
				.nim("D96000" + id)
				.name("Mahasiswa " + id)
				.startAcademicYear(tahun)
				.startTerm(term)
				.walletBalance(BigDecimal.ZERO)
				.active(true)
				.build();
		when(studentRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(student));
		return student;
	}

	@Test
	@DisplayName("semester pertama dibuat di tahun akademik mahasiswanya sendiri")
	void semesterPertama() {
		Student student = mahasiswa(1L, "2026/2027", AcademicTerm.GASAL);

		var hasil = service.jalankan(LocalDate.of(2026, 9, 1));

		assertThat(hasil.dibuat()).isEqualTo(1);
		verify(generationService).generate(
				student, PaymentCategory.UKT, "2026/2027", AcademicTerm.GASAL);
	}

	@Test
	@DisplayName("angkatan Genap tidak ikut jadwal angkatan Gasal")
	void angkatanGenap() {
		// Dua angkatan berada di bulan kalender yang sama tapi semester yang
		// berbeda. Menurunkan tahun akademik dari tanggal akan salah untuk
		// salah satunya, apa pun pilihannya.
		Student student = mahasiswa(2L, "2026/2027", AcademicTerm.GENAP);

		service.jalankan(LocalDate.of(2027, 3, 1));

		verify(generationService).generate(
				student, PaymentCategory.UKT, "2026/2027", AcademicTerm.GENAP);
	}

	@Test
	@DisplayName("semester yang belum tiba tidak dibuat lebih dulu")
	void belumWaktunya() {
		// Semester kedua baru boleh terbit Februari. Membuatnya September
		// berarti tarif September ikut terkunci untuk yang dibayar lima bulan
		// lagi — aturan snapshot tarif akan dilanggar diam-diam.
		Student student = mahasiswa(3L, "2026/2027", AcademicTerm.GASAL);

		var hasil = service.jalankan(LocalDate.of(2026, 9, 1));

		assertThat(hasil.dibuat()).isEqualTo(1);
		verify(generationService, never()).generate(
				student, PaymentCategory.UKT, "2026/2027", AcademicTerm.GENAP);
	}

	@Test
	@DisplayName("mahasiswa yang tertinggal dikejar sampai semester berjalan")
	void mengejarYangTertinggal() {
		// Diimpor Maret, jadi melewatkan putaran September. Tanpa pengejaran,
		// semester pertamanya tidak akan pernah terbentuk — jalur manual sudah
		// tidak ada lagi.
		Student student = mahasiswa(4L, "2026/2027", AcademicTerm.GASAL);

		var hasil = service.jalankan(LocalDate.of(2027, 3, 1));

		assertThat(hasil.dibuat()).isEqualTo(2);
		verify(generationService).generate(
				student, PaymentCategory.UKT, "2026/2027", AcademicTerm.GASAL);
		verify(generationService).generate(
				student, PaymentCategory.UKT, "2026/2027", AcademicTerm.GENAP);
	}

	@Test
	@DisplayName("yang tagihannya sudah lengkap dilewati, bukan dibuat ulang")
	void sudahLengkap() {
		mahasiswa(5L, "2026/2027", AcademicTerm.GASAL);
		when(planRepository.countByStudentIdAndCategoryAndStatusNot(anyLong(), any(), any()))
				.thenReturn(6L);

		var hasil = service.jalankan(LocalDate.of(2029, 3, 1));

		assertThat(hasil.dibuat()).isZero();
		assertThat(hasil.dilewati()).isEqualTo(1);
		verify(generationService, never()).generate(any(), any(), anyString(), any());
	}

	@Test
	@DisplayName("tidak pernah melewati enam semester")
	void batasEnamSemester() {
		mahasiswa(6L, "2026/2027", AcademicTerm.GASAL);
		when(planRepository.countByStudentIdAndCategoryAndStatusNot(anyLong(), any(), any()))
				.thenReturn(0L);

		// Tanggal jauh di depan: kalau batasnya lupa dipasang, seluruh semester
		// akan terbit sekaligus.
		var hasil = service.jalankan(LocalDate.of(2040, 3, 1));

		assertThat(hasil.dibuat()).isEqualTo(6);
		verify(generationService, times(6)).generate(any(), any(), anyString(), any());
	}

	@Test
	@DisplayName("kegagalan satu mahasiswa dicatat beserta sebabnya, putaran jalan terus")
	void satuGagal() {
		Student gagal = Student.builder().id(7L).nim("D9600007").name("Gagal")
				.startAcademicYear("2026/2027").startTerm(AcademicTerm.GASAL)
				.walletBalance(BigDecimal.ZERO).active(true).build();
		Student lancar = Student.builder().id(8L).nim("D9600008").name("Lancar")
				.startAcademicYear("2026/2027").startTerm(AcademicTerm.GASAL)
				.walletBalance(BigDecimal.ZERO).active(true).build();
		when(studentRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(gagal, lancar));

		when(generationService.generate(eq(gagal), any(), anyString(), any()))
				.thenThrow(new BusinessRuleException("Tarif UKT untuk tahun 2026/2027 belum diatur."));

		var hasil = service.jalankan(LocalDate.of(2026, 9, 1));

		assertThat(hasil.gagal()).isEqualTo(1);
		assertThat(hasil.dibuat()).isEqualTo(1);
		// Sebabnya ikut, bukan sekadar "gagal": penyebab tersering justru yang
		// paling mudah dibetulkan kalau disebutkan.
		assertThat(hasil.catatan()).singleElement().asString()
				.contains("Gagal").contains("belum diatur");
	}

	@Test
	@DisplayName("tahun akademik mahasiswa yang salah format dicatat, bukan menghentikan putaran")
	void tahunSalahFormat() {
		mahasiswa(9L, "2026", AcademicTerm.GASAL);

		var hasil = service.jalankan(LocalDate.of(2026, 9, 1));

		assertThat(hasil.gagal()).isEqualTo(1);
		assertThat(hasil.catatan()).singleElement().asString().contains("2026/2027");
		verify(generationService, never()).generate(any(), any(), anyString(), any());
	}

	@Test
	@DisplayName("putaran tidak berjalan bila dimatikan admin")
	void dimatikan() {
		mahasiswa(10L, "2026/2027", AcademicTerm.GASAL);
		when(settings.getOrDefault(eq(UktAutoService.ENABLED), anyString())).thenReturn("false");

		var hasil = service.jalankan(LocalDate.of(2026, 9, 1));

		assertThat(hasil.diperiksa()).isZero();
		verify(generationService, never()).generate(any(), any(), anyString(), any());
		// Waktu jalan terakhir tidak dicatat: putarannya memang tidak berjalan.
		verify(settings, never()).set(eq(UktAutoService.LAST_RUN), anyString());
	}

	@Test
	@DisplayName("Juli dan Agustus memakai Genap yang baru lewat, bukan Gasal yang belum mulai")
	void jedaAntarSemester() {
		// Kalau jeda ini dianggap Gasal, tagihan semester berikutnya terbit
		// sebulan lebih awal beserta tarif yang ikut terkunci lebih awal.
		assertThat(UktAutoService.semesterKalender(LocalDate.of(2027, 7, 15)))
				.isEqualTo(new SemesterUkt("2026/2027", AcademicTerm.GENAP));
		assertThat(UktAutoService.semesterKalender(LocalDate.of(2027, 8, 31)))
				.isEqualTo(new SemesterUkt("2026/2027", AcademicTerm.GENAP));
		assertThat(UktAutoService.semesterKalender(LocalDate.of(2027, 9, 1)))
				.isEqualTo(new SemesterUkt("2027/2028", AcademicTerm.GASAL));
	}

	@Test
	@DisplayName("hasil putaran dicatat supaya admin tahu kapan terakhir jalan")
	void catatanTerakhir() {
		mahasiswa(11L, "2026/2027", AcademicTerm.GASAL);

		service.jalankan(LocalDate.of(2026, 9, 1));

		verify(settings).set(eq(UktAutoService.LAST_RUN),
				org.mockito.ArgumentMatchers.contains("2026-09-01"));
	}
}
