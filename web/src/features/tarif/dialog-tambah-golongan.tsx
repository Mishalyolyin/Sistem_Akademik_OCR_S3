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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api";
import { formatRupiah } from "@/lib/format";
import { tarifDasar } from "./konstanta";
import { useCreateTier } from "./api";

/** Kode dipakai apa adanya di berkas import, jadi bentuknya dibatasi. */
const POLA_KODE = /^[A-Z][A-Z0-9_]*$/;

/** Mengubah nama jadi kode yang sah, supaya admin tidak perlu memikirkannya. */
function kodeDariNama(nama: string): string {
  return nama
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .replace(/^([0-9])/, "G$1");
}

export function DialogTambahGolongan({
  open,
  onOpenChange,
  tarifDasarUkt = tarifDasar.UKT,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Tarif dasar UKT yang berlaku; angka brosur hanya dipakai bila belum termuat. */
  tarifDasarUkt?: number;
}) {
  const buat = useCreateTier();
  const [nama, setNama] = useState("");
  const [kode, setKode] = useState("");
  const [persen, setPersen] = useState("");
  const [kodeDisunting, setKodeDisunting] = useState(false);

  const kodeAkhir = kodeDisunting ? kode : kodeDariNama(nama);
  const angkaPersen = Number(persen);

  const persenTidakMasukAkal =
    persen.length > 0 && (Number.isNaN(angkaPersen) || angkaPersen < 0 || angkaPersen > 100);
  const kodeTidakSah = kodeAkhir.length > 0 && !POLA_KODE.test(kodeAkhir);
  const bolehSimpan =
    nama.trim().length > 0 &&
    kodeAkhir.length > 0 &&
    !kodeTidakSah &&
    persen.length > 0 &&
    !persenTidakMasukAkal;

  const uktSetelahPotongan = Math.round(
    tarifDasarUkt * (1 - (angkaPersen || 0) / 100),
  );

  function tutup() {
    setNama("");
    setKode("");
    setPersen("");
    setKodeDisunting(false);
    onOpenChange(false);
  }

  function simpan() {
    buat.mutate(
      { tier: kodeAkhir, label: nama.trim(), percent: angkaPersen },
      {
        onSuccess: (tier) => {
          toast.success(
            `Golongan ${tier.label} ditambahkan — UKT ${formatRupiah(tier.uktPerSemester)} per semester.`,
          );
          tutup();
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal menambah golongan.",
          ),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={(next) => (next ? onOpenChange(true) : tutup())}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Tambah golongan potongan</DialogTitle>
          <DialogDescription>
            Potongan hanya berlaku untuk UKT. Pendaftaran dan biaya ujian tetap
            penuh untuk semua golongan.
          </DialogDescription>
        </DialogHeader>

        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (bolehSimpan) simpan();
          }}
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="nama-golongan">Nama golongan</Label>
            <Input
              id="nama-golongan"
              placeholder="Mitra Instansi"
              value={nama}
              onChange={(event) => setNama(event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="kode-golongan">Kode</Label>
            <Input
              id="kode-golongan"
              placeholder="MITRA_INSTANSI"
              value={kodeAkhir}
              aria-invalid={kodeTidakSah}
              onChange={(event) => {
                setKodeDisunting(true);
                setKode(event.target.value.toUpperCase());
              }}
            />
            {kodeTidakSah ? (
              <p className="text-xs text-danger">
                Hanya huruf kapital, angka, dan garis bawah. Contoh:
                MITRA_INSTANSI.
              </p>
            ) : (
              <p className="text-xs text-muted-foreground">
                Dipakai di kolom <code>discount_tier</code> pada berkas import.
                Terisi otomatis dari nama.
              </p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="persen-golongan">Potongan UKT (%)</Label>
            <Input
              id="persen-golongan"
              type="number"
              inputMode="numeric"
              min={0}
              max={100}
              placeholder="30"
              value={persen}
              aria-invalid={persenTidakMasukAkal}
              onChange={(event) => setPersen(event.target.value)}
            />
            {persenTidakMasukAkal && (
              <p className="text-xs text-danger">Isi antara 0 dan 100.</p>
            )}
          </div>

          {persen.length > 0 && !persenTidakMasukAkal && (
            <dl className="flex flex-col divide-y divide-border rounded-lg border border-border text-sm">
              <div className="flex justify-between px-3 py-2">
                <dt className="text-muted-foreground">Tarif dasar UKT</dt>
                <dd className="tnum">{formatRupiah(tarifDasarUkt)}</dd>
              </div>
              <div className="flex justify-between px-3 py-2">
                <dt className="text-muted-foreground">UKT per semester</dt>
                <dd className="tnum font-medium text-success">
                  {formatRupiah(uktSetelahPotongan)}
                </dd>
              </div>
            </dl>
          )}

          <p className="flex gap-2 rounded-md border border-border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
            <Info className="size-4 shrink-0" />
            <span>
              Golongan baru langsung bisa dipilih di halaman mahasiswa dan
              diterima berkas import, tanpa perlu deploy ulang.
            </span>
          </p>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={tutup}>
              Batal
            </Button>
            <Button type="submit" disabled={!bolehSimpan || buat.isPending}>
              {buat.isPending && <Loader2 className="animate-spin" />}
              Tambah golongan
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
