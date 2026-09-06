import type { Metadata } from "next";
import { HalamanDisertasi } from "./halaman-disertasi";

export const metadata: Metadata = { title: "Disertasi" };

export default function Page() {
  return <HalamanDisertasi />;
}
