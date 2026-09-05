import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { apiFetch, type Page } from "@/lib/api";
import type { PaymentStatus } from "@/components/status-badge";

export type BarisForensik = {
  paymentId: number;
  namaMahasiswa: string;
  nim: string;
  status: PaymentStatus;
  nominalDiklaim: string;
  /** Pecahan 0–1, bukan persen — sama seperti yang tersimpan. */
  keyakinan: string | null;
  adaHasilOcr: boolean;
  jumlahCatatan: number;
  diunggah: string;
};

export type RiwayatKeputusan = {
  ke: PaymentStatus;
  otomatis: boolean;
  waktu: string;
  /**
   * Ketiganya bisa TIDAK ADA di badan jawaban, bukan sekadar null: backend
   * menghilangkan field kosong. `dari` kosong pada keputusan pertama, dan
   * `adminId` kosong berarti mesin yang memutuskan.
   */
  dari?: PaymentStatus;
  adminId?: number;
  catatan?: string;
};

export type DetailForensik = {
  paymentId: number;
  namaMahasiswa: string;
  nim: string;
  status: PaymentStatus;
  nominalDiklaim: string;
  keyakinan: string | null;
  alasanDitolak?: string;
  diunggah: string;
  diverifikasi?: string;
  /** Bentuknya mengikuti jawaban service OCR dan sengaja tidak diketik ketat. */
  ocrMentah: Record<string, unknown> | null;
  catatan: string[];
  riwayat: RiwayatKeputusan[];
};

export type FilterForensik = {
  status?: PaymentStatus[];
  cari?: string;
  maksKeyakinan?: number;
  page?: number;
};

const KEY = "forensik";

export function useForensikList(filter: FilterForensik) {
  return useQuery({
    queryKey: [KEY, filter],
    queryFn: () =>
      apiFetch<Page<BarisForensik>>("/forensik/pembayaran", {
        params: {
          status: filter.status?.length ? filter.status.join(",") : undefined,
          cari: filter.cari,
          maksKeyakinan: filter.maksKeyakinan,
          page: filter.page ?? 0,
          size: 20,
        },
      }),
    placeholderData: keepPreviousData,
  });
}

export function useForensikDetail(paymentId: number | null) {
  return useQuery({
    queryKey: [KEY, "detail", paymentId],
    enabled: paymentId !== null,
    queryFn: () =>
      apiFetch<DetailForensik>(`/forensik/pembayaran/${paymentId}`),
  });
}
