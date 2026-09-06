"use client";

import { ErrorState, TableSkeleton } from "@/components/page-header";
import { useProfil } from "@/features/portal/api";
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

      <dl className="flex flex-col divide-y divide-border rounded-2xl border border-border/70 bg-card shadow-sm">
        <Baris label="Nama" nilai={data.nama} />
        <Baris label="NIM" nilai={data.nim} mono />
        <Baris label="Email" nilai={data.email ?? "—"} mono />
        <Baris label="Kelas" nilai={data.kelas ?? "—"} />
        <Baris
          label="Golongan potongan"
          nilai={
            data.golonganLabel
          }
        />
        <Baris label="Nomor WhatsApp" nilai={data.telepon ?? "—"} />
        <Baris label="Alamat" nilai={data.alamat ?? "—"} />
        <Baris label="Saldo" nilai={formatRupiah(data.saldo)} />
      </dl>

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
