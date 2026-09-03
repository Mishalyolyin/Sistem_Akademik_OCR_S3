import type { Metadata } from "next";
import { HalamanKelas } from "./halaman-kelas";

export const metadata: Metadata = { title: "Kelas" };

export default function Page() {
  return <HalamanKelas />;
}
