"use client";

import { Bot, Loader2, User } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { PaymentStatusBadge } from "@/components/status-badge";
import { KeyakinanOcr } from "@/features/verifikasi/keyakinan-ocr";
import { ErrorState } from "@/components/page-header";
import { ApiError } from "@/lib/api";
import { formatRupiah, formatTanggalJam } from "@/lib/format";
import { useForensikDetail } from "./api";

/**
 * Isi satu pembacaan: hasil OCR mentah, catatan mesin, dan riwayat keputusan.
 *
 * <p>Hasil OCR sengaja ditampilkan sebagai JSON apa adanya. Merapikannya jadi
 * beberapa field yang dikenal justru menghilangkan yang dicari saat menelusuri:
 * bentuk jawaban service OCR bisa berubah, dan yang menarik biasanya field yang
 * tidak diduga ada.
 */
export function PanelForensik({
  paymentId,
  onClose,
}: {
  paymentId: number;
  onClose: () => void;
}) {
  const { data, isPending, error } = useForensikDetail(paymentId);

  return (
    <Dialog open onOpenChange={(next) => (next ? undefined : onClose())}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>
            {data ? data.namaMahasiswa : `Bukti bayar #${paymentId}`}
          </DialogTitle>
          <DialogDescription>
            {data ? `${data.nim} · bukti #${data.paymentId}` : "Memuat…"}
          </DialogDescription>
        </DialogHeader>

        {isPending ? (
          <div className="flex justify-center py-10">
            <Loader2 className="size-5 animate-spin text-muted-foreground" />
          </div>
        ) : error || !data ? (
          <ErrorState
            message={
              error instanceof ApiError
                ? error.message
                : "Hasil pembacaan tidak bisa dimuat."
            }
          />
        ) : (
          <div className="flex max-h-[70vh] flex-col gap-4 overflow-y-auto">
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-lg border border-border p-3 text-sm sm:grid-cols-4">
              <div>
                <dt className="text-xs text-muted-foreground">Status</dt>
                <dd className="mt-1">
                  <PaymentStatusBadge status={data.status} />
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">
                  Nominal diklaim
                </dt>
                <dd className="tnum mt-1">
                  {formatRupiah(data.nominalDiklaim)}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Keyakinan</dt>
                <dd className="mt-1">
                  {data.keyakinan === null ? (
                    <span className="text-muted-foreground">belum terbaca</span>
                  ) : (
                    <KeyakinanOcr value={Number(data.keyakinan)} />
                  )}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Diunggah</dt>
                <dd className="mt-1">{formatTanggalJam(data.diunggah)}</dd>
              </div>
            </dl>

            {data.alasanDitolak && (
              <p className="rounded-lg border border-danger/25 bg-danger-soft px-3 py-2 text-sm text-danger">
                Alasan ditolak: {data.alasanDitolak}
              </p>
            )}

            {data.catatan.length > 0 && (
              <section className="flex flex-col gap-2">
                <h4 className="text-sm font-semibold">Catatan mesin</h4>
                <ul className="flex flex-col gap-1.5 rounded-lg border border-border p-3 text-sm">
                  {data.catatan.map((catatan, index) => (
                    <li key={index} className="flex gap-2">
                      <Bot className="mt-0.5 size-3.5 shrink-0 text-muted-foreground" />
                      <span>{catatan}</span>
                    </li>
                  ))}
                </ul>
              </section>
            )}

            <section className="flex flex-col gap-2">
              <h4 className="text-sm font-semibold">Riwayat keputusan</h4>
              {data.riwayat.length === 0 ? (
                <p className="rounded-lg border border-dashed border-border p-3 text-sm text-muted-foreground">
                  Belum ada keputusan apa pun atas bukti ini.
                </p>
              ) : (
                <ol className="flex flex-col divide-y divide-border rounded-lg border border-border text-sm">
                  {data.riwayat.map((langkah, index) => (
                    <li key={index} className="flex gap-2.5 px-3 py-2">
                      {langkah.otomatis ? (
                        <Bot className="mt-0.5 size-4 shrink-0 text-info" />
                      ) : (
                        <User className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                      )}
                      <div className="min-w-0 flex-1">
                        <span>
                          {langkah.dari ? `${langkah.dari} → ` : ""}
                          <strong>{langkah.ke}</strong>
                          <span className="text-muted-foreground">
                            {langkah.otomatis
                              ? " · otomatis oleh sistem"
                              : ` · admin #${langkah.adminId}`}
                          </span>
                        </span>
                        {langkah.catatan && (
                          <span className="block text-xs text-muted-foreground">
                            {langkah.catatan}
                          </span>
                        )}
                      </div>
                      <span className="shrink-0 text-xs text-muted-foreground">
                        {formatTanggalJam(langkah.waktu)}
                      </span>
                    </li>
                  ))}
                </ol>
              )}
            </section>

            <section className="flex flex-col gap-2">
              <h4 className="text-sm font-semibold">Hasil OCR mentah</h4>
              {data.ocrMentah === null ? (
                <p className="rounded-lg border border-dashed border-border p-3 text-sm text-muted-foreground">
                  Belum ada hasil pembacaan. Pekerjaan OCR-nya belum jalan, atau
                  gagal dan masuk dead-letter queue.
                </p>
              ) : (
                <pre className="overflow-x-auto rounded-lg border border-border bg-muted/40 p-3 font-mono text-xs">
                  {JSON.stringify(data.ocrMentah, null, 2)}
                </pre>
              )}
            </section>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
