"use client";

import { useRouter } from "next/navigation";
import { LogOut, UserRound } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "@/lib/auth";

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
      <DropdownMenuTrigger
        render={
          <Button
            variant="ghost"
            size="icon"
            className="size-9"
            aria-label="Menu pengguna"
          >
            <Avatar className="size-7">
              <AvatarFallback className="text-[11px] font-semibold">
                {initialsOf(user.name)}
              </AvatarFallback>
            </Avatar>
          </Button>
        }
      />
      <DropdownMenuContent side="right" align="end" className="w-56">
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
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
