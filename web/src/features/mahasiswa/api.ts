import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { apiFetch, type Page } from "@/lib/api";
import type { DiscountTier } from "@/features/tarif/konstanta";

export type DocumentStep = "PHOTO" | "KTP" | "KK" | "IJAZAH" | "ADDRESS";
export type AcademicTerm = "GASAL" | "GENAP";

export type StudentSummary = {
  id: number;
  nim: string;
  name: string;
  phone: string | null;
  studyClassId: number | null;
  className: string | null;
  discountTier: DiscountTier;
  startTerm: AcademicTerm;
  startAcademicYear: string;
  walletBalance: string;
  pendaftaranExempt: boolean;
  active: boolean;
  nextDocumentStep: DocumentStep | null;
  documentsComplete: boolean;
  discountTierLocked: boolean;
};

export type StudentFilters = {
  search?: string;
  classId?: number;
  tier?: DiscountTier;
  page?: number;
  size?: number;
};

const KEY = "students";

export function useStudents(filters: StudentFilters) {
  return useQuery({
    queryKey: [KEY, filters],
    queryFn: () =>
      apiFetch<Page<StudentSummary>>("/students", {
        params: {
          search: filters.search,
          classId: filters.classId,
          tier: filters.tier,
          page: filters.page ?? 0,
          size: filters.size ?? 20,
        },
      }),
    // Tahan data lama saat halaman berganti, supaya tabel tidak berkedip kosong.
    placeholderData: keepPreviousData,
  });
}

export function useStudent(id: number) {
  return useQuery({
    queryKey: [KEY, "detail", id],
    queryFn: () => apiFetch<StudentSummary>(`/students/${id}`),
  });
}

export function useChangeDiscountTier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, tier }: { id: number; tier: DiscountTier }) =>
      apiFetch<StudentSummary>(`/students/${id}/discount-tier`, {
        method: "PATCH",
        body: { discountTier: tier },
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

export function useUpdateStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      ...body
    }: {
      id: number;
      name?: string;
      phone?: string;
      studyClassId?: number;
      pendaftaranExempt?: boolean;
      active?: boolean;
    }) =>
      apiFetch<StudentSummary>(`/students/${id}`, { method: "PUT", body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

export function useDeleteStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) =>
      apiFetch<void>(`/students/${id}`, { method: "DELETE" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

/**
 * Mengembalikan kata sandi mahasiswa ke NIM-nya. Mahasiswa tidak mengelola
 * kata sandinya sendiri; kalau lupa, admin yang mengembalikannya.
 */
export function useResetKataSandi() {
  return useMutation({
    mutationFn: (id: number) =>
      apiFetch<{ kataSandiBaru: string }>(`/students/${id}/reset-kata-sandi`, {
        method: "POST",
      }),
  });
}

export const documentStepLabels: Record<DocumentStep, string> = {
  PHOTO: "Foto profil",
  KTP: "KTP",
  KK: "Kartu Keluarga",
  IJAZAH: "Ijazah",
  ADDRESS: "Alamat",
};
