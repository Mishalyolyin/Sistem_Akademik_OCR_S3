package ac.kampus.pembayaran.student.self;

import ac.kampus.pembayaran.billing.PaymentGenerationService;
import ac.kampus.pembayaran.billing.PaymentPlanRepository;
import ac.kampus.pembayaran.payment.PaymentRepository;
import ac.kampus.pembayaran.payment.PaymentService;
import ac.kampus.pembayaran.student.DiscountTier;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lapisan HTTP portal mahasiswa.
 *
 * <p>Ini satu-satunya bagian sistem yang dibuka untuk pengguna di luar staf,
 * jadi yang dijaga di sini adalah batas wewenangnya: admin tidak boleh masuk
 * lewat pintu ini, dan mahasiswa tidak boleh mendaftarkan tagihan yang menjadi
 * wewenang admin.
 */
@ControllerTest(StudentSelfController.class)
class StudentSelfControllerTest extends ControllerTestSupport {

	@MockitoBean
	private StudentSelfService selfService;
	@MockitoBean
	private PaymentGenerationService generationService;
	@MockitoBean
	private PaymentPlanRepository planRepository;
	@MockitoBean
	private PaymentRepository paymentRepository;
	@MockitoBean
	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		when(selfService.current()).thenReturn(mahasiswaDokumenLengkap());
		when(planRepository.findByStudentIdOrderByCategoryAscSemesterNumberAsc(any()))
				.thenReturn(List.of());
	}

	/** Mahasiswa yang seluruh dokumen wajibnya sudah terisi. */
	private static Student mahasiswaDokumenLengkap() {
		return Student.builder()
				.id(1L).nim("2612600001").name("Uji Coba")
				.discountTier(DiscountTier.NON_ALUMNI)
				.walletBalance(BigDecimal.ZERO).active(true)
				.profilePicture("foto.png")
				.nik("3201010101010001").ktpFilePath("ktp.png")
				.kkNumber("3201010101010002").kkFilePath("kk.png")
				.ijazahFilePath("ijazah.png")
				.address("Jalan Contoh Nomor Satu, Bandung")
				.build();
	}

	// --- Batas wewenang ---

	@Test
	@DisplayName("admin tidak boleh masuk lewat pintu portal mahasiswa")
	void adminDitolak() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/me/profil")))
				.andExpect(status().isForbidden());

		verify(selfService, never()).current();
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get("/me/profil"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("mahasiswa boleh membuka profilnya sendiri")
	void mahasiswaBolehLihatProfil() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/me/profil")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nim").value("2612600001"));
	}

	// --- Tagihan yang boleh didaftarkan sendiri ---

	@Test
	@DisplayName("UKT tidak bisa didaftarkan sendiri, itu wewenang admin")
	void uktTidakBisaDidaftarkanSendiri() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/me/tagihan"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("category", "UKT"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("dibuat oleh admin")));

		verify(generationService, never()).generate(any(), any(), anyString(), any());
	}

	@Test
	@DisplayName("kategori yang tidak dikenal ditolak 400, bukan 500")
	void kategoriNgawur() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/me/tagihan"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("category", "SULTAN"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("PENDAFTARAN")));
	}

	@Test
	@DisplayName("dokumen wajib belum lengkap menahan pendaftaran tagihan")
	void dokumenBelumLengkap() throws Exception {
		Student belumLengkap = mahasiswaDokumenLengkap();
		belumLengkap.setIjazahFilePath(null);
		when(selfService.current()).thenReturn(belumLengkap);

		mockMvc.perform(sebagaiMahasiswa(post("/me/tagihan"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("category", "PENDAFTARAN"))))
				.andExpect(status().isConflict());

		verify(generationService, never()).generate(any(), any(), anyString(), any());
	}

	// --- Ganti kata sandi ---

	@Test
	@DisplayName("kata sandi baru yang terlalu pendek ditolak 400")
	void sandiTerlaluPendek() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/me/kata-sandi"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("lama", "2612600001", "baru", "pendek"))))
				.andExpect(status().isBadRequest());

		verify(selfService, never()).gantiKataSandi(anyString(), anyString());
	}

	// --- Alamat ---

	@Test
	@DisplayName("alamat yang terlalu pendek ditolak 400 dengan keterangan")
	void alamatTerlaluPendek() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/me/dokumen/alamat"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("alamat", "Jl. A"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}
}
