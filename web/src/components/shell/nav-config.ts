import {
  BadgeCheck,
  FileBarChart,
  LayoutDashboard,
  School,
  Settings,
  Users,
  Wallet,
  type LucideIcon,
} from "lucide-react";

export type NavChild = {
  title: string;
  href: string;
  /** Keterangan kecil di bawah judul pada panel sub-navigasi. */
  hint?: string;
};

export type NavSection = {
  id: string;
  title: string;
  icon: LucideIcon;
  /** Section tanpa anak langsung menuju href ini. */
  href?: string;
  children?: NavChild[];
};

export const navSections: NavSection[] = [
  {
    id: "dashboard",
    title: "Dashboard",
    icon: LayoutDashboard,
    href: "/dashboard",
  },
  {
    id: "verifikasi",
    title: "Verifikasi Pembayaran",
    icon: BadgeCheck,
    children: [
      {
        title: "Semua kategori",
        href: "/verifikasi/semua",
        hint: "Seluruh bukti bayar",
      },
      {
        title: "Pendaftaran",
        href: "/verifikasi/pendaftaran",
        hint: "Rp 1 jt, sekali",
      },
      { title: "UKT", href: "/verifikasi/ukt", hint: "5 cicilan bulanan" },
      {
        title: "Seminar Proposal",
        href: "/verifikasi/seminar-proposal",
        hint: "Tahap 1",
      },
      {
        title: "Ujian Kelayakan",
        href: "/verifikasi/ujian-kelayakan",
        hint: "Tahap 2",
      },
      {
        title: "Ujian Tertutup",
        href: "/verifikasi/ujian-tertutup",
        hint: "Tahap 3",
      },
      {
        title: "Ujian Terbuka",
        href: "/verifikasi/ujian-terbuka",
        hint: "Tahap 4",
      },
    ],
  },
  {
    id: "mahasiswa",
    title: "Mahasiswa",
    icon: Users,
    children: [
      { title: "Semua Mahasiswa", href: "/mahasiswa" },
      { title: "Import Excel", href: "/mahasiswa/import" },
    ],
  },
  {
    id: "keuangan",
    title: "Tarif & Potongan",
    icon: Wallet,
    href: "/keuangan/tarif",
  },
  {
    id: "kelas",
    title: "Kelas",
    icon: School,
    href: "/kelas",
  },
  { id: "laporan", title: "Laporan", icon: FileBarChart, href: "/laporan" },
  {
    id: "pengaturan",
    title: "Pengaturan",
    icon: Settings,
    children: [
      { title: "OCR & Verifikasi", href: "/pengaturan/ocr" },
      { title: "Rekening & Notifikasi", href: "/pengaturan/sistem" },
    ],
  },
];

/** Section yang cocok dengan pathname saat ini. */
export function findActiveSection(pathname: string): NavSection | undefined {
  return navSections.find((section) => {
    if (section.href && pathname.startsWith(section.href)) return true;
    return section.children?.some((child) => pathname.startsWith(child.href));
  });
}

/** Judul halaman untuk top bar. */
export function findPageTitle(pathname: string): string {
  for (const section of navSections) {
    if (section.href && pathname.startsWith(section.href)) return section.title;
    const child = section.children?.find((c) => pathname.startsWith(c.href));
    if (child) return `${section.title} — ${child.title}`;
  }
  return "Sistem Pembayaran";
}
