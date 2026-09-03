import type { Metadata } from "next";
import { HalamanProfil } from "./halaman-profil";

export const metadata: Metadata = { title: "Profil" };

export default function Page() {
  return <HalamanProfil />;
}
