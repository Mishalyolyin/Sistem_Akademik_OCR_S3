"use client";

import { useMemo, useState } from "react";
import { BadgeCheck, Clock, TriangleAlert, Wallet } from "lucide-react";
import { StatRow, StatTile } from "@/components/stat-tile";
import {
  ErrorState,
  PageHeader,
  TableSkeleton,
} from "@/components/page-header";
import type { PaymentStatus } from "@/components/status-badge";
import { usePayments, type PaymentRow } from "@/features/verifikasi/api";
import { PanelTinjau } from "@/features/verifikasi/panel-tinjau";
import { TabelVerifikasi } from "@/features/verifikasi/tabel-verifikasi";
import type { VerifikasiView } from "@/features/verifikasi/kategori";
import { ApiError } from "@/lib/api";
import { formatRupiah } from "@/lib/format";

const SEMUA = "SEMUA";

export function HalamanVerifikasi({ view }: { view: VerifikasiView }) {
  const [statusFilter, setStatusFilter] = useState<string>(SEMUA);
  const [search, setSearch] = useState("");
  const [dipilih, setDipilih] = useState<PaymentRow | null>(null);

  const { data, isPending, error } = usePayments({
    status: statusFilter === SEMUA ? undefined : [statusFilter as PaymentStatus],
    size: 100,
  });

  // Penyaringan kategori dilakukan di klien karena satu halaman hanya
  // menampilkan satu kategori dan jumlahnya masih kecil.
  const rows = useMemo(
    () =>
      (data?.content ?? []).filter(
        (row) => row.categoryLabel === view.title || row.categoryLabel === null,
      ),
    [data, view.title],
  );

  const terpilih = dipilih
    ? (rows.find((r) => r.id === dipilih.id) ?? dipilih)
    : null;

  const ringkasan = useMemo(() => {
    const perluDitinjau = rows.filter((r) => r.status === "NEEDS_REVIEW").length;
    const tidakCocok = rows.filter((r) => {
      const terbaca = r.ocrData?.extracted_amount;
      return terbaca !== null && terbaca !== undefined && terbaca !== Number(r.amount);
    }).length;
    const terverifikasi = rows.filter(
      (r) => r.status === "AUTO_VERIFIED" || r.status === "VERIFIED",
    );
    return {
      perluDitinjau,
      tidakCocok,
      jumlahTerverifikasi: terverifikasi.length,
      nominalMasuk: terverifikasi.reduce((total, r) => total + Number(r.amount), 0),
    };
  }, [rows]);

  return (
    <div className="flex flex-col gap-5 p-6">
      <PageHeader title={view.title} description={view.hint} />

      <StatRow>
        <StatTile
          label="Perlu ditinjau"
          value={String(ringkasan.perluDitinjau)}
          sublabel="OCR kurang yakin"
          icon={Clock}
          tone="warning"
        />
        <StatTile
          label="Nominal tidak cocok"
          value={String(ringkasan.tidakCocok)}
          sublabel="Bukti beda dengan tagihan"
          icon={TriangleAlert}
        />
        <StatTile
          label="Sudah terverifikasi"
          value={String(ringkasan.jumlahTerverifikasi)}
          icon={BadgeCheck}
          tone="success"
        />
        <StatTile
          label="Nominal masuk"
          value={formatRupiah(ringkasan.nominalMasuk)}
          sublabel="Dari yang terverifikasi"
          icon={Wallet}
        />
      </StatRow>

      {isPending ? (
        <TableSkeleton />
      ) : error ? (
        <ErrorState
          message={
            error instanceof ApiError ? error.message : "Coba muat ulang halaman."
          }
        />
      ) : (
        <TabelVerifikasi
          data={rows}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          search={search}
          onSearchChange={setSearch}
          onReview={setDipilih}
        />
      )}

      {/*
        `key` per pembayaran membuat panelnya terpasang ulang saat berpindah
        baris, sehingga isian alasan ikut kosong tanpa perlu effect yang
        memicu render berantai.
      */}
      <PanelTinjau
        key={terpilih?.id ?? "kosong"}
        payment={terpilih}
        onClose={() => setDipilih(null)}
      />
    </div>
  );
}
