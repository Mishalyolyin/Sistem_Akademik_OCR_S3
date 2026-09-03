import { AlertCircle, Loader2 } from "lucide-react";
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
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h2 className="font-heading text-xl font-semibold tracking-tight">
          {title}
        </h2>
        {description && (
          <p className="text-sm text-muted-foreground">{description}</p>
        )}
      </div>
      {children && <div className="flex items-center gap-2">{children}</div>}
    </div>
  );
}

export function TableSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <div className="flex flex-col gap-2 rounded-lg border border-border bg-card p-4">
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
    <div className="flex flex-col items-center gap-1 rounded-lg border border-dashed border-border px-6 py-12 text-center">
      <p className="font-medium">{title}</p>
      {description && (
        <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
      )}
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
