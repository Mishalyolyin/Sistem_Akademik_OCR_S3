"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/lib/auth";

/**
 * Menahan DEVELOPER di halaman forensik.
 *
 * <p>Peran ini memakai kerangka yang sama dengan admin — sidebar, top bar, tema
 * — tapi seluruh endpoint admin menolaknya 403. Tanpa penjagaan ini ia bisa
 * membuka /mahasiswa dan mendapat halaman yang gagal memuat tanpa penjelasan,
 * padahal sebabnya bukan galat melainkan memang bukan haknya.
 *
 * <p>Penjagaan sesungguhnya tetap di backend; ini hanya supaya yang terlihat di
 * layar masuk akal.
 */
export function GerbangForensik({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  const menyimpang =
    user?.role === "DEVELOPER" && !pathname.startsWith("/forensik");

  useEffect(() => {
    if (menyimpang) router.replace("/forensik");
  }, [menyimpang, router]);

  if (menyimpang) {
    return (
      <div className="flex h-dvh items-center justify-center">
        <span className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          Mengalihkan ke forensik…
        </span>
      </div>
    );
  }

  return <>{children}</>;
}
