import { cn } from "@/lib/utils";

export type PaymentStatus =
  | "PENDING"
  | "NEEDS_REVIEW"
  | "AUTO_VERIFIED"
  | "VERIFIED"
  | "REJECTED"
  | "FAILED";

export type InstallmentStatus = "UNPAID" | "PARTIAL" | "PAID" | "OVERDUE";

type Tone = "success" | "warning" | "danger" | "info" | "neutral";

const toneClass: Record<Tone, string> = {
  success: "bg-success-soft text-success border-success/25",
  warning: "bg-warning-soft text-warning border-warning/25",
  danger: "bg-danger-soft text-danger border-danger/25",
  info: "bg-info-soft text-info border-info/25",
  neutral: "bg-muted text-muted-foreground border-border",
};

const paymentMeta: Record<PaymentStatus, { label: string; tone: Tone }> = {
  PENDING: { label: "Menunggu OCR", tone: "neutral" },
  NEEDS_REVIEW: { label: "Perlu ditinjau", tone: "warning" },
  AUTO_VERIFIED: { label: "Terverifikasi otomatis", tone: "success" },
  VERIFIED: { label: "Terverifikasi", tone: "success" },
  REJECTED: { label: "Ditolak", tone: "danger" },
  FAILED: { label: "Gagal diproses", tone: "danger" },
};

const installmentMeta: Record<
  InstallmentStatus,
  { label: string; tone: Tone }
> = {
  UNPAID: { label: "Belum bayar", tone: "neutral" },
  PARTIAL: { label: "Kurang bayar", tone: "warning" },
  PAID: { label: "Lunas", tone: "success" },
  OVERDUE: { label: "Terlambat", tone: "danger" },
};

function Pill({
  label,
  tone,
  className,
}: {
  label: string;
  tone: Tone;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-xs font-medium whitespace-nowrap",
        toneClass[tone],
        className,
      )}
    >
      <span className="size-1.5 rounded-full bg-current" aria-hidden />
      {label}
    </span>
  );
}

export function PaymentStatusBadge({
  status,
  className,
}: {
  status: PaymentStatus;
  className?: string;
}) {
  const meta = paymentMeta[status];
  return <Pill label={meta.label} tone={meta.tone} className={className} />;
}

export function InstallmentStatusBadge({
  status,
  className,
}: {
  status: InstallmentStatus;
  className?: string;
}) {
  const meta = installmentMeta[status];
  return <Pill label={meta.label} tone={meta.tone} className={className} />;
}
