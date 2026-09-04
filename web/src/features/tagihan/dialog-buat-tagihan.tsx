"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
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
import {
  kategoriLabel,
  tarifDasar,
  urutanUjian,
  type PaymentCategory,
} from "@/features/tarif/konstanta";
import { useGolongan } from "@/features/tarif/api";
import type { DiscountTier } from "@/features/tarif/konstanta";
import type { AcademicTerm } from "@/features/mahasiswa/api";
import { useCreatePlan } from "./api";
import { ApiError } from "@/lib/api";
import { formatRupiah } from "@/lib/format";

const kategoriPilihan: PaymentCategory[] = [
  "PENDAFTARAN",
  "UKT",
  ...urutanUjian,
];

export function DialogBuatTagihan({
  open,
  onOpenChange,
  studentId,
  studentTier,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  studentId: number;
  studentTier: DiscountTier;
}) {
  const [category, setCategory] = useState<PaymentCategory>("UKT");
  const [academicYear, setAcademicYear] = useState("2026/2027");
  const [term, setTerm] = useState<AcademicTerm>("GASAL");

  const buat = useCreatePlan(studentId);
  const golongan = useGolongan();

  // Pratinjau nominal, supaya admin tahu yang akan terbentuk sebelum menekan simpan.
  const kenaPotongan = category === "UKT";
  const persen = kenaPotongan ? golongan.persen(studentTier) : 0;
  const total = Math.round(tarifDasar[category] * (1 - persen / 100));
  const jumlahCicilan = category === "UKT" ? 5 : 1;

  function simpan() {
    buat.mutate(
      { category, academicYear, term },
      {
        onSuccess: (plan) => {
          toast.success(
            `Tagihan ${plan.categoryLabel} dibuat: ${formatRupiah(plan.totalAmount)} dalam ${plan.installments.length} cicilan.`,
          );
          onOpenChange(false);
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal membuat tagihan.",
          ),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Buat tagihan</DialogTitle>
          <DialogDescription>
            Nominalnya dihitung otomatis dari tarif dan golongan potongan
            mahasiswa.
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="kategori">Kategori</Label>
            <Select
              value={category}
              onValueChange={(value) =>
                value && setCategory(value as PaymentCategory)
              }
            >
              <SelectTrigger id="kategori">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {kategoriPilihan.map((item) => (
                  <SelectItem key={item} value={item}>
                    {kategoriLabel[item]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="tahun">Tahun akademik</Label>
              <Input
                id="tahun"
                value={academicYear}
                onChange={(event) => setAcademicYear(event.target.value)}
                placeholder="2026/2027"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="term">Term</Label>
              <Select
                value={term}
                onValueChange={(value) =>
                  value && setTerm(value as AcademicTerm)
                }
              >
                <SelectTrigger id="term">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="GASAL">Gasal</SelectItem>
                  <SelectItem value="GENAP">Genap</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <dl className="flex flex-col divide-y divide-border rounded-lg border border-border text-sm">
            <div className="flex justify-between px-3 py-2">
              <dt className="text-muted-foreground">Tarif dasar</dt>
              <dd className="tnum">{formatRupiah(tarifDasar[category])}</dd>
            </div>
            <div className="flex justify-between px-3 py-2">
              <dt className="text-muted-foreground">
                Potongan {golongan.label(studentTier)}
              </dt>
              <dd>
                {kenaPotongan && persen > 0 ? (
                  <span className="text-success">−{persen}%</span>
                ) : (
                  <span className="text-muted-foreground">
                    tidak berlaku
                  </span>
                )}
              </dd>
            </div>
            <div className="flex justify-between px-3 py-2 font-medium">
              <dt>Akan ditagih</dt>
              <dd className="tnum">
                {formatRupiah(total)}
                <span className="ml-1 text-xs font-normal text-muted-foreground">
                  ÷ {jumlahCicilan} cicilan
                </span>
              </dd>
            </div>
          </dl>

          {!kenaPotongan && (
            <p className="text-xs text-muted-foreground">
              Potongan hanya berlaku untuk UKT. Pendaftaran dan biaya ujian sama
              untuk semua golongan.
            </p>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Batal
          </Button>
          <Button onClick={simpan} disabled={buat.isPending}>
            {buat.isPending && <Loader2 className="animate-spin" />}
            Buat tagihan
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
