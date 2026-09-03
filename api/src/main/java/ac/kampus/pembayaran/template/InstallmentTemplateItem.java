package ac.kampus.pembayaran.template;

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

@Entity
@Table(name = "installment_template_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentTemplateItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "installment_no", nullable = false)
	private int installmentNo;

	/** 0 = bulan pertama term (September untuk Gasal, Februari untuk Genap). */
	@Column(name = "month_offset", nullable = false)
	private int monthOffset;

	@Column(name = "due_day", nullable = false)
	private int dueDay;
}
