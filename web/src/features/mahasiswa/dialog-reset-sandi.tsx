"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Info, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ApiError } from "@/lib/api";
import { useResetKataSandi } from "./api";

/**
 * Mengembalikan kata sandi mahasiswa ke NIM-nya.
 *
 * <p>Dimintakan konfirmasi lebih dulu karena tindakannya mencabut seluruh sesi
 * mahasiswa itu: kalau ia sedang mengunggah bukti bayar, unggahannya terputus.
 */
export function DialogResetSandi({
  open,
  onOpenChange,
  studentId,
  nim,
  nama,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  studentId: number;
  nim: string;
  nama: string;
}) {
  const reset = useResetKataSandi();
  const [selesai, setSelesai] = useState(false);

  function tutup() {
    setSelesai(false);
    onOpenChange(false);
  }

  function jalankan() {
    reset.mutate(studentId, {
      onSuccess: () => {
        setSelesai(true);
        toast.success(`Kata sandi ${nama} dikembalikan ke NIM.`);
      },
      onError: (e) =>
        toast.error(
          e instanceof ApiError ? e.message : "Gagal mengembalikan kata sandi.",
        ),
    });
  }

  return (
    <Dialog open={open} onOpenChange={(next) => (next ? onOpenChange(true) : tutup())}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Kembalikan kata sandi ke NIM</DialogTitle>
          <DialogDescription>
            {selesai
              ? "Sudah dikembalikan. Sampaikan ke mahasiswa yang bersangkutan."
              : `Kata sandi ${nama} akan diatur ulang menjadi NIM-nya.`}
          </DialogDescription>
        </DialogHeader>

        <dl className="flex flex-col divide-y divide-border rounded-lg border border-border text-sm">
          <div className="flex justify-between px-3 py-2">
            <dt className="text-muted-foreground">Mahasiswa</dt>
            <dd>{nama}</dd>
          </div>
          <div className="flex justify-between px-3 py-2">
            <dt className="text-muted-foreground">Kata sandi barunya</dt>
            <dd className="tnum font-medium">{nim}</dd>
          </div>
        </dl>

        {!selesai && (
          <p className="flex gap-2 rounded-md border border-warning/25 bg-warning-soft px-3 py-2 text-xs text-warning">
            <Info className="size-4 shrink-0" />
            <span>
              Semua sesi mahasiswa ini akan diakhiri. Kalau ia sedang mengunggah
              bukti bayar, unggahannya terputus.
            </span>
          </p>
        )}

        <DialogFooter>
          <Button type="button" variant="outline" onClick={tutup}>
            {selesai ? "Tutup" : "Batal"}
          </Button>
          {!selesai && (
            <Button type="button" onClick={jalankan} disabled={reset.isPending}>
              {reset.isPending && <Loader2 className="animate-spin" />}
              Kembalikan ke NIM
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
