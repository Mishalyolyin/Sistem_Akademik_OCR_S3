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
  active: boolean;
  sortOrder: number;
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

/**
 * Golongan potongan beserta pencari labelnya.
 *
 * <p>Daftarnya datang dari database karena admin bisa menambah golongan baru.
 * Selagi masih dimuat, kode golongan dipakai apa adanya supaya layar tidak
 * menampilkan tempat kosong.
 */
export function useGolongan(academicYear = "2026/2027") {
  const query = useTierRates(academicYear);
  const daftar = query.data ?? [];

  return {
    ...query,
    daftar,
    aktif: daftar.filter((tier) => tier.active),
    label: (kode: DiscountTier) =>
      daftar.find((tier) => tier.tier === kode)?.label ?? kode,
    persen: (kode: DiscountTier) =>
      Number(daftar.find((tier) => tier.tier === kode)?.percent ?? 0),
  };
}

export function useCreateTier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: {
      tier: string;
      label: string;
      percent: number;
      sortOrder?: number;
    }) => apiFetch<TierRate>("/tuition/tiers", { method: "POST", body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [TIERS_KEY] }),
  });
}

export function useUpdateTierPercent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      tier,
      ...body
    }: {
      tier: DiscountTier;
      percent: number;
      label?: string;
      active?: boolean;
      sortOrder?: number;
    }) =>
      apiFetch<TierRate>(`/tuition/tiers/${tier}`, { method: "PUT", body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [TIERS_KEY] }),
  });
}
