package ac.kampus.pembayaran.auth.dto;

import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

	private AuthDtos() {
	}

	public record LoginRequest(
			@NotBlank(message = "Email wajib diisi.")
			@Email(message = "Format email tidak valid.")
			String email,

			@NotBlank(message = "Kata sandi wajib diisi.")
			String password
	) {
	}

	public record UserSummary(Long id, String name, String email, UserRole role) {
		public static UserSummary from(User user) {
			return new UserSummary(
					user.getId(), user.getName(), user.getEmail(), user.getRole());
		}
	}

	/**
	 * Hasil internal service. Refresh token TIDAK ikut ke body respons —
	 * controller memasangnya sebagai cookie HttpOnly.
	 */
	public record IssuedTokens(
			String accessToken,
			String refreshToken,
			long expiresInSeconds,
			UserSummary user
	) {
		public TokenResponse toResponse() {
			return new TokenResponse(accessToken, expiresInSeconds, user);
		}
	}

	/** Yang dikirim ke browser. Sengaja tanpa refresh token. */
	public record TokenResponse(
			String accessToken,
			long expiresInSeconds,
			UserSummary user
	) {
	}
}
