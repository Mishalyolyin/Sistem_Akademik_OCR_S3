package ac.kampus.pembayaran.payment;

import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Penyaji gambar bukti bayar.
 *
 * <p>Bukti transfer memuat nama, nominal, dan nomor rekening. Berkasnya tidak
 * ditaruh di folder publik justru supaya setiap permintaan melewati pemeriksaan
 * hak akses — dan pemeriksaan itulah yang dikunci di sini. Kalau lepas, siapa
 * pun yang punya akun bisa membaca bukti orang lain hanya dengan mengganti
 * angka di URL.
 */
@ControllerTest(ProofFileController.class)
class ProofFileControllerTest extends ControllerTestSupport {

	/** Id mahasiswa pemilik bukti, berbeda dari id pengguna yang memegang token. */
	private static final long ID_PEMILIK = 1L;
	private static final long ID_ORANG_LAIN = 2L;

	@TempDir
	Path folderBukti;

	@MockitoBean
	private PaymentRepository repository;
	@MockitoBean
	private FileStorageService storage;
	@MockitoBean
	private StudentRepository studentRepository;

	private void adaBuktiMilik(long studentId) throws IOException {
		Path berkas = folderBukti.resolve("bukti.png");
		Files.write(berkas, new byte[] { 1, 2, 3 });

		Payment payment = Payment.builder()
				.id(7L)
				.student(Student.builder().id(studentId).nim("2612600001").name("Pemilik").build())
				.amount(new BigDecimal("1200000"))
				.proofFilePath("bukti.png")
				.status(PaymentStatus.VERIFIED)
				.build();

		when(repository.findById(7L)).thenReturn(Optional.of(payment));
		when(storage.resolve(anyString())).thenReturn(berkas);
	}

	/** Mengaitkan pengguna pemegang token ke satu baris mahasiswa. */
	private void tokenMahasiswaMilik(long studentId) {
		when(studentRepository.findByUserId(ID_MAHASISWA)).thenReturn(Optional.of(
				Student.builder().id(studentId).nim("2612600001").name("Pemegang token").build()));
	}

	@Test
	@DisplayName("mahasiswa boleh membuka bukti bayarnya sendiri")
	void pemilikBoleh() throws Exception {
		adaBuktiMilik(ID_PEMILIK);
		tokenMahasiswaMilik(ID_PEMILIK);

		mockMvc.perform(sebagaiMahasiswa(get("/payments/7/proof")))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/png"));
	}

	@Test
	@DisplayName("mahasiswa lain ditolak 403 walau menebak nomor pembayaran yang benar")
	void bukanPemilikDitolak() throws Exception {
		adaBuktiMilik(ID_PEMILIK);
		tokenMahasiswaMilik(ID_ORANG_LAIN);

		mockMvc.perform(sebagaiMahasiswa(get("/payments/7/proof")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("akun mahasiswa yang tidak terkait baris mahasiswa mana pun ditolak 403")
	void tanpaBarisMahasiswaDitolak() throws Exception {
		adaBuktiMilik(ID_PEMILIK);
		when(studentRepository.findByUserId(ID_MAHASISWA)).thenReturn(Optional.empty());

		mockMvc.perform(sebagaiMahasiswa(get("/payments/7/proof")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("admin boleh membuka bukti siapa pun, karena tugasnya memang memverifikasi")
	void adminBoleh() throws Exception {
		adaBuktiMilik(ID_PEMILIK);

		mockMvc.perform(sebagaiAdmin(get("/payments/7/proof")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("gambar disajikan tanpa cache bersama, karena isinya data pribadi")
	void cacheTidakDibagikan() throws Exception {
		adaBuktiMilik(ID_PEMILIK);

		mockMvc.perform(sebagaiAdmin(get("/payments/7/proof")))
				.andExpect(header().string("Cache-Control",
						org.hamcrest.Matchers.containsString("private")));
	}

	@Test
	@DisplayName("pembayaran yang tidak ada dijawab 404")
	void pembayaranTidakAda() throws Exception {
		when(repository.findById(7L)).thenReturn(Optional.empty());

		mockMvc.perform(sebagaiAdmin(get("/payments/7/proof")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("baris pembayaran ada tapi berkasnya hilang dijawab 404, bukan 500")
	void berkasHilang() throws Exception {
		adaBuktiMilik(ID_PEMILIK);
		when(storage.resolve(anyString())).thenReturn(folderBukti.resolve("tidak-ada.png"));

		mockMvc.perform(sebagaiAdmin(get("/payments/7/proof")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get("/payments/7/proof"))
				.andExpect(status().isUnauthorized());
	}
}
