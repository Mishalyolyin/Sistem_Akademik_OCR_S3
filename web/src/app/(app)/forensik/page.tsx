import type { Metadata } from "next";
import { HalamanForensik } from "./halaman-forensik";

export const metadata: Metadata = { title: "Forensik OCR" };

export default function Page() {
  return <HalamanForensik />;
}
