package ac.kampus.pembayaran.auth;

import ac.kampus.pembayaran.auth.dto.AuthDtos.IssuedTokens;
import ac.kampus.pembayaran.auth.dto.AuthDtos.UserSummary;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import ac.kampus.pembayaran.user.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lapisan HTTP autentikasi.
 *
 * <p>Yang dijaga di sini terutama satu hal yang mudah lolos tanpa disadari:
 * refresh token tidak boleh ikut ke badan jawaban. Begitu ia sampai ke
 * JavaScript, cookie HttpOnly yang dipasang di sebelahnya kehilangan gunanya.
 */
@ControllerTest(AuthController.class)
class AuthControllerTest extends ControllerTestSupport {

	private static final String REFRESH_TOKEN = "refresh-token-rahasia";

	@MockitoBean
	private AuthService authService;

	private static IssuedTokens tokenTerbit() {
		return new IssuedTokens(
				"access-token-singkat", REFRESH_TOKEN, 900,
				new UserSummary(9L, "Admin Uji", "admin@kampus.ac.id", UserRole.ADMIN));
	}

	// --- Masuk ---

	@Test
	@DisplayName("refresh token dipasang sebagai cookie HttpOnly, tidak ikut ke badan jawaban")
	void refreshTokenHanyaDiCookie() throws Exception {
		when(authService.login(any())).thenReturn(tokenTerbit());

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("email", "admin@kampus.ac.id",
								"password", "rahasia123"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token-singkat"))
				.andExpect(cookie().httpOnly("refresh_token", true))
				.andExpect(cookie().value("refresh_token", REFRESH_TOKEN))
				// Badan jawaban tidak boleh memuat refresh token di field mana pun.
				.andExpect(content -> {
					String badan = content.getResponse().getContentAsString();
					org.assertj.core.api.Assertions.assertThat(badan)
							.doesNotContain(REFRESH_TOKEN);
				});
	}

	@Test
	@DisplayName("cookie refresh dibatasi ke jalur /api/auth saja")
	void cookieDibatasiJalurnya() throws Exception {
		when(authService.login(any())).thenReturn(tokenTerbit());

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("email", "admin@kampus.ac.id",
								"password", "rahasia123"))))
				.andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")))
				.andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
	}

	@Test
	@DisplayName("email yang formatnya salah ditolak 400 sebelum menyentuh service")
	void emailTidakValid() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("email", "bukan-email", "password", "rahasia123"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("email")));

		verify(authService, never()).login(any());
	}

	@Test
	@DisplayName("kata sandi kosong ditolak 400")
	void sandiKosong() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("email", "admin@kampus.ac.id", "password", ""))))
				.andExpect(status().isBadRequest());

		verify(authService, never()).login(any());
	}

	@Test
	@DisplayName("kredensial salah dijawab 401, dan pesannya tidak menyebut email mana yang ada")
	void kredensialSalah() throws Exception {
		when(authService.login(any()))
				.thenThrow(new BadCredentialsException("Email atau kata sandi salah."));

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("email", "admin@kampus.ac.id",
								"password", "salah"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Email atau kata sandi salah."))
				.andExpect(jsonPath("$.detail", not(containsString("tidak terdaftar"))));
	}

	// --- Perbarui sesi ---

	@Test
	@DisplayName("tanpa cookie, permintaan perbarui dijawab 401 bukan 500")
	void refreshTanpaCookie() throws Exception {
		when(authService.refresh(any())).thenThrow(new JwtException("Tidak ada sesi tersimpan."));

		mockMvc.perform(post("/auth/refresh"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	// --- Keluar ---

	@Test
	@DisplayName("keluar mencabut sesi di server, bukan hanya menghapus cookie")
	void keluarMencabutSesi() throws Exception {
		mockMvc.perform(sebagaiAdmin(post("/auth/logout")))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("refresh_token", 0));

		verify(authService).logout();
	}

	@Test
	@DisplayName("keluar tanpa token ditolak 401 dan tidak mencabut sesi siapa pun")
	void keluarTanpaToken() throws Exception {
		mockMvc.perform(post("/auth/logout"))
				.andExpect(status().isUnauthorized());

		verify(authService, never()).logout();
	}

	@Test
	@DisplayName("profil pengguna yang sedang login butuh token")
	void profilButuhToken() throws Exception {
		mockMvc.perform(get("/auth/me"))
				.andExpect(status().isUnauthorized());

		verify(authService, never()).currentUser();
	}
}
