package ac.kampus.pembayaran.reminder;

/**
 * Pengirim pesan WhatsApp.
 *
 * <p>Dipisah jadi antarmuka bukan demi kerapian: aturan siapa yang perlu
 * diingatkan bisa dibangun dan diuji penuh tanpa gateway mana pun, dan
 * penggantian penyedia nanti tidak menyentuh satu baris pun aturan bisnisnya.
 */
public interface WhatsAppSender {

	/**
	 * @throws WhatsAppException bila gateway menolak atau tidak bisa dihubungi.
	 *                           Kegagalan pengiriman bukan kegagalan sistem —
	 *                           pemanggilnya mencatatnya dan lanjut ke penerima
	 *                           berikutnya.
	 */
	void kirim(String nomor, String pesan);

	/** Apakah gateway benar-benar siap dipakai, atau kirimannya hanya dicatat. */
	boolean siap();

	class WhatsAppException extends RuntimeException {
		public WhatsAppException(String message) {
			super(message);
		}

		public WhatsAppException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
