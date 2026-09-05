package ac.kampus.pembayaran.payment;

public enum PaymentStatus {
	/** Baru diunggah, menunggu dibaca OCR. */
	PENDING,
	/** OCR kurang yakin, butuh keputusan admin. */
	NEEDS_REVIEW,
	/** Diverifikasi otomatis oleh OCR. */
	AUTO_VERIFIED,
	/** Diverifikasi manual oleh admin. */
	VERIFIED,
	REJECTED,
	/** OCR gagal setelah semua percobaan ulang habis. */
	FAILED;

	/** Status yang masih boleh diubah oleh hasil OCR yang datang belakangan. */
	public boolean bolehDitimpaOcr() {
		return this == PENDING || this == NEEDS_REVIEW;
	}

	public boolean sudahDiverifikasi() {
		return this == AUTO_VERIFIED || this == VERIFIED;
	}

	/**
	 * Sudah ada keputusan atasnya, diterima maupun ditolak.
	 *
	 * <p>Dibedakan dari {@link #sudahDiverifikasi()}: yang ditolak juga sudah
	 * diputuskan, dan keputusan itu pun bisa keliru — bukti yang sah bisa
	 * terlanjur ditolak karena gambarnya kurang jelas.
	 */
	public boolean sudahDiputuskan() {
		return sudahDiverifikasi() || this == REJECTED;
	}
}
