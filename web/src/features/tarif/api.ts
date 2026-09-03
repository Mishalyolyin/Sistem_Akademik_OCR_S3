import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import type { DiscountTier, PaymentCategory } from "./konstanta";

export type TuitionRate = {
  id: number;
  category: PaymentCategory;
  categoryLabel: string;
  academicYear: string;
  amount: string;
  active: boolean;
};

export type TierRate = {
  tier: DiscountTier;
  label: string;
  percent: string;
  uktPerSemester: string;
  uktPerInstallment: string;
};

const RATES_KEY = "tuition-rates";
const TIERS_KEY = "tuition-tiers";

export function useTuitionRates() {
  return useQuery({
    queryKey: [RATES_KEY],
    queryFn: () => apiFetch<TuitionRate[]>("/tuition/rates"),
  });
}

export function useTierRates(academicYear = "2026/2027") {
  return useQuery({
    queryKey: [TIERS_KEY, academicYear],
    queryFn: () =>
      apiFetch<TierRate[]>("/tuition/tiers", { params: { academicYear } }),
  });
}

export function useUpdateTuitionRate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      ...body
    }: {
      id: number;
      category: PaymentCategory;
      academicYear: string;
      amount: number;
      active?: boolean;
    }) => apiFetch<TuitionRate>(`/tuition/rates/${id}`, { method: "PUT", body }),
    onSuccess: () => {
      // Mengubah tarif dasar UKT ikut mengubah hitungan tiap golongan.
      queryClient.invalidateQueries({ queryKey: [RATES_KEY] });
      queryClient.invalidateQueries({ queryKey: [TIERS_KEY] });
    },
  });
}

export function useUpdateTierPercent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ tier, percent }: { tier: DiscountTier; percent: number }) =>
      apiFetch<TierRate>(`/tuition/tiers/${tier}`, {
        method: "PUT",
        body: { percent },
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [TIERS_KEY] }),
  });
}
