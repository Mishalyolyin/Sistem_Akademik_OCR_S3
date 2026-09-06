"use client";

import { useSyncExternalStore } from "react";
import { CalendarRange } from "lucide-react";
import { cn } from "@/lib/utils";
import { labelSemester, semesterAktif } from "@/lib/semester";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";

/**
 * Semester akademik yang sedang berjalan, diturunkan dari tanggal.
 *
 * <h2>Kenapa tidak dirender di server</h2>
 *
 * <p>Halaman ini dibangun sebagai HTML statis saat build. Kalau semesternya
 * dihitung di sana, yang tercetak adalah semester pada saat build — dan ia
 * akan tetap berbunyi "Gasal 2026/2027" berbulan-bulan setelah Genap dimulai,
 * tanpa satu pun galat yang memberi tahu. Karena itu penanda ini sengaja
 * kosong sampai hidrasi, lalu diisi dari jam peramban.
 *
 * <p>Placeholder-nya berukuran sama supaya tata letak tidak melompat.
 */
const tanpaLangganan = () => () => {};

function useSudahDiHidrasi() {
  return useSyncExternalStore(
    tanpaLangganan,
    () => true,
    () => false,
  );
}

/** Bentuk ringkas untuk top bar admin. */
export function ChipSemester({ className }: { className?: string }) {
  const siap = useSudahDiHidrasi();
  if (!siap) {
    return <div className={cn("h-6 w-32", className)} aria-hidden />;
  }

  const semester = semesterAktif();

  return (
    <Tooltip>
      <TooltipTrigger
        render={
          <span
            className={cn(
              "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs",
              semester.jeda
                ? "border-border bg-muted text-muted-foreground"
                : "border-primary/25 bg-primary/10 text-primary",
              className,
            )}
          >
            <CalendarRange className="size-3.5" aria-hidden />
            {semester.jeda ? "Jeda semester" : labelSemester(semester)}
          </span>
        }
      />
      <TooltipContent side="bottom">
        {semester.jeda ? (
          <>
            Tidak ada angsuran jatuh tempo di Juli–Agustus.
            <br />
            {labelSemester(semester)} dimulai September.
          </>
        ) : (
          <>
            Semester berjalan menurut tanggal hari ini.
            <br />
            Angsuran jatuh tempo {semester.bulan}.
          </>
        )}
      </TooltipContent>
    </Tooltip>
  );
}

/**
 * Bentuk kartu untuk portal mahasiswa.
 *
 * <p>Isinya lebih dari sekadar nama semester: yang dicari mahasiswa saat
 * membuka halaman tagihan adalah kapan ia harus membayar, bukan istilah
 * akademiknya. Karena itu bulan jatuh temponya ikut disebut.
 */
export function KartuSemester({ className }: { className?: string }) {
  const siap = useSudahDiHidrasi();
  if (!siap) {
    return <div className={cn("h-16 rounded-lg bg-muted/40", className)} aria-hidden />;
  }

  const semester = semesterAktif();

  return (
    <section
      className={cn(
        "flex items-start gap-3 rounded-lg border px-4 py-3",
        semester.jeda
          ? "border-border bg-muted/40"
          : "border-primary/20 bg-primary/5",
        className,
      )}
    >
      <CalendarRange
        className={cn(
          "mt-0.5 size-5 shrink-0",
          semester.jeda ? "text-muted-foreground" : "text-primary",
        )}
        aria-hidden
      />
      <div className="min-w-0">
        {semester.jeda ? (
          <>
            <p className="font-heading text-sm font-semibold">
              Jeda antar semester
            </p>
            <p className="text-sm text-muted-foreground">
              Tidak ada angsuran yang jatuh tempo bulan ini. Semester{" "}
              {labelSemester(semester)} dimulai September, dengan angsuran{" "}
              {semester.bulan}.
            </p>
          </>
        ) : (
          <>
            <p className="font-heading text-sm font-semibold">
              Semester berjalan: {labelSemester(semester)}
            </p>
            <p className="text-sm text-muted-foreground">
              Angsuran UKT semester ini jatuh tempo {semester.bulan}, lima kali,
              satu tiap bulan.
            </p>
          </>
        )}
      </div>
    </section>
  );
}
