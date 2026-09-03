import { cn } from "@/lib/utils";

/**
 * Keyakinan OCR sebagai bar + persentase. Ambangnya mengikuti logika
 * ProcessPaymentOcr: di bawah 10% ditolak otomatis, di atas ambang batas
 * (bawaan 80%) diverifikasi otomatis, di antaranya perlu ditinjau manusia.
 */
export function KeyakinanOcr({
  value,
  showLabel = true,
}: {
  value: number | null | undefined;
  showLabel?: boolean;
}) {
  if (value === null || value === undefined) {
    return <span className="text-muted-foreground">—</span>;
  }

  const persen = Math.round(value * 100);
  const tone =
    persen >= 80 ? "success" : persen >= 40 ? "warning" : "danger";

  return (
    <div className="flex min-w-20 items-center gap-2">
      <div
        className="h-1.5 w-12 shrink-0 overflow-hidden rounded-full bg-muted"
        role="img"
        aria-label={`Keyakinan OCR ${persen} persen`}
      >
        <div
          className={cn(
            "h-full rounded-full",
            tone === "success" && "bg-success",
            tone === "warning" && "bg-warning",
            tone === "danger" && "bg-danger",
          )}
          style={{ width: `${Math.max(persen, 2)}%` }}
        />
      </div>
      {showLabel && (
        <span
          className={cn(
            "text-xs tabular-nums",
            tone === "success" && "text-success",
            tone === "warning" && "text-warning",
            tone === "danger" && "text-danger",
          )}
        >
          {persen}%
        </span>
      )}
    </div>
  );
}
