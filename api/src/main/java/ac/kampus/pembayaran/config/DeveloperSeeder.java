package ac.kampus.pembayaran.config;

import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import ac.kampus.pembayaran.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Membuat akun DEVELOPER bila diminta lewat environment.
 *
 * <p>Peran ini sebelumnya tidak punya jalan lahir sama sekali: sistem tidak
 * menyediakan endpoint pembuat pengguna, jadi satu-satunya cara adalah INSERT
 * langsung ke database. Perannya ada di enum tapi tidak pernah bisa dipakai
 * siapa pun.
 *
 * <p>Berbeda dari admin, ini **mati secara bawaan**. Akun forensik hanya
 * dibutuhkan saat ada yang ditelusuri, dan akun yang menganggur di produksi
 * hanya menambah permukaan serangan tanpa memberi apa pun. Nyalakan dengan
 * {@code APP_SEED_DEVELOPER=true} beserta email dan kata sandinya.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-developer", havingValue = "true")
public class DeveloperSeeder {

	@Bean
	ApplicationRunner seedDeveloper(
			UserRepository users,
			PasswordEncoder encoder,
			@Value("${app.developer.email:developer@kampus.ac.id}") String email,
			@Value("${app.developer.password:}") String password,
			@Value("${app.developer.name:Developer}") String name) {

		return args -> {
			if (users.existsByEmailIgnoreCase(email)) {
				return;
			}

			// Tidak ada kata sandi bawaan. Akun forensik bisa membaca hasil
			// pembacaan bukti bayar milik semua mahasiswa; menyalakannya dengan
			// kata sandi yang tertulis di kode sama saja membukanya untuk umum.
			if (password.isBlank()) {
				log.error("""

						===================================================================
						APP_SEED_DEVELOPER menyala tapi APP_DEVELOPER_PASSWORD kosong.
						Akun developer TIDAK dibuat — tidak ada kata sandi bawaan untuk
						peran ini. Isi kata sandinya, lalu nyalakan ulang.
						===================================================================
						""");
				return;
			}

			users.save(User.builder()
					.name(name)
					.email(email)
					.passwordHash(encoder.encode(password))
					.role(UserRole.DEVELOPER)
					.active(true)
					.build());

			log.info("Akun developer dibuat: {}", email);
		};
	}
}
