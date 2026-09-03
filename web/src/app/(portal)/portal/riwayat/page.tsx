import type { Metadata } from "next";
import { HalamanRiwayat } from "./halaman-riwayat";

export const metadata: Metadata = { title: "Riwayat Pembayaran" };

export default function Page() {
  return <HalamanRiwayat />;
}
