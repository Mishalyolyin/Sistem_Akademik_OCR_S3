import { describe, expect, it } from "vitest";
import {
  JUMLAH_CICILAN_UKT,
  JUMLAH_SEMESTER_UKT,
  totalBiayaStudi,
  uktPerCicilan,
  uktPerSemester,
} from "./konstanta";

/**
 * Hitungan tarif untuk pratinjau di layar.
 *
 * Angka pembandingnya diambil dari brosur resmi Program Doktor PAI 2026, sama
 * dengan yang dipakai `PaymentGenerationServiceTest` di backend. Keduanya harus
 * cocok: kalau pratinjau di layar berbeda dari nominal yang benar-benar dibuat
 * backend, admin akan menyimpan tagihan yang tidak sesuai dengan yang ia lihat.
 */
describe("hitungan UKT per golongan", () => {
  const brosur = [
    { persen: 0, semester: 10_000_000, cicilan: 2_000_000, total: 91_000_000 },
    { persen: 20, semester: 8_000_000, cicilan: 1_600_000, total: 79_000_000 },
    { persen: 25, semester: 7_500_000, cicilan: 1_500_000, total: 76_000_000 },
    { persen: 35, semester: 6_500_000, cicilan: 1_300_000, total: 70_000_000 },
    { persen: 40, semester: 6_000_000, cicilan: 1_200_000, total: 67_000_000 },
  ];

  it.each(brosur)(
    "potongan $persen% menghasilkan angka brosur",
    ({ persen, semester, cicilan, total }) => {
      expect(uktPerSemester(persen)).toBe(semester);
      expect(uktPerCicilan(persen)).toBe(cicilan);
      expect(totalBiayaStudi(persen)).toBe(total);
    },
  );

  it("lima cicilan berjumlah tepat sama dengan UKT satu semester", () => {
    for (const { persen } of brosur) {
      expect(uktPerCicilan(persen) * JUMLAH_CICILAN_UKT).toBe(
        uktPerSemester(persen),
      );
    }
  });

  it("potongan penuh menghasilkan UKT nol, tapi biaya ujian tetap ditagih", () => {
    expect(uktPerSemester(100)).toBe(0);
    // Pendaftaran 1 jt + empat tahap ujian 30 jt, sama untuk semua golongan.
    expect(totalBiayaStudi(100)).toBe(31_000_000);
  });

  it("enam semester UKT, sesuai batas yang ditegakkan backend", () => {
    expect(JUMLAH_SEMESTER_UKT).toBe(6);
    expect(totalBiayaStudi(0) - 31_000_000).toBe(
      uktPerSemester(0) * JUMLAH_SEMESTER_UKT,
    );
  });
});
