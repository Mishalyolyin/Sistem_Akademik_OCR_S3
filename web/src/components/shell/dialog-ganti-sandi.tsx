"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { Info, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError, apiFetch } from "@/lib/api";
import { useAuth } from "@/lib/auth";

const PANJANG_MINIMAL = 8;

/**
 * Ganti kata sandi sendiri, berlaku untuk peran apa pun.
 *
 * <p>Mengganti kata sandi mencabut seluruh sesi di server, termasuk sesi yang
 * sedang dipakai. Daripada membiarkan pengguna terlempar keluar sendiri
 * beberapa menit kemudian tanpa penjelasan, di sini ia langsung diantar ke
 * halaman masuk.
 */
export function DialogGantiSandi({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { logout } = useAuth();
  const [lama, setLama] = useState("");
  const [baru, setBaru] = useState("");
  const [ulangi, setUlangi] = useState("");

  const ganti = useMutation({
    mutationFn: (body: { lama: string; baru: string }) =>
      apiFetch<void>("/auth/kata-sandi", { method: "POST", body }),
  });

  const terlaluPendek = baru.length > 0 && baru.length < PANJANG_MINIMAL;
  const tidakCocok = ulangi.length > 0 && baru !== ulangi;
  const samaDenganLama = baru.length > 0 && baru === lama;
  const bolehSimpan =
    lama.length > 0 &&
    baru.length >= PANJANG_MINIMAL &&
    baru === ulangi &&
    !samaDenganLama;

  function tutup() {
    setLama("");
    setBaru("");
    setUlangi("");
    onOpenChange(false);
  }

  function simpan() {
    ganti.mutate(
      { lama, baru },
      {
        onSuccess: async () => {
          toast.success("Kata sandi diganti. Masuk lagi dengan yang baru.");
          tutup();
          await logout();
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal mengganti kata sandi.",
          ),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={(next) => (next ? onOpenChange(true) : tutup())}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Ganti kata sandi</DialogTitle>
          <DialogDescription>
            Minimal {PANJANG_MINIMAL} karakter. Setelah diganti, semua perangkat
            yang sedang masuk akan dikeluarkan.
          </DialogDescription>
        </DialogHeader>

        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (bolehSimpan) simpan();
          }}
        >
          {/* Nama pengguna disertakan supaya pengelola kata sandi peramban
              tahu akun mana yang sedang diperbarui. */}
          <input type="text" autoComplete="username" hidden readOnly value="" />

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="sandi-lama">Kata sandi sekarang</Label>
            <Input
              id="sandi-lama"
              type="password"
              autoComplete="current-password"
              value={lama}
              onChange={(event) => setLama(event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="sandi-baru">Kata sandi baru</Label>
            <Input
              id="sandi-baru"
              type="password"
              autoComplete="new-password"
              aria-invalid={terlaluPendek || samaDenganLama}
              value={baru}
              onChange={(event) => setBaru(event.target.value)}
            />
            {terlaluPendek && (
              <p className="text-xs text-danger">
                Minimal {PANJANG_MINIMAL} karakter.
              </p>
            )}
            {samaDenganLama && (
              <p className="text-xs text-danger">
                Sama dengan kata sandi sekarang. Pilih yang lain.
              </p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="sandi-ulangi">Ulangi kata sandi baru</Label>
            <Input
              id="sandi-ulangi"
              type="password"
              autoComplete="new-password"
              aria-invalid={tidakCocok}
              value={ulangi}
              onChange={(event) => setUlangi(event.target.value)}
            />
            {tidakCocok && (
              <p className="text-xs text-danger">Belum sama dengan yang di atas.</p>
            )}
          </div>

          <p className="flex gap-2 rounded-md border border-border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
            <Info className="size-4 shrink-0" />
            <span>
              Anda akan diminta masuk lagi setelah kata sandi berganti.
            </span>
          </p>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={tutup}>
              Batal
            </Button>
            <Button type="submit" disabled={!bolehSimpan || ganti.isPending}>
              {ganti.isPending && <Loader2 className="animate-spin" />}
              Ganti kata sandi
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
