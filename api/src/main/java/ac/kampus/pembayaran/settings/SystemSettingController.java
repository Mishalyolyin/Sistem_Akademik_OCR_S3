package ac.kampus.pembayaran.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Pengaturan Sistem")
public class SystemSettingController {

	private final SystemSettingService service;

	public record SettingResponse(
			String key,
			String value,
			String description,
			Instant updatedAt
	) {
		static SettingResponse from(SystemSetting setting) {
			return new SettingResponse(
					setting.getKey(), setting.getValue(),
					setting.getDescription(), setting.getUpdatedAt());
		}
	}

	public record UpdateRequest(
			@Size(max = 2000, message = "Nilai maksimal 2000 karakter.")
			String value
	) {
	}

	@GetMapping
	@Operation(summary = "Semua pengaturan sistem beserta keterangannya")
	public List<SettingResponse> all() {
		return service.all().stream().map(SettingResponse::from).toList();
	}

	@PutMapping("/{key}")
	@Operation(summary = "Ubah satu pengaturan")
	public SettingResponse update(
			@PathVariable String key, @Valid @RequestBody UpdateRequest request) {

		return SettingResponse.from(service.set(key, request.value()));
	}
}
