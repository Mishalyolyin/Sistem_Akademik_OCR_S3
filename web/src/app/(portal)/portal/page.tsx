import type { Metadata } from "next";
import { HalamanTagihan } from "./halaman-tagihan";

export const metadata: Metadata = { title: "Tagihan Saya" };

export default function Page() {
  return <HalamanTagihan />;
}
