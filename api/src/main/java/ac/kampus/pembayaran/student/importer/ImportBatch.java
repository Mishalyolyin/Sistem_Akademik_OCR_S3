package ac.kampus.pembayaran.student.importer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Riwayat satu kali import Excel, lengkap dengan galat per baris. */
@Entity
@Table(name = "import_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String filename;

	@Column(name = "total_rows", nullable = false)
	private int totalRows;

	@Column(name = "success_rows", nullable = false)
	private int successRows;

	@Column(name = "failed_rows", nullable = false)
	private int failedRows;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	@Builder.Default
	private List<RowError> errors = new ArrayList<>();

	@Column(name = "admin_id")
	private Long adminId;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	/** Nomor baris mengikuti penomoran Excel, jadi admin bisa langsung mencarinya. */
	public record RowError(int row, String nim, String message) {
	}
}
