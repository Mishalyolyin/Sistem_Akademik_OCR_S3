import type { Metadata } from "next";
import { HalamanDashboard } from "./halaman-dashboard";

export const metadata: Metadata = { title: "Dashboard" };

export default function Page() {
  return <HalamanDashboard />;
}
