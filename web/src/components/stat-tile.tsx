import Link from "next/link";
import { cn } from "@/lib/utils";
import type { LucideIcon } from "lucide-react";

export function StatTile({
  label,
  value,
  sublabel,
  icon: Icon,
  tone = "default",
  className,
  href,
}: {
  label: string;
  value: string;
  sublabel?: string;
  icon?: LucideIcon;
  tone?: "default" | "warning" | "success";
  className?: string;
  /** Bila diisi, seluruh kotak jadi tautan ke daftar yang sudah tersaring. */
  href?: string;
}) {
  const isi = (
    <>
      <div className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
        {Icon && <Icon className="size-3.5" />}
        {label}
      </div>
      <div
        className={cn(
          "font-heading text-2xl font-semibold tracking-tight tnum",
          tone === "warning" && "text-warning",
          tone === "success" && "text-success",
        )}
      >
        {value}
      </div>
      {sublabel && (
        <div className="text-xs text-muted-foreground">{sublabel}</div>
      )}
    </>
  );

  const kelas = cn(
    "flex flex-col gap-1 border-r border-border px-5 py-4 last:border-r-0",
    href &&
      "transition-colors hover:bg-muted focus-visible:bg-muted focus-visible:outline-none",
    className,
  );

  if (href) {
    return (
      <Link href={href} className={kelas}>
        {isi}
      </Link>
    );
  }

  return <div className={kelas}>{isi}</div>;
}

/** Baris ringkasan: beberapa StatTile dalam satu kotak, bukan kartu terpisah-pisah. */
export function StatRow({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-2 divide-y divide-border overflow-hidden rounded-lg border border-border bg-card sm:grid-cols-2 sm:divide-y-0 lg:grid-cols-4">
      {children}
    </div>
  );
}
