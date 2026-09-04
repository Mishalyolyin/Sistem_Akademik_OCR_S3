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

/**
 * Kode golongan potongan, misalnya "ALUMNI".
 *
 * Sengaja bukan union tetap: golongan bisa ditambah admin tanpa deploy ulang,
 * jadi daftarnya datang dari API lewat {@link useGolongan}, bukan dari kode.
 */
export type DiscountTier = string;

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

/** UKT satu semester setelah potongan. Potongan hanya berlaku untuk UKT. */
export function uktPerSemester(persen: number): number {
  return Math.round(tarifDasar.UKT * (1 - persen / 100));
}

/** Nominal satu cicilan UKT. */
export function uktPerCicilan(persen: number): number {
  return Math.floor(uktPerSemester(persen) / JUMLAH_CICILAN_UKT);
}

/** Total seluruh biaya studi untuk satu tingkat potongan. */
export function totalBiayaStudi(persen: number): number {
  const biayaUjian =
    tarifDasar.SEMINAR_PROPOSAL +
    tarifDasar.UJIAN_KELAYAKAN +
    tarifDasar.UJIAN_TERTUTUP +
    tarifDasar.UJIAN_TERBUKA;

  return (
    tarifDasar.PENDAFTARAN +
    uktPerSemester(persen) * JUMLAH_SEMESTER_UKT +
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
