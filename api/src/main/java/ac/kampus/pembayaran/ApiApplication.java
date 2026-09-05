package ac.kampus.pembayaran;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Penjadwalan dipakai pengingat jatuh tempo; tanpa anotasi ini @Scheduled
// diabaikan tanpa satu pun peringatan.
@EnableScheduling
@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
