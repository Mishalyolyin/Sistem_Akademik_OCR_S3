package ac.kampus.pembayaran.support;

import ac.kampus.pembayaran.auth.JwtAuthenticationFilter;
import ac.kampus.pembayaran.auth.JwtService;
import ac.kampus.pembayaran.common.GlobalExceptionHandler;
import ac.kampus.pembayaran.config.ProblemDetailErrorResponder;
import ac.kampus.pembayaran.config.SecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Menyalakan satu controller beserta lapisan keamanan yang sesungguhnya.
 *
 * <p>Rantai filter sengaja ikut dimuat, bukan dimatikan seperti kebiasaan pada
 * uji controller. Sebagian besar aturan yang penting di lapisan ini justru ada
 * di sana: peran yang boleh masuk, token yang ditolak, dan bentuk badan jawaban
 * penolakan. Mematikannya membuat test lolos untuk endpoint yang sebenarnya
 * terbuka lebar.
 *
 * <p>Rahasia JWT di sini hanya untuk uji; tidak ada kaitannya dengan yang dipakai
 * saat berjalan sungguhan.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
		ProblemDetailErrorResponder.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = {
		"app.jwt.secret=rahasia-uji-coba-minimal-32-karakter-untuk-hs256",
		"app.jwt.access-token-minutes=15",
		"app.jwt.refresh-token-days=7",
		"app.cors.allowed-origins=http://localhost:3000",
		"app.cookie.secure=false",
})
public @interface ControllerTest {

	/** Controller yang diuji. */
	@AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
	Class<?>[] value() default {};
}
