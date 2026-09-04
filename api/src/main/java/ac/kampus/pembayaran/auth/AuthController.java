package ac.kampus.pembayaran.auth;

import ac.kampus.pembayaran.auth.dto.AuthDtos.IssuedTokens;
import ac.kampus.pembayaran.auth.dto.AuthDtos.LoginRequest;
import ac.kampus.pembayaran.auth.dto.AuthDtos.TokenResponse;
import ac.kampus.pembayaran.auth.dto.AuthDtos.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autentikasi")
public class AuthController {

	static final String REFRESH_COOKIE = "refresh_token";

	private final AuthService authService;
	private final JwtService jwtService;

	/**
	 * Cookie Secure hanya dikirim lewat HTTPS. Di produksi ini WAJIB true;
	 * dimatikan hanya untuk pengembangan lokal yang memakai HTTP.
	 */
	@org.springframework.beans.factory.annotation.Value("${app.cookie.secure:false}")
	private boolean cookieSecure;

	@PostMapping("/login")
	@Operation(summary = "Masuk dengan email dan kata sandi")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return respondWithTokens(authService.login(request));
	}

	@PostMapping("/refresh")
	@Operation(summary = "Perbarui access token memakai cookie refresh")
	public ResponseEntity<TokenResponse> refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
		return respondWithTokens(authService.refresh(refreshToken));
	}

	@PostMapping("/logout")
	@Operation(summary = "Keluar, cabut refresh token, dan hapus cookie sesi")
	public ResponseEntity<Void> logout() {
		authService.logout();
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
				.build();
	}

	@GetMapping("/me")
	@Operation(summary = "Profil pengguna yang sedang login")
	public UserSummary me() {
		return authService.currentUser();
	}

	public record GantiSandiRequest(
			@NotBlank(message = "Kata sandi lama wajib diisi.")
			String lama,

			@NotBlank(message = "Kata sandi baru wajib diisi.")
			@Size(min = 8, message = "Kata sandi baru minimal 8 karakter.")
			String baru
	) {
	}

	/**
	 * Hanya untuk staf. Mahasiswa sengaja tidak mengelola kata sandinya sendiri:
	 * kalau lupa, admin mengembalikannya ke NIM lewat
	 * {@code POST /students/{id}/reset-kata-sandi}.
	 */
	@PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
	@PostMapping("/kata-sandi")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Ganti kata sandi sendiri; seluruh sesi lain ikut dicabut")
	public void gantiKataSandi(@Valid @RequestBody GantiSandiRequest request) {
		authService.gantiKataSandi(request.lama(), request.baru());
	}

	private ResponseEntity<TokenResponse> respondWithTokens(IssuedTokens tokens) {
		ResponseCookie cookie =
				refreshCookie(tokens.refreshToken(), jwtService.refreshTokenTtl());

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(tokens.toResponse());
	}

	/**
	 * Refresh token disimpan sebagai cookie HttpOnly supaya tidak bisa dibaca
	 * JavaScript. Access token yang berumur pendek tetap dikirim di body dan
	 * disimpan di memori frontend.
	 */
	private ResponseCookie refreshCookie(String value, Duration maxAge) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
				.httpOnly(true)
				.secure(cookieSecure)
				.sameSite("Lax")
				.path("/api/auth")
				.maxAge(maxAge)
				.build();
	}
}
