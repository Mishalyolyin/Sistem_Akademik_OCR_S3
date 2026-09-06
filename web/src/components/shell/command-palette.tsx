"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useTheme } from "next-themes";
import {
  CornerDownLeft,
  LogOut,
  Monitor,
  Moon,
  Search,
  Sun,
  User,
} from "lucide-react";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { navUntukPeran } from "@/components/shell/nav-config";
import {
  cocok,
  itemHalaman,
  kataKunci,
  type GrupPalet,
  type ItemPalet,
} from "@/components/shell/palet-pencocokan";
import { apiFetch, type Page } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { StudentSummary } from "@/features/mahasiswa/api";
import { cn } from "@/lib/utils";

/** Panjang ketikan minimal sebelum mencari mahasiswa ke server. */
const MIN_CARI_MAHASISWA = 2;
const JEDA_KETIK_MS = 250;
const MAKS_MAHASISWA = 6;

/**
 * Pencarian cepat ala command palette: Ctrl+K dari mana saja di shell admin.
 *
 * <p>Isinya sengaja dipisah ke komponen tersendiri yang hanya dirender saat
 * palet terbuka. Dengan begitu ketikan dan sorotan sesi sebelumnya hilang
 * karena komponennya memang mati, bukan karena ada effect yang membersihkannya
 * — dan tidak ada satu pun setState di dalam effect.
 */
export function CommandPalette({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key.toLowerCase() === "k" && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        onOpenChange(!open);
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, onOpenChange]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        showCloseButton={false}
        className="top-[15%] max-w-[calc(100%-2rem)] translate-y-0 gap-0 p-0 sm:max-w-xl"
      >
        <DialogTitle className="sr-only">Pencarian cepat</DialogTitle>
        {open && <IsiPalet onTutup={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  );
}

function IsiPalet({ onTutup }: { onTutup: () => void }) {
  const router = useRouter();
  const { user, logout } = useAuth();
  const { setTheme } = useTheme();

  const [ketikan, setKetikan] = useState("");
  const [tertunda, setTertunda] = useState("");
  const [aktif, setAktif] = useState(0);
  const daftarRef = useRef<HTMLDivElement>(null);

  // Menahan ketikan sejenak sebelum memanggil server: mengetik satu nama
  // seharusnya jadi satu permintaan, bukan satu per huruf.
  useEffect(() => {
    const id = setTimeout(() => setTertunda(ketikan.trim()), JEDA_KETIK_MS);
    return () => clearTimeout(id);
  }, [ketikan]);

  // Hanya ADMIN yang boleh membaca daftar mahasiswa; untuk DEVELOPER
  // permintaan ini pasti dijawab 403, jadi tidak usah dikirim sama sekali.
  const bolehCariMahasiswa = user?.role === "ADMIN";

  const mahasiswa = useQuery({
    queryKey: ["palet-mahasiswa", tertunda],
    queryFn: () =>
      apiFetch<Page<StudentSummary>>("/students", {
        params: { search: tertunda, page: 0, size: MAKS_MAHASISWA },
      }),
    enabled: bolehCariMahasiswa && tertunda.length >= MIN_CARI_MAHASISWA,
    // Nama yang sama dicari berkali-kali dalam satu sesi; jawabannya tidak
    // perlu diambil ulang tiap palet dibuka.
    staleTime: 60_000,
  });

  const items = useMemo(() => {
    const tutupLalu = (aksi: () => void) => () => {
      onTutup();
      aksi();
    };

    const halaman = itemHalaman(navUntukPeran(user?.role), (href) => {
      onTutup();
      router.push(href);
    });

    const tindakan: ItemPalet[] = [
      {
        id: "aksi:tema-terang",
        grup: "Tindakan",
        label: "Tema terang",
        icon: Sun,
        alias: "light mode warna",
        jalankan: tutupLalu(() => setTheme("light")),
      },
      {
        id: "aksi:tema-gelap",
        grup: "Tindakan",
        label: "Tema gelap",
        icon: Moon,
        alias: "dark mode warna",
        jalankan: tutupLalu(() => setTheme("dark")),
      },
      {
        id: "aksi:tema-sistem",
        grup: "Tindakan",
        label: "Tema ikut sistem",
        icon: Monitor,
        alias: "auto warna",
        jalankan: tutupLalu(() => setTheme("system")),
      },
      {
        id: "aksi:keluar",
        grup: "Tindakan",
        label: "Keluar",
        icon: LogOut,
        alias: "logout sign out",
        jalankan: tutupLalu(async () => {
          await logout();
          router.replace("/login");
        }),
      },
    ];

    const kata = kataKunci(ketikan);
    const tersaring = kata.length
      ? [...halaman, ...tindakan].filter((item) => cocok(item, kata))
      : // Tanpa ketikan, tindakan disembunyikan: yang dicari orang saat menekan
        // Ctrl+K hampir selalu sebuah halaman, dan "Keluar" tidak layak duduk
        // di daftar pembuka.
        halaman;

    const hasilMahasiswa: ItemPalet[] = (mahasiswa.data?.content ?? []).map(
      (s) => ({
        id: `mahasiswa:${s.id}`,
        grup: "Mahasiswa",
        label: s.name,
        keterangan: [s.nim, s.className ?? "Tanpa kelas"].join(" · "),
        icon: User,
        jalankan: tutupLalu(() => router.push(`/mahasiswa/${s.id}`)),
      }),
    );

    // Mahasiswa didahulukan: begitu seseorang mengetik nama, yang dia tuju
    // adalah orangnya, bukan menu yang kebetulan mengandung potongan kata itu.
    return [...hasilMahasiswa, ...tersaring];
  }, [user?.role, ketikan, mahasiswa.data, router, onTutup, setTheme, logout]);

  // Daftarnya berubah isi, jadi sorotan harus kembali ke atas — kalau tidak,
  // indeks lama menunjuk item yang sama sekali lain dan Enter membuka yang
  // tidak diminta. Penyesuaian dilakukan saat render, bukan di effect: ini
  // memang pola yang disarankan React untuk state turunan.
  const kunci = items.map((i) => i.id).join("|");
  const [kunciTersorot, setKunciTersorot] = useState(kunci);
  if (kunciTersorot !== kunci) {
    setKunciTersorot(kunci);
    setAktif(0);
  }

  useEffect(() => {
    daftarRef.current
      ?.querySelector('[data-aktif="true"]')
      ?.scrollIntoView({ block: "nearest" });
  }, [aktif, kunci]);

  function onKeyDown(e: React.KeyboardEvent) {
    if (items.length === 0) return;

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setAktif((i) => (i + 1) % items.length);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setAktif((i) => (i - 1 + items.length) % items.length);
    } else if (e.key === "Home") {
      e.preventDefault();
      setAktif(0);
    } else if (e.key === "End") {
      e.preventDefault();
      setAktif(items.length - 1);
    } else if (e.key === "Enter") {
      e.preventDefault();
      items[aktif]?.jalankan();
    }
  }

  // Judul grup hanya dicetak saat grupnya berganti, jadi daftarnya tetap satu
  // urutan datar — indeks sorotan dan urutan tampil tidak mungkin berbeda.
  let grupTerakhir: GrupPalet | null = null;

  return (
    <>
      <div className="flex items-center gap-2 border-b border-border px-3">
        <Search className="size-4 shrink-0 text-muted-foreground" aria-hidden />
        <input
          autoFocus
          type="text"
          role="combobox"
          aria-expanded
          aria-controls="palet-daftar"
          aria-activedescendant={items[aktif]?.id}
          aria-autocomplete="list"
          aria-label="Cari halaman, mahasiswa, atau tindakan"
          placeholder={
            bolehCariMahasiswa
              ? "Cari halaman, mahasiswa, atau tindakan…"
              : "Cari halaman atau tindakan…"
          }
          value={ketikan}
          onChange={(e) => setKetikan(e.target.value)}
          onKeyDown={onKeyDown}
          className="h-11 w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
        />
        {mahasiswa.isFetching && (
          <span className="shrink-0 text-xs text-muted-foreground">
            mencari…
          </span>
        )}
      </div>

      <div
        ref={daftarRef}
        id="palet-daftar"
        role="listbox"
        aria-label="Hasil"
        className="max-h-80 overflow-y-auto p-1.5"
      >
        {items.length === 0 && (
          <p className="px-2 py-6 text-center text-sm text-muted-foreground">
            Tidak ada yang cocok dengan &ldquo;{ketikan}&rdquo;.
          </p>
        )}

        {items.map((item, i) => {
          const Icon = item.icon;
          const judulGrup = item.grup !== grupTerakhir ? item.grup : null;
          grupTerakhir = item.grup;

          return (
            <div key={item.id}>
              {judulGrup && (
                <p className="px-2 pt-2 pb-1 text-xs font-medium text-muted-foreground">
                  {judulGrup}
                </p>
              )}
              <div
                id={item.id}
                role="option"
                aria-selected={i === aktif}
                data-aktif={i === aktif}
                // Sorotan mengikuti tetikus supaya klik selalu mengenai item
                // yang tampak tersorot, bukan yang terakhir disorot keyboard.
                onMouseMove={() => setAktif(i)}
                onClick={item.jalankan}
                className={cn(
                  "flex cursor-pointer items-center gap-2.5 rounded-md px-2 py-2 text-sm",
                  i === aktif && "bg-accent text-accent-foreground",
                )}
              >
                <Icon
                  className="size-4 shrink-0 text-muted-foreground"
                  aria-hidden
                />
                <span className="min-w-0 flex-1">
                  <span className="block truncate">{item.label}</span>
                  {item.keterangan && (
                    <span className="block truncate text-xs text-muted-foreground">
                      {item.keterangan}
                    </span>
                  )}
                </span>
                {i === aktif && (
                  <CornerDownLeft
                    className="size-3.5 shrink-0 text-muted-foreground"
                    aria-hidden
                  />
                )}
              </div>
            </div>
          );
        })}

        {bolehCariMahasiswa &&
          ketikan.trim().length > 0 &&
          ketikan.trim().length < MIN_CARI_MAHASISWA && (
            <p className="px-2 py-2 text-xs text-muted-foreground">
              Ketik minimal {MIN_CARI_MAHASISWA} huruf untuk mencari mahasiswa.
            </p>
          )}
      </div>

      <div className="flex items-center gap-3 border-t border-border px-3 py-2 text-xs text-muted-foreground">
        <span>
          <kbd className="rounded border border-border bg-muted px-1 font-mono">
            ↑
          </kbd>{" "}
          <kbd className="rounded border border-border bg-muted px-1 font-mono">
            ↓
          </kbd>{" "}
          pilih
        </span>
        <span>
          <kbd className="rounded border border-border bg-muted px-1 font-mono">
            Enter
          </kbd>{" "}
          buka
        </span>
        <span>
          <kbd className="rounded border border-border bg-muted px-1 font-mono">
            Esc
          </kbd>{" "}
          tutup
        </span>
      </div>
    </>
  );
}
