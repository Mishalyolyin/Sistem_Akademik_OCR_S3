"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { apiFetch, setAccessToken } from "@/lib/api";

export type UserRole = "ADMIN" | "MAHASISWA" | "DEVELOPER";

export type CurrentUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
};

type TokenResponse = {
  accessToken: string;
  expiresInSeconds: number;
  user: CurrentUser;
};

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

type AuthContextValue = {
  user: CurrentUser | null;
  status: AuthStatus;
  login: (email: string, password: string) => Promise<CurrentUser>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");
  const refreshTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  /**
   * Perpanjangan token dijadwalkan oleh {@code applyTokens}, sementara
   * perpanjangan itu sendiri memanggil {@code applyTokens} lagi setelah
   * berhasil — keduanya saling membutuhkan. Ref memutus lingkarannya: penjadwal
   * memanggil lewat sini, jadi ia tidak perlu menyebut fungsi yang belum
   * dideklarasikan, dan tetap memakai versi terbaru saat waktunya tiba.
   */
  const renewRef = useRef<() => Promise<void>>(async () => {});

  const clearTimer = useCallback(() => {
    if (refreshTimer.current) {
      clearTimeout(refreshTimer.current);
      refreshTimer.current = null;
    }
  }, []);

  /** Simpan token, lalu jadwalkan perpanjangan 1 menit sebelum kedaluwarsa. */
  const applyTokens = useCallback(
    (tokens: TokenResponse) => {
      setAccessToken(tokens.accessToken);
      setUser(tokens.user);
      setStatus("authenticated");

      clearTimer();
      const delayMs = Math.max(tokens.expiresInSeconds - 60, 30) * 1000;
      refreshTimer.current = setTimeout(() => {
        void renewRef.current();
      }, delayMs);
    },
    [clearTimer],
  );

  const signOutLocally = useCallback(() => {
    clearTimer();
    setAccessToken(null);
    setUser(null);
    setStatus("unauthenticated");
  }, [clearTimer]);

  /** Tukar cookie refresh (HttpOnly) dengan access token baru. */
  const renew = useCallback(async () => {
    try {
      const tokens = await apiFetch<TokenResponse>("/auth/refresh", {
        method: "POST",
      });
      applyTokens(tokens);
    } catch {
      signOutLocally();
    }
  }, [applyTokens, signOutLocally]);

  useEffect(() => {
    renewRef.current = renew;
  }, [renew]);

  // Saat halaman dimuat ulang, access token di memori hilang — pulihkan dari
  // cookie. Ini justru pemakaian effect yang tepat: menyelaraskan React dengan
  // sistem di luarnya, yaitu cookie HttpOnly yang hanya dipegang peramban.
  // State baru berubah setelah permintaan jaringan selesai, bukan seketika,
  // jadi tidak ada render berantai — pemeriksa tidak bisa memastikan itu.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void renew();
    return clearTimer;
    // Sengaja hanya sekali saat provider terpasang: menjalankannya ulang setiap
    // `renew` berubah akan memanggil endpoint refresh berkali-kali.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const tokens = await apiFetch<TokenResponse>("/auth/login", {
        method: "POST",
        body: { email, password },
      });
      applyTokens(tokens);
      return tokens.user;
    },
    [applyTokens],
  );

  const logout = useCallback(async () => {
    try {
      await apiFetch<void>("/auth/logout", { method: "POST" });
    } finally {
      signOutLocally();
    }
  }, [signOutLocally]);

  const value = useMemo(
    () => ({ user, status, login, logout }),
    [user, status, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth harus dipakai di dalam <AuthProvider>.");
  }
  return context;
}
