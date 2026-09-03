package ac.kampus.pembayaran.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BEARER = "bearerAuth";

	@Bean
	public OpenAPI apiDocumentation() {
		return new OpenAPI()
				.info(new Info()
						.title("API Sistem Pembayaran Kampus")
						.version("v1")
						.description("""
								Tagihan mahasiswa, verifikasi bukti bayar berbasis OCR,
								alokasi pembayaran, dan pelaporan.

								Klik Authorize lalu tempel access token dari POST /auth/login.
								"""))
				.addSecurityItem(new SecurityRequirement().addList(BEARER))
				.components(new Components().addSecuritySchemes(BEARER,
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
