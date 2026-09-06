"use client";

import { PaymentStatusBadge } from "@/components/status-badge";
import {
  EmptyState,
  ErrorState,
  TableSkeleton,
} from "@/components/page-header";
import { useRiwayat } from "@/features/portal/api";
import { ApiError } from "@/lib/api";
import { formatRupiah, formatTanggal, formatTanggalJam } from "@/lib/format";

export function HalamanRiwayat() {
  const { data, isPending, error } = useRiwayat();

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h2 className="font-heading text-xl font-semibold tracking-tight">
          Riwayat pembayaran
        </h2>
        <p className="text-sm text-muted-foreground">
          Status berubah sendiri setelah sistem selesai membaca bukti
        </p>
      </div>

      {isPending ? (
        <TableSkeleton rows={4} />
      ) : error ? (
        <ErrorState
          message={
            error instanceof ApiError
              ? error.message
              : "Coba muat ulang halaman."
          }
        />
      ) : data.length === 0 ? (
        <EmptyState
          title="Belum ada pembayaran"
          description="Bukti transfer yang kamu unggah akan muncul di sini."
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {data.map((item) => (
            <li
              key={item.id}
              className="flex flex-col gap-2 rounded-2xl border border-border/70 bg-card shadow-sm p-4"
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-medium">
                    {item.kategori ?? "Pembayaran"}
                    {item.cicilanKe && (
                      <span className="ml-2 text-sm font-normal text-muted-foreground">
                        cicilan {item.cicilanKe}
                      </span>
                    )}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Diunggah {formatTanggalJam(item.diunggah)}
                    {item.bank && ` · ${item.bank}`}
                    {item.tanggalBukti &&
                      ` · transaksi ${formatTanggal(item.tanggalBukti)}`}
                  </p>
                </div>

                <div className="flex flex-col items-end gap-1.5">
                  <span className="font-heading font-semibold tnum">
                    {formatRupiah(item.nominal)}
                  </span>
                  <PaymentStatusBadge status={item.status} />
                </div>
              </div>

              {item.alasanDitolak && (
                <p className="rounded-md border border-danger/25 bg-danger-soft px-3 py-2 text-sm text-danger">
                  {item.alasanDitolak}
                </p>
              )}

              {item.status === "PENDING" && (
                <p className="text-xs text-muted-foreground">
                  Sedang dibaca sistem. Halaman ini menyegarkan sendiri.
                </p>
              )}
              {item.status === "NEEDS_REVIEW" && (
                <p className="text-xs text-muted-foreground">
                  Menunggu pemeriksaan admin.
                </p>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
