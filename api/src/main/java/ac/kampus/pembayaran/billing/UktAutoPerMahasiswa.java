package ac.kampus.pembayaran.billing;

import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.student.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Pekerjaan satu putaran untuk satu mahasiswa, dalam transaksinya sendiri.
 *
 * <p>Kelas terpisah, bukan method di {@link UktAutoService}. Spring memasang
 * transaksi lewat proxy, dan proxy tidak pernah menangkap panggilan sebuah
 * kelas ke methodnya sendiri — {@code REQUIRES_NEW} di sana akan diam-diam
 * tidak berlaku, dan seluruh putaran kembali jadi satu transaksi raksasa.
 *
 * <p>Akibatnya kalau digabung: satu mahasiswa yang gagal — tarif tahun
 * akademiknya belum diatur, misalnya — akan menandai transaksi sebagai
 * rollback-only, dan tagihan yang sudah terbentuk untuk semua mahasiswa
 * sebelumnya ikut hilang saat putaran selesai.
 */
@Service
@RequiredArgsConstructor
public class UktAutoPerMahasiswa {

	private final PaymentPlanRepository planRepository;
	private final PaymentGenerationService generationService;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public UktAutoService.Hasil kerjakan(Student student, SemesterUkt sekarang) {
		int sudahAda = (int) planRepository.countByStudentIdAndCategoryAndStatusNot(
				student.getId(), PaymentCategory.UKT, PlanStatus.CANCELLED);

		int dibuat = 0;
		List<String> catatan = new ArrayList<>();

		for (int semester = sudahAda + 1;
			 semester <= PaymentGenerationService.MAX_SEMESTER_UKT;
			 semester++) {

			SemesterUkt sasaran;
			try {
				sasaran = SemesterUkt.hitung(
						student.getStartAcademicYear(), student.getStartTerm(), semester);
			} catch (RuntimeException e) {
				catatan.add("%s (%s): %s".formatted(
						student.getName(), student.getNim(), e.getMessage()));
				return new UktAutoService.Hasil(1, dibuat, 0, 1, catatan);
			}

			// Yang belum tiba waktunya berhenti di sini, beserta semua sesudahnya.
			if (!sasaran.sudahWaktunya(sekarang)) {
				break;
			}

			try {
				generationService.generate(
						student, PaymentCategory.UKT, sasaran.academicYear(), sasaran.term());
				dibuat++;
			} catch (RuntimeException e) {
				// Sebabnya ikut dicatat apa adanya: "gagal" tanpa keterangan
				// memaksa admin menebak, dan sebab tersering — tarif tahun itu
				// belum diatur — justru yang paling mudah dibetulkan kalau
				// disebutkan.
				catatan.add("%s (%s) semester %d: %s".formatted(
						student.getName(), student.getNim(), semester, e.getMessage()));
				return new UktAutoService.Hasil(1, dibuat, 0, 1, catatan);
			}
		}

		return new UktAutoService.Hasil(1, dibuat, dibuat == 0 ? 1 : 0, 0, catatan);
	}
}
