import { describe, expect, it } from "vitest";
import {
  formatRupiah,
  formatSemester,
  formatTanggal,
  formatTanggalJam,
} from "./format";

/**
 * Pemformat angka dan tanggal.
 *
 * Nominal datang dari backend sebagai string (BigDecimal di-JSON-kan jadi
 * "1200000.00"), bukan number — jadi yang dijaga di sini terutama: string
 * tetap terbaca, dan nilai kosong tidak berubah jadi "Rp NaN" di layar.
 */
describe("formatRupiah", () => {
  it("menerima string dari BigDecimal backend", () => {
    expect(formatRupiah("1200000.00")).toContain("1.200.000");
  });

  it("menerima number", () => {
    expect(formatRupiah(2_000_000)).toContain("2.000.000");
  });

  it("nol tetap ditampilkan, bukan dianggap kosong", () => {
    expect(formatRupiah(0)).toContain("0");
    expect(formatRupiah("0")).toContain("0");
  });

  it("kosong dan bukan angka jadi tanda pisah, bukan NaN", () => {
    for (const nilai of [null, undefined, "", "entah"]) {
      expect(formatRupiah(nilai)).toBe("—");
    }
  });
});

describe("formatTanggal", () => {
  it("membaca tanggal ISO dari backend", () => {
    expect(formatTanggal("2026-09-10")).toBe("10 Sep 2026");
  });

  it("tanggal ngawur tidak jadi 'Invalid Date' di layar", () => {
    expect(formatTanggal("bukan-tanggal")).toBe("—");
    expect(formatTanggal(null)).toBe("—");
  });

  it("format berjam memuat tanggal dan waktunya", () => {
    const hasil = formatTanggalJam("2026-09-10T09:41:00Z");
    expect(hasil).toContain("Sep 2026");
    expect(hasil).toMatch(/\d{2}[.:]\d{2}/);
  });
});

describe("formatSemester", () => {
  it("enum backend yang huruf besar semua jadi enak dibaca", () => {
    expect(formatSemester("2026/2027", "GASAL")).toBe("2026/2027 Gasal");
    expect(formatSemester("2026/2027", "GENAP")).toBe("2026/2027 Genap");
  });
});
