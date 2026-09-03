package ac.kampus.pembayaran.template;

import ac.kampus.pembayaran.common.AcademicTerm;
import ac.kampus.pembayaran.common.PaymentCategory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

/**
 * Jadwal angsuran untuk satu kategori dan satu term. Bulan disimpan sebagai
 * offset dari awal term, jadi template yang sama dipakai ulang tiap tahun.
 */
@Entity
@Table(name = "installment_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "payment_category")
	private PaymentCategory category;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "term", nullable = false, columnDefinition = "academic_term")
	private AcademicTerm term;

	@Column(name = "installments_count", nullable = false)
	private int installmentsCount;

	@Column(nullable = false)
	private boolean active;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "installment_template_id")
	@OrderBy("installmentNo ASC")
	@Builder.Default
	private List<InstallmentTemplateItem> items = new ArrayList<>();

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;
}
