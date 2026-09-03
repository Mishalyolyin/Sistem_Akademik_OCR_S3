package ac.kampus.pembayaran.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Terpetakan dari blok {@code app.jwt} di application.yml.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		String secret,
		long accessTokenMinutes,
		long refreshTokenDays
) {
}
