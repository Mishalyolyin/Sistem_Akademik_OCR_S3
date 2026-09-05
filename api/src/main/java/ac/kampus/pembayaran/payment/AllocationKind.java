package ac.kampus.pembayaran.payment;

/** Ke mana satu bagian uang pembayaran mengalir. */
public enum AllocationKind {
	INSTALLMENT,
	WALLET,
	/** Sisa kecil dalam toleransi; tidak pernah benar-benar diterima kampus. */
	DISCARDED
}
