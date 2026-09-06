"use client";

import { useState } from "react";
import { toast } from "sonner";
import { AlertTriangle, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ErrorState, TableSkeleton } from "@/components/page-header";
import { ApiError } from "@/lib/api";
import { formatTanggalJam } from "@/lib/format";
import {
  useJalankanPengingat,
  useStatusPengingat,
  type StatusPengingat,
} from "./api";

const labelStatus: Record<StatusPengingat, { teks: string; kelas: string }> = {
  SENT: { teks: "Terkirim", kelas: "text-success" },
  FAILED: { teks: "Gagal", kelas: "text-danger" },
  SKIPPED: { teks: "Dilewati", kelas: "text-muted-foreground" },
};

/**
 * Kesiapan gateway, tombol jalankan sekarang, dan riwayat kiriman.
 *
 * <p>Tombolnya ada karena penjadwal hanya berjalan sekali sehari: tanpa cara
 * menjalankannya sendiri, admin baru tahu pengaturannya salah keesokan harinya —
 * atau tidak tahu sama sekali, karena kegagalannya cuma tercatat di log server.
 */
export function PanelPengingat() {
  const status = useStatusPengingat();
  const [konfirmasi, setKonfirmasi] = useState(false);

  return (
    <section className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h3 className="font-heading text-sm font-semibold">Riwayat kiriman</h3>
        <Button variant="outline" onClick={() => setKonfirmasi(true)}>
          <Send />
          Jalankan sekarang
        </Button>
      </div>

      {status.data && !status.data.gatewaySiap && (
        <p className="flex gap-2 rounded-lg border border-warning/25 bg-warning-soft px-4 py-3 text-sm text-warning">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" />
          <span>
            Token gateway belum diisi, jadi pesan hanya dicatat di log server
            dan <strong>tidak dikirim</strong>. Aturannya tetap berjalan, jadi
            ini aman dipakai untuk mencoba dulu.
          </span>
        </p>
      )}

      {status.isPending ? (
        <TableSkeleton rows={4} />
      ) : status.error ? (
        <ErrorState
          message={
            status.error instanceof ApiError
              ? status.error.message
              : "Coba muat ulang halaman."
          }
        />
      ) : status.data.riwayat.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground">
          Belum ada pengingat yang dikirim.
        </p>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-border/70 bg-card shadow-sm">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Waktu</TableHead>
                <TableHead>Jenis</TableHead>
                <TableHead>Nomor</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Keterangan</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {status.data.riwayat.map((baris) => (
                <TableRow key={baris.id}>
                  <TableCell className="whitespace-nowrap text-muted-foreground">
                    {formatTanggalJam(baris.waktu)}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {baris.jenis === "LEWAT_TEMPO"
                      ? "Lewat tempo"
                      : "Menjelang jatuh tempo"}
                  </TableCell>
                  <TableCell className="font-mono text-xs">
                    {baris.nomor ?? "—"}
                  </TableCell>
                  <TableCell className={labelStatus[baris.status].kelas}>
                    {labelStatus[baris.status].teks}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {baris.galat ?? "—"}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {konfirmasi && <DialogJalankan onClose={() => setKonfirmasi(false)} />}
    </section>
  );
}

function DialogJalankan({ onClose }: { onClose: () => void }) {
  const jalankan = useJalankanPengingat();

  return (
    <Dialog open onOpenChange={(next) => (next ? undefined : onClose())}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Jalankan pengingat sekarang?</DialogTitle>
          <DialogDescription>
            Tanpa menunggu jadwal hariannya.
          </DialogDescription>
        </DialogHeader>

        <p className="flex gap-2 rounded-lg border border-warning/25 bg-warning-soft px-4 py-3 text-sm text-warning">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" />
          <span>
            Kalau gateway sudah diatur, pesannya benar-benar terkirim ke ponsel
            mahasiswa. Yang sudah pernah diingatkan tidak akan dikirimi lagi.
          </span>
        </p>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            Batal
          </Button>
          <Button
            type="button"
            loading={jalankan.isPending}
            onClick={() =>
              jalankan.mutate(undefined, {
                onSuccess: (hasil) => {
                  toast.success(
                    `${hasil.diperiksa} cicilan diperiksa — ${hasil.terkirim} terkirim, ${hasil.dilewati} dilewati, ${hasil.gagal} gagal.`,
                  );
                  onClose();
                },
                onError: (e) =>
                  toast.error(
                    e instanceof ApiError
                      ? e.message
                      : "Gagal menjalankan pengingat.",
                  ),
              })
            }
          >
            Jalankan
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
