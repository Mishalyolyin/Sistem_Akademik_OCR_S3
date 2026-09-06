"use client";

import { useState } from "react";
import { ChevronLeft, ChevronRight, Search, TriangleAlert } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { PaymentStatusBadge } from "@/components/status-badge";
import { KeyakinanOcr } from "@/features/verifikasi/keyakinan-ocr";
import {
  EmptyState,
  ErrorState,
  PageHeader,
  TableSkeleton,
} from "@/components/page-header";
import {
  useForensikList,
  type BarisForensik,
} from "@/features/forensik/api";
import { ApiError } from "@/lib/api";
import { formatRupiah, formatTanggalJam } from "@/lib/format";
import { PanelForensik } from "@/features/forensik/panel-forensik";

/**
 * Di bawah ini pembacaannya patut dicurigai; angkanya mengikuti ambang bawaan
 * verifikasi otomatis. Pecahan 0–1, satuan yang sama dengan yang tersimpan —
 * memakai 80 di sini akan mencocokkan SELURUH baris, karena tidak ada keyakinan
 * yang lebih besar dari 1.
 */
const AMBANG_KEYAKINAN_RENDAH = 0.8;

/**
 * Forensik OCR: apa yang sebenarnya terbaca di balik tiap keputusan otomatis.
 *
 * <p>Keputusan atas bukti bayar diambil mesin. Kalau hasilnya terasa salah,
 * yang perlu dilihat bukan kesimpulannya melainkan bacaannya — termasuk field
 * yang tidak dikenal kode mana pun, yang justru sering jadi petunjuknya.
 */
export function HalamanForensik() {
  const [cari, setCari] = useState("");
  const [hanyaRagu, setHanyaRagu] = useState(false);
  const [page, setPage] = useState(0);
  const [dipilih, setDipilih] = useState<number | null>(null);

  const daftar = useForensikList({
    cari: cari.trim() || undefined,
    maksKeyakinan: hanyaRagu ? AMBANG_KEYAKINAN_RENDAH : undefined,
    page,
  });

  return (
    <div className="flex flex-col gap-6 p-6">
      <PageHeader
        title="Forensik OCR"
        description="Hasil pembacaan mentah di balik tiap keputusan otomatis"
      />

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative">
          <Search className="absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Cari nama atau NIM"
            className="w-64 pl-8"
            value={cari}
            onChange={(event) => {
              setPage(0);
              setCari(event.target.value);
            }}
          />
        </div>

        <Button
          variant={hanyaRagu ? "default" : "outline"}
          onClick={() => {
            setPage(0);
            setHanyaRagu((sebelumnya) => !sebelumnya);
          }}
        >
          <TriangleAlert />
          Pembacaan meragukan
        </Button>

        {hanyaRagu && (
          <span className="text-sm text-muted-foreground">
            Keyakinan di bawah {AMBANG_KEYAKINAN_RENDAH * 100}%, termasuk yang
            belum pernah terbaca sama sekali.
          </span>
        )}
      </div>

      {daftar.isPending ? (
        <TableSkeleton rows={8} />
      ) : daftar.error ? (
        <ErrorState
          message={
            daftar.error instanceof ApiError
              ? daftar.error.message
              : "Coba muat ulang halaman."
          }
        />
      ) : daftar.data.content.length === 0 ? (
        <EmptyState
          title="Tidak ada bukti bayar"
          description={
            hanyaRagu
              ? "Tidak ada pembacaan yang keyakinannya rendah — itu kabar baik."
              : "Belum ada bukti bayar yang diunggah."
          }
        />
      ) : (
        <>
          <TabelForensik
            data={daftar.data.content}
            dipilih={dipilih}
            onPilih={setDipilih}
          />

          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">
              Halaman {daftar.data.number + 1} dari{" "}
              {Math.max(daftar.data.totalPages, 1)} ·{" "}
              {daftar.data.totalElements} bukti
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={daftar.data.first}
                onClick={() => setPage((p) => Math.max(p - 1, 0))}
              >
                <ChevronLeft />
                Sebelumnya
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={daftar.data.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Berikutnya
                <ChevronRight />
              </Button>
            </div>
          </div>
        </>
      )}

      {/* key: panel terpasang ulang saat berpindah bukti, jadi tidak ada sisa
          hasil pembacaan sebelumnya yang sempat terlihat. */}
      {dipilih !== null && (
        <PanelForensik
          key={dipilih}
          paymentId={dipilih}
          onClose={() => setDipilih(null)}
        />
      )}
    </div>
  );
}

function TabelForensik({
  data,
  dipilih,
  onPilih,
}: {
  data: BarisForensik[];
  dipilih: number | null;
  onPilih: (id: number) => void;
}) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-border/70 bg-card shadow-sm">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Mahasiswa</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Nominal diklaim</TableHead>
            <TableHead>Keyakinan</TableHead>
            <TableHead className="text-right">Catatan mesin</TableHead>
            <TableHead>Diunggah</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((baris) => (
            <TableRow
              key={baris.paymentId}
              className={cn(
                "cursor-pointer",
                dipilih === baris.paymentId && "bg-muted/60",
              )}
              onClick={() => onPilih(baris.paymentId)}
            >
              <TableCell>
                <span className="block font-medium">{baris.namaMahasiswa}</span>
                <span className="block font-mono text-xs text-muted-foreground">
                  {baris.nim}
                </span>
              </TableCell>

              <TableCell>
                <PaymentStatusBadge status={baris.status} />
              </TableCell>

              <TableCell className="text-right whitespace-nowrap">
                {formatRupiah(baris.nominalDiklaim)}
              </TableCell>

              <TableCell>
                {!baris.adaHasilOcr ? (
                  <span className="text-xs text-muted-foreground">
                    belum terbaca
                  </span>
                ) : (
                  <KeyakinanOcr value={Number(baris.keyakinan)} />
                )}
              </TableCell>

              <TableCell className="text-right text-muted-foreground">
                {baris.jumlahCatatan > 0 ? baris.jumlahCatatan : "—"}
              </TableCell>

              <TableCell className="whitespace-nowrap text-muted-foreground">
                {formatTanggalJam(baris.diunggah)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
