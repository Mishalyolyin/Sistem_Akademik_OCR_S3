package ac.kampus.pembayaran.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;

import java.util.Optional;

public interface StudentRepository
		extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

	Optional<Student> findByNim(String nim);

	/**
	 * Ambil mahasiswa beserta kelasnya. Wajib dipakai kalau hasilnya akan
	 * dipetakan ke DTO di luar transaksi — {@code open-in-view} sengaja
	 * dimatikan, jadi relasi lazy tidak bisa diakses setelah transaksi tutup.
	 */
	@EntityGraph(attributePaths = "studyClass")
	Optional<Student> findWithClassById(Long id);

	boolean existsByNim(String nim);

	Optional<Student> findByUserId(Long userId);

	/** Mahasiswa yang sedang login, beserta kelas dan akunnya. */
	@EntityGraph(attributePaths = { "studyClass", "user" })
	Optional<Student> findWithClassByUserId(Long userId);

	/**
	 * Ambil kelas sekaligus dalam satu query, supaya daftar mahasiswa tidak
	 * memicu satu query tambahan per baris (masalah N+1).
	 */
	@Override
	@EntityGraph(attributePaths = "studyClass")
	Page<Student> findAll(@Nullable Specification<Student> spec, Pageable pageable);
}
