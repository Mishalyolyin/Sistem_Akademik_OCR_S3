package ac.kampus.pembayaran.config;

import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import ac.kampus.pembayaran.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pembuatan akun forensik OCR.
 *
 * <p>Bedanya dari admin ada di satu hal yang penting: **tidak ada kata sandi
 * bawaan**. Akun ini bisa membaca hasil pembacaan bukti bayar seluruh
 * mahasiswa, jadi kata sandi yang tertulis di kode sama saja membukanya untuk
 * siapa pun yang pernah melihat repositori ini.
 */
class DeveloperSeederTest {

	private UserRepository users;
	private PasswordEncoder encoder;
	private DeveloperSeeder seeder;

	@BeforeEach
	void setUp() {
		users = mock(UserRepository.class);
		encoder = new BCryptPasswordEncoder(4);
		seeder = new DeveloperSeeder();

		when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private void jalankan(String email, String password) throws Exception {
		seeder.seedDeveloper(users, encoder, email, password, "Developer").run(null);
	}

	private User yangDisimpan() {
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(users).save(captor.capture());
		return captor.getValue();
	}

	@Test
	@DisplayName("membuat akun berperan DEVELOPER dengan kata sandi yang di-hash")
	void membuatAkunDeveloper() throws Exception {
		when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);

		jalankan("dev@kampus.ac.id", "kata-sandi-forensik-yang-kuat");

		User dev = yangDisimpan();
		assertThat(dev.getEmail()).isEqualTo("dev@kampus.ac.id");
		assertThat(dev.getRole()).isEqualTo(UserRole.DEVELOPER);
		assertThat(dev.isActive()).isTrue();
		assertThat(encoder.matches("kata-sandi-forensik-yang-kuat", dev.getPasswordHash()))
				.isTrue();
		assertThat(dev.getPasswordHash()).startsWith("$2");
	}

	@Test
	@DisplayName("kata sandi kosong berarti akun TIDAK dibuat, bukan dibuat dengan bawaan")
	void tanpaKataSandiTidakDibuat() throws Exception {
		when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);

		jalankan("dev@kampus.ac.id", "");

		verify(users, never()).save(any());
	}

	@Test
	@DisplayName("kata sandi berisi spasi saja juga ditolak")
	void kataSandiSpasiDitolak() throws Exception {
		when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);

		jalankan("dev@kampus.ac.id", "   ");

		verify(users, never()).save(any());
	}

	@Test
	@DisplayName("tidak menimpa akun yang sudah ada, jadi aman dijalankan tiap kali menyala")
	void tidakMenimpaYangSudahAda() throws Exception {
		when(users.existsByEmailIgnoreCase("dev@kampus.ac.id")).thenReturn(true);

		jalankan("dev@kampus.ac.id", "kata-sandi-lain");

		verify(users, never()).save(any());
	}
}
