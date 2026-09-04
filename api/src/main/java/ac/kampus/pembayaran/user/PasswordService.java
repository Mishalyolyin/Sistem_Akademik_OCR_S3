package ac.kampus.pembayaran.user;

import ac.kampus.pembayaran.common.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Penggantian kata sandi, satu tempat untuk semua peran.
 *
 * <p>Aturan yang berlaku untuk siapa pun ada di sini. Aturan khusus satu peran
 * — misalnya kata sandi mahasiswa tidak boleh sama dengan NIM — diperiksa
 * pemanggilnya sebelum memanggil kelas ini.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

	static final int PANJANG_MINIMAL = 8;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Mengganti kata sandi setelah memastikan yang lama benar.
	 *
	 * <p>Seluruh sesi lain ikut dicabut. Mengganti kata sandi biasanya justru
	 * dilakukan karena curiga ada yang tahu; membiarkan refresh token lama tetap
	 * sah selama tujuh hari membuat penggantiannya nyaris tidak ada gunanya.
	 */
	@Transactional
	public void ubah(User user, String lama, String baru) {
		if (lama == null || !passwordEncoder.matches(lama, user.getPasswordHash())) {
			throw new BusinessRuleException("Kata sandi lama salah.");
		}
		if (baru == null || baru.length() < PANJANG_MINIMAL) {
			throw new BusinessRuleException(
					"Kata sandi baru minimal %d karakter.".formatted(PANJANG_MINIMAL));
		}
		if (baru.equals(lama)) {
			throw new BusinessRuleException(
					"Kata sandi baru sama dengan yang lama. Pilih yang lain.");
		}

		user.setPasswordHash(passwordEncoder.encode(baru));
		user.setTokensValidFrom(Instant.now());
		userRepository.save(user);

		log.info("Pengguna {} mengganti kata sandi; sesi lain dicabut.", user.getEmail());
	}
}
