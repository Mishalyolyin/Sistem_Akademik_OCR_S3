package ac.kampus.pembayaran.billing;

public enum InstallmentStatus {
	UNPAID,
	PARTIAL,
	PAID,
	OVERDUE;

	/**
	 * Status dihitung ulang dari perbandingan yang sudah dibayar dengan nominal
	 * tagihan. Dipakai baik saat alokasi pembayaran maupun saat admin mengubah
	 * nominal cicilan, supaya aturannya hanya ada di satu tempat.
	 */
	public static InstallmentStatus of(java.math.BigDecimal amountPaid, java.math.BigDecimal amount) {
		if (amountPaid.compareTo(amount) >= 0) return PAID;
		if (amountPaid.signum() > 0) return PARTIAL;
		return UNPAID;
	}
}
