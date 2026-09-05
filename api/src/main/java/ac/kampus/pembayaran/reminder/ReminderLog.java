package ac.kampus.pembayaran.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Jejak satu pengingat.
 *
 * <p>Nomor dan isi pesan disimpan apa adanya saat itu, bukan disusun ulang saat
 * dibaca: nomor mahasiswa bisa berubah dan template pesannya bisa diedit admin,
 * sementara yang dicari saat menelusuri keluhan justru pesan yang benar-benar
 * terkirim.
 */
@Entity
@Table(name = "reminder_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "installment_id", nullable = false)
	private Long installmentId;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "reminder_kind")
	private ReminderKind kind;

	@Column(length = 25)
	private String phone;

	@Column(columnDefinition = "text")
	private String message;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "reminder_status")
	private ReminderStatus status;

	@Column(columnDefinition = "text")
	private String error;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
