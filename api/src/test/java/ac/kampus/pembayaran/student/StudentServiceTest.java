package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.studyclass.StudyClassRepository;
import ac.kampus.pembayaran.tuition.DiscountTierRate;
import ac.kampus.pembayaran.tuition.DiscountTierRateRepository;
import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import ac.kampus.pembayaran.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dua jalur yang hanya dipegang admin: mengembalikan kata sandi mahasiswa, dan
 * memindahkan mahasiswa ke golongan potongan lain.
 *
 * <p>Mahasiswa sengaja tidak mengelola kata sandinya sendiri. Kalau lupa, ia
 * datang ke bagian keuangan dan admin mengembalikannya ke NIM — jadi jalur itu
 * satu-satunya cara masuk lagi, dan harus benar-benar bekerja.
 *
 * <p>Golongan bukan lagi enum, jadi kode yang salah ketik tidak lagi tersaring
 * saat kompilasi maupun oleh pengurai JSON. Kalau service membiarkannya lewat,
 * yang tersisa hanya kunci asing di database — dan mahasiswanya sudah telanjur
 * disimpan dengan golongan yang tarifnya tidak bisa dihitung.
 */
class StudentServiceTest {

	private static final String NIM = "2612600001";

	private StudentRepository studentRepository;
	private UserRepository userRepository;
	private DiscountTierRateRepository tierRepository;
	private PasswordEncoder encoder;
	private StudentService service;

	private Student student;
	private User user;

	@BeforeEach
	void setUp() {
		studentRepository = mock(StudentRepository.class);
		userRepository = mock(UserRepository.class);
		StudyClassRepository studyClassRepository = mock(StudyClassRepository.class);
		StudentTierLockPolicy tierLockPolicy = mock(StudentTierLockPolicy.class);
		tierRepository = mock(DiscountTierRateRepository.class);
		encoder = new BCryptPasswordEncoder(4);

		service = new StudentService(studentRepository, studyClassRepository,
				tierLockPolicy, userRepository, encoder, tierRepository);

		user = User.builder()
				.id(5L).name("Uji Coba").email("uji@kampus.ac.id")
				.passwordHash(encoder.encode("kata-sandi-lama-mahasiswa"))
				.role(UserRole.MAHASISWA).active(true)
				.build();

		student = Student.builder()
				.id(1L).nim(NIM).name("Uji Coba").user(user)
				.discountTier("NON_ALUMNI")
				.walletBalance(BigDecimal.ZERO).active(true)
				.build();

		when(studentRepository.findWithClassById(1L)).thenReturn(Optional.of(student));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private static DiscountTierRate golongan(String kode, boolean aktif) {
		return DiscountTierRate.builder()
				.tier(kode).label(kode.equals("ALUMNI") ? "Alumni" : kode)
				.percent(new BigDecimal("25")).active(aktif).sortOrder(1)
				.build();
	}

	@Test
	@DisplayName("kata sandi kembali menjadi NIM, tersimpan sebagai hash")
	void kembaliKeNim() {
		String hasil = service.resetKataSandi(1L);

		assertThat(hasil).isEqualTo(NIM);
		assertThat(encoder.matches(NIM, user.getPasswordHash())).isTrue();
		assertThat(user.getPasswordHash()).isNotEqualTo(NIM);
	}

	@Test
	@DisplayName("kata sandi lama tidak berlaku lagi")
	void sandiLamaTidakBerlaku() {
		service.resetKataSandi(1L);

		assertThat(encoder.matches("kata-sandi-lama-mahasiswa", user.getPasswordHash()))
				.isFalse();
	}

	@Test
	@DisplayName("seluruh sesi mahasiswa dicabut, karena NIM diketahui banyak orang")
	void sesiDicabut() {
		assertThat(user.getTokensValidFrom()).isNull();

		service.resetKataSandi(1L);

		assertThat(user.getTokensValidFrom()).isNotNull();
	}

	@Test
	@DisplayName("mahasiswa tanpa akun dijawab dengan penjelasan, bukan NullPointerException")
	void tanpaAkun() {
		student.setUser(null);

		assertThatThrownBy(() -> service.resetKataSandi(1L))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("belum punya akun");

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("mahasiswa tidak ditemukan")
	void tidakDitemukan() {
		when(studentRepository.findWithClassById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resetKataSandi(99L))
				.isInstanceOf(NotFoundException.class);

		verify(userRepository, never()).save(any());
	}

	// --- Pindah golongan potongan ---

	@Test
	@DisplayName("golongan yang ada dan aktif diterima")
	void golonganDiterima() {
		when(tierRepository.findById("ALUMNI"))
				.thenReturn(Optional.of(golongan("ALUMNI", true)));

		assertThat(service.changeDiscountTier(1L, "ALUMNI").getDiscountTier())
				.isEqualTo("ALUMNI");
	}

	@Test
	@DisplayName("kode golongan yang tidak ada ditolak, lengkap dengan pilihan yang ada")
	void golonganTidakDikenal() {
		when(tierRepository.findById("SALAH_KETIK")).thenReturn(Optional.empty());
		when(tierRepository.findByActiveTrueOrderBySortOrderAscTierAsc())
				.thenReturn(List.of(golongan("NON_ALUMNI", true), golongan("ALUMNI", true)));

		assertThatThrownBy(() -> service.changeDiscountTier(1L, "SALAH_KETIK"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("tidak dikenal")
				.hasMessageContaining("NON_ALUMNI, ALUMNI");

		assertThat(student.getDiscountTier()).isEqualTo("NON_ALUMNI");
		verify(studentRepository, never()).save(any());
	}

	@Test
	@DisplayName("golongan yang sudah dinonaktifkan tidak bisa dipakai lagi")
	void golonganNonaktifDitolak() {
		when(tierRepository.findById("ALUMNI"))
				.thenReturn(Optional.of(golongan("ALUMNI", false)));

		assertThatThrownBy(() -> service.changeDiscountTier(1L, "ALUMNI"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("tidak aktif");

		assertThat(student.getDiscountTier()).isEqualTo("NON_ALUMNI");
		verify(studentRepository, never()).save(any());
	}
}
