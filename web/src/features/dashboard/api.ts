import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import type { PaymentStatus } from "@/components/status-badge";

export type KategoriRingkas = {
  kategori: string;
  jumlahTagihan: number;
  tertagih: string;
  terkumpul: string;
};

export type KelasRingkas = {
  kelas: string;
  jumlahMahasiswa: number;
  tertagih: string;
  terkumpul: string;
  /** Bukti bayar yang sudah pernah dibaca OCR di kelas ini. */
  buktiDibaca: number;
  buktiPerluDitinjau: number;
  buktiGagalDibaca: number;
  /** Hilang dari JSON bila belum ada satu pun bukti yang dibaca. */
  rataKeyakinan?: string | null;
};

export type AntreanItem = {
  paymentId: number;
  namaMahasiswa: string;
  nim: string;
  kategori: string | null;
  nominal: string;
  status: PaymentStatus;
  keyakinan: string | null;
  diunggah: string;
};

/** Jumlah bukti bayar per status, untuk kartu ringkasan yang bisa diklik. */
export type RingkasanStatus = {
  menungguDibaca: number;
  perluDitinjau: number;
  ditolak: number;
  terverifikasi: number;
  gagalDibaca: number;
};

export type DashboardSummary = {
  mahasiswaAktif: number;
  perluDitinjau: number;
  gagalDibaca: number;
  status: RingkasanStatus;
  totalTertagih: string;
  totalTerkumpul: string;
  totalSaldoMahasiswa: string;
  perKategori: KategoriRingkas[];
  perKelas: KelasRingkas[];
  antrean: AntreanItem[];
};

export function useDashboard() {
  return useQuery({
    queryKey: ["dashboard"],
    queryFn: () => apiFetch<DashboardSummary>("/dashboard/summary"),
    refetchInterval: 30_000,
  });
}
