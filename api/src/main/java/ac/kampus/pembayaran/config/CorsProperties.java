package ac.kampus.pembayaran.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Terpetakan dari blok {@code app.cors} di application.yml.
 * Nilai default hanya mengizinkan frontend Next.js lokal.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
