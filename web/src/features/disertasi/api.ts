import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { useBerkasTerlindungi } from "@/lib/berkas";

export type JenisBerkasDisertasi = "DISERTASI" | "ARTIKEL";

export type Disertasi = {
  title: string;
  promotor: string;
  copromotor: string;
  adaNaskah: boolean;
  adaArtikel: boolean;
};

const KEY = "disertasi";

/**
 * Badan kosong berarti belum pernah diisi, bukan galat — backend sengaja
 * menjawab 200 supaya layar bisa menampilkan formulir kosong alih-alih halaman
 * galat. Bentuk "kosong" yang datang bisa `null` maupun string kosong,
 * tergantung apakah Spring sempat menyetel content-type; keduanya diseragamkan
 * jadi `null` di sini supaya pemanggilnya tidak perlu tahu bedanya.
 */
async function ambilDisertasi(path: string): Promise<Disertasi | null> {
  const hasil = await apiFetch<Disertasi | null>(path);
  return hasil && typeof hasil === "object" ? hasil : null;
}

export function useDisertasiSaya() {
  return useQuery({
    queryKey: [KEY, "saya"],
    queryFn: () => ambilDisertasi("/me/disertasi"),
  });
}

export function useDisertasiMahasiswa(studentId: number) {
  return useQuery({
    queryKey: [KEY, studentId],
    queryFn: () => ambilDisertasi(`/students/${studentId}/disertasi`),
  });
}

export function useSimpanDisertasi() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: {
      title: string;
      promotor: string;
      copromotor: string;
    }) => apiFetch<Disertasi>("/me/disertasi", { method: "PUT", body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

export function useUnggahBerkasDisertasi() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      jenis,
      file,
    }: {
      jenis: JenisBerkasDisertasi;
      file: File;
    }) => {
      const body = new FormData();
      body.append("file", file);
      return apiFetch<Disertasi>(`/me/disertasi/berkas?jenis=${jenis}`, {
        method: "POST",
        body,
      });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

/**
 * Naskah tidak ditaruh di folder publik: ia karya yang belum terbit, dan
 * tautannya tidak boleh bisa ditebak. Pengambilannya melewati pemeriksaan hak
 * akses seperti bukti bayar dan dokumen wajib.
 */
export function useNaskahSendiri(jenis: JenisBerkasDisertasi | null) {
  return useBerkasTerlindungi(jenis ? `me/disertasi/berkas/${jenis}` : null);
}

export function useNaskahMahasiswa(
  studentId: number,
  jenis: JenisBerkasDisertasi | null,
) {
  return useBerkasTerlindungi(
    jenis ? `students/${studentId}/disertasi/berkas/${jenis}` : null,
  );
}
