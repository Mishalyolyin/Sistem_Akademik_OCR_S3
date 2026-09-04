import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import type { PaymentCategory } from "@/features/tarif/konstanta";

export type DocumentStep = "PHOTO" | "KTP" | "KK" | "IJAZAH" | "ADDRESS";

export type Profil = {
  id: number;
  nim: string;
  nama: string;
  email: string | null;
  kelas: string | null;
  golongan: string;
  golonganLabel: string;
  telepon: string | null;
  alamat: string | null;
  dokumenLengkap: boolean;
  langkahDokumenBerikutnya: DocumentStep | null;
  namaLangkahBerikutnya: string | null;
  pendaftaranLunas: boolean;
  saldo: string;
};

export type CicilanRingkas = {
  id: number;
  nomor: number;
  jatuhTempo: string;
  nominal: string;
  dibayar: string;
  sisa: string;
  status: "UNPAID" | "PARTIAL" | "PAID" | "OVERDUE";
};

export type Tagihan = {
  id: number;
  kategori: string;
  semester: number | null;
  tahunAkademik: string;
  total: string;
  dibayar: string;
  sisa: string;
  status: string;
  cicilan: CicilanRingkas[];
};

export type RiwayatItem = {
  id: number;
  nominal: string;
  kategori: string | null;
  cicilanKe: number | null;
  status:
    | "PENDING"
    | "NEEDS_REVIEW"
    | "AUTO_VERIFIED"
    | "VERIFIED"
    | "REJECTED"
    | "FAILED";
  bank: string | null;
  tanggalBukti: string | null;
  alasanDitolak: string | null;
  diunggah: string;
};

const PROFIL = "me-profil";
const TAGIHAN = "me-tagihan";
const RIWAYAT = "me-riwayat";

export function useProfil() {
  return useQuery({
    queryKey: [PROFIL],
    queryFn: () => apiFetch<Profil>("/me/profil"),
  });
}

export function useTagihan(aktif: boolean) {
  return useQuery({
    queryKey: [TAGIHAN],
    queryFn: () => apiFetch<Tagihan[]>("/me/tagihan"),
    enabled: aktif,
  });
}

export function useRiwayat() {
  return useQuery({
    queryKey: [RIWAYAT],
    queryFn: () => apiFetch<RiwayatItem[]>("/me/pembayaran"),
    // Status berubah sendiri setelah OCR selesai membaca.
    refetchInterval: 10_000,
  });
}

function useSegarkanSemua() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: [PROFIL] });
    queryClient.invalidateQueries({ queryKey: [TAGIHAN] });
    queryClient.invalidateQueries({ queryKey: [RIWAYAT] });
  };
}

/** Satu hook untuk kelima langkah dokumen, karena bentuk kirimannya sama. */
export function useUnggahDokumen() {
  const segarkan = useSegarkanSemua();

  return useMutation({
    mutationFn: async ({
      langkah,
      file,
      nomor,
    }: {
      langkah: Exclude<DocumentStep, "ADDRESS">;
      file: File;
      nomor?: string;
    }) => {
      const body = new FormData();
      body.append("file", file);

      const path =
        langkah === "PHOTO"
          ? "/me/dokumen/foto"
          : langkah === "KTP"
            ? `/me/dokumen/ktp?nik=${encodeURIComponent(nomor ?? "")}`
            : langkah === "KK"
              ? `/me/dokumen/kk?nomorKk=${encodeURIComponent(nomor ?? "")}`
              : "/me/dokumen/ijazah";

      return apiFetch<Profil>(path, { method: "POST", body });
    },
    onSuccess: segarkan,
  });
}

export function useSimpanAlamat() {
  const segarkan = useSegarkanSemua();

  return useMutation({
    mutationFn: (body: { alamat: string; telepon?: string }) =>
      apiFetch<Profil>("/me/dokumen/alamat", { method: "POST", body }),
    onSuccess: segarkan,
  });
}

export function useDaftarTagihan() {
  const segarkan = useSegarkanSemua();

  return useMutation({
    mutationFn: (category: PaymentCategory) =>
      apiFetch<Tagihan>("/me/tagihan", { method: "POST", body: { category } }),
    onSuccess: segarkan,
  });
}

export function useUnggahBukti() {
  const segarkan = useSegarkanSemua();

  return useMutation({
    mutationFn: ({
      installmentId,
      amount,
      file,
    }: {
      installmentId: number;
      amount: number;
      file: File;
    }) => {
      const body = new FormData();
      body.append("file", file);
      return apiFetch<RiwayatItem>(
        `/me/pembayaran?installmentId=${installmentId}&amount=${amount}`,
        { method: "POST", body },
      );
    },
    onSuccess: segarkan,
  });
}
