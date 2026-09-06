"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { FileCheck2, Lock, Plus, Upload, Wallet } from "lucide-react";
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

      <RingkasanSaya
        nama={profil.data.nama}
        kelas={profil.data.kelas}
        nim={profil.data.nim}
        saldo={profil.data.saldo}
        tagihan={tagihan.data ?? []}
      />

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
        disabled={terkunci}
        loading={daftar.isPending}
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
        <Plus />
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
          <p className="text-xs text-muted-foreground">
            {tagihan.tahunAkademik}
          </p>
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
              <Button
                size="sm"
                variant="outline"
                onClick={() => onBayar(cicilan)}
              >
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
          <Button onClick={kirim} loading={unggah.isPending}>
            Kirim bukti
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/**
 * Ringkasan kewajiban mahasiswa, di paling atas halaman.
 *
 * <p>Sebelumnya halaman ini langsung menampilkan daftar tagihan tanpa satu pun
 * angka gabungan. Padahal yang dicari mahasiswa saat membukanya hampir selalu
 * satu hal: <b>berapa lagi yang harus saya bayar</b>. Menjawabnya menuntut dia
 * menjumlahkan sendiri sisa tiap kartu di bawah.
 */
function RingkasanSaya({
  nama,
  kelas,
  nim,
  saldo,
  tagihan,
}: {
  nama: string;
  kelas: string | null;
  nim: string;
  saldo: string;
  tagihan: Tagihan[];
}) {
  const total = tagihan.reduce((n, t) => n + Number(t.total), 0);
  const dibayar = tagihan.reduce((n, t) => n + Number(t.dibayar), 0);
  const sisa = Math.max(total - dibayar, 0);
  const persen = total === 0 ? 0 : Math.min((dibayar / total) * 100, 100);
  const lunas = total > 0 && sisa === 0;

  return (
    <section className="relative overflow-hidden rounded-3xl border border-border/70 bg-gradient-to-br from-primary/10 via-card to-success/10 px-6 py-6 shadow-sm">
      <div
        aria-hidden
        className="pointer-events-none absolute -top-20 -right-16 size-56 rounded-full bg-primary/15 blur-3xl"
      />

      <div className="relative flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <h2 className="font-heading text-2xl font-bold tracking-tight text-balance">
            {nama}
          </h2>
          <p className="mt-0.5 text-sm text-muted-foreground">
            {kelas ?? "Belum berkelas"} · {nim}
          </p>
        </div>

        {Number(saldo) > 0 && (
          <span className="flex items-center gap-1.5 rounded-full border border-info/25 bg-info/10 px-3 py-1.5 text-sm font-medium text-info">
            <Wallet className="size-4" />
            Saldo {formatRupiah(saldo)}
          </span>
        )}
      </div>

      {total > 0 && (
        <>
          <dl className="relative mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3">
            <div>
              <dt className="text-xs text-muted-foreground">Total tagihan</dt>
              <dd className="mt-0.5 font-heading text-xl font-bold tabular-nums">
                {formatRupiah(total)}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Sudah dibayar</dt>
              <dd className="mt-0.5 font-heading text-xl font-bold text-success tabular-nums">
                {formatRupiah(dibayar)}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">
                {lunas ? "Sisa" : "Masih harus dibayar"}
              </dt>
              <dd
                className={cn(
                  "mt-0.5 font-heading text-xl font-bold tabular-nums",
                  lunas ? "text-success" : "text-warning",
                )}
              >
                {formatRupiah(sisa)}
              </dd>
            </div>
          </dl>

          <div className="relative mt-5 flex items-center gap-3">
            <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-muted/70">
              <div
                className={cn(
                  "h-full rounded-full transition-[width] duration-1000 ease-out",
                  lunas
                    ? "bg-gradient-to-r from-success to-success/70"
                    : "bg-gradient-to-r from-primary to-success",
                )}
                style={{ width: `${persen}%` }}
              />
            </div>
            <span className="shrink-0 text-sm font-medium tabular-nums">
              {Math.round(persen)}%
            </span>
          </div>
        </>
      )}
    </section>
  );
}
