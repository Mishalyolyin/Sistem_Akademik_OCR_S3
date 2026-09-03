import type { Metadata } from "next";
import { HalamanTarif } from "./halaman-tarif";

export const metadata: Metadata = { title: "Tarif & Potongan" };

export default function Page() {
  return <HalamanTarif />;
}
