import { AppShell } from "@/components/shell/app-shell";
import { AuthGuard } from "@/components/shell/auth-guard";
import { GerbangForensik } from "@/components/shell/gerbang-forensik";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    // DEVELOPER ikut masuk kerangka ini karena halaman forensiknya ada di
    // dalamnya; GerbangForensik yang menahannya supaya tidak berkeliaran ke
    // halaman admin yang pasti menolaknya.
    <AuthGuard roles={["ADMIN", "DEVELOPER"]}>
      <GerbangForensik>
        <AppShell>{children}</AppShell>
      </GerbangForensik>
    </AuthGuard>
  );
}
