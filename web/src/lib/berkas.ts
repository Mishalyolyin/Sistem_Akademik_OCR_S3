"use client";

import { useQuery } from "@tanstack/react-query";
import { API_BASE_URL, getAccessToken } from "@/lib/api";

export type BerkasTerlindungi = {
  url: string;
  isPdf: boolean;
};

/**
 * Mengambil berkas yang butuh header Authorization.
 *
 * Bukti bayar dan dokumen wajib mahasiswa sengaja tidak ditaruh di folder
 * publik, jadi alamatnya tidak bisa dipasang langsung ke `<img src>` — tidak ada
 * cara menitipkan token di sana. Berkasnya diambil sebagai blob lalu dijadikan
 * object URL.
 *
 * Yang memakai hook ini WAJIB melepas object URL-nya saat komponennya dilepas
 * (`URL.revokeObjectURL`), kalau tidak berkasnya menumpuk di memori peramban.
 * Pelepasannya sengaja tidak ditaruh di sini: satu berkas yang sama bisa dipakai
 * dua komponen sekaligus, dan yang satu tidak boleh mencabut URL milik yang lain.
 *
 * Kuncinya berdiri sendiri, tidak menempel pada kunci daftar pembayaran: isi
 * berkasnya tidak pernah berubah setelah diunggah, jadi tidak perlu ikut diambil
 * ulang tiap kali daftarnya disegarkan.
 */
export function useBerkasTerlindungi(path: string | null) {
  return useQuery({
    queryKey: ["berkas", path],
    enabled: path !== null,
    staleTime: 5 * 60_000,
    queryFn: async (): Promise<BerkasTerlindungi> => {
      const base = API_BASE_URL.endsWith("/") ? API_BASE_URL : `${API_BASE_URL}/`;
      const response = await fetch(new URL(path as string, base), {
        headers: { Authorization: `Bearer ${getAccessToken()}` },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("Berkas tidak bisa dimuat.");
      }

      const blob = await response.blob();
      return {
        url: URL.createObjectURL(blob),
        isPdf: blob.type === "application/pdf",
      };
    },
  });
}
