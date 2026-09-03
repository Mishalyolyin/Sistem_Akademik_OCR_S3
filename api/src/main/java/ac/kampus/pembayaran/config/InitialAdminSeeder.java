package ac.kampus.pembayaran.config;

import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import ac.kampus.pembayaran.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Membuat satu akun admin saat database masih kosong, supaya bisa langsung login
 * setelah `docker compose up`. Matikan di produksi dengan app.seed-admin=false.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-admin", havingValue = "true", matchIfMissing = true)
public class InitialAdminSeeder {

	private static final String EMAIL = "admin@kampus.ac.id";
	private static final String DEFAULT_PASSWORD = "admin123";

	@Bean
	ApplicationRunner seedInitialAdmin(UserRepository users, PasswordEncoder encoder) {
		return args -> {
			if (users.existsByEmailIgnoreCase(EMAIL)) {
				return;
			}

			users.save(User.builder()
					.name("Admin Keuangan")
					.email(EMAIL)
					.passwordHash(encoder.encode(DEFAULT_PASSWORD))
					.role(UserRole.ADMIN)
					.active(true)
					.build());

			log.warn("""

					===================================================================
					Akun admin awal dibuat: {} / {}
					GANTI KATA SANDI INI sebelum dipakai di luar mesin pengembangan.
					===================================================================
					""", EMAIL, DEFAULT_PASSWORD);
		};
	}
}
