package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.studyclass.StudyClassRepository;
import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import ac.kampus.pembayaran.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pengembalian kata sandi mahasiswa oleh admin.
 *
 * <p>Mahasiswa sengaja tidak mengelola kata sandinya sendiri. Kalau lupa, ia
 * datang ke bagian keuangan dan admin mengembalikannya ke NIM — jadi jalur ini
 * satu-satunya cara masuk lagi, dan harus benar-benar bekerja.
 */
class StudentServiceTest {

	private static final String NIM = "2612600001";

	private StudentRepository studentRepository;
	private UserRepository userRepository;
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
		encoder = new BCryptPasswordEncoder(4);

		service = new StudentService(studentRepository, studyClassRepository,
				tierLockPolicy, userRepository, encoder);

		user = User.builder()
				.id(5L).name("Uji Coba").email("uji@kampus.ac.id")
				.passwordHash(encoder.encode("kata-sandi-lama-mahasiswa"))
				.role(UserRole.MAHASISWA).active(true)
				.build();

		student = Student.builder()
				.id(1L).nim(NIM).name("Uji Coba").user(user)
				.walletBalance(BigDecimal.ZERO).active(true)
				.build();

		when(studentRepository.findWithClassById(1L)).thenReturn(Optional.of(student));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
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
}
