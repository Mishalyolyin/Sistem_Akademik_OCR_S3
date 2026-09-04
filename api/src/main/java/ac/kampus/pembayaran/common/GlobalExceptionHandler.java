package ac.kampus.pembayaran.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Semua error dikembalikan sebagai RFC 7807 ProblemDetail, sehingga frontend
 * cukup membaca field {@code detail} (lihat web/src/lib/api.ts).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
		Map<String, String> errors = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors().forEach(fieldError ->
				errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));

		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Data tidak valid");
		problem.setDetail(errors.values().iterator().next());
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler({ BadCredentialsException.class, JwtException.class })
	public ProblemDetail handleUnauthorized(Exception e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problem.setTitle("Gagal masuk");
		problem.setDetail(e instanceof BadCredentialsException
				? e.getMessage()
				: "Sesi berakhir. Silakan masuk lagi.");
		return problem;
	}

	@ExceptionHandler(DisabledException.class)
	public ProblemDetail handleDisabled(DisabledException e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		problem.setTitle("Akun dinonaktifkan");
		problem.setDetail(e.getMessage());
		return problem;
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		problem.setTitle("Akses ditolak");
		problem.setDetail("Anda tidak punya izin untuk tindakan ini.");
		return problem;
	}

	/**
	 * Body JSON tidak bisa dibaca — paling sering karena nilai enum yang salah,
	 * misalnya golongan "SULTAN". Tanpa penanganan ini Spring membalas 500,
	 * padahal ini murni kesalahan masukan.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Data tidak valid");
		problem.setDetail(pilihanEnum(e.getCause())
				.orElse("Format data yang dikirim tidak sesuai."));
		return problem;
	}

	/** Parameter di URL bertipe salah, misalnya ?tier=SULTAN. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Data tidak valid");

		Class<?> enumType = tipeEnum(e);
		String pilihan = enumType == null
				? ""
				: " Pilihan: " + String.join(", ", enumNames(enumType)) + ".";
		problem.setDetail("Nilai \"%s\" tidak berlaku untuk %s.%s"
				.formatted(e.getValue(), e.getName(), pilihan));
		return problem;
	}

	/**
	 * Enum di balik parameter, termasuk bila parameternya berupa daftar seperti
	 * {@code List<PaymentStatus> status}. Tanpa membuka pembungkusnya, penyaring
	 * yang boleh diisi lebih dari satu nilai kehilangan daftar pilihan di pesan
	 * galatnya, padahal justru di situ pengguna paling butuh dituntun.
	 */
	private static Class<?> tipeEnum(MethodArgumentTypeMismatchException e) {
		Class<?> type = e.getRequiredType();
		if (type != null && type.isEnum()) {
			return type;
		}
		if (type != null && Collection.class.isAssignableFrom(type)) {
			Class<?> elemen = e.getParameter().nested().getNestedParameterType();
			return elemen.isEnum() ? elemen : null;
		}
		return null;
	}

	private Optional<String> pilihanEnum(Throwable cause) {
		if (cause instanceof InvalidFormatException invalid) {
			Class<?> type = invalid.getTargetType();
			if (type != null && type.isEnum()) {
				return Optional.of("Nilai \"%s\" tidak dikenal. Pilihan: %s."
						.formatted(invalid.getValue(), String.join(", ", enumNames(type))));
			}
		}
		return Optional.empty();
	}

	private static String[] enumNames(Class<?> type) {
		Object[] constants = type.getEnumConstants();
		String[] names = new String[constants.length];
		for (int i = 0; i < constants.length; i++) {
			names[i] = String.valueOf(constants[i]);
		}
		return names;
	}

	@ExceptionHandler(NotFoundException.class)
	public ProblemDetail handleNotFound(NotFoundException e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("Tidak ditemukan");
		problem.setDetail(e.getMessage());
		return problem;
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ProblemDetail handleBusinessRule(BusinessRuleException e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		problem.setTitle("Tidak bisa dilakukan");
		problem.setDetail(e.getMessage());
		return problem;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Permintaan tidak valid");
		problem.setDetail(e.getMessage());
		return problem;
	}

	/**
	 * Alamat yang tidak dikenal, atau metode HTTP yang salah untuk alamat yang
	 * ada. Keduanya kesalahan pemanggil, bukan kegagalan server — tanpa
	 * penanganan ini keduanya jatuh ke penangkap serba-guna di bawah dan dijawab
	 * 500, sekaligus mengotori log dengan ERROR untuk hal yang bukan galat.
	 */
	@ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
	public ProblemDetail handleNoHandler(Exception e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("Tidak ditemukan");
		problem.setDetail("Alamat yang diminta tidak ada.");
		return problem;
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
		problem.setTitle("Metode tidak didukung");
		problem.setDetail("Metode %s tidak berlaku untuk alamat ini.".formatted(e.getMethod()));
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception e, HttpServletRequest request) {
		log.error("Error tak tertangani di {} {}", request.getMethod(), request.getRequestURI(), e);

		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setTitle("Terjadi kesalahan");
		// Pesan asli tidak dibocorkan ke klien; detailnya ada di log server.
		problem.setDetail("Terjadi kesalahan di server. Coba lagi atau hubungi admin.");
		return problem;
	}
}
