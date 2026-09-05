package ac.kampus.pembayaran.reminder;

public enum ReminderStatus {
	SENT,
	/** Gateway menolak atau tidak bisa dihubungi; boleh dicoba lagi besok. */
	FAILED,
	/** Tidak jadi dikirim, misalnya nomor teleponnya belum diisi. */
	SKIPPED
}
