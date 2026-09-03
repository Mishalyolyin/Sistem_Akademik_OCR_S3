package ac.kampus.pembayaran.report;

import ac.kampus.pembayaran.common.BusinessRuleException;
import ac.kampus.pembayaran.common.NotFoundException;
import ac.kampus.pembayaran.payment.Payment;
import ac.kampus.pembayaran.payment.PaymentRepository;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Kuitansi pembayaran dalam PDF. Padanan DomPDF di sistem Laravel lama. */
@Component
@RequiredArgsConstructor
public class ReceiptGenerator {

	private static final Color HIJAU = new Color(4, 44, 34);
	private static final Color ABU = new Color(110, 110, 110);
	private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

	private static final DateTimeFormatter TANGGAL =
			DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", new Locale("id", "ID"))
					.withZone(JAKARTA);

	private final PaymentRepository repository;

	@Transactional(readOnly = true)
	public byte[] generate(Long paymentId) {
		Payment payment = repository.findWithDetailsById(paymentId)
				.orElseThrow(() -> NotFoundException.of("Pembayaran", paymentId));

		// Kuitansi hanya untuk uang yang benar-benar sudah diterima.
		if (!payment.getStatus().sudahDiverifikasi()) {
			throw new BusinessRuleException(
					"Kuitansi hanya bisa dicetak untuk pembayaran yang sudah diverifikasi. "
							+ "Status sekarang: " + payment.getStatus() + ".");
		}

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
			PdfWriter.getInstance(doc, out);
			doc.open();

			doc.add(judul("PROGRAM DOKTOR PENDIDIKAN AGAMA ISLAM", 13, HIJAU));
			doc.add(judul("Universitas Islam Sultan Agung", 9, ABU));
			doc.add(spasi(14));
			doc.add(judul("KUITANSI PEMBAYARAN", 11, HIJAU));
			doc.add(spasi(4));
			doc.add(judul("No. " + nomorKuitansi(payment), 8, ABU));
			doc.add(spasi(14));

			PdfPTable tabel = new PdfPTable(2);
			tabel.setWidthPercentage(100);
			tabel.setWidths(new float[] { 38, 62 });

			var student = payment.getStudent();
			baris(tabel, "Nama", student.getName());
			baris(tabel, "NIM", student.getNim());
			baris(tabel, "Kelas",
					student.getStudyClass() == null ? "-" : student.getStudyClass().displayName());

			var plan = payment.getPaymentPlan();
			if (plan != null) {
				baris(tabel, "Kategori", plan.getCategory().label());
				if (plan.getSemesterNumber() != null) {
					baris(tabel, "Semester", String.valueOf(plan.getSemesterNumber()));
				}
				baris(tabel, "Tahun Akademik",
						plan.getAcademicYear() + " " + plan.getTerm());
			}
			if (payment.getInstallment() != null) {
				baris(tabel, "Cicilan ke",
						String.valueOf(payment.getInstallment().getInstallmentNo()));
			}
			baris(tabel, "Bank", payment.getBankName() == null ? "-" : payment.getBankName());
			baris(tabel, "Tanggal Verifikasi",
					payment.getVerifiedAt() == null ? "-" : TANGGAL.format(payment.getVerifiedAt()));

			doc.add(tabel);
			doc.add(spasi(16));

			PdfPTable nominal = new PdfPTable(1);
			nominal.setWidthPercentage(100);
			PdfPCell sel = new PdfPCell(new Phrase(
					rupiah(payment.getAmount()),
					FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, HIJAU)));
			sel.setHorizontalAlignment(Element.ALIGN_CENTER);
			sel.setPadding(12);
			sel.setBackgroundColor(new Color(240, 247, 244));
			sel.setBorderColor(HIJAU);
			nominal.addCell(sel);
			doc.add(nominal);

			doc.add(spasi(6));
			doc.add(judul(terbilang(payment.getAmount()), 8, ABU));
			doc.add(spasi(20));

			doc.add(judul(
					"Kuitansi ini dicetak otomatis oleh sistem dan sah tanpa tanda tangan basah.",
					7, ABU));
			doc.add(judul("Dicetak " + TANGGAL.format(java.time.Instant.now()), 7, ABU));

			doc.close();
			return out.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("Gagal membuat kuitansi PDF.", e);
		}
	}

	private String nomorKuitansi(Payment payment) {
		return "KW-%s-%06d".formatted(
				payment.getStudent().getNim(), payment.getId());
	}

	private Paragraph judul(String teks, float ukuran, Color warna) {
		Paragraph p = new Paragraph(teks, FontFactory.getFont(FontFactory.HELVETICA, ukuran, warna));
		p.setAlignment(Element.ALIGN_CENTER);
		return p;
	}

	private Paragraph spasi(float tinggi) {
		Paragraph p = new Paragraph(" ");
		p.setSpacingAfter(tinggi);
		return p;
	}

	private void baris(PdfPTable tabel, String label, String nilai) {
		Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, ABU);
		Font fontNilai = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);

		PdfPCell kiri = new PdfPCell(new Phrase(label, fontLabel));
		kiri.setBorder(0);
		kiri.setPaddingBottom(5);

		PdfPCell kanan = new PdfPCell(new Phrase(nilai, fontNilai));
		kanan.setBorder(0);
		kanan.setPaddingBottom(5);

		tabel.addCell(kiri);
		tabel.addCell(kanan);
	}

	private String rupiah(BigDecimal nilai) {
		NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
		format.setMaximumFractionDigits(0);
		return format.format(nilai);
	}

	/** Nominal dalam huruf, seperti lazimnya kuitansi resmi. */
	static String terbilang(BigDecimal nilai) {
		long angka = nilai.longValue();
		if (angka == 0) return "Nol rupiah";

		String hasil = eja(angka).trim().replaceAll("\\s+", " ");
		return hasil.substring(0, 1).toUpperCase(Locale.ROOT) + hasil.substring(1) + " rupiah";
	}

	private static String eja(long n) {
		String[] satuan = {
				"", "satu", "dua", "tiga", "empat", "lima",
				"enam", "tujuh", "delapan", "sembilan", "sepuluh", "sebelas" };

		if (n < 12) return satuan[(int) n];
		if (n < 20) return eja(n - 10) + " belas";
		if (n < 100) return eja(n / 10) + " puluh " + eja(n % 10);
		if (n < 200) return "seratus " + eja(n - 100);
		if (n < 1000) return eja(n / 100) + " ratus " + eja(n % 100);
		if (n < 2000) return "seribu " + eja(n - 1000);
		if (n < 1_000_000) return eja(n / 1000) + " ribu " + eja(n % 1000);
		if (n < 1_000_000_000) return eja(n / 1_000_000) + " juta " + eja(n % 1_000_000);
		return eja(n / 1_000_000_000) + " miliar " + eja(n % 1_000_000_000);
	}
}
