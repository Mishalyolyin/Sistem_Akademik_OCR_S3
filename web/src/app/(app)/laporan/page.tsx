import type { Metadata } from "next";
import { HalamanLaporan } from "./halaman-laporan";

export const metadata: Metadata = { title: "Laporan" };

export default function Page() {
  return <HalamanLaporan />;
}
