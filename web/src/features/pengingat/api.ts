import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";

export type JenisPengingat = "JATUH_TEMPO" | "LEWAT_TEMPO";
export type StatusPengingat = "SENT" | "FAILED" | "SKIPPED";

export type RiwayatPengingat = {
  id: number;
  installmentId: number;
  studentId: number;
  jenis: JenisPengingat;
  status: StatusPengingat;
  waktu: string;
  /** Keduanya bisa tidak ada di jawaban: backend menghilangkan field kosong. */
  nomor?: string;
  galat?: string;
};

export type StatusPengingatResponse = {
  gatewaySiap: boolean;
  riwayat: RiwayatPengingat[];
};

export type HasilPutaran = {
  diperiksa: number;
  terkirim: number;
  dilewati: number;
  gagal: number;
};

const KEY = "pengingat";

export function useStatusPengingat() {
  return useQuery({
    queryKey: [KEY],
    queryFn: () => apiFetch<StatusPengingatResponse>("/pengingat"),
  });
}

/**
 * Menjalankan putaran pengingat sekarang.
 *
 * Ini benar-benar mengirim pesan ke ponsel mahasiswa, jadi pemanggilnya wajib
 * meminta konfirmasi lebih dulu.
 */
export function useJalankanPengingat() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      apiFetch<HasilPutaran>("/pengingat/jalankan", { method: "POST" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}
