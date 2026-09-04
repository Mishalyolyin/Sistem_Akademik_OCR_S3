"use client";

import { useSyncExternalStore } from "react";
import { useTheme } from "next-themes";
import { Monitor, Moon, Sun } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

const order = ["light", "dark", "system"] as const;
const labels = {
  light: "Terang",
  dark: "Gelap",
  system: "Ikut sistem",
} as const;

/** Selalu palsu di server, benar di peramban. Tidak berlangganan apa pun. */
const tanpaLangganan = () => () => {};

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  // Tema baru diketahui setelah hidrasi; sebelum itu render placeholder
  // berukuran sama supaya layout tidak melompat.
  //
  // Memakai useSyncExternalStore, bukan state yang diisi di dalam effect:
  // mengisi state di effect memicu render berantai, dan React 19 memang
  // menyediakan kait ini untuk membedakan server dari peramban.
  const sudahDiHidrasi = useSyncExternalStore(
    tanpaLangganan,
    () => true,
    () => false,
  );

  if (!sudahDiHidrasi) {
    return <div className="size-8" aria-hidden />;
  }

  const current = (theme ?? "system") as (typeof order)[number];
  const next = order[(order.indexOf(current) + 1) % order.length];
  const Icon = current === "light" ? Sun : current === "dark" ? Moon : Monitor;

  return (
    <Tooltip>
      <TooltipTrigger
        render={
          <Button
            variant="ghost"
            size="icon"
            className="size-8"
            onClick={() => setTheme(next)}
            aria-label={`Tema: ${labels[current]}. Ganti ke ${labels[next]}.`}
          >
            <Icon className="size-4" />
          </Button>
        }
      />
      <TooltipContent side="bottom">Tema: {labels[current]}</TooltipContent>
    </Tooltip>
  );
}
