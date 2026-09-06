"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { CalendarCheck, Loader2, Play, TriangleAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ApiError, apiFetch } from "@/lib/api";

export type HasilPutaranUkt = {
  diperiksa: number;
  dibuat: number;
  dilewati: number;
  gagal: number;
  catatan: string[];
};

function useJalankanUkt() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      apiFetch<HasilPutaranUkt>("/tagihan-ukt/jalankan", { method: "POST" }),
    onSuccess: () => {
      // Pengaturan ikut disegarkan: putaran ini menuliskan kapan ia terakhir
      // berjalan ke system_settings.
      queryClient.invalidateQueries({ queryKey: ["settings"] });
      queryClient.invalidateQueries({ queryKey: ["plans"] });
    },
  });
}

/**
 * Pembuatan tagihan UKT otomatis.
 *
 * <p>Putaran ini berjalan sendiri tiap hari, jadi tombol di sini bukan
 * satu-satunya jalan — ia memakai kode yang sama persis dengan penjadwal, hanya
 * dipicu lebih awal. Gunanya saat mahasiswa baru diimpor dan admin tidak ingin
 * menunggu putaran besok pagi.
 */
export function PanelUktOtomatis() {
  const jalankan = useJalankanUkt();
  const [hasil, setHasil] = useState<HasilPutaranUkt | null>(null);

  function jalankanSekarang() {
    jalankan.mutate(undefined, {
      onSuccess: (data) => {
        setHasil(data);
        toast.success(
          data.dibuat === 0
            ? "Tidak ada tagihan baru yang perlu dibuat."
            : `${data.dibuat} tagihan UKT dibuat.`,
        );
      },
      onError: (e) =>
        toast.error(
          e instanceof ApiError ? e.message : "Putaran gagal dijalankan.",
        ),
    });
  }

  return (
    <section className="flex flex-col gap-3 rounded-lg border border-border bg-card px-5 py-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex min-w-0 items-start gap-3">
          <CalendarCheck
            className="mt-0.5 size-5 shrink-0 text-primary"
            aria-hidden
          />
          <div className="min-w-0">
            <h3 className="font-heading text-sm font-semibold">
              Jalankan putaran sekarang
            </h3>
            <p className="max-w-2xl text-sm text-muted-foreground">
              Membuat tagihan UKT yang sudah waktunya untuk semua mahasiswa
              aktif. Tahun akademiknya diturunkan dari angkatan tiap mahasiswa —{" "}
              <strong>bukan</strong> dari tanggal hari ini — jadi angkatan Gasal
              dan Genap berjalan di jalurnya masing-masing. Aman dijalankan
              berapa kali pun: yang sudah ada tidak dibuat ulang, yang belum
              tiba waktunya tidak dibuat lebih dulu.
            </p>
          </div>
        </div>

        <Button
          variant="outline"
          disabled={jalankan.isPending}
          onClick={jalankanSekarang}
        >
          {jalankan.isPending ? <Loader2 className="animate-spin" /> : <Play />}
          Jalankan
        </Button>
      </div>

      {hasil && (
        <div className="rounded-md border border-border bg-muted/40 px-4 py-3 text-sm">
          <p>
            <span className="font-medium">{hasil.diperiksa}</span> mahasiswa
            diperiksa · <span className="font-medium">{hasil.dibuat}</span>{" "}
            tagihan dibuat · {hasil.dilewati} dilewati
            {hasil.gagal > 0 && (
              <span className="text-destructive">
                {" "}
                · {hasil.gagal} gagal
              </span>
            )}
          </p>

          {/*
            Sebab kegagalan ditampilkan apa adanya, bukan diringkas jadi satu
            angka. Sebab tersering — tarif tahun akademik itu belum diatur —
            justru yang paling mudah dibetulkan, tapi hanya kalau disebutkan.
          */}
          {hasil.catatan.length > 0 && (
            <ul className="mt-2 flex flex-col gap-1">
              {hasil.catatan.map((baris) => (
                <li
                  key={baris}
                  className="flex items-start gap-2 text-xs text-muted-foreground"
                >
                  <TriangleAlert
                    className="mt-0.5 size-3.5 shrink-0 text-warning"
                    aria-hidden
                  />
                  {baris}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}
