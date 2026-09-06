"use client";

import Link from "next/link";
import {
  Layers,
  School,
  ArrowRight,
  BadgeCheck,
  Clock,
  Hourglass,
  PiggyBank,
  TriangleAlert,
  Users,
  Wallet,
  XCircle,
  type LucideIcon,
} from "lucide-react";
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
import {
  useDashboard,
  type KategoriRingkas,
  type KelasRingkas,
} from "@/features/dashboard/api";
import {
  kategoriLabel,
  type PaymentCategory,
} from "@/features/tarif/konstanta";
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
            error instanceof ApiError
              ? error.message
              : "Coba muat ulang halaman."
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
          label="Total tertagih"
          value={formatRupiah(tertagih)}
          sublabel="Seluruh kategori"
          icon={Wallet}
          tone="primary"
        />
        <StatTile
          label="Total terkumpul"
          value={formatRupiah(terkumpul)}
          sublabel={`${persen}% dari tagihan`}
          icon={BadgeCheck}
          tone="success"
        />
        <StatTile
          label="Saldo mahasiswa"
          value={formatRupiah(data.totalSaldoMahasiswa)}
          sublabel="Kelebihan bayar"
          icon={PiggyBank}
          tone="warning"
        />
      </StatRow>

      <section className="flex flex-col gap-2">
        <h3 className="font-heading text-sm font-semibold">
          Status bukti bayar
        </h3>
        {/* Tiap kotak menautkan ke daftar yang sudah tersaring statusnya, jadi
            angka yang menarik perhatian bisa langsung ditelusuri. */}
        <StatRow>
          <StatTile
            label="Menunggu dibaca"
            value={String(data.status.menungguDibaca)}
            sublabel="Antre di OCR"
            icon={Hourglass}
            tone="info"
            href="/verifikasi/semua?status=PENDING"
          />
          <StatTile
            label="Perlu ditinjau"
            value={String(data.status.perluDitinjau)}
            sublabel={
              data.status.gagalDibaca > 0
                ? `${data.status.gagalDibaca} gagal dibaca`
                : "OCR kurang yakin"
            }
            icon={data.status.gagalDibaca > 0 ? TriangleAlert : Clock}
            tone={
              data.status.perluDitinjau > 0 || data.status.gagalDibaca > 0
                ? "warning"
                : undefined
            }
            href="/verifikasi/semua?status=NEEDS_REVIEW"
          />
          <StatTile
            label="Ditolak"
            value={String(data.status.ditolak)}
            sublabel="Perlu unggah ulang"
            icon={XCircle}
            tone={data.status.ditolak > 0 ? "warning" : undefined}
            href="/verifikasi/semua?status=REJECTED"
          />
          <StatTile
            label="Terverifikasi"
            value={String(data.status.terverifikasi)}
            sublabel="Uang sudah masuk"
            icon={BadgeCheck}
            tone={data.status.terverifikasi > 0 ? "success" : undefined}
            href="/verifikasi/semua?status=TERVERIFIKASI"
          />
        </StatRow>
      </section>

      <div className="grid gap-5 lg:grid-cols-2">
        <RingkasanKategori data={data.perKategori} />
        <RingkasanKelas data={data.perKelas} />
      </div>

      <section className="overflow-hidden rounded-2xl border border-border/70 bg-card shadow-sm">
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
            nativeButton={false}
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
    <section className="overflow-hidden rounded-2xl border border-border/70 bg-card shadow-sm">
      <KepalaKartu
        icon={Layers}
        judul="Per kategori tagihan"
        keterangan="Seberapa jauh tiap jenis biaya terkumpul"
      />
      <div className="border-t border-border/70">
        {data.length === 0 ? (
          <p className="px-5 py-6 text-sm text-muted-foreground">
            Belum ada tagihan.
          </p>
        ) : (
          <ul className="divide-y divide-border/60">
            {data.map((item) => {
              const tertagih = Number(item.tertagih);
              const terkumpul = Number(item.terkumpul);
              const persen = tertagih === 0 ? 0 : (terkumpul / tertagih) * 100;

              return (
                <li
                  key={item.kategori}
                  className="flex flex-col gap-1.5 px-5 py-3"
                >
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
                  <Bar persen={persen} tampilkanPersen />
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </section>
  );
}

function RingkasanKelas({ data }: { data: KelasRingkas[] }) {
  return (
    <section className="flex flex-col overflow-hidden rounded-2xl border border-border/70 bg-card shadow-sm">
      <KepalaKartu
        icon={School}
        judul="Per kelas"
        keterangan="Uang terkumpul dan keadaan pembacaan buktinya"
      />
      <div className="flex-1 border-t border-border/70">
        {data.length === 0 ? (
          <p className="px-5 py-6 text-sm text-muted-foreground">
            Belum ada kelas.
          </p>
        ) : (
          <ul className="divide-y divide-border/60">
            {data.map((item) => {
              const tertagih = Number(item.tertagih);
              const terkumpul = Number(item.terkumpul);
              const persen = tertagih === 0 ? 0 : (terkumpul / tertagih) * 100;

              return (
                <li
                  key={item.kelas}
                  className="flex flex-col gap-1.5 px-5 py-3"
                >
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

                  {/*
                    Angka OCR ikut per kelas karena masalah pembacaan hampir
                    selalu berkelompok: satu kelas yang diajari memfoto struk
                    dengan cara yang sama akan menghasilkan bukti yang sama
                    sulitnya dibaca. Satu angka global menyembunyikan justru
                    kelas yang perlu dibantu.
                  */}
                  {item.buktiDibaca > 0 && (
                    <p className="flex flex-wrap items-center gap-x-3 gap-y-0.5 text-xs text-muted-foreground">
                      <span>{item.buktiDibaca} bukti terbaca</span>
                      {item.buktiPerluDitinjau > 0 && (
                        <span className="text-warning">
                          {item.buktiPerluDitinjau} perlu ditinjau
                        </span>
                      )}
                      {item.buktiGagalDibaca > 0 && (
                        <span className="text-destructive">
                          {item.buktiGagalDibaca} gagal dibaca
                        </span>
                      )}
                      {item.rataKeyakinan != null && (
                        <span>
                          keyakinan rata-rata{" "}
                          {(Number(item.rataKeyakinan) * 100).toFixed(0)}%
                        </span>
                      )}
                    </p>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </section>
  );
}

/**
 * Kepala kartu ringkasan: ikon berlatar lembut, judul, dan satu baris
 * keterangan. Sebelumnya hanya teks kecil setebal isi kartunya, sehingga dua
 * kartu bersebelahan tidak punya pembeda selain kata pertamanya.
 */
function KepalaKartu({
  icon: Icon,
  judul,
  keterangan,
}: {
  icon: LucideIcon;
  judul: string;
  keterangan: string;
}) {
  return (
    <div className="flex items-start gap-3 px-5 py-4">
      <span className="flex size-8 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
        <Icon className="size-4" />
      </span>
      <div className="min-w-0">
        <h3 className="font-heading text-sm font-semibold">{judul}</h3>
        <p className="text-xs text-muted-foreground">{keterangan}</p>
      </div>
    </div>
  );
}

function Bar({
  persen,
  tampilkanPersen,
}: {
  persen: number;
  tampilkanPersen?: boolean;
}) {
  const nilai = Math.min(Math.max(persen, 0), 100);
  const batang = (
    <div
      className="h-2.5 w-full overflow-hidden rounded-full bg-muted/80 inset-shadow-sm"
      role="img"
      aria-label={`${Math.round(nilai)} persen terkumpul`}
    >
      <div
        className={cn(
          "h-full rounded-full transition-[width] duration-700 ease-out",
          nilai >= 100
            ? "bg-gradient-to-r from-success to-success/70"
            : nilai > 0
              ? "bg-gradient-to-r from-primary to-success"
              : "bg-transparent",
        )}
        style={{ width: `${nilai}%` }}
      />
    </div>
  );

  if (!tampilkanPersen) {
    return batang;
  }

  // Angka persen ikut ditulis, bukan hanya digambar. Batang sepanjang seperempat
  // dan sepertiga sulit dibedakan sekilas, dan yang dicari admin justru
  // bedanya.
  return (
    <div className="flex items-center gap-3">
      {batang}
      <span
        className={cn(
          "w-10 shrink-0 text-right text-xs font-medium tabular-nums",
          nilai >= 100 ? "text-success" : "text-muted-foreground",
        )}
      >
        {Math.round(nilai)}%
      </span>
    </div>
  );
}
