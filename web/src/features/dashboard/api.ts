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

export type DashboardSummary = {
  mahasiswaAktif: number;
  perluDitinjau: number;
  gagalDibaca: number;
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
