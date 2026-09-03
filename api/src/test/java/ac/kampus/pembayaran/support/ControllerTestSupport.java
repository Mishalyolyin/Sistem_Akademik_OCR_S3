package ac.kampus.pembayaran.support;

import ac.kampus.pembayaran.auth.JwtService;
import ac.kampus.pembayaran.user.User;
import ac.kampus.pembayaran.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Perkakas bersama untuk uji controller: penyusun token dan penulis badan JSON.
 */
public abstract class ControllerTestSupport {

	/** Id pengguna yang dipakai semua token di uji, kecuali disebut lain. */
	protected static final long ID_ADMIN = 9L;
	protected static final long ID_MAHASISWA = 3L;

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JwtService jwtService;

	@Autowired
	protected ObjectMapper objectMapper;

	protected String token(UserRole role, long userId) {
		return jwtService.generateAccessToken(User.builder()
				.id(userId)
				.name("Uji " + role)
				.email(role.name().toLowerCase() + "@kampus.ac.id")
				.passwordHash("hash")
				.role(role)
				.active(true)
				.build());
	}

	protected String tokenAdmin() {
		return token(UserRole.ADMIN, ID_ADMIN);
	}

	protected String tokenMahasiswa() {
		return token(UserRole.MAHASISWA, ID_MAHASISWA);
	}

	/** Menempelkan header Authorization berisi token admin. */
	protected MockHttpServletRequestBuilder sebagaiAdmin(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin());
	}

	protected MockHttpServletRequestBuilder sebagaiMahasiswa(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMahasiswa());
	}

	protected String json(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("Gagal menyusun JSON untuk uji.", e);
		}
	}
}
