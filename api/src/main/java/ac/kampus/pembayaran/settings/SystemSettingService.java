package ac.kampus.pembayaran.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Pembaca pengaturan sistem. Nilai diambil dari database supaya admin bisa
 * mengubah ambang batas OCR tanpa deploy ulang.
 */
@Service
@RequiredArgsConstructor
public class SystemSettingService {

	public static final String OCR_CONFIDENCE_THRESHOLD = "ocr_confidence_threshold";
	public static final String OCR_AUTO_REJECT_THRESHOLD = "ocr_auto_reject_threshold";
	public static final String PAYMENT_TOLERANCE_AMOUNT = "payment_tolerance_amount";
	public static final String OCR_BLACKLIST_KEYWORDS = "ocr_blacklist_keywords";
	public static final String OCR_DATE_VALIDATION_DAYS = "ocr_date_validation_days";
	public static final String BANK_ACCOUNT_NUMBER = "bank_account_number";

	private final SystemSettingRepository repository;

	@Transactional(readOnly = true)
	public Optional<String> get(String key) {
		return repository.findById(key)
				.map(SystemSetting::getValue)
				.filter(value -> value != null && !value.isBlank());
	}

	@Transactional(readOnly = true)
	public String getOrDefault(String key, String fallback) {
		return get(key).orElse(fallback);
	}

	/** Persentase 0–100 dibaca sebagai pecahan 0–1, sesuai keluaran OCR. */
	@Transactional(readOnly = true)
	public double getPercentAsFraction(String key, double fallback) {
		return get(key)
				.map(value -> {
					try {
						return Double.parseDouble(value) / 100.0;
					} catch (NumberFormatException e) {
						return fallback;
					}
				})
				.orElse(fallback);
	}

	@Transactional(readOnly = true)
	public BigDecimal getAmount(String key, BigDecimal fallback) {
		return get(key)
				.map(value -> {
					try {
						return new BigDecimal(value);
					} catch (NumberFormatException e) {
						return fallback;
					}
				})
				.orElse(fallback);
	}

	@Transactional(readOnly = true)
	public Optional<Integer> getInteger(String key) {
		return get(key).flatMap(value -> {
			try {
				return Optional.of(Integer.parseInt(value));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		});
	}

	@Transactional(readOnly = true)
	public List<SystemSetting> all() {
		return repository.findAllByOrderByKeyAsc();
	}

	@Transactional
	public SystemSetting set(String key, String value) {
		SystemSetting setting = repository.findById(key)
				.orElseGet(() -> new SystemSetting(key, null, null, null));
		setting.setValue(value);
		return repository.save(setting);
	}
}
