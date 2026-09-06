package ac.kampus.pembayaran.dissertation;

import ac.kampus.pembayaran.student.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Identitas disertasi seorang mahasiswa.
 *
 * <p>Satu baris per mahasiswa, bukan per tagihan. Keempat tahap ujian di
 * Program Doktor mengacu ke disertasi yang sama, jadi menyimpannya per tagihan
 * — seperti sistem S2 — akan menduplikasi judul yang sama empat kali dan
 * membiarkan keempat salinan itu menyimpang tanpa ada yang menyadarinya.
 */
@Entity
@Table(name = "dissertation_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DissertationDetail {

	/** Sama dengan {@code student.id}: satu mahasiswa satu disertasi. */
	@Id
	@Column(name = "student_id")
	private Long studentId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "student_id")
	private Student student;

	@Column(nullable = false, length = 300)
	private String title;

	@Column(nullable = false, length = 150)
	private String promotor;

	@Column(nullable = false, length = 150)
	private String copromotor;

	/** Keduanya boleh kosong: naskahnya menyusul setelah judul ditetapkan. */
	@Column(name = "dissertation_file_path", length = 500)
	private String dissertationFilePath;

	@Column(name = "article_file_path", length = 500)
	private String articleFilePath;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;
}
