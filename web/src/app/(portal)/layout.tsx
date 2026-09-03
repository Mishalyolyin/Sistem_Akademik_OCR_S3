import { AuthGuard } from "@/components/shell/auth-guard";
import { PortalShell } from "@/components/shell/portal-shell";

export default function PortalLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthGuard role="MAHASISWA">
      <PortalShell>{children}</PortalShell>
    </AuthGuard>
  );
}
