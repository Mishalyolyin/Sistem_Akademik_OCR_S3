package ac.kampus.pembayaran.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Membaca header {@code Authorization: Bearer ...} dan mengisi SecurityContext.
 * Setara peran middleware auth di Laravel.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String HEADER = "Authorization";
	private static final String PREFIX = "Bearer ";

	private final JwtService jwtService;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String header = request.getHeader(HEADER);

		if (header != null && header.startsWith(PREFIX)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = header.substring(PREFIX.length()).trim();
			try {
				Claims claims = jwtService.parseAccessToken(token);
				var authorities = List.of(
						new SimpleGrantedAuthority("ROLE_" + jwtService.roleOf(claims)));

				var authentication = new UsernamePasswordAuthenticationToken(
						jwtService.userIdOf(claims), null, authorities);
				authentication.setDetails(
						new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (JwtException | IllegalArgumentException e) {
				// Token tidak valid dibiarkan tanpa autentikasi; entry point yang
				// akan menolak dengan 401. Jangan lempar exception di sini.
				log.debug("Token ditolak: {}", e.getMessage());
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}
}
