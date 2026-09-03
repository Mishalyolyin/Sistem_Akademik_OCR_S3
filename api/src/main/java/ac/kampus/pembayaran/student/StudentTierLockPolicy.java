package ac.kampus.pembayaran.student;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Golongan potongan terkunci begitu mahasiswa pernah mengunggah bukti bayar —
 * termasuk unggahan yang ditolak, karena itu tetap menandakan mahasiswa sudah
 * mengacu ke nominal lama.
 *
 * <p>Koreksi golongan yang baru ketahuan belakangan tidak lewat jalur ini,
 * melainkan lewat Penyesuaian atau ubah nominal cicilan, yang keduanya
 * meninggalkan jejak audit.
 */
@Component
@RequiredArgsConstructor
public class StudentTierLockPolicy {

	private final PaymentRepository paymentRepository;

	public boolean hasUploadedProof(Long studentId) {
		return studentId != null && paymentRepository.existsByStudentId(studentId);
	}

	public void assertTierChangeAllowed(Student student) {
		if (hasUploadedProof(student.getId())) {
			throw new BusinessRuleException(
					("Golongan %s tidak bisa diubah karena mahasiswa sudah pernah mengunggah "
							+ "bukti bayar. Pakai fitur Penyesuaian atau ubah nominal cicilan "
							+ "bila perlu koreksi.")
							.formatted(student.getName()));
		}
	}
}
