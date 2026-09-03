package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.common.PaymentCategory;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Locale;

/**
 * Filter opsional untuk daftar pembayaran. Memakai Specification, bukan pola
 * {@code (:param IS NULL OR ...)} di JPQL, karena pola itu gagal di PostgreSQL
 * saat parameternya bernilai null.
 */
public final class PaymentSpecifications {

	private PaymentSpecifications() {
	}

	public static Specification<Payment> hasStatus(List<PaymentStatus> statuses) {
		if (statuses == null || statuses.isEmpty()) return null;
		return (root, query, cb) -> root.get("status").in(statuses);
	}

	public static Specification<Payment> inCategory(PaymentCategory category) {
		if (category == null) return null;
		return (root, query, cb) ->
				cb.equal(root.join("paymentPlan", JoinType.INNER).get("category"), category);
	}

	public static Specification<Payment> studentMatches(String keyword) {
		if (keyword == null || keyword.isBlank()) return null;
		String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

		return (root, query, cb) -> {
			var student = root.join("student", JoinType.INNER);
			return cb.or(
					cb.like(cb.lower(student.get("name")), pattern),
					cb.like(cb.lower(student.get("nim")), pattern));
		};
	}

	public static Specification<Payment> forStudent(Long studentId) {
		if (studentId == null) return null;
		return (root, query, cb) -> cb.equal(root.get("student").get("id"), studentId);
	}
}
