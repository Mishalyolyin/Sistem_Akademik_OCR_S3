package ac.kampus.pembayaran.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Menjawab penolakan yang terjadi di rantai filter, sebelum request sempat
 * sampai ke controller.
 *
 * <p>{@code GlobalExceptionHandler} hanya menangkap exception dari controller,
 * jadi tanpa kelas ini request tanpa token dijawab 401 bertubuh kosong.
 * Frontend membaca field {@code detail} untuk menampilkan pesan (lihat
 * {@code web/src/lib/api.ts}), sehingga body kosong muncul sebagai galat tanpa
 * keterangan apa pun di layar pengguna.
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		tulis(response, HttpStatus.UNAUTHORIZED, "Belum masuk",
				"Sesi tidak ditemukan atau sudah berakhir. Silakan masuk lagi.", request);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		tulis(response, HttpStatus.FORBIDDEN, "Akses ditolak",
				"Anda tidak punya izin untuk tindakan ini.", request);
	}

	private void tulis(HttpServletResponse response, HttpStatus status, String judul,
			String detail, HttpServletRequest request) throws IOException {
		if (response.isCommitted()) {
			return;
		}

		ProblemDetail problem = ProblemDetail.forStatus(status);
		problem.setTitle(judul);
		problem.setDetail(detail);
		problem.setInstance(URI.create(request.getRequestURI()));

		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
