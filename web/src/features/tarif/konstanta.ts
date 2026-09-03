/**
 * Struktur biaya Program Doktor PAI, sesuai brosur resmi 2026.
 *
 * Nilai di sini hanya untuk tampilan sementara sampai endpoint tarif jadi
 * (Fase 2). Sumber kebenaran nantinya tabel `tuition_rates` di backend —
 * jangan pakai konstanta ini untuk menghitung tagihan sungguhan.
 */

export type PaymentCategory =
  | "PENDAFTARAN"
  | "UKT"
  | "SEMINAR_PROPOSAL"
  | "UJIAN_KELAYAKAN"
  | "UJIAN_TERTUTUP"
  | "UJIAN_TERBUKA";

export type DiscountTier =
  | "NON_ALUMNI"
  | "KERABAT_ALUMNI"
  | "ALUMNI"
  | "ALUMNI_PASUTRI"
  | "KERJASAMA";

export const kategoriLabel: Record<PaymentCategory, string> = {
  PENDAFTARAN: "Pendaftaran",
  UKT: "UKT",
  SEMINAR_PROPOSAL: "Seminar Proposal",
  UJIAN_KELAYAKAN: "Ujian Kelayakan",
  UJIAN_TERTUTUP: "Ujian Tertutup",
  UJIAN_TERBUKA: "Ujian Terbuka",
};

/** Tarif dasar sebelum potongan. UKT dihitung per semester. */
export const tarifDasar: Record<PaymentCategory, number> = {
  PENDAFTARAN: 1_000_000,
  UKT: 10_000_000,
  SEMINAR_PROPOSAL: 5_000_000,
  UJIAN_KELAYAKAN: 5_000_000,
  UJIAN_TERTUTUP: 10_000_000,
  UJIAN_TERBUKA: 10_000_000,
};

export const JUMLAH_SEMESTER_UKT = 6;
export const JUMLAH_CICILAN_UKT = 5;

/** Potongan hanya berlaku untuk UKT. Pendaftaran dan biaya ujian tetap penuh. */
export const potongan: Record<
  DiscountTier,
  { label: string; persen: number }
> = {
  NON_ALUMNI: { label: "Non alumni", persen: 0 },
  KERABAT_ALUMNI: { label: "Kerabat alumni", persen: 20 },
  ALUMNI: { label: "Alumni", persen: 25 },
  ALUMNI_PASUTRI: { label: "Alumni + pasutri", persen: 35 },
  KERJASAMA: { label: "Kerjasama", persen: 40 },
};

/** UKT satu semester setelah potongan. */
export function uktPerSemester(tier: DiscountTier): number {
  return Math.round(tarifDasar.UKT * (1 - potongan[tier].persen / 100));
}

/** Nominal satu cicilan UKT. */
export function uktPerCicilan(tier: DiscountTier): number {
  return Math.floor(uktPerSemester(tier) / JUMLAH_CICILAN_UKT);
}

/** Total seluruh biaya studi untuk satu tingkat potongan. */
export function totalBiayaStudi(tier: DiscountTier): number {
  const biayaUjian =
    tarifDasar.SEMINAR_PROPOSAL +
    tarifDasar.UJIAN_KELAYAKAN +
    tarifDasar.UJIAN_TERTUTUP +
    tarifDasar.UJIAN_TERBUKA;

  return (
    tarifDasar.PENDAFTARAN +
    uktPerSemester(tier) * JUMLAH_SEMESTER_UKT +
    biayaUjian
  );
}

/**
 * Urutan wajib tahap ujian. Mahasiswa tidak boleh mendaftar tahap berikutnya
 * sebelum tahap sebelumnya lunas. Backend tetap wajib memvalidasi ini —
 * daftar di sini hanya untuk menampilkan urutannya di UI.
 */
export const urutanUjian: PaymentCategory[] = [
  "SEMINAR_PROPOSAL",
  "UJIAN_KELAYAKAN",
  "UJIAN_TERTUTUP",
  "UJIAN_TERBUKA",
];

/** Jatuh tempo 5 cicilan UKT, bulanan. */
export const bulanCicilan: Record<"GASAL" | "GENAP", string[]> = {
  GASAL: ["September", "Oktober", "November", "Desember", "Januari"],
  GENAP: ["Februari", "Maret", "April", "Mei", "Juni"],
};
