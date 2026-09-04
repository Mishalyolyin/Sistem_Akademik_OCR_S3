import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { apiFetch, type Page } from "@/lib/api";
import { useBerkasTerlindungi } from "@/lib/berkas";
import type { DiscountTier } from "@/features/tarif/konstanta";

export type DocumentStep = "PHOTO" | "KTP" | "KK" | "IJAZAH" | "ADDRESS";

/** Kode dokumen yang berupa berkas. Alamat tidak masuk: itu teks, bukan berkas. */
export type JenisDokumen = "foto" | "ktp" | "kk" | "ijazah";

export const dokumenLabels: Record<JenisDokumen, string> = {
  foto: "Foto profil",
  ktp: "KTP",
  kk: "Kartu Keluarga",
  ijazah: "Ijazah",
};
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
  nik: string | null;
  kkNumber: string | null;
  /** Dokumen yang berkasnya sudah ada, jadi tombol bukanya boleh muncul. */
  dokumenTersedia: JenisDokumen[];
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

export type HasilHapus = {
  id: number;
  berhasil: boolean;
  /**
   * Ketiganya bisa TIDAK ADA di badan jawaban, bukan sekadar null: backend
   * menghilangkan field kosong. Baris yang mahasiswanya tidak ditemukan hanya
   * membawa id dan alasan.
   */
  nim?: string;
  nama?: string;
  alasan?: string;
};

export type HapusMassalResponse = {
  diminta: number;
  berhasil: number;
  ditolak: number;
  rincian: HasilHapus[];
};

/**
 * Hapus beberapa mahasiswa sekaligus, untuk membereskan salah import.
 *
 * Backend melaporkannya per baris: mahasiswa yang sudah punya riwayat uang
 * ditolak, dan penolakannya tidak membatalkan penghapusan yang lain.
 */
export function useHapusMassalStudents() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (ids: number[]) =>
      apiFetch<HapusMassalResponse>("/students/hapus-massal", {
        method: "POST",
        body: { ids },
      }),
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

/**
 * Berkas dokumen wajib satu mahasiswa. Berkasnya tidak ada di folder publik,
 * jadi harus lewat hook yang menyertakan token; yang memakainya wajib melepas
 * object URL-nya saat selesai.
 */
export function useDokumenMahasiswa(
  studentId: number,
  jenis: JenisDokumen | null,
) {
  return useBerkasTerlindungi(
    jenis === null ? null : `students/${studentId}/dokumen/${jenis}`,
  );
}
