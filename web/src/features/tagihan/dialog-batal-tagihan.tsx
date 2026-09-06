"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Loader2, TriangleAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/api";
import { formatRupiah } from "@/lib/format";
import { useCancelPlan, type PaymentPlan } from "./api";

const ALASAN_MINIMAL = 5;

/**
 * Membatalkan satu tagihan yang salah dibuat.
 *
 * <p>Yang dibatalkan adalah tagihannya, bukan uangnya. Tagihan yang sudah
 * menerima pembayaran terverifikasi akan ditolak backend, dan pesannya
 * menyebut berapa pembayaran dan berapa nominalnya — jadi tombol ini tetap
 * ditampilkan untuk tagihan seperti itu, bukan disembunyikan: admin berhak
 * tahu apa yang menghalangi, bukan sekadar menemukan tombol yang hilang.
 */
export function DialogBatalTagihan({
  open,
  onOpenChange,
  studentId,
  plan,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  studentId: number;
  plan: PaymentPlan | null;
}) {
  const batalkan = useCancelPlan(studentId);
  const [alasan, setAlasan] = useState("");

  if (!plan) return null;

  const sudahDibayar = Number(plan.amountPaid) > 0;
  const bolehKirim = alasan.trim().length >= ALASAN_MINIMAL;

  function tutup() {
    setAlasan("");
    onOpenChange(false);
  }

  async function kirim() {
    try {
      await batalkan.mutateAsync({ planId: plan!.id, reason: alasan.trim() });
      toast.success(`Tagihan ${plan!.categoryLabel} dibatalkan.`);
      tutup();
    } catch (error) {
      toast.error(
        error instanceof ApiError
          ? error.message
          : "Tagihan gagal dibatalkan.",
      );
    }
  }

  return (
    <Dialog open={open} onOpenChange={(next) => (next ? onOpenChange(true) : tutup())}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Batalkan tagihan</DialogTitle>
          <DialogDescription>
            {plan.categoryLabel}
            {plan.semesterNumber ? ` semester ${plan.semesterNumber}` : ""} ·{" "}
            {plan.academicYear} {plan.term === "GASAL" ? "Gasal" : "Genap"} ·{" "}
            {formatRupiah(plan.totalAmount)}
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-3">
          <div className="flex items-start gap-2.5 rounded-md border border-border bg-muted/50 px-3 py-2 text-sm">
            <TriangleAlert
              className="mt-0.5 size-4 shrink-0 text-warning"
              aria-hidden
            />
            <p className="text-muted-foreground">
              Tagihan yang dibatalkan berhenti dihitung: tidak lagi muncul di
              layar mahasiswa, tidak lagi memakan jatah semester, dan slotnya
              bebas dipakai tagihan baru. Barisnya tetap tersimpan beserta
              alasan ini.
            </p>
          </div>

          {sudahDibayar && (
            <div className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm">
              Tagihan ini sudah menerima {formatRupiah(plan.amountPaid)}.
              Batalkan dulu keputusan verifikasi tiap pembayarannya di panel
              verifikasi — masing-masing dengan alasannya sendiri — baru
              tagihannya bisa dibatalkan.
            </div>
          )}

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="alasan-batal">Alasan</Label>
            <Textarea
              id="alasan-batal"
              rows={3}
              placeholder="Contoh: salah pilih mahasiswa, tagihan ganda"
              value={alasan}
              onChange={(e) => setAlasan(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Wajib diisi, minimal {ALASAN_MINIMAL} karakter. Alasan ini yang
              akan dibaca orang lain ketika tagihan ini dipertanyakan nanti.
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={tutup}>
            Batal
          </Button>
          <Button
            variant="destructive"
            disabled={!bolehKirim || batalkan.isPending}
            onClick={kirim}
          >
            {batalkan.isPending && <Loader2 className="animate-spin" />}
            Batalkan tagihan
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
