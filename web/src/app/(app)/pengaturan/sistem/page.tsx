import type { Metadata } from "next";
import { HalamanPengaturan } from "../halaman-pengaturan";

export const metadata: Metadata = { title: "Rekening" };

export default function Page() {
  return (
    <HalamanPengaturan
      bagian="sistem"
      judul="Rekening"
      keterangan="Rekening tujuan yang dicocokkan OCR saat membaca bukti transfer"
    />
  );
}
