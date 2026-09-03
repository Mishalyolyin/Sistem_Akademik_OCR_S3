package ac.kampus.pembayaran.payment.ocr;

/**
 * Pesan yang dikirim ke antrean. Sengaja hanya berisi id, bukan seluruh data
 * pembayaran — supaya pekerja selalu membaca kondisi terbaru dari database,
 * bukan salinan yang mungkin sudah basi saat pesannya diproses.
 */
public record OcrJobMessage(Long paymentId) {
}
