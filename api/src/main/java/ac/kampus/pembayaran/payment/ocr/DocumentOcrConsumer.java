package ac.kampus.pembayaran.payment.ocr;

import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentDocument;
import ac.kampus.pembayaran.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Membaca satu dokumen wajib mahasiswa lalu menyimpan hasilnya.
 *
 * <p>Bedanya dari pembacaan bukti bayar: di sini <b>tidak ada keputusan apa
 * pun</b>. Tidak ada dokumen yang diterima atau ditolak otomatis, dan tidak ada
 * gate yang terbuka atau tertutup karenanya. Tesseract tidak bisa memeriksa
 * hologram, stempel, atau tanda tangan — ia hanya bisa membaca tulisan. Yang
 * disajikan adalah bahan supaya admin memeriksa dengan mata yang sudah
 * diarahkan: NIK yang terbaca cocok atau tidak dengan yang diketik, nama di
 * ijazah cocok atau tidak dengan nama mahasiswa.
 *
 * <p>Satu-satunya nilai yang ditulis balik adalah tempat dan tanggal lahir dari
 * ijazah, karena keduanya memang tidak punya isian manual di mana pun.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentOcrConsumer {

	private final StudentRepository studentRepository;
	private final FileStorageService storage;
	private final OcrClient ocrClient;

	@RabbitListener(queues = RabbitConfig.QUEUE_DOKUMEN)
	@Transactional
	public void handle(DocumentOcrJobMessage message) {
		Student student = studentRepository.findById(message.studentId()).orElse(null);
		if (student == null) {
			log.warn("Mahasiswa {} tidak ditemukan, pembacaan dokumen dilewati.",
					message.studentId());
			return;
		}

		StudentDocument jenis = message.jenis();
		String path = jenis.pathOf(student);
		if (path == null || path.isBlank()) {
			log.warn("Mahasiswa {} tidak punya berkas {}, pembacaan dilewati.",
					student.getId(), jenis);
			return;
		}

		Path berkas = storage.resolve(path);
		if (!Files.exists(berkas)) {
			// Bukan alasan untuk mencoba lagi: berkasnya memang tidak ada, dan
			// percobaan berikutnya akan menemukan hal yang sama.
			log.warn("Berkas {} mahasiswa {} tidak ada di penyimpanan.", jenis, student.getId());
			return;
		}

		log.info("Membaca {} mahasiswa {}", jenis, student.getId());

		Map<String, Object> hasil = ocrClient.read(
				berkas,
				OcrClient.OcrRequest.forDocument(jenis.kode(), student.getName()));

		simpan(student, jenis, hasil);
		studentRepository.save(student);
	}

	private void simpan(Student student, StudentDocument jenis, Map<String, Object> mentah) {
		// Service OCR ikut mengirim gambar hasil praprosesnya sebagai base64.
		// Untuk dokumen wajib gambar itu tidak disimpan — nilainya hanya sebagai
		// bahan dataset, dan yang dilatih adalah pembacaan bukti bayar, bukan
		// KTP. Yang penting ia tidak ikut mengendap puluhan kilobyte per
		// mahasiswa di kolom jsonb, terkirim tiap halaman forensik dibuka.
		Map<String, Object> hasil = new java.util.HashMap<>(mentah);
		hasil.remove("processed_image_b64");

		switch (jenis) {
			case FOTO -> student.setProfilePictureAnalysis(hasil);
			case KTP -> student.setKtpOcrData(hasil);
			case KK -> student.setKkOcrData(hasil);
			case IJAZAH -> {
				student.setIjazahOcrData(hasil);
				terapkanKelahiran(student, hasil);
			}
		}
	}

	/**
	 * Tempat dan tanggal lahir dari ijazah.
	 *
	 * <p>Hanya diisi bila masih kosong: sekali seorang admin membetulkannya
	 * dengan mata sendiri, pembacaan ulang tidak boleh menimpanya lagi dengan
	 * tebakan mesin.
	 */
	private void terapkanKelahiran(Student student, Map<String, Object> hasil) {
		Object tempat = hasil.get("extracted_birth_place");
		if (student.getBirthPlace() == null && tempat instanceof String teks && !teks.isBlank()) {
			student.setBirthPlace(teks.trim());
		}

		Object tanggal = hasil.get("extracted_birth_date");
		if (student.getBirthDate() == null && tanggal instanceof String teks && !teks.isBlank()) {
			try {
				student.setBirthDate(LocalDate.parse(teks.trim()));
			} catch (DateTimeParseException e) {
				// Tanggal yang tidak terbaca bentuknya bukan kegagalan pekerjaan:
				// hasil mentahnya tetap tersimpan dan admin bisa membacanya.
				log.info("Tanggal lahir \"{}\" pada ijazah mahasiswa {} tidak berbentuk tanggal.",
						teks, student.getId());
			}
		}
	}
}
