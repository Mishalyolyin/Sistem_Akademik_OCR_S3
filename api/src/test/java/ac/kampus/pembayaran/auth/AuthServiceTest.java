package ac.kampus.pembayaran.auth;

import ac.kampus.pembayaran.config.JwtProperties;
import ac.kampus.pembayaran.user.PasswordService;
import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import ac.kampus.pembayaran.user.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Aturan sesi: siapa yang boleh memperbarui token, dan sampai kapan.
 *
 * <p>Memakai {@link JwtService} sungguhan, bukan tiruan, supaya token yang diuji
 * benar-benar melewati penandatanganan dan pembacaan seperti di produksi.
 */
class AuthServiceTest {

	private UserRepository userRepository;
	private JwtService jwtService;
	private AuthService service;
	private User user;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		jwtService = new JwtService(new JwtProperties(
				"rahasia-uji-coba-minimal-32-karakter-untuk-hs256", 15, 7));

		service = new AuthService(userRepository, passwordEncoder, jwtService,
				new PasswordService(userRepository, passwordEncoder));

		user = User.builder()
				.id(1L).name("Admin Uji").email("admin@kampus.ac.id")
				.passwordHash("$2a$12$hash").role(UserRole.ADMIN).active(true)
				.build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private void sedangLoginSebagai(long userId) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(userId, null, List.of()));
	}

	// --- Pencabutan sesi ---

	@Test
	@DisplayName("logout menandai sejak kapan token dianggap sah")
	void logoutMengisiTokensValidFrom() {
		sedangLoginSebagai(1L);
		assertThat(user.getTokensValidFrom()).isNull();

		service.logout();

		assertThat(user.getTokensValidFrom()).isNotNull();
	}

	@Test
	@DisplayName("refresh token yang terbit sebelum logout ditolak")
	void tokenLamaDitolakSetelahLogout() {
		String refreshToken = jwtService.generateRefreshToken(user);
		// Pencabutan terjadi sesudah token ini terbit.
		user.setTokensValidFrom(Instant.now().plusSeconds(2));

		assertThatThrownBy(() -> service.refresh(refreshToken))
				.isInstanceOf(JwtException.class)
				.hasMessageContaining("Sesi sudah diakhiri");
	}

	@Test
	@DisplayName("refresh token yang terbit setelah logout tetap berlaku")
	void tokenBaruTetapBerlaku() {
		user.setTokensValidFrom(Instant.now().minusSeconds(5));
		String refreshToken = jwtService.generateRefreshToken(user);

		assertThatCode(() -> service.refresh(refreshToken)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("masuk lagi pada detik yang sama dengan saat keluar tidak ikut tertolak")
	void masukLagiDiDetikYangSamaTidakTertolak() {
		// Klaim iat hanya berpresisi detik. Tanpa pemotongan ke detik pada sisi
		// pencabutan, token yang baru saja terbit bisa terbaca "lebih tua"
		// daripada waktu logout dan langsung ditolak.
		Instant sekarang = Instant.now();
		user.setTokensValidFrom(sekarang.truncatedTo(ChronoUnit.SECONDS).plusMillis(400));
		String refreshToken = jwtService.generateRefreshToken(user);

		assertThatCode(() -> service.refresh(refreshToken)).doesNotThrowAnyException();
	}

	// --- Aturan yang sudah ada, dikunci supaya tidak lepas ---

	@Test
	@DisplayName("access token tidak bisa dipakai sebagai refresh token")
	void accessTokenDitolakDiEndpointRefresh() {
		String accessToken = jwtService.generateAccessToken(user);

		assertThatThrownBy(() -> service.refresh(accessToken))
				.isInstanceOf(JwtException.class)
				.hasMessageContaining("bukan refresh token");
	}

	@Test
	@DisplayName("akun yang dinonaktifkan tidak bisa memperbarui token")
	void akunNonaktifDitolak() {
		String refreshToken = jwtService.generateRefreshToken(user);
		user.setActive(false);

		assertThatThrownBy(() -> service.refresh(refreshToken))
				.isInstanceOf(DisabledException.class);
	}

	@Test
	@DisplayName("cookie kosong dijawab galat sesi, bukan NullPointerException")
	void cookieKosong() {
		assertThatThrownBy(() -> service.refresh(null)).isInstanceOf(JwtException.class);
		assertThatThrownBy(() -> service.refresh("  ")).isInstanceOf(JwtException.class);
	}
}
