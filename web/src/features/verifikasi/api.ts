import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { apiFetch, type Page } from "@/lib/api";
import { useBerkasTerlindungi } from "@/lib/berkas";
import type { PaymentStatus } from "@/components/status-badge";

export type PaymentRow = {
  id: number;
  studentId: number;
  studentName: string;
  studentNim: string;
  className: string | null;
  installmentId: number | null;
  installmentNo: number | null;
  categoryLabel: string | null;
  semesterNumber: number | null;
  amount: string;
  proofFilePath: string;
  bankName: string | null;
  paymentProofDate: string | null;
  status: PaymentStatus;
  ocrConfidence: string | null;
  ocrData: OcrData | null;
  rejectReason: string | null;
  verifiedAt: string | null;
  createdAt: string;
};

/** Bentuknya mengikuti keluaran ocr_processor.py. */
export type OcrData = {
  confidence?: number;
  extracted_amount?: number | null;
  bank_name?: string | null;
  extracted_date?: string | null;
  verification_status?: string;
  flags?: string[];
  raw_text?: string;
};

export type VerificationLogRow = {
  id: number;
  fromStatus: PaymentStatus | null;
  toStatus: PaymentStatus;
  adminId: number | null;
  note: string | null;
  createdAt: string;
};

const KEY = "payments";

export function usePayments(params: {
  status?: PaymentStatus[];
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: [KEY, params],
    queryFn: () =>
      apiFetch<Page<PaymentRow>>("/payments", {
        params: {
          // Spring menerima daftar sebagai parameter berulang: ?status=A&status=B
          status: params.status?.join(","),
          page: params.page ?? 0,
          size: params.size ?? 50,
        },
      }),
    placeholderData: keepPreviousData,
    // Bukti baru bisa masuk kapan saja dan OCR berjalan di belakang layar,
    // jadi daftarnya disegarkan berkala.
    refetchInterval: 15_000,
  });
}

export function usePaymentLogs(paymentId: number | null) {
  return useQuery({
    queryKey: [KEY, "logs", paymentId],
    queryFn: () => apiFetch<VerificationLogRow[]>(`/payments/${paymentId}/logs`),
    enabled: paymentId !== null,
  });
}

export function useDecidePayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      approve,
      note,
    }: {
      id: number;
      approve: boolean;
      note?: string;
    }) =>
      apiFetch<PaymentRow>(`/payments/${id}/decide`, {
        method: "POST",
        body: { approve, note },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [KEY] });
      // Alokasi mengubah cicilan dan saldo mahasiswa.
      queryClient.invalidateQueries({ queryKey: ["plans"] });
      queryClient.invalidateQueries({ queryKey: ["students"] });
    },
  });
}

/**
 * Membatalkan keputusan yang sudah diambil, beserta uang yang telanjur
 * dibagikan ke cicilan dan saldo.
 *
 * Alasannya wajib: yang dibatalkan adalah pernyataan bahwa kampus menerima
 * sejumlah uang.
 */
export function useBatalkanKeputusan() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, alasan }: { id: number; alasan: string }) =>
      apiFetch<PaymentRow>(`/payments/${id}/batalkan`, {
        method: "POST",
        body: { alasan },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [KEY] });
      // Saldo dan cicilan ikut berubah, jadi halaman mahasiswa harus disegarkan.
      queryClient.invalidateQueries({ queryKey: ["students"] });
      queryClient.invalidateQueries({ queryKey: ["plans"] });
    },
  });
}

export function useRequeueOcr() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) =>
      apiFetch<void>(`/payments/${id}/requeue`, { method: "POST" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

/**
 * Gambar bukti bayar. Pengambilannya sama dengan dokumen mahasiswa, jadi
 * mekanismenya ada di {@link useBerkasTerlindungi} — termasuk kewajiban melepas
 * object URL-nya, yang diurus `BuktiTransfer`.
 */
export function useProofImage(paymentId: number | null) {
  return useBerkasTerlindungi(
    paymentId === null ? null : `payments/${paymentId}/proof`,
  );
}
