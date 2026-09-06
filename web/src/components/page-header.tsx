import { AlertCircle, Inbox, Loader2 } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

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

/**
 * Rangka tabel selagi datanya dimuat.
 *
 * <p>Sebelumnya sekadar tumpukan kotak abu selebar penuh. Bentuk itu tidak
 * menyerupai apa pun, sehingga begitu data datang seluruh halaman melompat:
 * kolom bermunculan, tinggi baris berubah, dan mata harus mencari ulang dari
 * awal. Rangka ini menirukan susunan tabel yang akan menggantikannya — kepala
 * tabel, lalu baris dengan lebar kolom yang berbeda-beda seperti isi
 * sungguhan.
 */
export function TableSkeleton({ rows = 6 }: { rows?: number }) {
  // Lebar yang tidak seragam. Kolom yang semuanya sama lebar terbaca sebagai
  // pemuat, bukan sebagai tabel yang sedang datang.
  const kolom = ["w-40", "w-28", "w-24", "w-32", "w-20"];

  return (
    <div className="overflow-hidden rounded-2xl border border-border/70 bg-card shadow-sm">
      <div className="flex items-center gap-6 border-b border-border/70 bg-muted/40 px-4 py-3.5">
        {kolom.map((lebar) => (
          <Skeleton key={lebar} className={cn("h-3", lebar)} />
        ))}
      </div>

      <div className="divide-y divide-border/50">
        {Array.from({ length: rows }, (_, baris) => (
          <div key={baris} className="flex items-center gap-6 px-4 py-3.5">
            {kolom.map((lebar, i) => (
              <Skeleton
                key={lebar}
                className={cn(
                  "h-4",
                  lebar,
                  // Kolom pertama sedikit lebih pekat: di tabel sungguhan ia
                  // memang berisi nama, yang paling menarik perhatian.
                  i === 0 ? "opacity-100" : "opacity-60",
                )}
              />
            ))}
          </div>
        ))}
      </div>
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
