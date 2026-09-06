import type { Metadata } from "next";
import { Beranda } from "./(landing)/beranda";

export const metadata: Metadata = {
  title: "Sistem Pembayaran Program Doktor PAI · UNISSULA",
  description:
    "Seluruh tagihan Program Doktor Pendidikan Agama Islam UNISSULA dalam satu tempat: pendaftaran, UKT enam semester, dan empat tahap ujian.",
};

export default function Page() {
  return <Beranda />;
}
