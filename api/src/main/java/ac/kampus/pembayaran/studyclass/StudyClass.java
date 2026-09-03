package ac.kampus.pembayaran.studyclass;

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

import java.time.Instant;
import java.util.Locale;

/**
 * Kelas mahasiswa. Jumlahnya tidak dipatok — dibuat otomatis saat import Excel
 * atau ditambah admin lewat menu Kelas.
 */
@Entity
@Table(name = "study_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyClass {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Nama singkat seperti "A", "B", atau "Kerjasama A". */
	@Column(nullable = false, length = 60)
	private String name;

	@Column(nullable = false)
	private boolean kerjasama;

	@Column(name = "academic_year", nullable = false, length = 9)
	private String academicYear;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	/** "Kelas A" atau "Kelas Kerjasama A". */
	public String displayName() {
		return name.toLowerCase(Locale.ROOT).startsWith("kerjasama")
				? "Kelas " + name
				: "Kelas " + name;
	}

	/**
	 * Menebak apakah sebuah nama kelas menandakan kelas kerjasama. Dipakai saat
	 * import Excel membuat kelas baru; admin tetap bisa mengoreksinya lewat menu.
	 */
	public static boolean looksLikeKerjasama(String name) {
		return name != null && name.toLowerCase(Locale.ROOT).contains("kerjasama");
	}
}
