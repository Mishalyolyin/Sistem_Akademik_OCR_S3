package ac.kampus.pembayaran.dissertation;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.payment.FileStorageService;
import ac.kampus.pembayaran.student.Student;
import ac.kampus.pembayaran.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Identitas disertasi: judul, kedua promotor, dan naskahnya.
 *
 * <p>Yang paling dijaga di sini adalah pemisahan antara identitas dan berkas.
 * Judul dan promotor ditetapkan jauh sebelum naskahnya jadi; kalau keduanya
 * dipaksa diisi sekaligus, tidak ada seorang pun bisa mendaftar Seminar
 * Proposal sampai disertasinya hampir selesai — persis kebalikan dari urutan
 * yang sebenarnya terjadi.
 */
class DissertationServiceTest {

	private DissertationRepository repository;
	private StudentRepository studentRepository;
	private FileStorageService storage;
	private DissertationService service;

	private static final Long MAHASISWA = 3L;
	private static final String JUDUL =
			"Implementasi Nilai Pendidikan Agama Islam di Pesantren Modern";

	@BeforeEach
	void setUp() {
		repository = mock(DissertationRepository.class);
		studentRepository = mock(StudentRepository.class);
		storage = mock(FileStorageService.class);
		service = new DissertationService(repository, studentRepository, storage);

		when(repository.save(any(DissertationDetail.class)))
				.thenAnswer((Answer<DissertationDetail>) inv -> inv.getArgument(0));
		when(studentRepository.findById(MAHASISWA)).thenReturn(Optional.of(
				Student.builder().id(MAHASISWA).name("Uji Coba").build()));
		when(repository.findById(anyLong())).thenReturn(Optional.empty());
	}

	private DissertationDetail sudahAda() {
		DissertationDetail detail = DissertationDetail.builder()
				.studentId(MAHASISWA)
				.title(JUDUL)
				.promotor("Prof. Ahmad")
				.copromotor("Dr. Siti")
				.build();
		when(repository.findById(MAHASISWA)).thenReturn(Optional.of(detail));
		return detail;
	}

	private static MockMultipartFile pdf(String nama) {
		return new MockMultipartFile("file", nama, "application/pdf", "%PDF-palsu".getBytes());
	}

	@Test
	@DisplayName("identitas tersimpan, spasi berlebih dibuang")
	void simpanIdentitas() {
		DissertationDetail hasil = service.simpanIdentitas(
				MAHASISWA, "  " + JUDUL + "  ", " Prof. Ahmad ", " Dr. Siti ");

		assertThat(hasil.getTitle()).isEqualTo(JUDUL);
		assertThat(hasil.getPromotor()).isEqualTo("Prof. Ahmad");
		assertThat(hasil.getCopromotor()).isEqualTo("Dr. Siti");
	}

	@Test
	@DisplayName("judul yang terlalu pendek ditolak")
	void judulPendek() {
		assertThatThrownBy(() -> service.simpanIdentitas(
				MAHASISWA, "Disertasi", "Prof. Ahmad", "Dr. Siti"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("minimal 10 karakter");

		assertThatThrownBy(() -> service.simpanIdentitas(
				MAHASISWA, null, "Prof. Ahmad", "Dr. Siti"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("kedua promotor wajib diisi")
	void promotorWajib() {
		assertThatThrownBy(() -> service.simpanIdentitas(MAHASISWA, JUDUL, "", "Dr. Siti"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("promotor");

		assertThatThrownBy(() -> service.simpanIdentitas(MAHASISWA, JUDUL, "Prof. Ahmad", " "))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("ko-promotor");
	}

	@Test
	@DisplayName("promotor dan ko-promotor tidak boleh orang yang sama")
	void promotorSama() {
		// Salah tempel nama adalah kekeliruan yang paling mudah terjadi di
		// formulir dua kolom, dan tidak ada yang memeriksanya belakangan.
		assertThatThrownBy(() -> service.simpanIdentitas(
				MAHASISWA, JUDUL, "Prof. Ahmad", "prof. ahmad"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("tidak boleh orang yang sama");
	}

	@Test
	@DisplayName("identitas boleh diperbarui, judul memang lazim berubah")
	void identitasBisaDiubah() {
		sudahAda();

		DissertationDetail hasil = service.simpanIdentitas(
				MAHASISWA, JUDUL + " Berbasis Kitab Kuning", "Prof. Ahmad", "Dr. Budi");

		assertThat(hasil.getTitle()).endsWith("Berbasis Kitab Kuning");
		assertThat(hasil.getCopromotor()).isEqualTo("Dr. Budi");
		// Mahasiswanya tidak dicari ulang: barisnya sudah ada.
		verify(studentRepository, never()).findById(anyLong());
	}

	@Test
	@DisplayName("mahasiswa yang tidak ada dijawab 404")
	void mahasiswaTidakAda() {
		when(studentRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.simpanIdentitas(
				99L, JUDUL, "Prof. Ahmad", "Dr. Siti"))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	@DisplayName("naskah tersimpan dan pathnya menempel di kolom yang benar")
	void unggahNaskah() {
		sudahAda();
		when(storage.store(any(), any(), anyLong())).thenReturn("disertasi/naskah/x.pdf");

		DissertationDetail hasil = service.unggahBerkas(
				MAHASISWA, DissertationService.JenisBerkas.DISERTASI, pdf("naskah.pdf"));

		assertThat(hasil.getDissertationFilePath()).isEqualTo("disertasi/naskah/x.pdf");
		assertThat(hasil.getArticleFilePath()).isNull();
	}

	@Test
	@DisplayName("artikel tersimpan terpisah dari naskah disertasi")
	void unggahArtikel() {
		DissertationDetail detail = sudahAda();
		detail.setDissertationFilePath("disertasi/naskah/lama.pdf");
		when(storage.store(any(), any(), anyLong())).thenReturn("disertasi/artikel/y.pdf");

		DissertationDetail hasil = service.unggahBerkas(
				MAHASISWA, DissertationService.JenisBerkas.ARTIKEL, pdf("artikel.pdf"));

		assertThat(hasil.getArticleFilePath()).isEqualTo("disertasi/artikel/y.pdf");
		// Naskah yang sudah ada tidak boleh ikut tertimpa.
		assertThat(hasil.getDissertationFilePath()).isEqualTo("disertasi/naskah/lama.pdf");
	}

	@Test
	@DisplayName("naskah selain PDF ditolak")
	void bukanPdf() {
		sudahAda();

		assertThatThrownBy(() -> service.unggahBerkas(
				MAHASISWA, DissertationService.JenisBerkas.DISERTASI,
				new MockMultipartFile("file", "naskah.docx", "application/msword", "x".getBytes())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("PDF");

		verify(storage, never()).store(any(), any(), anyLong());
	}

	@Test
	@DisplayName("naskah tanpa identitas ditolak, bukan tersimpan jadi berkas yatim")
	void unggahTanpaIdentitas() {
		assertThatThrownBy(() -> service.unggahBerkas(
				MAHASISWA, DissertationService.JenisBerkas.DISERTASI, pdf("naskah.pdf")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Isi dulu judul disertasi");

		verify(storage, never()).store(any(), any(), anyLong());
	}

	@Test
	@DisplayName("sudahDiisi hanya melihat identitas, bukan naskahnya")
	void sudahDiisiTidakButuhNaskah() {
		// Inilah yang membuat mahasiswa bisa mendaftar Seminar Proposal jauh
		// sebelum naskahnya ada.
		when(repository.existsByStudentId(MAHASISWA)).thenReturn(true);

		assertThat(service.sudahDiisi(MAHASISWA)).isTrue();
	}
}
