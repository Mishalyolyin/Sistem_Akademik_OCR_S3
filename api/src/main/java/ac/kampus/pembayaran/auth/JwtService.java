package ac.kampus.pembayaran.auth;

import ac.kampus.pembayaran.config.JwtProperties;
import ac.kampus.pembayaran.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_TYPE = "typ";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";

	private final SecretKey key;
	private final Duration accessTtl;
	private final Duration refreshTtl;

	public JwtService(JwtProperties properties) {
		byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < 32) {
			throw new IllegalStateException(
					"app.jwt.secret harus minimal 32 karakter untuk HS256. "
							+ "Set environment variable JWT_SECRET.");
		}
		this.key = Keys.hmacShaKeyFor(secret);
		this.accessTtl = Duration.ofMinutes(properties.accessTokenMinutes());
		this.refreshTtl = Duration.ofDays(properties.refreshTokenDays());
	}

	public String generateAccessToken(User user) {
		return build(user, TYPE_ACCESS, accessTtl);
	}

	public String generateRefreshToken(User user) {
		return build(user, TYPE_REFRESH, refreshTtl);
	}

	public Duration accessTokenTtl() {
		return accessTtl;
	}

	public Duration refreshTokenTtl() {
		return refreshTtl;
	}

	private String build(User user, String type, Duration ttl) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(user.getId()))
				.claim(CLAIM_ROLE, user.getRole().name())
				.claim(CLAIM_TYPE, type)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(ttl)))
				.signWith(key)
				.compact();
	}

	/**
	 * @return claim bila token valid dan bertipe access; kosong bila tidak.
	 */
	public Claims parseAccessToken(String token) {
		Claims claims = parse(token);
		if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new JwtException("Token bukan access token.");
		}
		return claims;
	}

	public Claims parseRefreshToken(String token) {
		Claims claims = parse(token);
		if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new JwtException("Token bukan refresh token.");
		}
		return claims;
	}

	private Claims parse(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public Long userIdOf(Claims claims) {
		return Long.valueOf(claims.getSubject());
	}

	public String roleOf(Claims claims) {
		return claims.get(CLAIM_ROLE, String.class);
	}

	/** Waktu token diterbitkan. Dipakai untuk menolak token yang sudah dicabut. */
	public Instant issuedAtOf(Claims claims) {
		Date issuedAt = claims.getIssuedAt();
		return issuedAt == null ? Instant.EPOCH : issuedAt.toInstant();
	}
}
