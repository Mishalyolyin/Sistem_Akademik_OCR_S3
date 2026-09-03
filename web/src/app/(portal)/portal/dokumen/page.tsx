import type { Metadata } from "next";
import { HalamanDokumen } from "./halaman-dokumen";

export const metadata: Metadata = { title: "Dokumen Wajib" };

export default function Page() {
  return <HalamanDokumen />;
}
