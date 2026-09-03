import type { Metadata } from "next";
import { HalamanMahasiswa } from "./halaman-mahasiswa";

export const metadata: Metadata = { title: "Mahasiswa" };

export default function Page() {
  return <HalamanMahasiswa />;
}
