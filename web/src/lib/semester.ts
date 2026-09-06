/**
 * Menurunkan semester akademik yang sedang berjalan dari tanggal.
 *
 * <p>Angsuran UKT Program Doktor jatuh tempo September–Januari untuk Gasal dan
 * Februari–Juni untuk Genap. Dua bulan tersisa — Juli dan Agustus — tidak masuk
 * keduanya, dan itu bukan kelalaian: memang tidak ada angsuran di sana. Fungsi
 * ini menyebutnya jeda, bukan memaksakannya masuk salah satu term. Memaksakan
 * akan membuat layar mengumumkan "Semester Genap" di bulan Agustus, padahal
 * cicilan terakhir Genap sudah lewat sejak Juni.
 *
 * <p>Tahun akademiknya mengikuti kebiasaan Indonesia: satu tahun akademik
 * dimulai Gasal dan berakhir Genap, jadi Januari masih milik tahun akademik
 * yang dimulai September sebelumnya.
 */

export type AcademicTerm = "GASAL" | "GENAP";

export type SemesterAktif = {
  term: AcademicTerm;
  academicYear: string;
  /**
   * Benar bila tanggalnya jatuh di Juli–Agustus: tidak ada angsuran jatuh tempo,
   * dan `term` menunjuk semester yang akan datang, bukan yang sedang berjalan.
   */
  jeda: boolean;
  /** Bulan-bulan jatuh tempo term itu, untuk ditampilkan apa adanya. */
  bulan: string;
};

const BULAN_GASAL = "September–Januari";
const BULAN_GENAP = "Februari–Juni";

export function semesterAktif(tanggal: Date = new Date()): SemesterAktif {
  const bulan = tanggal.getMonth() + 1;
  const tahun = tanggal.getFullYear();

  // September–Desember: Gasal, tahun akademik dimulai tahun ini.
  if (bulan >= 9) {
    return {
      term: "GASAL",
      academicYear: `${tahun}/${tahun + 1}`,
      jeda: false,
      bulan: BULAN_GASAL,
    };
  }

  // Januari: masih Gasal, tapi tahun akademiknya dimulai tahun lalu.
  if (bulan === 1) {
    return {
      term: "GASAL",
      academicYear: `${tahun - 1}/${tahun}`,
      jeda: false,
      bulan: BULAN_GASAL,
    };
  }

  // Februari–Juni: Genap dari tahun akademik yang sama.
  if (bulan <= 6) {
    return {
      term: "GENAP",
      academicYear: `${tahun - 1}/${tahun}`,
      jeda: false,
      bulan: BULAN_GENAP,
    };
  }

  // Juli–Agustus: jeda. Yang ditunjuk adalah Gasal yang akan datang.
  return {
    term: "GASAL",
    academicYear: `${tahun}/${tahun + 1}`,
    jeda: true,
    bulan: BULAN_GASAL,
  };
}

export function labelTerm(term: AcademicTerm): string {
  return term === "GASAL" ? "Gasal" : "Genap";
}

/** Satu baris siap tampil, misalnya "Gasal 2026/2027". */
export function labelSemester(semester: SemesterAktif): string {
  return `${labelTerm(semester.term)} ${semester.academicYear}`;
}
