"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  BookOpen,
  FileCheck2,
  History,
  LogOut,
  ReceiptText,
  UserRound,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { useAuth } from "@/lib/auth";

const menu = [
  { href: "/portal", label: "Tagihan", icon: ReceiptText, exact: true },
  { href: "/portal/dokumen", label: "Dokumen", icon: FileCheck2 },
  { href: "/portal/disertasi", label: "Disertasi", icon: BookOpen },
  { href: "/portal/riwayat", label: "Riwayat", icon: History },
  { href: "/portal/profil", label: "Profil", icon: UserRound },
];

/**
 * Kerangka halaman mahasiswa. Sengaja dibuat berbeda dari panel admin:
 * navigasi mendatar dan tanpa rail, karena menunya sedikit dan sebagian besar
 * mahasiswa membukanya dari ponsel.
 */
export function PortalShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const router = useRouter();

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex w-full max-w-4xl items-center gap-3 px-4 py-3">
          <div className="flex size-9 items-center justify-center rounded-lg bg-primary font-heading text-sm font-bold text-primary-foreground">
            SP
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate font-heading text-sm font-semibold">
              {user?.name ?? "Mahasiswa"}
            </p>
            <p className="text-xs text-muted-foreground">Program Doktor PAI</p>
          </div>
          <ThemeToggle />
          <Button
            variant="ghost"
            size="icon"
            className="size-8"
            aria-label="Keluar"
            onClick={async () => {
              await logout();
              router.replace("/login");
            }}
          >
            <LogOut className="size-4" />
          </Button>
        </div>

        <nav
          aria-label="Menu mahasiswa"
          className="mx-auto flex w-full max-w-4xl gap-1 overflow-x-auto px-2"
        >
          {menu.map((item) => {
            const aktif = item.exact
              ? pathname === item.href
              : pathname.startsWith(item.href);
            const Icon = item.icon;

            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={aktif ? "page" : undefined}
                className={cn(
                  "flex items-center gap-1.5 border-b-2 px-3 py-2.5 text-sm whitespace-nowrap transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                  aktif
                    ? "border-primary font-medium text-foreground"
                    : "border-transparent text-muted-foreground hover:text-foreground",
                )}
              >
                <Icon className="size-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>
      </header>

      <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-6">{children}</main>
    </div>
  );
}
