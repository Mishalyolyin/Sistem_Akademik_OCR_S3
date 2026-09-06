import { AlertCircle, Inbox, Loader2 } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";

export function PageHeader({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        {/*
          Judul halaman sebelumnya text-xl — nyaris sama besar dengan teks
          biasa di sekitarnya, sehingga tidak ada yang menuntun mata saat
          halaman terbuka. Sekarang cukup besar untuk jadi titik masuk.
        */}
        <h2 className="font-heading text-2xl leading-tight font-bold tracking-tight text-balance sm:text-[1.75rem]">
          {title}
        </h2>
        {description && (
          <p className="mt-1 max-w-2xl text-sm text-pretty text-muted-foreground">
            {description}
          </p>
        )}
      </div>
      {children && <div className="flex items-center gap-2">{children}</div>}
    </div>
  );
}

export function TableSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <div className="flex flex-col gap-2 rounded-2xl border border-border/70 bg-card shadow-sm p-4">
      {Array.from({ length: rows }, (_, index) => (
        <Skeleton key={index} className="h-9 w-full" />
      ))}
    </div>
  );
}

export function ErrorState({ message }: { message: string }) {
  return (
    <div
      role="alert"
      className="flex items-start gap-2.5 rounded-lg border border-danger/25 bg-danger-soft px-4 py-3 text-sm text-danger"
    >
      <AlertCircle className="mt-0.5 size-4 shrink-0" />
      <div>
        <p className="font-medium">Gagal memuat data</p>
        <p>{message}</p>
      </div>
    </div>
  );
}

export function EmptyState({
  title,
  description,
}: {
  title: string;
  description?: string;
}) {
  return (
    <div className="flex flex-col items-center gap-2.5 rounded-2xl border border-dashed border-border/70 bg-muted/20 px-6 py-9 text-center">
      {/*
        Sebelumnya py-12 tanpa apa pun di dalamnya, jadi keadaan "tidak ada
        apa-apa" justru memakan ruang paling besar di halaman. Ikon dan padding
        yang lebih ringkas membuatnya terbaca sebagai catatan, bukan sebagai
        lubang.
      */}
      <span className="flex size-9 items-center justify-center rounded-xl bg-muted text-muted-foreground">
        <Inbox className="size-4.5" />
      </span>
      <div>
        <p className="font-medium">{title}</p>
        {description && (
          <p className="mx-auto mt-0.5 max-w-sm text-sm text-pretty text-muted-foreground">
            {description}
          </p>
        )}
      </div>
    </div>
  );
}

export function InlineSpinner({ label }: { label: string }) {
  return (
    <span className="flex items-center gap-2 text-sm text-muted-foreground">
      <Loader2 className="size-4 animate-spin" />
      {label}
    </span>
  );
}
