package ac.kampus.pembayaran.tuition;

import ac.kampus.pembayaran.common.PaymentCategory;
import ac.kampus.pembayaran.student.StudentRepository;
import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pengelolaan golongan potongan.
 *
 * <p>Golongan menentukan nominal UKT, jadi menambahnya bukan sekadar menyimpan
 * baris: kodenya dipakai apa adanya di berkas import dan di kunci asing tabel
 * mahasiswa, dan golongan yang masih dipakai tidak boleh hilang dari peredaran.
 */
@ControllerTest(TuitionController.class)
class TuitionControllerTest extends ControllerTestSupport {

	@MockitoBean
	private TuitionRateRepository rateRepository;
	@MockitoBean
	private DiscountTierRateRepository tierRepository;
	@MockitoBean
	private StudentRepository studentRepository;

	@BeforeEach
	void setUp() {
		// Tarif dasar UKT dibutuhkan untuk menghitung kolom ringkasannya.
		when(rateRepository.findByCategoryAndAcademicYearAndActiveTrue(
				PaymentCategory.UKT, "2026/2027"))
				.thenReturn(Optional.of(tarifUkt()));
		when(tierRepository.save(any(DiscountTierRate.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		when(tierRepository.findAll()).thenReturn(List.of());
	}

	private static TuitionRate tarifUkt() {
		TuitionRate rate = new TuitionRate();
		rate.setCategory(PaymentCategory.UKT);
		rate.setAcademicYear("2026/2027");
		rate.setAmount(new BigDecimal("10000000"));
		rate.setActive(true);
		return rate;
	}

	private static DiscountTierRate golongan(String kode, String persen, boolean aktif) {
		return DiscountTierRate.builder()
				.tier(kode).label(kode).percent(new BigDecimal(persen))
				.active(aktif).sortOrder(1)
				.build();
	}

	private static Map<String, Object> permintaanBaru() {
		Map<String, Object> body = new HashMap<>();
		body.put("tier", "MITRA_INSTANSI");
		body.put("label", "Mitra Instansi");
		body.put("percent", 30);
		return body;
	}

	// --- Menambah golongan ---

	@Test
	@DisplayName("golongan baru tersimpan dan langsung ikut terhitung")
	void golonganBaruTersimpan() throws Exception {
		when(tierRepository.existsById("MITRA_INSTANSI")).thenReturn(false);
		when(tierRepository.findAllByOrderBySortOrderAscTierAsc())
				.thenReturn(List.of(golongan("MITRA_INSTANSI", "30", true)));

		mockMvc.perform(sebagaiAdmin(post("/tuition/tiers"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(permintaanBaru())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tier").value("MITRA_INSTANSI"))
				// 10.000.000 dikurangi 30 persen.
				.andExpect(jsonPath("$.uktPerSemester").value(7000000));
	}

	@Test
	@DisplayName("kode dengan spasi atau huruf kecil ditolak, karena dipakai apa adanya di berkas import")
	void kodeHarusRapi() throws Exception {
		for (String kode : new String[] { "mitra instansi", "Mitra-Instansi", "1MITRA" }) {
			Map<String, Object> body = permintaanBaru();
			body.put("tier", kode);

			mockMvc.perform(sebagaiAdmin(post("/tuition/tiers"))
							.contentType(MediaType.APPLICATION_JSON)
							.content(json(body)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.detail", containsString("huruf kapital")));
		}

		verify(tierRepository, never()).save(any());
	}

	@Test
	@DisplayName("kode yang sudah ada ditolak 409, bukan menimpa golongan lama")
	void kodeGandaDitolak() throws Exception {
		when(tierRepository.existsById("MITRA_INSTANSI")).thenReturn(true);

		mockMvc.perform(sebagaiAdmin(post("/tuition/tiers"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(permintaanBaru())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("sudah ada")));

		verify(tierRepository, never()).save(any());
	}

	@Test
	@DisplayName("persentase di luar 0 sampai 100 ditolak")
	void persentaseTidakMasukAkal() throws Exception {
		for (int persen : new int[] { -5, 140 }) {
			Map<String, Object> body = permintaanBaru();
			body.put("percent", persen);

			mockMvc.perform(sebagaiAdmin(post("/tuition/tiers"))
							.contentType(MediaType.APPLICATION_JSON)
							.content(json(body)))
					.andExpect(status().isBadRequest());
		}

		verify(tierRepository, never()).save(any());
	}

	// --- Menonaktifkan golongan ---

	@Test
	@DisplayName("golongan yang masih dipakai mahasiswa tidak boleh dinonaktifkan")
	void golonganTerpakaiTidakBolehDimatikan() throws Exception {
		when(tierRepository.findById("ALUMNI"))
				.thenReturn(Optional.of(golongan("ALUMNI", "25", true)));
		when(studentRepository.countByDiscountTier("ALUMNI")).thenReturn(4L);

		mockMvc.perform(sebagaiAdmin(put("/tuition/tiers/ALUMNI"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("percent", 25, "active", false))))
				.andExpect(status().isConflict())
				// Jumlahnya ikut disebut: admin perlu tahu seberapa besar
				// pekerjaan memindahkan mereka sebelum mencoba lagi.
				.andExpect(jsonPath("$.detail", containsString("masih dipakai 4 mahasiswa")));
	}

	@Test
	@DisplayName("golongan tanpa mahasiswa boleh dinonaktifkan")
	void golonganKosongBolehDimatikan() throws Exception {
		when(tierRepository.findById("MITRA_INSTANSI"))
				.thenReturn(Optional.of(golongan("MITRA_INSTANSI", "30", true)));
		when(studentRepository.countByDiscountTier("MITRA_INSTANSI")).thenReturn(0L);
		when(tierRepository.findAllByOrderBySortOrderAscTierAsc())
				.thenReturn(List.of(golongan("MITRA_INSTANSI", "30", false)));

		mockMvc.perform(sebagaiAdmin(put("/tuition/tiers/MITRA_INSTANSI"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("percent", 30, "active", false))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	@DisplayName("golongan yang tidak ada dijawab 404")
	void golonganTidakAda() throws Exception {
		when(tierRepository.findById(anyString())).thenReturn(Optional.empty());

		mockMvc.perform(sebagaiAdmin(put("/tuition/tiers/TIDAK_ADA"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("percent", 10))))
				.andExpect(status().isNotFound());
	}

	// --- Peran ---

	@Test
	@DisplayName("mahasiswa tidak boleh menambah golongan potongan")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(post("/tuition/tiers"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(permintaanBaru())))
				.andExpect(status().isForbidden());

		verify(tierRepository, never()).save(any());
	}
}
