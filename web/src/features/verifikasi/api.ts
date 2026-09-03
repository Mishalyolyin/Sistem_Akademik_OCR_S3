import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { API_BASE_URL, apiFetch, getAccessToken, type Page } from "@/lib/api";
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

export function useRequeueOcr() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) =>
      apiFetch<void>(`/payments/${id}/requeue`, { method: "POST" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

/**
 * Gambar bukti butuh header Authorization, jadi tidak bisa dipasang langsung
 * ke <img src>. Berkasnya diambil sebagai blob lalu dijadikan object URL.
 */
export function useProofImage(paymentId: number | null) {
  return useQuery({
    queryKey: [KEY, "proof", paymentId],
    enabled: paymentId !== null,
    staleTime: 5 * 60_000,
    queryFn: async () => {
      const base = API_BASE_URL.endsWith("/") ? API_BASE_URL : `${API_BASE_URL}/`;
      const response = await fetch(new URL(`payments/${paymentId}/proof`, base), {
        headers: { Authorization: `Bearer ${getAccessToken()}` },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("Gambar bukti tidak bisa dimuat.");
      }

      const blob = await response.blob();
      return {
        url: URL.createObjectURL(blob),
        isPdf: blob.type === "application/pdf",
      };
    },
  });
}
