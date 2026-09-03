"use client";

import { useRouter } from "next/navigation";
import { LogOut, UserRound } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { buttonVariants } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/utils";

const roleLabels = {
  ADMIN: "Admin",
  MAHASISWA: "Mahasiswa",
  DEVELOPER: "Developer",
} as const;

function initialsOf(name: string) {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("");
}

export function UserMenu() {
  const { user, logout } = useAuth();
  const router = useRouter();

  if (!user) return null;

  async function handleLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <DropdownMenu>
      {/*
        Pemicunya sengaja berupa <button> asli, bukan komponen Button kita.
        MenuTrigger memeriksa elemen yang diberikan lewat `render`, dan sebuah
        komponen bukan <button> di matanya — di build produksi pemeriksaan itu
        dilempar sebagai galat, dan seluruh halaman ikut mati.
      */}
      <DropdownMenuTrigger
        render={
          <button
            type="button"
            aria-label="Menu pengguna"
            className={cn(
              buttonVariants({ variant: "ghost", size: "icon" }),
              "size-9",
            )}
          >
            <Avatar className="size-7">
              <AvatarFallback className="text-[11px] font-semibold">
                {initialsOf(user.name)}
              </AvatarFallback>
            </Avatar>
          </button>
        }
      />
      <DropdownMenuContent side="right" align="end" className="w-56">
        {/*
          Label WAJIB berada di dalam Group. DropdownMenuLabel memakai
          Menu.GroupLabel, dan tanpa Group di atasnya Base UI melempar galat
          yang mematikan seluruh halaman begitu menunya dibuka.
        */}
        <DropdownMenuGroup>
          <DropdownMenuLabel>
            <span className="block text-sm font-medium">{user.name}</span>
            <span className="block text-xs font-normal text-muted-foreground">
              {user.email}
            </span>
            <span className="mt-1 inline-block rounded border border-border bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
              {roleLabels[user.role]}
            </span>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem>
            <UserRound />
            Profil saya
          </DropdownMenuItem>
          <DropdownMenuItem variant="destructive" onClick={handleLogout}>
            <LogOut />
            Keluar
          </DropdownMenuItem>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
