import type { Metadata } from "next";
import { HalamanPengaturan } from "../halaman-pengaturan";

export const metadata: Metadata = { title: "Rekening & Notifikasi" };

export default function Page() {
  return (
    <HalamanPengaturan
      bagian="sistem"
      judul="Rekening & Notifikasi"
      keterangan="Rekening tujuan yang dicocokkan OCR, dan nomor pemberitahuan admin"
    />
  );
}
