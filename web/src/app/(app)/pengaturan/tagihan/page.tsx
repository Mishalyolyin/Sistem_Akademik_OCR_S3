import type { Metadata } from "next";
import { HalamanPengaturan } from "../halaman-pengaturan";

export const metadata: Metadata = { title: "Tagihan UKT Otomatis" };

export default function Page() {
  return (
    <HalamanPengaturan
      bagian="tagihan"
      judul="Tagihan UKT Otomatis"
      keterangan="Tagihan UKT dibuat sistem tiap awal semester, menurut angkatan tiap mahasiswa"
    />
  );
}
