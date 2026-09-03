package ac.kampus.pembayaran.config;

import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import ac.kampus.pembayaran.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Membuat satu akun admin saat tabel pengguna masih kosong.
 *
 * <p>Tanpa ini, pemasangan baru tidak punya jalan masuk sama sekali: sistem
 * tidak menyediakan endpoint pembuat pengguna, jadi admin pertama tidak bisa
 * dibuat dari mana pun kecuali langsung di database.
 *
 * <p>Di produksi, isi {@code APP_ADMIN_EMAIL} dan {@code APP_ADMIN_PASSWORD}
 * lewat environment variable. Nilai bawaannya sengaja lemah dan hanya pantas
 * untuk mesin pengembangan; kalau yang bawaan masih terpakai, peringatannya
 * dicetak besar-besar di log.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-admin", havingValue = "true", matchIfMissing = true)
public class InitialAdminSeeder {

	static final String DEFAULT_EMAIL = "admin@kampus.ac.id";
	static final String DEFAULT_PASSWORD = "admin123";

	@Bean
	ApplicationRunner seedInitialAdmin(
			UserRepository users,
			PasswordEncoder encoder,
			@Value("${app.admin.email:" + DEFAULT_EMAIL + "}") String email,
			@Value("${app.admin.password:" + DEFAULT_PASSWORD + "}") String password,
			@Value("${app.admin.name:Admin Keuangan}") String name) {

		return args -> {
			if (users.existsByEmailIgnoreCase(email)) {
				return;
			}

			users.save(User.builder()
					.name(name)
					.email(email)
					.passwordHash(encoder.encode(password))
					.role(UserRole.ADMIN)
					.active(true)
					.build());

			if (DEFAULT_PASSWORD.equals(password)) {
				log.warn("""

						===================================================================
						Akun admin awal dibuat: {} / {}
						KATA SANDI INI BAWAAN DAN DIKETAHUI UMUM. Set APP_ADMIN_PASSWORD
						sebelum dipakai di luar mesin pengembangan.
						===================================================================
						""", email, password);
			} else {
				log.info("Akun admin awal dibuat: {}", email);
			}
		};
	}
}
