"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { useAuth, type UserRole } from "@/lib/auth";

/** Halaman awal masing-masing peran setelah masuk. */
export function berandaUntuk(role: UserRole): string {
  switch (role) {
    case "MAHASISWA":
      return "/portal";
    // DEVELOPER tidak punya dashboard: seluruh endpoint admin menolaknya.
    // Sebelum ini ia diarahkan ke /dashboard yang justru menolaknya, lalu
    // dilempar ke sana lagi — berputar di tempat, dan yang terlihat di layar
    // hanya "Memeriksa sesi…" selamanya.
    case "DEVELOPER":
      return "/forensik";
    default:
      return "/dashboard";
  }
}

/**
 * Menahan halaman sampai sesi diketahui, lalu memastikan perannya sesuai.
 * Setara middleware `auth` dan `role` di Laravel, tapi berjalan di sisi klien
 * karena access token disimpan di memori.
 *
 * <p>Ini hanya pengalaman pemakaian, bukan pengamanan: backend tetap menolak
 * permintaan yang perannya tidak cocok dengan 403.
 */
export function AuthGuard({
  children,
  role,
  roles,
}: {
  children: React.ReactNode;
  /** Satu peran yang diizinkan. */
  role?: UserRole;
  /** Beberapa peran sekaligus, untuk halaman yang dipakai lebih dari satu peran. */
  roles?: UserRole[];
}) {
  const { status, user } = useAuth();
  const router = useRouter();

  const diizinkan = roles ?? (role ? [role] : null);
  const salahPeran = Boolean(
    diizinkan && user && !diizinkan.includes(user.role),
  );

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/login");
    } else if (salahPeran && user) {
      router.replace(berandaUntuk(user.role));
    }
  }, [status, salahPeran, user, router]);

  if (status !== "authenticated" || salahPeran) {
    return (
      <div className="flex h-dvh items-center justify-center">
        <span className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          Memeriksa sesi…
        </span>
      </div>
    );
  }

  return <>{children}</>;
}
