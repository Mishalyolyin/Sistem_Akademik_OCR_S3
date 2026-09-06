import Link from "next/link";
import { cn } from "@/lib/utils";
import type { LucideIcon } from "lucide-react";

type Tone = "default" | "primary" | "warning" | "success" | "danger" | "info";

/**
 * Warna per nada, dipisah jadi tabel supaya seluruh kartu memakai resep yang
 * sama: ikon berlatar lembut, angka berwarna, dan cahaya tipis di sudut yang
 * baru muncul saat disorot.
 *
 * <p>Kelasnya ditulis lengkap, bukan dirangkai dari potongan string. Tailwind
 * memindai kode sebagai teks; kelas yang dirangkai saat berjalan
 * (`bg-${nada}/10`) tidak pernah ikut terbangun dan hasilnya kartu tanpa warna
 * sama sekali.
 */
const nada: Record<
  Tone,
  { ikon: string; angka: string; cahaya: string; garis: string }
> = {
  default: {
    ikon: "bg-muted text-muted-foreground",
    angka: "text-foreground",
    cahaya: "from-foreground/[0.04]",
    garis: "group-hover:border-border",
  },
  primary: {
    ikon: "bg-primary/10 text-primary",
    angka: "text-foreground",
    cahaya: "from-primary/15",
    garis: "group-hover:border-primary/30",
  },
  warning: {
    ikon: "bg-warning/12 text-warning",
    angka: "text-warning",
    cahaya: "from-warning/15",
    garis: "group-hover:border-warning/30",
  },
  success: {
    ikon: "bg-success/12 text-success",
    angka: "text-success",
    cahaya: "from-success/15",
    garis: "group-hover:border-success/30",
  },
  danger: {
    ikon: "bg-danger/12 text-danger",
    angka: "text-danger",
    cahaya: "from-danger/15",
    garis: "group-hover:border-danger/30",
  },
  info: {
    ikon: "bg-info/12 text-info",
    angka: "text-info",
    cahaya: "from-info/15",
    garis: "group-hover:border-info/30",
  },
};

/**
 * Satu angka ringkasan, berdiri sebagai kartunya sendiri.
 *
 * <p>Sebelumnya keempatnya disatukan dalam satu kotak bergaris rambut. Bentuk
 * itu membuat empat angka yang tidak berhubungan terbaca sebagai satu baris
 * tabel — mata membacanya menyamping seperti spreadsheet, bukan menimbang tiap
 * angka sendiri-sendiri.
 */
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
  tone?: Tone;
  className?: string;
  /** Bila diisi, seluruh kartu jadi tautan ke daftar yang sudah tersaring. */
  href?: string;
}) {
  const t = nada[tone];

  const isi = (
    <>
      {/* Cahaya sudut: hanya terasa saat disorot, jadi kartu yang bisa diklik
          memberi tanda tanpa perlu ikon panah tambahan. */}
      <span
        aria-hidden
        className={cn(
          "pointer-events-none absolute -top-16 -right-16 size-32 rounded-full bg-gradient-to-br to-transparent opacity-0 blur-2xl transition-opacity duration-300",
          t.cahaya,
          href && "group-hover:opacity-100",
        )}
      />

      <div className="relative flex items-center gap-2">
        {Icon && (
          <span
            className={cn(
              "flex size-7 items-center justify-center rounded-lg transition-transform duration-300 group-hover:scale-110",
              t.ikon,
            )}
          >
            <Icon className="size-4" />
          </span>
        )}
        <span className="text-xs font-medium text-muted-foreground">
          {label}
        </span>
      </div>

      <div
        className={cn(
          "relative mt-3 font-heading text-3xl leading-none font-bold tracking-tight tnum",
          t.angka,
        )}
      >
        {value}
      </div>

      {sublabel && (
        <div className="relative mt-1.5 text-xs text-muted-foreground">
          {sublabel}
        </div>
      )}
    </>
  );

  const kelas = cn(
    "group relative overflow-hidden rounded-2xl border border-border/70 bg-card px-5 py-4 shadow-sm transition-all duration-300",
    href &&
      "hover:-translate-y-0.5 hover:shadow-lg hover:shadow-foreground/[0.06] focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
    href && t.garis,
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

/** Deretan angka ringkasan. Tiap angka kartunya sendiri, tidak lagi disatukan. */
export function StatRow({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
      {children}
    </div>
  );
}
