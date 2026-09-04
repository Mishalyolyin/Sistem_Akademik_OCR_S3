package ac.kampus.pembayaran.user;

import ac.kampus.pembayaran.common.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Aturan penggantian kata sandi yang berlaku untuk semua peran.
 *
 * <p>Memakai penyandi bcrypt sungguhan, bukan tiruan: yang justru penting di
 * sini adalah bahwa yang tersimpan benar-benar hash yang bisa dicocokkan lagi.
 */
class PasswordServiceTest {

	private static final String SANDI_LAMA = "sandi-lama-yang-benar";

	private UserRepository userRepository;
	private PasswordEncoder encoder;
	private PasswordService service;
	private User user;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		encoder = new BCryptPasswordEncoder(4);
		service = new PasswordService(userRepository, encoder);

		user = User.builder()
				.id(1L).name("Admin Uji").email("admin@kampus.ac.id")
				.passwordHash(encoder.encode(SANDI_LAMA))
				.role(UserRole.ADMIN).active(true)
				.build();

		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	@Test
	@DisplayName("kata sandi tersimpan sebagai hash yang cocok dengan yang baru")
	void kataSandiBerganti() {
		service.ubah(user, SANDI_LAMA, "sandi-baru-yang-kuat");

		assertThat(encoder.matches("sandi-baru-yang-kuat", user.getPasswordHash())).isTrue();
		assertThat(encoder.matches(SANDI_LAMA, user.getPasswordHash())).isFalse();
	}

	@Test
	@DisplayName("seluruh sesi lain dicabut, karena penggantian biasanya karena curiga")
	void sesiLainDicabut() {
		assertThat(user.getTokensValidFrom()).isNull();

		service.ubah(user, SANDI_LAMA, "sandi-baru-yang-kuat");

		assertThat(user.getTokensValidFrom()).isNotNull();
		assertThat(user.getTokensValidFrom()).isBeforeOrEqualTo(Instant.now());
	}

	@Test
	@DisplayName("kata sandi lama yang salah ditolak dan tidak mengubah apa pun")
	void sandiLamaSalah() {
		String hashSemula = user.getPasswordHash();

		assertThatThrownBy(() -> service.ubah(user, "tebakan-ngawur", "sandi-baru-yang-kuat"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("lama salah");

		assertThat(user.getPasswordHash()).isEqualTo(hashSemula);
		assertThat(user.getTokensValidFrom()).isNull();
		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("kata sandi lama kosong ditolak, bukan dianggap cocok")
	void sandiLamaKosong() {
		assertThatThrownBy(() -> service.ubah(user, null, "sandi-baru-yang-kuat"))
				.isInstanceOf(BusinessRuleException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("kata sandi baru yang terlalu pendek ditolak")
	void sandiBaruTerlaluPendek() {
		assertThatThrownBy(() -> service.ubah(user, SANDI_LAMA, "pendek7"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("minimal 8 karakter");

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("kata sandi baru yang sama dengan yang lama ditolak, supaya tidak terasa berganti padahal tidak")
	void sandiBaruSamaDenganLama() {
		assertThatThrownBy(() -> service.ubah(user, SANDI_LAMA, SANDI_LAMA))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("sama dengan yang lama");

		assertThat(user.getTokensValidFrom()).isNull();
		verify(userRepository, never()).save(any());
	}
}
