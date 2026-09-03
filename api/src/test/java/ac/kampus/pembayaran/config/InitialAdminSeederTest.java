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
 * Pembuatan admin pertama.
 *
 * <p>Sistem ini tidak punya endpoint pembuat pengguna, jadi kelas ini adalah
 * satu-satunya jalan masuk ke pemasangan baru. Kalau ia diam saat seharusnya
 * bekerja, sistemnya terpasang rapi tapi tidak bisa dibuka siapa pun.
 */
class InitialAdminSeederTest {

	private UserRepository users;
	private PasswordEncoder encoder;
	private InitialAdminSeeder seeder;

	@BeforeEach
	void setUp() {
		users = mock(UserRepository.class);
		// Penyandi sungguhan, supaya yang diuji benar-benar hash yang bisa dicocokkan.
		encoder = new BCryptPasswordEncoder(4);
		seeder = new InitialAdminSeeder();

		when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private void jalankan(String email, String password) throws Exception {
		seeder.seedInitialAdmin(users, encoder, email, password, "Admin Keuangan").run(null);
	}

	private User yangDisimpan() {
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(users).save(captor.capture());
		return captor.getValue();
	}

	@Test
	@DisplayName("membuat admin memakai kredensial dari environment, bukan yang bawaan")
	void memakaiKredensialEnvironment() throws Exception {
		when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);

		jalankan("keuangan@kampus.ac.id", "kata-sandi-produksi-yang-kuat");

		User admin = yangDisimpan();
		assertThat(admin.getEmail()).isEqualTo("keuangan@kampus.ac.id");
		assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(admin.isActive()).isTrue();
		assertThat(encoder.matches("kata-sandi-produksi-yang-kuat", admin.getPasswordHash()))
				.isTrue();
	}

	@Test
	@DisplayName("kata sandi disimpan sebagai hash, tidak pernah apa adanya")
	void kataSandiDihash() throws Exception {
		when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);

		jalankan("keuangan@kampus.ac.id", "kata-sandi-produksi-yang-kuat");

		assertThat(yangDisimpan().getPasswordHash())
				.isNotEqualTo("kata-sandi-produksi-yang-kuat")
				.startsWith("$2");
	}

	@Test
	@DisplayName("tidak menimpa admin yang sudah ada, jadi aman dijalankan tiap kali menyala")
	void tidakMenimpaYangSudahAda() throws Exception {
		when(users.existsByEmailIgnoreCase("keuangan@kampus.ac.id")).thenReturn(true);

		jalankan("keuangan@kampus.ac.id", "kata-sandi-baru");

		verify(users, never()).save(any());
	}

	@Test
	@DisplayName("kredensial bawaan masih dipakai bila environment tidak diisi")
	void kredensialBawaan() throws Exception {
		when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);

		jalankan(InitialAdminSeeder.DEFAULT_EMAIL, InitialAdminSeeder.DEFAULT_PASSWORD);

		assertThat(yangDisimpan().getEmail()).isEqualTo(InitialAdminSeeder.DEFAULT_EMAIL);
	}
}
