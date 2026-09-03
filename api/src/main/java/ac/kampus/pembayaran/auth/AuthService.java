package ac.kampus.pembayaran.auth;

import ac.kampus.pembayaran.auth.dto.AuthDtos.IssuedTokens;
import ac.kampus.pembayaran.auth.dto.AuthDtos.LoginRequest;
import ac.kampus.pembayaran.auth.dto.AuthDtos.UserSummary;
import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Transactional(readOnly = true)
	public IssuedTokens login(LoginRequest request) {
		User user = userRepository.findByEmailIgnoreCase(request.email())
				// Pesan sengaja disamakan dengan kasus kata sandi salah supaya tidak
				// membocorkan email mana yang terdaftar.
				.orElseThrow(() -> new BadCredentialsException("Email atau kata sandi salah."));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadCredentialsException("Email atau kata sandi salah.");
		}
		requireActive(user);

		return issueTokens(user);
	}

	@Transactional(readOnly = true)
	public IssuedTokens refresh(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new JwtException("Tidak ada sesi tersimpan.");
		}

		var claims = jwtService.parseRefreshToken(refreshToken);
		User user = userRepository.findById(jwtService.userIdOf(claims))
				.orElseThrow(() -> new JwtException("Pengguna tidak ditemukan."));
		requireActive(user);

		return issueTokens(user);
	}

	@Transactional(readOnly = true)
	public UserSummary currentUser() {
		return userRepository.findById(currentUserId())
				.map(UserSummary::from)
				.orElseThrow(() -> new BadCredentialsException("Sesi tidak valid."));
	}

	/** Id pengguna yang sedang login, diisi oleh {@link JwtAuthenticationFilter}. */
	public static Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
			throw new BadCredentialsException("Sesi tidak valid.");
		}
		return userId;
	}

	private void requireActive(User user) {
		if (!user.isActive()) {
			throw new DisabledException("Akun ini dinonaktifkan. Hubungi admin.");
		}
	}

	private IssuedTokens issueTokens(User user) {
		return new IssuedTokens(
				jwtService.generateAccessToken(user),
				jwtService.generateRefreshToken(user),
				jwtService.accessTokenTtl().toSeconds(),
				UserSummary.from(user));
	}
}
