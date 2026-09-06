package ac.kampus.pembayaran.payment.ocr;

import ac.kampus.pembayaran.student.StudentDocument;

/**
 * Pekerjaan membaca satu dokumen wajib milik satu mahasiswa.
 *
 * <p>Hanya id dan jenisnya, bukan path berkasnya: mahasiswa bisa mengunggah
 * ulang sebelum pesannya sempat diproses, dan yang harus dibaca adalah berkas
 * terbaru — bukan yang sudah diganti.
 */
public record DocumentOcrJobMessage(Long studentId, StudentDocument jenis) {
}
