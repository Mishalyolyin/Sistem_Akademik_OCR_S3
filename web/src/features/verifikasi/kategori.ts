import type { PaymentCategory } from "@/features/tarif/konstanta";

export type VerifikasiView = {
  slug: string;
  title: string;
  /** Kosong berarti semua kategori ditampilkan sekaligus. */
  category?: PaymentCategory;
  /** Keterangan singkat di bawah judul halaman. */
  hint: string;
  /** Kategori bercicilan boleh disesuaikan nominalnya oleh admin. */
  editableAmount: boolean;
};

/** Field yang tidak cocok antara nominal tagihan dan hasil pembacaan OCR. */
export type Mismatch = { field: string; expected: string; found: string };

export const verifikasiViews: VerifikasiView[] = [
  {
    // Dashboard menghitung lintas kategori, jadi kartunya butuh satu tujuan
    // yang tidak terikat kategori mana pun.
    slug: "semua",
    title: "Semua kategori",
    hint: "Seluruh bukti bayar dari semua kategori",
    editableAmount: false,
  },
  {
    slug: "pendaftaran",
    title: "Pendaftaran",
    category: "PENDAFTARAN",
    hint: "Rp 1.000.000, sekali bayar",
    editableAmount: false,
  },
  {
    slug: "ukt",
    title: "UKT",
    category: "UKT",
    hint: "Per semester, 5 cicilan bulanan",
    editableAmount: true,
  },
  {
    slug: "seminar-proposal",
    title: "Seminar Proposal",
    category: "SEMINAR_PROPOSAL",
    hint: "Rp 5.000.000, tahap 1 dari 4",
    editableAmount: false,
  },
  {
    slug: "ujian-kelayakan",
    title: "Ujian Kelayakan",
    category: "UJIAN_KELAYAKAN",
    hint: "Rp 5.000.000, tahap 2 dari 4",
    editableAmount: false,
  },
  {
    slug: "ujian-tertutup",
    title: "Ujian Tertutup",
    category: "UJIAN_TERTUTUP",
    hint: "Rp 10.000.000, tahap 3 dari 4",
    editableAmount: false,
  },
  {
    slug: "ujian-terbuka",
    title: "Ujian Terbuka",
    category: "UJIAN_TERBUKA",
    hint: "Rp 10.000.000, tahap 4 dari 4",
    editableAmount: false,
  },
];

export function findView(slug: string): VerifikasiView | undefined {
  return verifikasiViews.find((view) => view.slug === slug);
}
