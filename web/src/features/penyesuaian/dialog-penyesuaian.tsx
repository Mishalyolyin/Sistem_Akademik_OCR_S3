"use client";

import { useMemo, useState } from "react";
import { toast } from "sonner";
import { History, Info, Minus, Plus } from "lucide-react";
import { cn } from "@/lib/utils";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/api";
import { formatRupiah, formatTanggalJam } from "@/lib/format";
import type { PaymentPlan } from "@/features/tagihan/api";
import { useAdjustments, useCreateAdjustment } from "./api";

const SASARAN_SALDO = "saldo";

type Arah = "tambah" | "kurangi";

/**
 * Admin memilih arah lewat tombol, bukan mengetik tanda minus.
 * Salah tanda di sini berarti uang bergerak ke arah yang berlawanan, dan
 * satu-satunya jejaknya cuma baris audit yang sudah terlanjur tersimpan.
 */
export function DialogPenyesuaian({
  open,
  onOpenChange,
  studentId,
  walletBalance,
  plans,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  studentId: number;
  walletBalance: string;
  plans: PaymentPlan[];
}) {
  const buat = useCreateAdjustment(studentId);
  const riwayat = useAdjustments(studentId);

  const [sasaran, setSasaran] = useState<string>(SASARAN_SALDO);
  const [arah, setArah] = useState<Arah>("tambah");
  const [nominal, setNominal] = useState("");
  const [alasan, setAlasan] = useState("");

  const cicilanPilihan = useMemo(
    () =>
      plans.flatMap((plan) =>
        plan.installments.map((cicilan) => ({
          id: cicilan.id,
          label: `${plan.categoryLabel} — cicilan ${cicilan.installmentNo}`,
          amount: cicilan.amount,
          amountPaid: cicilan.amountPaid,
        })),
      ),
    [plans],
  );

  const cicilan =
    sasaran === SASARAN_SALDO
      ? null
      : cicilanPilihan.find((c) => String(c.id) === sasaran);

  const angka = Number(nominal) || 0;
  const delta = arah === "tambah" ? angka : -angka;
  const sekarang = Number(cicilan ? cicilan.amountPaid : walletBalance);
  const sesudah = sekarang + delta;

  const alasanKurang = alasan.trim().length < 5;
  const nominalKosong = angka <= 0;
  const jadiMinus = sesudah < 0;
  const melebihiTagihan = Boolean(cicilan) && sesudah > Number(cicilan!.amount);
  const bolehSimpan =
    !alasanKurang && !nominalKosong && !jadiMinus && !melebihiTagihan;

  function tutup() {
    setSasaran(SASARAN_SALDO);
    setArah("tambah");
    setNominal("");
    setAlasan("");
    onOpenChange(false);
  }

  function simpan() {
    buat.mutate(
      {
        installmentId: cicilan ? cicilan.id : null,
        amount: delta,
        reason: alasan.trim(),
      },
      {
        onSuccess: (hasil) => {
          toast.success(
            `Penyesuaian tersimpan. ${cicilan ? "Cicilan" : "Saldo"} sekarang ${formatRupiah(hasil.balanceAfter)}.`,
          );
          tutup();
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal menyimpan penyesuaian.",
          ),
      },
    );
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => (next ? onOpenChange(true) : tutup())}
    >
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Penyesuaian</DialogTitle>
          <DialogDescription>
            Memindahkan uang tanpa bukti transfer. Dipakai untuk mengembalikan
            kelebihan bayar atau membetulkan salah catat — setiap penyesuaian
            tercatat beserta alasannya.
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="sasaran">Sasaran</Label>
            <Select
              value={sasaran}
              onValueChange={(value) => value && setSasaran(String(value))}
            >
              <SelectTrigger id="sasaran">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={SASARAN_SALDO}>
                  Saldo mahasiswa — {formatRupiah(walletBalance)}
                </SelectItem>
                {cicilanPilihan.map((item) => (
                  <SelectItem key={item.id} value={String(item.id)}>
                    {item.label} — terbayar {formatRupiah(item.amountPaid)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Arah</Label>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant={arah === "tambah" ? "default" : "outline"}
                  className="flex-1"
                  onClick={() => setArah("tambah")}
                >
                  <Plus />
                  Tambah
                </Button>
                <Button
                  type="button"
                  variant={arah === "kurangi" ? "default" : "outline"}
                  className="flex-1"
                  onClick={() => setArah("kurangi")}
                >
                  <Minus />
                  Kurangi
                </Button>
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="nominal">Nominal</Label>
              <Input
                id="nominal"
                type="number"
                inputMode="numeric"
                min={1}
                step={1}
                placeholder="200000"
                value={nominal}
                onChange={(event) => setNominal(event.target.value)}
              />
            </div>
          </div>

          <dl className="flex flex-col divide-y divide-border rounded-lg border border-border text-sm">
            <div className="flex justify-between px-3 py-2">
              <dt className="text-muted-foreground">
                {cicilan ? "Terbayar sekarang" : "Saldo sekarang"}
              </dt>
              <dd className="tnum">{formatRupiah(sekarang)}</dd>
            </div>
            <div className="flex justify-between px-3 py-2">
              <dt className="text-muted-foreground">Setelah penyesuaian</dt>
              <dd
                className={cn(
                  "tnum font-medium",
                  jadiMinus || melebihiTagihan ? "text-danger" : "text-success",
                )}
              >
                {formatRupiah(sesudah)}
              </dd>
            </div>
          </dl>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="alasan">Alasan</Label>
            <Textarea
              id="alasan"
              rows={2}
              placeholder="Contoh: pengembalian kelebihan bayar cicilan 1."
              value={alasan}
              onChange={(event) => setAlasan(event.target.value)}
            />
            {alasan.length > 0 && alasanKurang && (
              <p className="text-xs text-danger">
                Alasan wajib diisi, minimal 5 karakter.
              </p>
            )}
          </div>

          {jadiMinus && (
            <p className="flex gap-2 rounded-md border border-danger/25 bg-danger-soft px-3 py-2 text-xs text-danger">
              <Info className="size-4 shrink-0" />
              <span>
                {cicilan ? "Uang yang tercatat masuk" : "Saldo"} tidak boleh
                minus. Kurangi nominalnya.
              </span>
            </p>
          )}

          {melebihiTagihan && (
            <p className="flex gap-2 rounded-md border border-danger/25 bg-danger-soft px-3 py-2 text-xs text-danger">
              <Info className="size-4 shrink-0" />
              <span>
                Melebihi tagihan cicilan ({formatRupiah(cicilan!.amount)}).
                Kelebihan tidak boleh menumpuk di cicilan — masukkan ke saldo
                mahasiswa.
              </span>
            </p>
          )}

          {riwayat.data && riwayat.data.length > 0 && (
            <div className="flex flex-col gap-1.5">
              <p className="flex items-center gap-1.5 text-xs font-semibold text-muted-foreground uppercase">
                <History className="size-3.5" />
                Riwayat penyesuaian
              </p>
              <ul className="flex max-h-40 flex-col gap-1.5 overflow-y-auto rounded-lg border border-border p-3 text-xs">
                {riwayat.data.map((item) => (
                  <li key={item.id}>
                    <span
                      className={cn(
                        "tnum font-medium",
                        Number(item.amount) > 0
                          ? "text-success"
                          : "text-danger",
                      )}
                    >
                      {Number(item.amount) > 0 ? "+" : "−"}
                      {formatRupiah(Math.abs(Number(item.amount)))}
                    </span>
                    <span className="text-muted-foreground">
                      {" "}
                      ke {item.target === "SALDO" ? "saldo" : "cicilan"} · jadi{" "}
                      {formatRupiah(item.balanceAfter)}
                    </span>
                    <span className="block text-muted-foreground">
                      {item.reason} · {formatTanggalJam(item.createdAt)}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={tutup}>
            Batal
          </Button>
          <Button
            type="button"
            onClick={simpan}
            disabled={!bolehSimpan}
            loading={buat.isPending}
          >
            Simpan penyesuaian
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
