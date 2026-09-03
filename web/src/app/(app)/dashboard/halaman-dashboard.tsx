"use client";

import Link from "next/link";
import { ArrowRight, BadgeCheck, Clock, TriangleAlert, Users, Wallet } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { PaymentStatusBadge } from "@/components/status-badge";
import { StatRow, StatTile } from "@/components/stat-tile";
import {
  EmptyState,
  ErrorState,
  PageHeader,
  TableSkeleton,
} from "@/components/page-header";
import { useDashboard, type KategoriRingkas } from "@/features/dashboard/api";
import { kategoriLabel, type PaymentCategory } from "@/features/tarif/konstanta";
import { ApiError } from "@/lib/api";
import { formatRupiah, formatTanggalJam } from "@/lib/format";

export function HalamanDashboard() {
  const { data, isPending, error } = useDashboard();

  if (isPending) {
    return (
      <div className="flex flex-col gap-5 p-6">
        <PageHeader title="Ringkasan" />
        <TableSkeleton rows={8} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col gap-5 p-6">
        <PageHeader title="Ringkasan" />
        <ErrorState
          message={
            error instanceof ApiError ? error.message : "Coba muat ulang halaman."
          }
        />
      </div>
    );
  }

  const tertagih = Number(data.totalTertagih);
  const terkumpul = Number(data.totalTerkumpul);
  const persen = tertagih === 0 ? 0 : Math.round((terkumpul / tertagih) * 100);

  return (
    <div className="flex flex-col gap-6 p-6">
      <PageHeader
        title="Ringkasan"
        description="Program Doktor PAI · Universitas Islam Sultan Agung"
      />

      <StatRow>
        <StatTile
          label="Mahasiswa aktif"
          value={data.mahasiswaAktif.toLocaleString("id-ID")}
          sublabel="Semua kelas"
          icon={Users}
        />
        <StatTile
          label="Perlu ditinjau"
          value={String(data.perluDitinjau)}
          sublabel={
            data.gagalDibaca > 0 ? `${data.gagalDibaca} gagal dibaca` : "OCR kurang yakin"
          }
          icon={data.gagalDibaca > 0 ? TriangleAlert : Clock}
          tone={data.perluDitinjau > 0 || data.gagalDibaca > 0 ? "warning" : undefined}
        />
        <StatTile
          label="Total tertagih"
          value={formatRupiah(tertagih)}
          icon={Wallet}
        />
        <StatTile
          label="Total terkumpul"
          value={formatRupiah(terkumpul)}
          sublabel={`${persen}% dari tagihan`}
          icon={BadgeCheck}
          tone={persen >= 100 ? "success" : undefined}
        />
      </StatRow>

      <div className="grid gap-5 lg:grid-cols-2">
        <RingkasanKategori data={data.perKategori} />
        <RingkasanKelas
          data={data.perKelas}
          totalSaldo={data.totalSaldoMahasiswa}
        />
      </div>

      <section className="overflow-hidden rounded-lg border border-border bg-card">
        <div className="flex items-center justify-between gap-3 px-5 py-3.5">
          <div>
            <h3 className="font-heading text-sm font-semibold">
              Antrean verifikasi
            </h3>
            <p className="text-xs text-muted-foreground">
              Bukti bayar yang belum diputuskan
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            render={<Link href="/verifikasi/ukt" />}
          >
            Buka verifikasi
            <ArrowRight />
          </Button>
        </div>

        {data.antrean.length === 0 ? (
          <div className="border-t border-border">
            <EmptyState
              title="Tidak ada antrean"
              description="Semua bukti bayar sudah diputuskan."
            />
          </div>
        ) : (
          <div className="overflow-x-auto border-t border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mahasiswa</TableHead>
                  <TableHead>Kategori</TableHead>
                  <TableHead className="text-right">Nominal</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Keyakinan</TableHead>
                  <TableHead>Diunggah</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.antrean.map((row) => (
                  <TableRow key={row.paymentId}>
                    <TableCell>
                      <span className="block font-medium">
                        {row.namaMahasiswa}
                      </span>
                      <span className="block font-mono text-xs text-muted-foreground">
                        {row.nim}
                      </span>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {row.kategori
                        ? (kategoriLabel[row.kategori as PaymentCategory] ??
                          row.kategori)
                        : "—"}
                    </TableCell>
                    <TableCell className="text-right font-medium">
                      {formatRupiah(row.nominal)}
                    </TableCell>
                    <TableCell>
                      <PaymentStatusBadge status={row.status} />
                    </TableCell>
                    <TableCell className="tabular-nums text-muted-foreground">
                      {row.keyakinan === null
                        ? "—"
                        : `${Math.round(Number(row.keyakinan) * 100)}%`}
                    </TableCell>
                    <TableCell className="whitespace-nowrap text-muted-foreground">
                      {formatTanggalJam(row.diunggah)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </section>
    </div>
  );
}

function RingkasanKategori({ data }: { data: KategoriRingkas[] }) {
  return (
    <section className="overflow-hidden rounded-lg border border-border bg-card">
      <h3 className="px-5 py-3.5 font-heading text-sm font-semibold">
        Per kategori tagihan
      </h3>
      <div className="border-t border-border">
        {data.length === 0 ? (
          <p className="px-5 py-6 text-sm text-muted-foreground">
            Belum ada tagihan.
          </p>
        ) : (
          <ul className="divide-y divide-border">
            {data.map((item) => {
              const tertagih = Number(item.tertagih);
              const terkumpul = Number(item.terkumpul);
              const persen = tertagih === 0 ? 0 : (terkumpul / tertagih) * 100;

              return (
                <li key={item.kategori} className="flex flex-col gap-1.5 px-5 py-3">
                  <div className="flex items-baseline justify-between gap-3">
                    <span className="text-sm font-medium">
                      {kategoriLabel[item.kategori as PaymentCategory] ??
                        item.kategori}
                      <span className="ml-2 text-xs font-normal text-muted-foreground">
                        {item.jumlahTagihan} tagihan
                      </span>
                    </span>
                    <span className="text-sm tabular-nums">
                      {formatRupiah(terkumpul)}
                      <span className="text-muted-foreground">
                        {" / "}
                        {formatRupiah(tertagih)}
                      </span>
                    </span>
                  </div>
                  <Bar persen={persen} />
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </section>
  );
}

function RingkasanKelas({
  data,
  totalSaldo,
}: {
  data: { kelas: string; jumlahMahasiswa: number; tertagih: string; terkumpul: string }[];
  totalSaldo: string;
}) {
  return (
    <section className="flex flex-col overflow-hidden rounded-lg border border-border bg-card">
      <h3 className="px-5 py-3.5 font-heading text-sm font-semibold">Per kelas</h3>
      <div className="flex-1 border-t border-border">
        {data.length === 0 ? (
          <p className="px-5 py-6 text-sm text-muted-foreground">
            Belum ada kelas.
          </p>
        ) : (
          <ul className="divide-y divide-border">
            {data.map((item) => {
              const tertagih = Number(item.tertagih);
              const terkumpul = Number(item.terkumpul);
              const persen = tertagih === 0 ? 0 : (terkumpul / tertagih) * 100;

              return (
                <li key={item.kelas} className="flex flex-col gap-1.5 px-5 py-3">
                  <div className="flex items-baseline justify-between gap-3">
                    <span className="text-sm font-medium">
                      Kelas {item.kelas}
                      <span className="ml-2 text-xs font-normal text-muted-foreground">
                        {item.jumlahMahasiswa} mahasiswa
                      </span>
                    </span>
                    <span className="text-sm tabular-nums text-muted-foreground">
                      {formatRupiah(tertagih)}
                    </span>
                  </div>
                  <Bar persen={persen} />
                </li>
              );
            })}
          </ul>
        )}
      </div>
      <div className="flex items-center justify-between border-t border-border px-5 py-3 text-sm">
        <span className="text-muted-foreground">Saldo mahasiswa (kelebihan bayar)</span>
        <span className="font-medium tabular-nums">{formatRupiah(totalSaldo)}</span>
      </div>
    </section>
  );
}

function Bar({ persen }: { persen: number }) {
  const nilai = Math.min(Math.max(persen, 0), 100);
  return (
    <div
      className="h-1.5 w-full overflow-hidden rounded-full bg-muted"
      role="img"
      aria-label={`${Math.round(nilai)} persen terkumpul`}
    >
      <div
        className={cn(
          "h-full rounded-full",
          nilai >= 100 ? "bg-success" : nilai > 0 ? "bg-primary" : "bg-transparent",
        )}
        style={{ width: `${nilai}%` }}
      />
    </div>
  );
}
