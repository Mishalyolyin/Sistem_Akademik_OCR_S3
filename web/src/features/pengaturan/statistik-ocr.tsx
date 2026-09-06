"use client";

import { useQuery } from "@tanstack/react-query";
import {
  Brain,
  CircleCheck,
  CircleX,
  Images,
  TriangleAlert,
} from "lucide-react";
import { apiFetch } from "@/lib/api";
import { cn } from "@/lib/utils";

export type StatistikOcr = {
  berlabel: number;
  diterima: number;
  ditolak: number;
  adaGambar: number;
  sudahDibaca: number;
  belumDiputuskan: number;
  mesinSepakat: number;
  mesinKeliru: number;
  /**
   * Keduanya hilang sama sekali dari JSON saat nilainya kosong, bukan terkirim
   * sebagai null — karena itu tipenya ikut menyebut `undefined`, dan
   * perbandingannya di bawah memakai `== null` yang menangkap keduanya.
   */
  rataKeyakinan?: string | null;
  /** Persentase kesepakatan mesin dengan admin; kosong bila belum ada bandingannya. */
  akurasiMesin?: string | null;
};

export function useStatistikOcr() {
  return useQuery({
    queryKey: ["statistik-ocr"],
    queryFn: () => apiFetch<StatistikOcr>("/reports/statistik-ocr"),
  });
}

/**
 * Seberapa jauh sistem sudah belajar dari keputusan admin.
 *
 * <p>Angka yang paling menentukan bukan berapa banyak bukti yang sudah dibaca,
 * melainkan berapa kali admin memutuskan berbeda dari usul mesin. Itulah
 * satu-satunya ukuran yang menjawab apakah ambang keyakinan di atas
 * kekencangan atau kekendoran — tanpanya, angka ambang itu tebakan yang tidak
 * pernah diperiksa.
 */
export function PanelStatistikOcr() {
  const { data, isPending, error } = useStatistikOcr();

  if (isPending || error || !data) {
    return null;
  }

  const akurasi = data.akurasiMesin == null ? null : Number(data.akurasiMesin);
  const dibandingkan = data.mesinSepakat + data.mesinKeliru;

  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-border/70 bg-card shadow-sm px-5 py-4">
      <div className="flex items-start gap-3">
        <Brain className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden />
        <div>
          <h3 className="font-heading text-sm font-semibold">
            Bahan belajar yang sudah terkumpul
          </h3>
          <p className="text-sm text-muted-foreground">
            Tiap keputusan admin atas satu bukti menambah satu contoh berlabel.
            Makin banyak yang terkumpul, makin bisa diukur apakah ambang di atas
            sudah pas.
          </p>
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Angka
          label="Contoh berlabel"
          nilai={data.berlabel}
          keterangan="diputuskan admin"
          icon={Brain}
        />
        <Angka
          label="Diterima"
          nilai={data.diterima}
          keterangan="contoh positif"
          icon={CircleCheck}
          warna="text-success"
        />
        <Angka
          label="Ditolak"
          nilai={data.ditolak}
          keterangan="contoh negatif"
          icon={CircleX}
          warna="text-destructive"
        />
        <Angka
          label="Punya gambar"
          nilai={data.adaGambar}
          keterangan="siap jadi dataset gambar"
          icon={Images}
        />
      </div>

      {/* Kesepakatan mesin — bagian yang benar-benar menjawab "ambangnya pas atau tidak". */}
      <div className="rounded-md border border-border bg-muted/40 px-4 py-3">
        {dibandingkan === 0 ? (
          <p className="text-sm text-muted-foreground">
            Belum ada bukti yang pernah diusulkan mesin sekaligus diputuskan
            admin, jadi belum ada yang bisa dibandingkan. Angka ini mulai
            berarti setelah beberapa puluh bukti diverifikasi.
          </p>
        ) : (
          <>
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <p className="text-sm font-medium">Mesin sepakat dengan admin</p>
              <p
                className={cn(
                  "font-heading text-lg font-semibold tnum",
                  akurasi !== null && akurasi >= 90
                    ? "text-success"
                    : akurasi !== null && akurasi >= 70
                      ? "text-warning"
                      : "text-destructive",
                )}
              >
                {akurasi?.toFixed(1)}%
              </p>
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              {data.mesinSepakat} sepakat, {data.mesinKeliru} berbeda, dari{" "}
              {dibandingkan} bukti yang pernah diusulkan mesin.
              {data.rataKeyakinan != null && (
                <>
                  {" "}
                  Rata-rata keyakinan pembacaan{" "}
                  {(Number(data.rataKeyakinan) * 100).toFixed(0)}%.
                </>
              )}
            </p>
            {akurasi !== null && akurasi < 70 && (
              <p className="mt-2 flex items-start gap-2 text-xs text-warning">
                <TriangleAlert
                  className="mt-0.5 size-3.5 shrink-0"
                  aria-hidden
                />
                Mesin lebih sering keliru daripada seharusnya. Naikkan ambang
                verifikasi otomatis supaya lebih banyak bukti masuk antrean
                tinjauan, bukan langsung diterima.
              </p>
            )}
          </>
        )}
      </div>

      {data.belumDiputuskan > 0 && (
        <p className="text-xs text-muted-foreground">
          {data.belumDiputuskan} bukti masih menunggu keputusan. Tiap satu yang
          diputuskan menambah satu contoh berlabel.
        </p>
      )}
    </section>
  );
}

function Angka({
  label,
  nilai,
  keterangan,
  icon: Icon,
  warna,
}: {
  label: string;
  nilai: number;
  keterangan: string;
  icon: React.ComponentType<{ className?: string }>;
  warna?: string;
}) {
  return (
    <div className="rounded-md border border-border px-3 py-2.5">
      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <Icon className={cn("size-3.5", warna)} aria-hidden />
        {label}
      </div>
      <p className="mt-0.5 font-heading text-xl font-semibold tnum">{nilai}</p>
      <p className="text-xs text-muted-foreground">{keterangan}</p>
    </div>
  );
}
