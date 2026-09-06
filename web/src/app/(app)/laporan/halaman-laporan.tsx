"use client";

import { useState } from "react";
import { toast } from "sonner";
import {
  Download,
  FileSpreadsheet,
  Loader2,
  ShieldAlert,
  Table2,
  type LucideIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/page-header";
import { unduhBerkas } from "@/features/pengaturan/api";

type Laporan = {
  id: string;
  nama: string;
  keterangan: string;
  path: string;
  berkas: string;
  icon: LucideIcon;
  warnaIkon: string;
  labelTombol: string;
};

const laporan: Laporan[] = [
  {
    id: "pembayaran",
    nama: "Laporan Pembayaran",
    keterangan:
      "Tiga lembar: ringkasan per kelas, tagihan per mahasiswa, dan riwayat seluruh pembayaran.",
    path: "reports/pembayaran.xlsx",
    berkas: "laporan-pembayaran.xlsx",
    icon: FileSpreadsheet,
    warnaIkon: "text-success",
    labelTombol: "Unduh Excel",
  },
  {
    id: "dataset-ocr",
    nama: "Dataset Pembacaan Bukti",
    keterangan:
      "Satu baris per bukti bayar yang sudah diputuskan admin: apa yang dibaca mesin " +
      "disandingkan dengan keputusan akhirnya. Bahan untuk mengukur ambang keyakinan " +
      "dan melatih model. Verifikasi otomatis yang belum disentuh admin tidak ikut — " +
      "itu tebakan mesin sendiri, bukan jawaban.",
    path: "reports/dataset-ocr.csv",
    berkas: "dataset-ocr.csv",
    icon: Table2,
    warnaIkon: "text-primary",
    labelTombol: "Unduh CSV",
  },
];

export function HalamanLaporan() {
  const [mengunduh, setMengunduh] = useState<string | null>(null);
  // Mati secara bawaan, dan sengaja tidak diingat antar kunjungan: menyertakan
  // teks struk harus jadi keputusan sadar tiap kali, bukan setelan yang
  // terlanjur menyala.
  const [sertakanTeks, setSertakanTeks] = useState(false);

  async function unduh(item: Laporan) {
    const ikutTeks = item.id === "dataset-ocr" && sertakanTeks;
    const path = ikutTeks ? `${item.path}?sertakanTeks=true` : item.path;

    setMengunduh(item.id);
    try {
      await unduhBerkas(path, item.berkas);
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
        description="Unduh rekap tagihan, pembayaran, dan dataset pembacaan bukti"
      />

      <ul className="flex flex-col gap-3">
        {laporan.map((item) => {
          const Icon = item.icon;
          return (
            <li
              key={item.id}
              className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-border bg-card px-5 py-4"
            >
              <div className="flex items-start gap-3">
                <Icon className={`mt-0.5 size-5 shrink-0 ${item.warnaIkon}`} />
                <div>
                  <p className="font-medium">{item.nama}</p>
                  <p className="max-w-xl text-sm text-muted-foreground">
                    {item.keterangan}
                  </p>

                  {item.id === "dataset-ocr" && (
                    <div className="mt-3 flex max-w-xl items-start gap-2.5 rounded-md border border-warning/40 bg-warning/10 px-3 py-2">
                      <Checkbox
                        id="sertakan-teks"
                        checked={sertakanTeks}
                        onCheckedChange={(nilai) =>
                          setSertakanTeks(nilai === true)
                        }
                        className="mt-0.5"
                      />
                      <Label
                        htmlFor="sertakan-teks"
                        className="flex flex-col items-start gap-0.5 font-normal"
                      >
                        <span className="flex items-center gap-1.5 font-medium">
                          <ShieldAlert className="size-3.5 text-warning" />
                          Sertakan teks mentah hasil OCR
                        </span>
                        <span className="text-xs text-muted-foreground">
                          Fitur paling berguna untuk model, tapi isinya seluruh
                          tulisan di struk — nama, nomor rekening, dan saldo.
                          Jangan sebar berkasnya.
                        </span>
                      </Label>
                    </div>
                  )}
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
                {item.labelTombol}
              </Button>
            </li>
          );
        })}
      </ul>

      <p className="text-sm text-muted-foreground">
        Kuitansi PDF per pembayaran diunduh dari panel verifikasi, pada
        pembayaran yang sudah diverifikasi.
      </p>
    </div>
  );
}
