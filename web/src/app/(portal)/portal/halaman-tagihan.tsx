"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { FileCheck2, Loader2, Lock, Plus, Upload, Wallet } from "lucide-react";
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
import { InstallmentStatusBadge } from "@/components/status-badge";
import { ErrorState, TableSkeleton } from "@/components/page-header";
import {
  useDaftarTagihan,
  useProfil,
  useTagihan,
  useUnggahBukti,
  type CicilanRingkas,
  type Tagihan,
} from "@/features/portal/api";
import { kategoriLabel, urutanUjian } from "@/features/tarif/konstanta";
import { ApiError } from "@/lib/api";
import { formatRupiah, formatTanggal } from "@/lib/format";
import { KartuSemester } from "@/components/penanda-semester";

export function HalamanTagihan() {
  const profil = useProfil();
  const tagihan = useTagihan(profil.data?.dokumenLengkap ?? false);
  const [bayar, setBayar] = useState<CicilanRingkas | null>(null);

  if (profil.isPending) return <TableSkeleton rows={4} />;
  if (profil.error) {
    return (
      <ErrorState
        message={
          profil.error instanceof ApiError
            ? profil.error.message
            : "Coba muat ulang halaman."
        }
      />
    );
  }

  // Gate pertama: dokumen wajib.
  if (!profil.data.dokumenLengkap) {
    return (
      <div className="flex flex-col items-center gap-4 rounded-lg border border-dashed border-border px-6 py-14 text-center">
        <Lock className="size-7 text-muted-foreground" />
        <div>
          <p className="font-medium">Lengkapi dokumen wajib dulu</p>
          <p className="mt-1 max-w-md text-sm text-muted-foreground">
            Tagihan baru bisa dibuka setelah semua dokumen lengkap. Langkah
            berikutnya: <strong>{profil.data.namaLangkahBerikutnya}</strong>.
          </p>
        </div>
        <Button nativeButton={false} render={<Link href="/portal/dokumen" />}>
          <FileCheck2 />
          Isi dokumen
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-5">
      <KartuSemester />

      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="font-heading text-xl font-semibold tracking-tight">
            Tagihan saya
          </h2>
          <p className="text-sm text-muted-foreground">
            {profil.data.kelas} · {profil.data.nim}
          </p>
        </div>

        {Number(profil.data.saldo) > 0 && (
          <span className="flex items-center gap-1.5 rounded-md border border-info/25 bg-info-soft px-2.5 py-1.5 text-sm text-info">
            <Wallet className="size-4" />
            Saldo {formatRupiah(profil.data.saldo)}
          </span>
        )}
      </div>

      {!profil.data.pendaftaranLunas && (
        <p className="rounded-lg border border-warning/25 bg-warning-soft px-4 py-3 text-sm text-warning">
          Biaya Pendaftaran belum lunas. Tagihan lain baru terbuka setelah ini
          selesai.
        </p>
      )}

      <DaftarSendiri
        sudahAda={(tagihan.data ?? []).map((t) => t.kategori)}
        pendaftaranLunas={profil.data.pendaftaranLunas}
      />

      {tagihan.isPending ? (
        <TableSkeleton rows={4} />
      ) : tagihan.error ? (
        <ErrorState
          message={
            tagihan.error instanceof ApiError
              ? tagihan.error.message
              : "Coba muat ulang halaman."
          }
        />
      ) : tagihan.data.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border px-6 py-12 text-center">
          <p className="font-medium">Belum ada tagihan</p>
          <p className="mt-1 text-sm text-muted-foreground">
            Daftarkan biaya Pendaftaran untuk memulai.
          </p>
        </div>
      ) : (
        tagihan.data.map((item) => (
          <KartuTagihan key={item.id} tagihan={item} onBayar={setBayar} />
        ))
      )}

      <DialogBayar cicilan={bayar} onClose={() => setBayar(null)} />
    </div>
  );
}

function DaftarSendiri({
  sudahAda,
  pendaftaranLunas,
}: {
  sudahAda: string[];
  pendaftaranLunas: boolean;
}) {
  const daftar = useDaftarTagihan();

  const belumAda = (["PENDAFTARAN", ...urutanUjian] as const).filter(
    (kategori) => !sudahAda.includes(kategoriLabel[kategori]),
  );

  if (belumAda.length === 0) return null;

  const berikutnya = belumAda[0];
  const terkunci = berikutnya !== "PENDAFTARAN" && !pendaftaranLunas;

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-border/70 bg-card shadow-sm px-4 py-3">
      <div>
        <p className="text-sm font-medium">
          Daftarkan {kategoriLabel[berikutnya]}
        </p>
        <p className="text-xs text-muted-foreground">
          {berikutnya === "PENDAFTARAN"
            ? "Biaya awal pendaftaran"
            : "Tahap ujian harus dilunasi berurutan"}
        </p>
      </div>
      <Button
        size="sm"
        disabled={daftar.isPending || terkunci}
        onClick={() =>
          daftar.mutate(berikutnya, {
            onSuccess: (t) =>
              toast.success(
                `Tagihan ${t.kategori} dibuat: ${formatRupiah(t.total)}`,
              ),
            onError: (e) =>
              toast.error(
                e instanceof ApiError ? e.message : "Gagal membuat tagihan.",
              ),
          })
        }
      >
        {daftar.isPending ? <Loader2 className="animate-spin" /> : <Plus />}
        Daftarkan
      </Button>
    </div>
  );
}

function KartuTagihan({
  tagihan,
  onBayar,
}: {
  tagihan: Tagihan;
  onBayar: (cicilan: CicilanRingkas) => void;
}) {
  const lunas = Number(tagihan.sisa) === 0;

  return (
    <section className="overflow-hidden rounded-2xl border border-border/70 bg-card shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
        <div>
          <h3 className="font-heading text-sm font-semibold">
            {tagihan.kategori}
            {tagihan.semester && (
              <span className="ml-2 font-normal text-muted-foreground">
                semester {tagihan.semester}
              </span>
            )}
          </h3>
          <p className="text-xs text-muted-foreground">{tagihan.tahunAkademik}</p>
        </div>
        <div className="text-right">
          <p className="font-heading text-base font-semibold tnum">
            {formatRupiah(tagihan.total)}
          </p>
          <p className={cn("text-xs", lunas ? "text-success" : "text-warning")}>
            {lunas ? "Lunas" : `sisa ${formatRupiah(tagihan.sisa)}`}
          </p>
        </div>
      </div>

      <ul className="divide-y divide-border border-t border-border">
        {tagihan.cicilan.map((cicilan) => (
          <li
            key={cicilan.id}
            className="flex flex-wrap items-center gap-3 px-4 py-3"
          >
            <div className="min-w-0 flex-1">
              <p className="text-sm">
                {tagihan.cicilan.length > 1
                  ? `Cicilan ${cicilan.nomor} dari ${tagihan.cicilan.length}`
                  : "Sekali bayar"}
              </p>
              <p className="text-xs text-muted-foreground">
                Jatuh tempo {formatTanggal(cicilan.jatuhTempo)}
              </p>
            </div>

            <div className="text-right">
              <p className="text-sm font-medium tnum">
                {formatRupiah(cicilan.nominal)}
              </p>
              {Number(cicilan.dibayar) > 0 && !lunas && (
                <p className="text-xs text-muted-foreground">
                  dibayar {formatRupiah(cicilan.dibayar)}
                </p>
              )}
            </div>

            <InstallmentStatusBadge status={cicilan.status} />

            {cicilan.status !== "PAID" && (
              <Button size="sm" variant="outline" onClick={() => onBayar(cicilan)}>
                <Upload />
                Bayar
              </Button>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}

function DialogBayar({
  cicilan,
  onClose,
}: {
  cicilan: CicilanRingkas | null;
  onClose: () => void;
}) {
  const unggah = useUnggahBukti();
  const fileInput = useRef<HTMLInputElement>(null);
  const [nominal, setNominal] = useState("");
  const [namaBerkas, setNamaBerkas] = useState<string | null>(null);

  if (!cicilan) return null;

  function kirim() {
    const file = fileInput.current?.files?.[0];
    if (!file) {
      toast.error("Pilih foto bukti transfernya dulu.");
      return;
    }

    const angka = Number(nominal || cicilan!.sisa);
    if (!Number.isFinite(angka) || angka <= 0) {
      toast.error("Nominal harus angka lebih besar dari nol.");
      return;
    }

    unggah.mutate(
      { installmentId: cicilan!.id, amount: angka, file },
      {
        onSuccess: () => {
          toast.success(
            "Bukti terkirim. Sistem sedang membacanya, status akan berubah sendiri.",
          );
          setNominal("");
          setNamaBerkas(null);
          onClose();
        },
        onError: (e) =>
          toast.error(e instanceof ApiError ? e.message : "Gagal mengunggah."),
      },
    );
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Unggah bukti transfer</DialogTitle>
          <DialogDescription>
            Sisa cicilan ini {formatRupiah(cicilan.sisa)}. Sistem akan membaca
            buktinya otomatis.
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="nominal">Nominal yang ditransfer</Label>
            <Input
              id="nominal"
              type="number"
              inputMode="numeric"
              placeholder={String(Math.round(Number(cicilan.sisa)))}
              value={nominal}
              onChange={(event) => setNominal(event.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Kosongkan untuk membayar sisa penuh.
            </p>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label>Foto bukti transfer</Label>
            <input
              ref={fileInput}
              type="file"
              accept=".jpg,.jpeg,.png,.webp,.pdf"
              className="hidden"
              onChange={(event) =>
                setNamaBerkas(event.target.files?.[0]?.name ?? null)
              }
            />
            <Button
              variant="outline"
              className="w-fit"
              onClick={() => fileInput.current?.click()}
            >
              <Upload />
              Pilih berkas
            </Button>
            {namaBerkas && (
              <span className="truncate text-sm text-muted-foreground">
                {namaBerkas}
              </span>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Batal
          </Button>
          <Button onClick={kirim} disabled={unggah.isPending}>
            {unggah.isPending && <Loader2 className="animate-spin" />}
            Kirim bukti
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
