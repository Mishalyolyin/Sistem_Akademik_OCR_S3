const rupiah = new Intl.NumberFormat("id-ID", {
  style: "currency",
  currency: "IDR",
  maximumFractionDigits: 0,
});

const rupiahCompact = new Intl.NumberFormat("id-ID", {
  notation: "compact",
  compactDisplay: "short",
  maximumFractionDigits: 1,
});

const tanggal = new Intl.DateTimeFormat("id-ID", {
  day: "numeric",
  month: "short",
  year: "numeric",
});

const tanggalJam = new Intl.DateTimeFormat("id-ID", {
  day: "numeric",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

/** Rp 2.500.000 */
export function formatRupiah(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === "") return "—";
  const n = typeof value === "string" ? Number(value) : value;
  if (Number.isNaN(n)) return "—";
  return rupiah.format(n);
}

/** Rp 2,5 jt — untuk kartu ringkasan yang sempit. */
export function formatRupiahSingkat(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  return `Rp ${rupiahCompact.format(value)}`;
}

/** 3 Sep 2026 */
export function formatTanggal(value: string | Date | null | undefined): string {
  if (!value) return "—";
  const d = typeof value === "string" ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return "—";
  return tanggal.format(d);
}

/** 3 Sep 2026, 09.41 */
export function formatTanggalJam(
  value: string | Date | null | undefined,
): string {
  if (!value) return "—";
  const d = typeof value === "string" ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return "—";
  return tanggalJam.format(d);
}

/** 2024/2025 GASAL -> "2024/2025 Gasal" */
export function formatSemester(academicYear: string, term: string): string {
  const t = term.charAt(0).toUpperCase() + term.slice(1).toLowerCase();
  return `${academicYear} ${t}`;
}
