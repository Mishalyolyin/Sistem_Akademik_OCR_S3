package ac.kampus.pembayaran.common;

/**
 * Kategori tagihan Program Doktor PAI. Urutan enum ini sengaja mengikuti
 * urutan pembayaran yang wajar selama masa studi.
 */
public enum PaymentCategory {
	PENDAFTARAN("Pendaftaran"),
	UKT("UKT"),
	SEMINAR_PROPOSAL("Seminar Proposal"),
	UJIAN_KELAYAKAN("Ujian Kelayakan"),
	UJIAN_TERTUTUP("Ujian Tertutup"),
	UJIAN_TERBUKA("Ujian Terbuka");

	private final String label;

	PaymentCategory(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	/** Hanya UKT yang mendapat potongan; sisanya penuh untuk semua golongan. */
	public boolean discountable() {
		return this == UKT;
	}

	/** Tahap ujian yang harus dilunasi berurutan. */
	public static java.util.List<PaymentCategory> examSequence() {
		return java.util.List.of(SEMINAR_PROPOSAL, UJIAN_KELAYAKAN, UJIAN_TERTUTUP, UJIAN_TERBUKA);
	}

	/** Kategori yang harus lunas sebelum kategori ini bisa diajukan. */
	public java.util.Optional<PaymentCategory> previousExamStage() {
		var sequence = examSequence();
		int index = sequence.indexOf(this);
		return index <= 0 ? java.util.Optional.empty() : java.util.Optional.of(sequence.get(index - 1));
	}
}
