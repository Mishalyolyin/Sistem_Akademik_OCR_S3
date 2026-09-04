"use client";

import { toast } from "sonner";
import { AlertTriangle, Loader2 } from "lucide-react";
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
import { useDeleteStudent, type StudentSummary } from "./api";

/**
 * Menghapus satu mahasiswa.
 *
 * <p>Dimintakan konfirmasi karena tindakannya tidak bisa dibatalkan, dan karena
 * di basis data seluruh tagihan serta pembayaran menempel ke baris mahasiswa
 * dengan {@code ON DELETE CASCADE}. Backend menolak menghapus mahasiswa yang
 * sudah punya riwayat uang, dan penolakannya ditampilkan apa adanya di sini —
 * bukan diringkas jadi "gagal", karena justru alasannya yang menuntun admin ke
 * jalan keluar yang benar: menonaktifkan.
 */
export function DialogHapusMahasiswa({
  open,
  onOpenChange,
  mahasiswa,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mahasiswa: StudentSummary;
}) {
  const hapus = useDeleteStudent();

  function jalankan() {
    hapus.mutate(mahasiswa.id, {
      onSuccess: () => {
        toast.success(`${mahasiswa.name} dihapus beserta akunnya.`);
        onOpenChange(false);
      },
      onError: (e) =>
        toast.error(
          e instanceof ApiError ? e.message : "Gagal menghapus mahasiswa.",
        ),
    });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Hapus mahasiswa?</DialogTitle>
          <DialogDescription>
            {mahasiswa.name} · {mahasiswa.nim}
          </DialogDescription>
        </DialogHeader>

        <p className="flex gap-2 rounded-lg border border-warning/25 bg-warning-soft px-4 py-3 text-sm text-warning">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" />
          <span>
            Data dan akunnya hilang permanen. Untuk mahasiswa yang sudah
            berjalan, <strong>nonaktifkan</strong> lewat Ubah data — riwayatnya
            tetap utuh dan namanya hilang dari daftar aktif.
          </span>
        </p>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
          >
            Batal
          </Button>
          <Button
            type="button"
            variant="destructive"
            disabled={hapus.isPending}
            onClick={jalankan}
          >
            {hapus.isPending && <Loader2 className="animate-spin" />}
            Hapus
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
