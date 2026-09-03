"use client";

import { useState } from "react";
import { toast } from "sonner";
import { KeyRound, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { ErrorState, TableSkeleton } from "@/components/page-header";
import { useGantiKataSandi, useProfil } from "@/features/portal/api";
import { potongan, type DiscountTier } from "@/features/tarif/konstanta";
import { ApiError } from "@/lib/api";
import { formatRupiah } from "@/lib/format";

export function HalamanProfil() {
  const { data, isPending, error } = useProfil();

  if (isPending) return <TableSkeleton rows={5} />;
  if (error) {
    return (
      <ErrorState
        message={
          error instanceof ApiError ? error.message : "Coba muat ulang halaman."
        }
      />
    );
  }

  const golongan = potongan[data.golongan as DiscountTier];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="font-heading text-xl font-semibold tracking-tight">
          Profil
        </h2>
        <p className="text-sm text-muted-foreground">
          Data ini dikelola admin. Hubungi bagian keuangan bila ada yang keliru.
        </p>
      </div>

      <dl className="flex flex-col divide-y divide-border rounded-lg border border-border bg-card">
        <Baris label="Nama" nilai={data.nama} />
        <Baris label="NIM" nilai={data.nim} mono />
        <Baris label="Email" nilai={data.email ?? "—"} mono />
        <Baris label="Kelas" nilai={data.kelas ?? "—"} />
        <Baris
          label="Golongan potongan"
          nilai={
            golongan
              ? `${golongan.label}${golongan.persen > 0 ? ` (−${golongan.persen}% UKT)` : ""}`
              : data.golongan
          }
        />
        <Baris label="Nomor WhatsApp" nilai={data.telepon ?? "—"} />
        <Baris label="Alamat" nilai={data.alamat ?? "—"} />
        <Baris label="Saldo" nilai={formatRupiah(data.saldo)} />
      </dl>

      <Separator />

      <GantiKataSandi nim={data.nim} />
    </div>
  );
}

function Baris({
  label,
  nilai,
  mono,
}: {
  label: string;
  nilai: string;
  mono?: boolean;
}) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 px-4 py-3">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd
        className={
          mono
            ? "text-right font-mono text-sm"
            : "max-w-md text-right text-sm font-medium"
        }
      >
        {nilai}
      </dd>
    </div>
  );
}

function GantiKataSandi({ nim }: { nim: string }) {
  const ganti = useGantiKataSandi();
  const [lama, setLama] = useState("");
  const [baru, setBaru] = useState("");
  const [ulangi, setUlangi] = useState("");

  const cocok = baru.length === 0 || baru === ulangi;
  const samaDenganNim = baru === nim && baru.length > 0;

  function kirim() {
    if (!cocok) {
      toast.error("Ulangi kata sandi belum sama.");
      return;
    }

    ganti.mutate(
      { lama, baru },
      {
        onSuccess: () => {
          toast.success("Kata sandi diganti.");
          setLama("");
          setBaru("");
          setUlangi("");
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal mengganti kata sandi.",
          ),
      },
    );
  }

  return (
    <section className="flex flex-col gap-4">
      <div>
        <h3 className="font-heading text-sm font-semibold">Ganti kata sandi</h3>
        <p className="text-sm text-muted-foreground">
          Kata sandi awalmu sama dengan NIM. Ganti sekarang supaya akunmu aman.
        </p>
      </div>

      <div className="flex max-w-sm flex-col gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="lama">Kata sandi sekarang</Label>
          <Input
            id="lama"
            type="password"
            autoComplete="current-password"
            value={lama}
            onChange={(event) => setLama(event.target.value)}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="baru">Kata sandi baru</Label>
          <Input
            id="baru"
            type="password"
            autoComplete="new-password"
            value={baru}
            onChange={(event) => setBaru(event.target.value)}
          />
          {samaDenganNim && (
            <p className="text-xs text-danger">
              Jangan pakai NIM sebagai kata sandi.
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="ulangi">Ulangi kata sandi baru</Label>
          <Input
            id="ulangi"
            type="password"
            autoComplete="new-password"
            value={ulangi}
            onChange={(event) => setUlangi(event.target.value)}
            aria-invalid={!cocok}
          />
          {!cocok && (
            <p className="text-xs text-danger">Ulangi kata sandi belum sama.</p>
          )}
        </div>

        <Button
          className="w-fit"
          disabled={
            ganti.isPending ||
            !lama ||
            baru.length < 8 ||
            !cocok ||
            samaDenganNim
          }
          onClick={kirim}
        >
          {ganti.isPending ? (
            <Loader2 className="animate-spin" />
          ) : (
            <KeyRound />
          )}
          Simpan kata sandi
        </Button>
      </div>
    </section>
  );
}
