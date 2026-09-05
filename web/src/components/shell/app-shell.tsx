"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Search } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { ThemeToggle } from "@/components/theme-toggle";
import { UserMenu } from "@/components/shell/user-menu";
import {
  findActiveSection,
  findPageTitle,
  navUntukPeran,
  type NavSection,
} from "@/components/shell/nav-config";
import { berandaUntuk } from "@/components/shell/auth-guard";
import { useAuth } from "@/lib/auth";

function sectionHref(section: NavSection) {
  return section.href ?? section.children?.[0]?.href ?? "/dashboard";
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { user } = useAuth();
  const activeSection = findActiveSection(pathname);
  const subNav = activeSection?.children;

  // Menu disaring per peran: DEVELOPER hanya punya forensik, dan menu admin
  // yang tetap terlihat olehnya hanya akan mengantar ke halaman yang menolaknya.
  const menu = navUntukPeran(user?.role);
  // Logo mengantar ke beranda perannya sendiri; /dashboard menolak DEVELOPER.
  const beranda = user ? berandaUntuk(user.role) : "/dashboard";

  return (
    <div className="flex h-dvh overflow-hidden">
      {/* Rail ikon — menggantikan sidebar lebar. */}
      <nav
        aria-label="Navigasi utama"
        className="flex w-14 shrink-0 flex-col items-center gap-1 border-r border-sidebar-border bg-sidebar py-3"
      >
        <Link
          href={beranda}
          className="mb-2 flex size-9 items-center justify-center rounded-lg bg-primary font-heading text-sm font-bold text-primary-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
          aria-label="Beranda"
        >
          SP
        </Link>

        {menu.map((section) => {
          const Icon = section.icon;
          const isActive = activeSection?.id === section.id;
          return (
            <Tooltip key={section.id}>
              <TooltipTrigger
                render={
                  <Link
                    href={sectionHref(section)}
                    // Isinya cuma ikon, jadi tanpa label ini seluruh menu utama
                    // terbaca sebagai "link" tanpa nama oleh pembaca layar.
                    // Tooltip tidak menggantikannya: ia hanya muncul saat
                    // disorot tetikus.
                    aria-label={section.title}
                    aria-current={isActive ? "page" : undefined}
                    className={cn(
                      "relative flex size-9 items-center justify-center rounded-lg transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                      isActive
                        ? "bg-sidebar-accent text-sidebar-accent-foreground"
                        : "text-muted-foreground hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground",
                    )}
                  >
                    {/* Penanda aktif di tepi rail, bukan sekadar warna latar. */}
                    <span
                      className={cn(
                        "absolute -left-3 h-5 w-0.5 rounded-r-full bg-primary transition-opacity",
                        isActive ? "opacity-100" : "opacity-0",
                      )}
                      aria-hidden
                    />
                    <Icon className="size-[18px]" />
                  </Link>
                }
              />
              <TooltipContent side="right">{section.title}</TooltipContent>
            </Tooltip>
          );
        })}

        <div className="mt-auto flex flex-col items-center gap-1">
          <ThemeToggle />
          <UserMenu />
        </div>
      </nav>

      {/* Panel sub-navigasi, hanya muncul untuk section yang punya anak. */}
      {subNav && (
        <aside
          aria-label={activeSection.title}
          className="hidden w-56 shrink-0 flex-col border-r border-sidebar-border bg-sidebar/40 md:flex"
        >
          <div className="px-4 py-4">
            <p className="font-heading text-sm font-semibold">
              {activeSection.title}
            </p>
          </div>
          <Separator />
          <div className="flex flex-1 flex-col gap-0.5 overflow-y-auto p-2">
            {subNav.map((child) => {
              const isActive = pathname.startsWith(child.href);
              return (
                <Link
                  key={child.href}
                  href={child.href}
                  aria-current={isActive ? "page" : undefined}
                  className={cn(
                    "rounded-md px-2.5 py-2 text-sm transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                    isActive
                      ? "bg-sidebar-accent font-medium text-sidebar-accent-foreground"
                      : "text-muted-foreground hover:bg-sidebar-accent/50 hover:text-foreground",
                  )}
                >
                  <span className="block">{child.title}</span>
                  {child.hint && (
                    <span className="block text-xs text-muted-foreground/80">
                      {child.hint}
                    </span>
                  )}
                </Link>
              );
            })}
          </div>
        </aside>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 shrink-0 items-center gap-3 border-b border-border px-5">
          <h1 className="truncate font-heading text-sm font-semibold">
            {findPageTitle(pathname)}
          </h1>
          <Button
            variant="outline"
            size="sm"
            className="ml-auto gap-2 text-muted-foreground"
            disabled
          >
            <Search className="size-3.5" />
            Cari
            <kbd className="rounded border border-border bg-muted px-1 font-mono text-[10px]">
              Ctrl K
            </kbd>
          </Button>
        </header>

        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
