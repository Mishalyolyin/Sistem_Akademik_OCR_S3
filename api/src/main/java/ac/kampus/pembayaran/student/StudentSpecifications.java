package ac.kampus.pembayaran.student;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Filter opsional untuk daftar mahasiswa.
 *
 * <p>Sengaja memakai Specification, bukan satu JPQL dengan pola
 * {@code (:param IS NULL OR ...)}. Pola itu gagal di PostgreSQL karena driver
 * tidak bisa menebak tipe parameter yang bernilai null, sehingga muncul
 * "function lower(bytea) does not exist". Specification membangun query
 * hanya dari filter yang benar-benar diisi.
 */
public final class StudentSpecifications {

	private StudentSpecifications() {
	}

	public static Specification<Student> nameOrNimContains(String keyword) {
		if (keyword == null || keyword.isBlank()) return null;
		String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

		return (root, query, cb) -> {
			Predicate byName = cb.like(cb.lower(root.get("name")), pattern);
			Predicate byNim = cb.like(cb.lower(root.get("nim")), pattern);
			return cb.or(byName, byNim);
		};
	}

	public static Specification<Student> inClass(Long classId) {
		if (classId == null) return null;
		return (root, query, cb) -> cb.equal(root.get("studyClass").get("id"), classId);
	}

	public static Specification<Student> hasTier(String tier) {
		if (tier == null || tier.isBlank()) return null;
		return (root, query, cb) -> cb.equal(root.get("discountTier"), tier);
	}

	public static Specification<Student> isActive(Boolean active) {
		if (active == null) return null;
		return (root, query, cb) -> cb.equal(root.get("active"), active);
	}
}
