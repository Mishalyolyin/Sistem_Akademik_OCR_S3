package ac.kampus.pembayaran.common;

public enum AcademicTerm {
	/** September sampai Januari. */
	GASAL,
	/** Februari sampai Juni. */
	GENAP;

	/** Bulan pertama term, dipakai menghitung jatuh tempo dari month_offset. */
	public int startMonth() {
		return this == GASAL ? 9 : 2;
	}
}
