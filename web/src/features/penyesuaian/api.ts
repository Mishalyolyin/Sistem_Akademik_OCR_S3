import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";

export type AdjustmentTarget = "SALDO" | "CICILAN";

export type Adjustment = {
  id: number;
  installmentId: number | null;
  amount: string;
  balanceAfter: string;
  reason: string;
  adminId: number;
  createdAt: string;
  target: AdjustmentTarget;
};

const KEY = "adjustments";

export function useAdjustments(studentId: number) {
  return useQuery({
    queryKey: [KEY, studentId],
    queryFn: () =>
      apiFetch<Adjustment[]>(`/students/${studentId}/adjustments`),
  });
}

export function useCreateAdjustment(studentId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: {
      installmentId: number | null;
      amount: number;
      reason: string;
    }) =>
      apiFetch<Adjustment>(`/students/${studentId}/adjustments`, {
        method: "POST",
        body,
      }),
    onSuccess: () => {
      // Penyesuaian menggeser saldo mahasiswa maupun cicilan, jadi keduanya
      // ikut dimuat ulang — kalau tidak, angka di kartu tagihan dan di kotak
      // ringkasan tetap menampilkan nilai lama.
      queryClient.invalidateQueries({ queryKey: [KEY, studentId] });
      queryClient.invalidateQueries({ queryKey: ["plans", studentId] });
      queryClient.invalidateQueries({ queryKey: ["students"] });
    },
  });
}
