"use client";

import { useState } from "react";
import { toast } from "sonner";
import { AlertTriangle } from "lucide-react";
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
import {
  useHapusMassalStudents,
  type HapusMassalResponse,
  type StudentSummary,
} from "./api";

/**
 * Hapus beberapa mahasiswa sekaligus, untuk membereskan salah import.
 *
 * <p>Hasilnya dilaporkan per baris, bukan sebagai satu kata "gagal": dalam satu
 * pilihan biasanya hanya sebagian yang ditolak, dan admin perlu tahu yang mana
 * beserta alasannya — mahasiswa yang sudah punya tagihan atau bukti bayar tidak
 * bisa dihapus karena riwayat uangnya akan ikut hilang.
 */
export function DialogHapusMassal({
  open,
  onOpenChange,
  terpilih,
  onSelesai,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  terpilih: StudentSummary[];
  onSelesai: (idTerhapus: number[]) => void;
}) {
  const hapus = useHapusMassalStudents();
  const [hasil, setHasil] = useState<HapusMassalResponse | null>(null);

  function tutup() {
    setHasil(null);
    onOpenChange(false);
  }

  function jalankan() {
    hapus.mutate(
      terpilih.map((mhs) => mhs.id),
      {
        onSuccess: (response) => {
          onSelesai(
            response.rincian.filter((r) => r.berhasil).map((r) => r.id),
          );

          if (response.ditolak === 0) {
            toast.success(`${response.berhasil} mahasiswa dihapus.`);
            tutup();
            return;
          }

          // Masih ada yang ditolak: dialognya tetap terbuka supaya alasannya
          // sempat dibaca, bukan lewat sebagai toast yang keburu hilang.
          setHasil(response);
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal menghapus mahasiswa.",
          ),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={(next) => (next ? undefined : tutup())}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {hasil
              ? "Sebagian tidak bisa dihapus"
              : "Hapus mahasiswa terpilih?"}
          </DialogTitle>
          <DialogDescription>
            {hasil
              ? `${hasil.berhasil} dihapus, ${hasil.ditolak} ditolak.`
              : `${terpilih.length} mahasiswa dipilih.`}
          </DialogDescription>
        </DialogHeader>

        {hasil ? (
          <ul className="flex max-h-72 flex-col divide-y divide-border overflow-y-auto rounded-lg border border-border text-sm">
            {hasil.rincian
              .filter((baris) => !baris.berhasil)
              .map((baris) => (
                <li key={baris.id} className="px-3 py-2">
                  <span className="font-medium">
                    {baris.nama ?? `Mahasiswa #${baris.id}`}
                  </span>
                  {baris.nim && (
                    <span className="ml-2 font-mono text-xs text-muted-foreground">
                      {baris.nim}
                    </span>
                  )}
                  <span className="block text-xs text-warning">
                    {baris.alasan}
                  </span>
                </li>
              ))}
          </ul>
        ) : (
          <p className="flex gap-2 rounded-lg border border-warning/25 bg-warning-soft px-4 py-3 text-sm text-warning">
            <AlertTriangle className="mt-0.5 size-4 shrink-0" />
            <span>
              Data dan akunnya hilang permanen. Yang sudah punya tagihan atau
              bukti bayar akan ditolak — riwayat uangnya tidak boleh ikut
              terhapus.
            </span>
          </p>
        )}

        <DialogFooter>
          {hasil ? (
            <Button type="button" onClick={tutup}>
              Tutup
            </Button>
          ) : (
            <>
              <Button type="button" variant="outline" onClick={tutup}>
                Batal
              </Button>
              <Button
                type="button"
                variant="destructive"
                loading={hapus.isPending}
                onClick={jalankan}
              >
                Hapus {terpilih.length} mahasiswa
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
