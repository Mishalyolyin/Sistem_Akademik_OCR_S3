package ac.kampus.pembayaran.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {

	@Id
	@Column(name = "key", length = 80)
	private String key;

	@Column(columnDefinition = "text")
	private String value;

	@Column(length = 300)
	private String description;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;
}
