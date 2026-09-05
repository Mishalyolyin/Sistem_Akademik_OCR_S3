import type { Metadata } from "next";
import { HalamanPengaturan } from "../halaman-pengaturan";
import { PanelPengingat } from "@/features/pengingat/panel-pengingat";

export const metadata: Metadata = { title: "Pengingat WhatsApp" };

export default function Page() {
  return (
    <HalamanPengaturan
      bagian="pengingat"
      judul="Pengingat WhatsApp"
      keterangan="Pengingat jatuh tempo cicilan yang dikirim otomatis tiap hari"
    >
      <PanelPengingat />
    </HalamanPengaturan>
  );
}
