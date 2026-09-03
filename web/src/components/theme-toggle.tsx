"use client";

import { useEffect, useState } from "react";
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

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  // Tema baru diketahui setelah hydrate; sebelum itu render placeholder
  // berukuran sama supaya layout tidak melompat.
  useEffect(() => setMounted(true), []);

  if (!mounted) {
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
