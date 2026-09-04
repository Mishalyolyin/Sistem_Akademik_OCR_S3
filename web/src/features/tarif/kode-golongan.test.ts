import { describe, expect, it } from "vitest";
import { kodeDariNama, POLA_KODE } from "./kode-golongan";

/**
 * Kode golongan yang diturunkan dari namanya.
 *
 * Aturannya harus sama dengan backend: `^[A-Z][A-Z0-9_]*$`, ditegakkan di
 * validasi permintaan dan di `CHECK` tabel `discount_tier_rates`. Karena itu
 * tiap kode yang dihasilkan di sini ikut diuji terhadap polanya — kode yang
 * lolos di layar tapi ditolak backend adalah galat yang paling membingungkan
 * bagi admin, sebab ia tidak pernah mengetik kodenya sendiri.
 */
describe("kodeDariNama", () => {
  const contoh: [string, string][] = [
    ["Mitra Instansi", "MITRA_INSTANSI"],
    ["alumni", "ALUMNI"],
    ["Alumni + pasutri", "ALUMNI_PASUTRI"],
    ["Kerjasama  (1 kelas)", "KERJASAMA_1_KELAS"],
    ["  Kerabat Alumni  ", "KERABAT_ALUMNI"],
    ["Beasiswa-2026", "BEASISWA_2026"],
  ];

  it.each(contoh)("\"%s\" menjadi %s", (nama, kode) => {
    expect(kodeDariNama(nama)).toBe(kode);
  });

  it("semua kode yang dihasilkan lolos pola yang dipakai backend", () => {
    for (const [nama] of contoh) {
      expect(kodeDariNama(nama)).toMatch(POLA_KODE);
    }
  });

  it("nama yang diawali angka diberi awalan huruf, karena pola melarangnya", () => {
    expect(kodeDariNama("2026 Kerjasama")).toBe("G2026_KERJASAMA");
    expect(kodeDariNama("2026 Kerjasama")).toMatch(POLA_KODE);
  });

  it("nama tanpa huruf maupun angka menghasilkan kode kosong, bukan kode cacat", () => {
    // Kosong ditahan tombol simpannya; yang penting ia tidak menghasilkan
    // sesuatu seperti "_" yang akan ditolak backend tanpa admin tahu sebabnya.
    expect(kodeDariNama("---")).toBe("");
    expect(kodeDariNama("")).toBe("");
  });
});
