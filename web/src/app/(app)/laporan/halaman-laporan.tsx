"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Download, FileSpreadsheet, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/page-header";
import { unduhBerkas } from "@/features/pengaturan/api";

const laporan = [
  {
    id: "pembayaran",
    nama: "Laporan Pembayaran",
    keterangan:
      "Tiga lembar: ringkasan per kelas, tagihan per mahasiswa, dan riwayat seluruh pembayaran.",
    path: "reports/pembayaran.xlsx",
    berkas: "laporan-pembayaran.xlsx",
  },
];

export function HalamanLaporan() {
  const [mengunduh, setMengunduh] = useState<string | null>(null);

  async function unduh(item: (typeof laporan)[number]) {
    setMengunduh(item.id);
    try {
      await unduhBerkas(item.path, item.berkas);
      toast.success(`${item.nama} diunduh.`);
    } catch {
      toast.error("Berkas gagal diunduh.");
    } finally {
      setMengunduh(null);
    }
  }

  return (
    <div className="flex flex-col gap-5 p-6">
      <PageHeader
        title="Laporan"
        description="Unduh rekap tagihan dan pembayaran dalam format Excel"
      />

      <ul className="flex flex-col gap-3">
        {laporan.map((item) => (
          <li
            key={item.id}
            className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-border bg-card px-5 py-4"
          >
            <div className="flex items-start gap-3">
              <FileSpreadsheet className="mt-0.5 size-5 shrink-0 text-success" />
              <div>
                <p className="font-medium">{item.nama}</p>
                <p className="max-w-xl text-sm text-muted-foreground">
                  {item.keterangan}
                </p>
              </div>
            </div>
            <Button
              variant="outline"
              disabled={mengunduh === item.id}
              onClick={() => unduh(item)}
            >
              {mengunduh === item.id ? (
                <Loader2 className="animate-spin" />
              ) : (
                <Download />
              )}
              Unduh Excel
            </Button>
          </li>
        ))}
      </ul>

      <p className="text-sm text-muted-foreground">
        Kuitansi PDF per pembayaran diunduh dari panel verifikasi, pada
        pembayaran yang sudah diverifikasi.
      </p>
    </div>
  );
}
