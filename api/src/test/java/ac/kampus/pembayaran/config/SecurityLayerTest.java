package ac.kampus.pembayaran.config;

import ac.kampus.pembayaran.dashboard.DashboardController;
import ac.kampus.pembayaran.dashboard.DashboardRepository;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Penjagaan pintu masuk HTTP, diuji lewat satu endpoint admin yang mewakili.
 *
 * <p>Aturan peran selama ini hanya terbukti lewat pencobaan manual. Padahal
 * satu anotasi {@code @PreAuthorize} yang hilang tidak membuat test mana pun
 * gagal, tidak membuat kompilasi gagal, dan tidak kelihatan di layar — endpoint
 * itu diam-diam terbuka untuk siapa saja yang punya token.
 *
 * <p>Diuji juga bentuk badan jawaban penolakan: frontend membaca field
 * {@code detail}, jadi 401 bertubuh kosong muncul sebagai galat tanpa
 * keterangan apa pun di layar pengguna.
 */
@ControllerTest(DashboardController.class)
class SecurityLayerTest extends ControllerTestSupport {

	@MockitoBean
	private DashboardRepository repository;

	private String tokenUntuk(UserRole role) {
		return token(role, 1L);
	}

	private void repositoryMengembalikanNol() {
		org.mockito.Mockito.when(repository.totalBilled()).thenReturn(BigDecimal.ZERO);
		org.mockito.Mockito.when(repository.totalCollected()).thenReturn(BigDecimal.ZERO);
		org.mockito.Mockito.when(repository.totalWalletBalance()).thenReturn(BigDecimal.ZERO);
		org.mockito.Mockito.when(repository.summaryByCategory()).thenReturn(List.of());
		org.mockito.Mockito.when(repository.summaryByClass()).thenReturn(List.of());
		org.mockito.Mockito.when(repository.verificationQueue()).thenReturn(List.of());
	}

	// --- Siapa yang boleh masuk ---

	@Test
	@DisplayName("admin boleh membuka endpoint admin")
	void adminDiterima() throws Exception {
		repositoryMengembalikanNol();

		mockMvc.perform(get("/dashboard/summary")
						.header("Authorization", "Bearer " + tokenUntuk(UserRole.ADMIN)))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("mahasiswa ditolak 403 di endpoint admin")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(get("/dashboard/summary")
						.header("Authorization", "Bearer " + tokenUntuk(UserRole.MAHASISWA)))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("developer ditolak 403 di endpoint admin")
	void developerDitolak() throws Exception {
		mockMvc.perform(get("/dashboard/summary")
						.header("Authorization", "Bearer " + tokenUntuk(UserRole.DEVELOPER)))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaTokenDitolak() throws Exception {
		mockMvc.perform(get("/dashboard/summary"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("refresh token tidak bisa dipakai sebagai bearer di endpoint biasa")
	void refreshTokenBukanAccessToken() throws Exception {
		String refresh = jwtService.generateRefreshToken(User.builder()
				.id(1L).name("Uji").email("uji@kampus.ac.id")
				.passwordHash("hash").role(UserRole.ADMIN).active(true)
				.build());

		mockMvc.perform(get("/dashboard/summary").header("Authorization", "Bearer " + refresh))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("token dengan tanda tangan asal ditolak, bukan dianggap tamu")
	void tokenPalsuDitolak() throws Exception {
		mockMvc.perform(get("/dashboard/summary")
						.header("Authorization", "Bearer bukan.sebuah.jwt"))
				.andExpect(status().isUnauthorized());
	}

	// --- Bentuk jawaban penolakan ---

	@Test
	@DisplayName("401 memakai format ProblemDetail berisi keterangan, bukan badan kosong")
	void penolakan401BerbentukProblemDetail() throws Exception {
		mockMvc.perform(get("/dashboard/summary"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").isNotEmpty())
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	@DisplayName("403 memakai format ProblemDetail berisi keterangan")
	void penolakan403BerbentukProblemDetail() throws Exception {
		mockMvc.perform(get("/dashboard/summary")
						.header("Authorization", "Bearer " + tokenUntuk(UserRole.MAHASISWA)))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.status").value(403));
	}
}
