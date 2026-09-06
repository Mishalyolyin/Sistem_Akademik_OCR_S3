import { describe, expect, it } from "vitest";
import { labelSemester, semesterAktif } from "./semester";

/**
 * Penurunan semester akademik dari tanggal.
 *
 * Batas-batasnya yang paling rawan: Januari masih Gasal tapi tahun akademiknya
 * dimulai tahun sebelumnya, dan Juli–Agustus bukan term mana pun. Salah di dua
 * tempat itu membuat layar mengumumkan tahun akademik yang keliru selama
 * berbulan-bulan tanpa ada yang menyadarinya — tidak ada galat, hanya angka
 * yang salah.
 */
describe("semesterAktif", () => {
  const pada = (iso: string) => semesterAktif(new Date(iso));

  it.each([
    ["2026-09-01", "GASAL", "2026/2027"],
    ["2026-10-15", "GASAL", "2026/2027"],
    ["2026-12-31", "GASAL", "2026/2027"],
  ])("%s masuk Gasal tahun berjalan", (iso, term, tahun) => {
    expect(pada(iso)).toMatchObject({ term, academicYear: tahun, jeda: false });
  });

  it("Januari masih Gasal, tapi tahun akademiknya dimulai tahun sebelumnya", () => {
    // Cicilan Gasal kelima jatuh tempo Januari, jadi bulan ini belum Genap.
    expect(pada("2027-01-10")).toMatchObject({
      term: "GASAL",
      academicYear: "2026/2027",
      jeda: false,
    });
  });

  it.each([
    ["2027-02-01", "2026/2027"],
    ["2027-04-20", "2026/2027"],
    ["2027-06-30", "2026/2027"],
  ])("%s masuk Genap tahun akademik yang sama", (iso, tahun) => {
    expect(pada(iso)).toMatchObject({
      term: "GENAP",
      academicYear: tahun,
      jeda: false,
    });
  });

  it("Juli dan Agustus ditandai jeda, dan menunjuk Gasal yang akan datang", () => {
    // Tidak ada angsuran jatuh tempo di dua bulan ini. Memaksakannya masuk
    // Genap akan membuat layar menyebut term yang cicilannya sudah lewat.
    for (const iso of ["2027-07-01", "2027-08-31"]) {
      expect(pada(iso)).toMatchObject({
        term: "GASAL",
        academicYear: "2027/2028",
        jeda: true,
      });
    }
  });

  it("pergantian tahun akademik terjadi di batas Agustus ke September", () => {
    expect(pada("2027-08-31").academicYear).toBe("2027/2028");
    expect(pada("2027-09-01")).toMatchObject({
      academicYear: "2027/2028",
      jeda: false,
    });
  });

  it("pergantian term terjadi di batas Januari ke Februari", () => {
    expect(pada("2027-01-31").term).toBe("GASAL");
    expect(pada("2027-02-01").term).toBe("GENAP");
    // Tahun akademiknya TIDAK ikut berganti di batas ini.
    expect(pada("2027-01-31").academicYear).toBe("2026/2027");
    expect(pada("2027-02-01").academicYear).toBe("2026/2027");
  });

  it("bulan jatuh tempo ikut disebut supaya tidak perlu dihafal", () => {
    expect(pada("2026-10-01").bulan).toBe("September–Januari");
    expect(pada("2027-03-01").bulan).toBe("Februari–Juni");
  });
});

describe("labelSemester", () => {
  it("merangkai term dan tahun jadi satu baris siap tampil", () => {
    expect(labelSemester(semesterAktif(new Date("2026-10-01")))).toBe(
      "Gasal 2026/2027",
    );
    expect(labelSemester(semesterAktif(new Date("2027-03-01")))).toBe(
      "Genap 2026/2027",
    );
  });
});
