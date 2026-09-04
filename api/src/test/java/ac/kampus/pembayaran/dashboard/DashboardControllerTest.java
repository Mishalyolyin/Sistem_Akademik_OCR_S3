package ac.kampus.pembayaran.dashboard;

import ac.kampus.pembayaran.support.ControllerTest;
import ac.kampus.pembayaran.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Angka ringkasan dashboard.
 *
 * <p>Aturan perannya sudah dikunci {@code SecurityLayerTest}, yang memang
 * memakai controller ini sebagai kelinci percobaan. Yang belum: bentuk jawaban
 * suksesnya. Kartu-kartu di layar membaca field ini satu per satu, dan nama
 * field yang berubah membuat angkanya diam-diam jadi kosong tanpa satu pun
 * galat — halamannya tetap terbuka, isinya saja yang bohong.
 */
@ControllerTest(DashboardController.class)
class DashboardControllerTest extends ControllerTestSupport {

	@MockitoBean
	private DashboardRepository repository;

	@BeforeEach
	void setUp() {
		when(repository.countActiveStudents()).thenReturn(42L);
		when(repository.countPaymentsByStatus())
				.thenReturn(new DashboardController.RingkasanStatus(3, 5, 2, 17, 1));
		when(repository.totalBilled()).thenReturn(new BigDecimal("91000000"));
		when(repository.totalCollected()).thenReturn(new BigDecimal("30000000"));
		when(repository.totalWalletBalance()).thenReturn(new BigDecimal("200000"));
		when(repository.summaryByCategory()).thenReturn(List.of(
				new DashboardController.KategoriRingkas("UKT", 6,
						new BigDecimal("60000000"), new BigDecimal("20000000"))));
		when(repository.summaryByClass()).thenReturn(List.of(
				new DashboardController.KelasRingkas("Kelas A", 12,
						new BigDecimal("30000000"), new BigDecimal("10000000"))));
		when(repository.verificationQueue()).thenReturn(List.of(
				new DashboardController.AntreanItem(9L, "Uji Coba", "2612600001", "UKT",
						new BigDecimal("1200000"), "NEEDS_REVIEW",
						new BigDecimal("72.50"), Instant.parse("2026-09-04T02:00:00Z"))));
	}

	@Test
	@DisplayName("ringkasan status dihitung sekali dan dipakai kedua bentuk field")
	void ringkasanStatus() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/dashboard/summary")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status.menungguDibaca").value(3))
				.andExpect(jsonPath("$.status.perluDitinjau").value(5))
				.andExpect(jsonPath("$.status.ditolak").value(2))
				.andExpect(jsonPath("$.status.terverifikasi").value(17))
				.andExpect(jsonPath("$.status.gagalDibaca").value(1))
				// Dua field lama dipertahankan supaya klien lama tetap terbaca,
				// dan nilainya wajib sama dengan yang di dalam `status`.
				.andExpect(jsonPath("$.perluDitinjau").value(5))
				.andExpect(jsonPath("$.gagalDibaca").value(1));

		// Satu kueri GROUP BY, bukan lima kueri untuk pertanyaan yang sama.
		verify(repository).countPaymentsByStatus();
	}

	@Test
	@DisplayName("angka uang dan rincian per kategori serta per kelas ikut terkirim")
	void angkaUang() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/dashboard/summary")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mahasiswaAktif").value(42))
				.andExpect(jsonPath("$.totalTertagih").value(91000000))
				.andExpect(jsonPath("$.totalTerkumpul").value(30000000))
				.andExpect(jsonPath("$.totalSaldoMahasiswa").value(200000))
				.andExpect(jsonPath("$.perKategori[0].kategori").value("UKT"))
				.andExpect(jsonPath("$.perKategori[0].jumlahTagihan").value(6))
				.andExpect(jsonPath("$.perKelas[0].kelas").value("Kelas A"))
				.andExpect(jsonPath("$.perKelas[0].jumlahMahasiswa").value(12));
	}

	@Test
	@DisplayName("antrean verifikasi membawa identitas mahasiswa dan keyakinan OCR-nya")
	void antreanVerifikasi() throws Exception {
		mockMvc.perform(sebagaiAdmin(get("/dashboard/summary")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.antrean[0].paymentId").value(9))
				.andExpect(jsonPath("$.antrean[0].nim").value("2612600001"))
				.andExpect(jsonPath("$.antrean[0].status").value("NEEDS_REVIEW"))
				.andExpect(jsonPath("$.antrean[0].keyakinan").value(72.50));
	}

	@Test
	@DisplayName("mahasiswa tidak boleh melihat angka seluruh kampus")
	void mahasiswaDitolak() throws Exception {
		mockMvc.perform(sebagaiMahasiswa(get("/dashboard/summary")))
				.andExpect(status().isForbidden());

		verify(repository, never()).countPaymentsByStatus();
	}

	@Test
	@DisplayName("tanpa token ditolak 401")
	void tanpaToken() throws Exception {
		mockMvc.perform(get("/dashboard/summary"))
				.andExpect(status().isUnauthorized());
	}
}
