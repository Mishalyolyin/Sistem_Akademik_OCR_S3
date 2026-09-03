import type { Metadata } from "next";
import { HalamanPengaturan } from "../halaman-pengaturan";

export const metadata: Metadata = { title: "Pengaturan OCR" };

export default function Page() {
  return (
    <HalamanPengaturan
      bagian="ocr"
      judul="OCR & Verifikasi"
      keterangan="Ambang keputusan otomatis saat membaca bukti bayar"
    />
  );
}
