"use client";

import { useEffect } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { History, Info } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { InstallmentStatusBadge } from "@/components/status-badge";
import { ApiError } from "@/lib/api";
import { formatRupiah, formatTanggalJam } from "@/lib/format";
import {
  useAmountChanges,
  useUpdateInstallmentAmount,
  type Installment,
} from "./api";

const schema = z.object({
  amount: z.coerce
    .number({ message: "Nominal harus berupa angka." })
    .int("Nominal tidak boleh mengandung pecahan.")
    .positive("Nominal harus lebih besar dari nol."),
  reason: z.string().trim().min(5, "Alasan wajib diisi, minimal 5 karakter."),
});

type FormValues = z.input<typeof schema>;

/** Status dihitung ulang dari amount_paid dibanding amount BARU. */
function hitungStatus(amount: number, amountPaid: number) {
  if (amountPaid >= amount) return "PAID" as const;
  if (amountPaid > 0) return "PARTIAL" as const;
  return "UNPAID" as const;
}

export function DialogUbahNominal({
  installment,
  studentId,
  onClose,
}: {
  installment: Installment | null;
  studentId: number;
  onClose: () => void;
}) {
  const ubah = useUpdateInstallmentAmount(studentId);
  const riwayat = useAmountChanges(installment?.id ?? null);

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { amount: 0, reason: "" },
  });

  useEffect(() => {
    if (installment) {
      reset({ amount: Math.round(Number(installment.amount)), reason: "" });
    }
  }, [installment, reset]);

  // useWatch, bukan watch(): lihat catatan yang sama di halaman-kelas.tsx.
  // Dipanggil sebelum keluar lebih awal, karena urutan hook harus tetap sama
  // di setiap render.
  const nominalDiisi = useWatch({ control, name: "amount" });

  if (!installment) return null;

  const sudahDibayar = Number(installment.amountPaid);
  const nominalBaru = Number(nominalDiisi) || 0;
  const statusBaru = hitungStatus(nominalBaru, sudahDibayar);
  const jadiLunasTanpaRefund = nominalBaru > 0 && sudahDibayar > nominalBaru;

  function onSubmit(values: FormValues) {
    ubah.mutate(
      {
        installmentId: installment!.id,
        amount: Number(values.amount),
        reason: values.reason,
      },
      {
        onSuccess: (change) => {
          toast.success(
            `Cicilan ${installment!.installmentNo} diubah: ${formatRupiah(change.oldAmount)} → ${formatRupiah(change.newAmount)}`,
          );
          onClose();
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal menyimpan perubahan.",
          ),
      },
    );
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            Ubah nominal cicilan {installment.installmentNo}
          </DialogTitle>
          <DialogDescription>
            Nominal biasanya terhitung otomatis dari golongan potongan. Ubah
            hanya untuk kasus khusus — setiap perubahan tercatat.
          </DialogDescription>
        </DialogHeader>

        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-4"
        >
          <dl className="flex flex-col divide-y divide-border rounded-lg border border-border text-sm">
            <div className="flex justify-between px-3 py-2">
              <dt className="text-muted-foreground">Nominal sekarang</dt>
              <dd className="tnum">{formatRupiah(installment.amount)}</dd>
            </div>
            <div className="flex justify-between px-3 py-2">
              <dt className="text-muted-foreground">Sudah dibayar</dt>
              <dd className="tnum">{formatRupiah(installment.amountPaid)}</dd>
            </div>
            <div className="flex items-center justify-between px-3 py-2">
              <dt className="text-muted-foreground">Status setelah diubah</dt>
              <dd>
                <InstallmentStatusBadge status={statusBaru} />
              </dd>
            </div>
          </dl>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="amount">Nominal baru</Label>
            <Input
              id="amount"
              type="number"
              inputMode="numeric"
              step={1}
              aria-invalid={Boolean(errors.amount)}
              {...register("amount")}
            />
            {errors.amount && (
              <p className="text-xs text-danger">{errors.amount.message}</p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="reason">Alasan</Label>
            <Textarea
              id="reason"
              rows={2}
              placeholder="Contoh: potongan tambahan sesuai MoU instansi."
              aria-invalid={Boolean(errors.reason)}
              {...register("reason")}
            />
            {errors.reason && (
              <p className="text-xs text-danger">{errors.reason.message}</p>
            )}
          </div>

          {jadiLunasTanpaRefund && (
            <p className="flex gap-2 rounded-md border border-warning/25 bg-warning-soft px-3 py-2 text-xs text-warning">
              <Info className="size-4 shrink-0" />
              <span>
                Nominal baru lebih kecil dari yang sudah dibayar. Cicilan jadi
                lunas, tapi kelebihan {formatRupiah(sudahDibayar - nominalBaru)}{" "}
                <strong>tidak dikembalikan otomatis</strong> — pakai fitur
                Penyesuaian bila perlu dikembalikan.
              </span>
            </p>
          )}

          {riwayat.data && riwayat.data.length > 0 && (
            <div className="flex flex-col gap-1.5">
              <p className="flex items-center gap-1.5 text-xs font-semibold text-muted-foreground uppercase">
                <History className="size-3.5" />
                Riwayat perubahan
              </p>
              <ul className="flex flex-col gap-1.5 rounded-lg border border-border p-3 text-xs">
                {riwayat.data.map((change) => (
                  <li key={change.id}>
                    <span className="tnum">
                      {formatRupiah(change.oldAmount)} →{" "}
                      {formatRupiah(change.newAmount)}
                    </span>
                    <span className="block text-muted-foreground">
                      {change.reason} · {formatTanggalJam(change.createdAt)}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Batal
            </Button>
            <Button type="submit" loading={ubah.isPending}>
              Simpan perubahan
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
