import type { Metadata } from "next";
import { HalamanImport } from "./halaman-import";

export const metadata: Metadata = { title: "Import Mahasiswa" };

export default function Page() {
  return <HalamanImport />;
}
