import type { LucideIcon } from "lucide-react";
import type { NavSection } from "@/components/shell/nav-config";

export type GrupPalet = "Halaman" | "Mahasiswa" | "Tindakan";

export type ItemPalet = {
  id: string;
  grup: GrupPalet;
  label: string;
  /** Baris kedua: konteks yang membedakan dua item berlabel mirip. */
  keterangan?: string;
  icon: LucideIcon;
  /**
   * Kata tambahan yang ikut dicocokkan tapi tidak ditampilkan — supaya
   * "logout" menemukan "Keluar" dan "dark" menemukan "Gelap".
   */
  alias?: string;
  jalankan: () => void;
};

/**
 * Item halaman untuk palet, diturunkan dari nav-config dan bukan didaftar ulang.
 *
 * <p>Menu yang ditambahkan nanti otomatis ikut tercari, dan penyaringan
 * perannya memakai daftar yang sama persis dengan rail ikon — palet tidak boleh
 * mengantar seseorang ke halaman yang endpointnya justru menolaknya.
 */
export function itemHalaman(
  menu: NavSection[],
  pergi: (href: string) => void,
): ItemPalet[] {
  return menu.flatMap((section) => {
    if (!section.children) {
      const href = section.href;
      if (!href) return [];
      return [
        {
          id: `nav:${section.id}`,
          grup: "Halaman" as const,
          label: section.title,
          icon: section.icon,
          jalankan: () => pergi(href),
        },
      ];
    }

    return section.children.map((child) => ({
      id: `nav:${section.id}:${child.href}`,
      grup: "Halaman" as const,
      label: child.title,
      keterangan: child.hint
        ? `${section.title} · ${child.hint}`
        : section.title,
      icon: section.icon,
      // Judul sectionnya ikut dicocokkan: mengetik "verifikasi ukt" harus
      // menemukan anak bernama "UKT" yang judulnya sendiri tidak memuat kata itu.
      alias: section.title,
      jalankan: () => pergi(child.href),
    }));
  });
}

/** Memecah ketikan jadi kata-kata pencarian. */
export function kataKunci(ketikan: string): string[] {
  return ketikan.trim().toLowerCase().split(/\s+/).filter(Boolean);
}

/**
 * Cocok bila SEMUA kata yang diketik muncul di teks item, urutannya bebas.
 *
 * <p>Sengaja bukan pencocokan fuzzy per huruf: "ui" tidak boleh menyeret
 * seluruh daftar hanya karena tiap label kebetulan punya huruf u dan i.
 */
export function cocok(item: ItemPalet, kata: string[]): boolean {
  const jerami =
    `${item.label} ${item.keterangan ?? ""} ${item.alias ?? ""}`.toLowerCase();
  return kata.every((k) => jerami.includes(k));
}
