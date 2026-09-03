"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import {
  Check,
  Download,
  History,
  Loader2,
  RefreshCw,
  TriangleAlert,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Textarea } from "@/components/ui/textarea";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { PaymentStatusBadge } from "@/components/status-badge";
import { unduhBerkas } from "@/features/pengaturan/api";
import { ApiError } from "@/lib/api";
import { formatRupiah, formatTanggal, formatTanggalJam } from "@/lib/format";
import { BuktiTransfer } from "./bukti-transfer";
import { KeyakinanOcr } from "./keyakinan-ocr";
import {
  useDecidePayment,
  usePaymentLogs,
  useRequeueOcr,
  type PaymentRow,
} from "./api";

export function PanelTinjau({
  payment,
  onClose,
}: {
  payment: PaymentRow | null;
  onClose: () => void;
}) {
  const [alasan, setAlasan] = useState("");
  const putuskan = useDecidePayment();
  const bacaUlang = useRequeueOcr();
  const riwayat = usePaymentLogs(payment?.id ?? null);

  useEffect(() => setAlasan(""), [payment?.id]);

  if (!payment) return null;

  const ocr = payment.ocrData;
  const terbaca = ocr?.extracted_amount ?? null;
  const selisih = terbaca === null ? null : terbaca - Number(payment.amount);
  const namaTidakCocok = (ocr?.flags ?? []).some((f) =>
    f.toLowerCase().includes("name not found"),
  );
  const sudahDiputuskan =
    payment.status === "VERIFIED" ||
    payment.status === "AUTO_VERIFIED" ||
    payment.status === "REJECTED";

  function putus(approve: boolean) {
    putuskan.mutate(
      { id: payment!.id, approve, note: alasan.trim() || undefined },
      {
        onSuccess: (hasil) => {
          toast.success(
            approve
              ? `Pembayaran ${formatRupiah(hasil.amount)} diverifikasi dan dialokasikan ke cicilan.`
              : "Pembayaran ditolak.",
          );
          onClose();
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal menyimpan keputusan.",
          ),
      },
    );
  }

  return (
    <Sheet open onOpenChange={(open) => !open && onClose()}>
      <SheetContent
        side="right"
        className="w-full gap-0 p-0 sm:max-w-full md:w-225 md:max-w-[92vw]"
      >
        <SheetHeader className="border-b border-border px-5 py-4">
          <SheetTitle className="font-heading text-base">
            {payment.studentName}
          </SheetTitle>
          <SheetDescription className="flex flex-wrap items-center gap-2">
            <span className="font-mono text-xs">{payment.studentNim}</span>
            <span aria-hidden>·</span>
            <span>{payment.className ?? "Tanpa kelas"}</span>
            <span aria-hidden>·</span>
            <span>
              {payment.installmentNo
                ? `${payment.semesterNumber ? `Semester ${payment.semesterNumber}, ` : ""}cicilan ${payment.installmentNo} dari 5`
                : "Sekali bayar"}
            </span>
            <PaymentStatusBadge status={payment.status} className="ml-1" />
          </SheetDescription>
        </SheetHeader>

        {/* Split view: bukti asli di kiri, hasil baca OCR di kanan. */}
        <div className="grid flex-1 grid-cols-1 overflow-hidden md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          <div className="overflow-y-auto border-b border-border p-5 md:border-r md:border-b-0">
            <BuktiTransfer paymentId={payment.id} />
          </div>

          <div className="flex flex-col gap-4 overflow-y-auto p-5">
            <div
              className={cn(
                "rounded-lg border p-4",
                selisih === null
                  ? "border-border bg-muted/40"
                  : selisih === 0
                    ? "border-success/25 bg-success-soft"
                    : "border-warning/25 bg-warning-soft",
              )}
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs text-muted-foreground">Tagihan</p>
                  <p className="font-heading text-lg font-semibold tnum">
                    {formatRupiah(payment.amount)}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-muted-foreground">Terbaca OCR</p>
                  <p
                    className={cn(
                      "font-heading text-lg font-semibold tnum",
                      selisih !== null && selisih !== 0 && "text-warning",
                    )}
                  >
                    {terbaca === null ? "Belum dibaca" : formatRupiah(terbaca)}
                  </p>
                </div>
              </div>

              {selisih !== null && selisih !== 0 && (
                <p className="mt-3 flex items-center gap-1.5 border-t border-warning/20 pt-3 text-sm text-warning">
                  <TriangleAlert className="size-4 shrink-0" />
                  Selisih {formatRupiah(Math.abs(selisih))}{" "}
                  {selisih > 0 ? "lebih" : "kurang"}
                </p>
              )}
              {selisih === 0 && (
                <p className="mt-3 flex items-center gap-1.5 border-t border-success/20 pt-3 text-sm text-success">
                  <Check className="size-4 shrink-0" />
                  Nominal cocok persis
                </p>
              )}
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between">
                <h3 className="text-xs font-semibold text-muted-foreground uppercase">
                  Hasil baca OCR
                </h3>
                <Button
                  variant="ghost"
                  size="xs"
                  disabled={bacaUlang.isPending || sudahDiputuskan}
                  onClick={() =>
                    bacaUlang.mutate(payment.id, {
                      onSuccess: () => toast.success("Bukti dibaca ulang."),
                      onError: (e) =>
                        toast.error(
                          e instanceof ApiError ? e.message : "Gagal membaca ulang.",
                        ),
                    })
                  }
                >
                  {bacaUlang.isPending ? (
                    <Loader2 className="animate-spin" />
                  ) : (
                    <RefreshCw />
                  )}
                  Baca ulang
                </Button>
              </div>

              <dl className="flex flex-col divide-y divide-border rounded-lg border border-border">
                <BarisData label="Keyakinan">
                  <KeyakinanOcr
                    value={
                      payment.ocrConfidence === null
                        ? null
                        : Number(payment.ocrConfidence)
                    }
                  />
                </BarisData>
                <BarisData label="Bank">{payment.bankName ?? "—"}</BarisData>
                <BarisData label="Tanggal transaksi">
                  {formatTanggal(payment.paymentProofDate)}
                </BarisData>
                <BarisData label="Nama pengirim" warning={namaTidakCocok}>
                  {namaTidakCocok ? "Tidak cocok" : "Cocok"}
                </BarisData>
                <BarisData label="Diunggah">
                  {formatTanggalJam(payment.createdAt)}
                </BarisData>
              </dl>
            </div>

            {(ocr?.flags ?? []).length > 0 && (
              <div className="rounded-lg border border-border p-3">
                <h3 className="mb-1.5 text-xs font-semibold text-muted-foreground uppercase">
                  Catatan sistem
                </h3>
                <ul className="flex list-disc flex-col gap-1 pl-4 text-sm">
                  {ocr!.flags!.map((flag) => (
                    <li key={flag}>{flag}</li>
                  ))}
                </ul>
              </div>
            )}

            {riwayat.data && riwayat.data.length > 0 && (
              <div>
                <h3 className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-muted-foreground uppercase">
                  <History className="size-3.5" />
                  Riwayat status
                </h3>
                <ul className="flex flex-col gap-1.5 rounded-lg border border-border p-3 text-xs">
                  {riwayat.data.map((log) => (
                    <li key={log.id}>
                      <span className="font-medium">
                        {log.fromStatus ? `${log.fromStatus} → ` : ""}
                        {log.toStatus}
                      </span>
                      <span className="block text-muted-foreground">
                        {log.adminId ? `Admin ${log.adminId}` : "Otomatis"} ·{" "}
                        {formatTanggalJam(log.createdAt)}
                        {log.note ? ` · ${log.note}` : ""}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {payment.rejectReason && (
              <p className="rounded-md border border-danger/25 bg-danger-soft px-3 py-2 text-sm text-danger">
                Alasan penolakan: {payment.rejectReason}
              </p>
            )}

            {sudahDiputuskan ? (
              <>
                <p className="rounded-md border border-border bg-muted/40 px-3 py-2 text-sm text-muted-foreground">
                  Pembayaran ini sudah diputuskan
                  {payment.verifiedAt
                    ? ` pada ${formatTanggalJam(payment.verifiedAt)}`
                    : ""}
                  .{" "}
                  <Link
                    href={`/mahasiswa/${payment.studentId}`}
                    className="underline"
                  >
                    Lihat tagihan mahasiswa
                  </Link>
                </p>

                {/* Kuitansi hanya untuk uang yang benar-benar diterima. */}
                {(payment.status === "VERIFIED" ||
                  payment.status === "AUTO_VERIFIED") && (
                  <Button
                    variant="outline"
                    onClick={() =>
                      unduhBerkas(
                        `reports/kuitansi/${payment.id}.pdf`,
                        `kuitansi-${payment.id}.pdf`,
                      ).catch(() => toast.error("Kuitansi gagal diunduh."))
                    }
                  >
                    <Download />
                    Unduh kuitansi PDF
                  </Button>
                )}
              </>
            ) : (
              <>
                <Separator />

                <div className="flex flex-col gap-2">
                  <label
                    htmlFor="alasan"
                    className="text-xs font-semibold text-muted-foreground uppercase"
                  >
                    Alasan (wajib bila menolak)
                  </label>
                  <Textarea
                    id="alasan"
                    value={alasan}
                    onChange={(event) => setAlasan(event.target.value)}
                    placeholder="Contoh: nominal pada bukti tidak sesuai tagihan."
                    rows={2}
                  />
                </div>

                <div className="flex gap-2">
                  <Button
                    className="flex-1"
                    disabled={putuskan.isPending}
                    onClick={() => putus(true)}
                  >
                    {putuskan.isPending ? (
                      <Loader2 className="animate-spin" />
                    ) : (
                      <Check />
                    )}
                    Verifikasi
                  </Button>
                  <Button
                    variant="destructive"
                    className="flex-1"
                    disabled={putuskan.isPending || alasan.trim().length < 5}
                    onClick={() => putus(false)}
                  >
                    <X />
                    Tolak
                  </Button>
                </div>
              </>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}

function BarisData({
  label,
  children,
  warning,
}: {
  label: string;
  children: React.ReactNode;
  warning?: boolean;
}) {
  return (
    <div className="flex items-center justify-between gap-4 px-3 py-2">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd
        className={cn(
          "flex items-center gap-1.5 text-right text-sm",
          warning && "font-medium text-warning",
        )}
      >
        {warning && <TriangleAlert className="size-3.5 shrink-0" />}
        {children}
      </dd>
    </div>
  );
}
