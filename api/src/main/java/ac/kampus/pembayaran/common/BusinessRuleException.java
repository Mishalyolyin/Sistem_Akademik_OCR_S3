package ac.kampus.pembayaran.common;

/**
 * Permintaan valid secara bentuk, tapi melanggar aturan bisnis —
 * misalnya mengubah golongan potongan mahasiswa yang sudah pernah upload bukti.
 * Dipetakan ke HTTP 409 Conflict.
 */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}
}
