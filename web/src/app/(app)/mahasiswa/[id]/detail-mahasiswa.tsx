"use client";

import { useState } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  Ban,
  KeyRound,
  PencilLine,
  Plus,
  Scale,
  Trash2,
  Wallet,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { InstallmentStatusBadge } from "@/components/status-badge";
import { StatRow, StatTile } from "@/components/stat-tile";
import {
  EmptyState,
  ErrorState,
  PageHeader,
  TableSkeleton,
} from "@/components/page-header";
import { documentStepLabels, useStudent } from "@/features/mahasiswa/api";
import { useGolongan } from "@/features/tarif/api";
import {
  useStudentPlans,
  type Installment,
  type PaymentPlan,
} from "@/features/tagihan/api";
import { DialogBuatTagihan } from "@/features/tagihan/dialog-buat-tagihan";
import { DialogUbahNominal } from "@/features/tagihan/dialog-ubah-nominal";
import { DialogBatalTagihan } from "@/features/tagihan/dialog-batal-tagihan";
import { DialogPenyesuaian } from "@/features/penyesuaian/dialog-penyesuaian";
import { DialogResetSandi } from "@/features/mahasiswa/dialog-reset-sandi";
import { DialogUbahMahasiswa } from "@/features/mahasiswa/dialog-ubah-mahasiswa";
import { DialogHapusMahasiswa } from "@/features/mahasiswa/dialog-hapus-mahasiswa";
import { KartuDokumen } from "@/features/mahasiswa/kartu-dokumen";
import { ApiError } from "@/lib/api";
import { formatRupiah, formatTanggal, formatTanggalJam } from "@/lib/format";

export function DetailMahasiswa({ studentId }: { studentId: number }) {
  const mahasiswa = useStudent(studentId);
  const plans = useStudentPlans(studentId);
  const golongan = useGolongan();

  const [buatTerbuka, setBuatTerbuka] = useState(false);
  const [penyesuaianTerbuka, setPenyesuaianTerbuka] = useState(false);
  const [resetSandiTerbuka, setResetSandiTerbuka] = useState(false);
  const [ubahTerbuka, setUbahTerbuka] = useState(false);
  const [hapusTerbuka, setHapusTerbuka] = useState(false);
  const [cicilanDiubah, setCicilanDiubah] = useState<Installment | null>(null);
  const [tagihanDibatalkan, setTagihanDibatalkan] = useState<PaymentPlan | null>(
    null,
  );

  if (mahasiswa.isPending) {
    return (
      <div className="p-6">
        <TableSkeleton rows={4} />
      </div>
    );
  }

  if (mahasiswa.error) {
    return (
      <div className="p-6">
        <ErrorState
          message={
            mahasiswa.error instanceof ApiError
              ? mahasiswa.error.message
              : "Mahasiswa tidak ditemukan."
          }
        />
      </div>
    );
  }

  const mhs = mahasiswa.data;
  const tier = {
    label: golongan.label(mhs.discountTier),
    persen: golongan.persen(mhs.discountTier),
  };

  const totalDitagih = (plans.data ?? []).reduce(
    (sum, plan) => sum + Number(plan.totalAmount),
    0,
  );
  const totalDibayar = (plans.data ?? []).reduce(
    (sum, plan) => sum + Number(plan.amountPaid),
    0,
  );

  return (
    <div className="flex flex-col gap-5 p-6">
      <Button
        variant="ghost"
        size="sm"
        className="-ml-2 w-fit text-muted-foreground"
        nativeButton={false}
        render={<Link href="/mahasiswa" />}
      >
        <ArrowLeft />
        Semua mahasiswa
      </Button>

      <PageHeader
        title={mhs.name}
        description={`${mhs.nim} · ${mhs.className ?? "Tanpa kelas"} · ${tier.label}${tier.persen > 0 ? ` (−${tier.persen}%)` : ""}`}
      >
        <Button variant="destructive" onClick={() => setHapusTerbuka(true)}>
          <Trash2 />
          Hapus
        </Button>
        <Button variant="outline" onClick={() => setUbahTerbuka(true)}>
          <PencilLine />
          Ubah data
        </Button>
        <Button variant="outline" onClick={() => setResetSandiTerbuka(true)}>
          <KeyRound />
          Reset kata sandi
        </Button>
        <Button variant="outline" onClick={() => setPenyesuaianTerbuka(true)}>
          <Scale />
          Penyesuaian
        </Button>
        <Button onClick={() => setBuatTerbuka(true)}>
          <Plus />
          Buat tagihan
        </Button>
      </PageHeader>

      <StatRow>
        <StatTile
          label="Total ditagih"
          value={formatRupiah(totalDitagih)}
          sublabel={`${plans.data?.length ?? 0} tagihan`}
        />
        <StatTile
          label="Sudah dibayar"
          value={formatRupiah(totalDibayar)}
          tone={totalDibayar > 0 ? "success" : undefined}
        />
        <StatTile
          label="Sisa"
          value={formatRupiah(totalDitagih - totalDibayar)}
          tone={totalDitagih - totalDibayar > 0 ? "warning" : "success"}
        />
        <StatTile
          label="Saldo mahasiswa"
          value={formatRupiah(mhs.walletBalance)}
          sublabel="Kelebihan bayar"
          icon={Wallet}
        />
      </StatRow>

      {!mhs.documentsComplete && (
        <p className="rounded-lg border border-warning/25 bg-warning-soft px-4 py-2.5 text-sm text-warning">
          Dokumen wajib belum lengkap — menunggu{" "}
          <strong>
            {mhs.nextDocumentStep
              ? documentStepLabels[mhs.nextDocumentStep]
              : "—"}
          </strong>
          . Mahasiswa belum bisa mengakses tagihannya sampai ini selesai.
        </p>
      )}

      <Separator />

      <KartuDokumen mahasiswa={mhs} />

      <Separator />

      <section className="flex flex-col gap-4">
        <h3 className="font-heading text-sm font-semibold">Tagihan</h3>

        {plans.isPending ? (
          <TableSkeleton rows={5} />
        ) : plans.error ? (
          <ErrorState
            message={
              plans.error instanceof ApiError
                ? plans.error.message
                : "Coba muat ulang halaman."
            }
          />
        ) : plans.data.length === 0 ? (
          <EmptyState
            title="Belum ada tagihan"
            description="Buat tagihan Pendaftaran atau UKT untuk mahasiswa ini."
          />
        ) : (
          plans.data.map((plan) => (
            <KartuTagihan
              key={plan.id}
              onBatalkan={setTagihanDibatalkan}
              plan={plan}
              onUbahCicilan={setCicilanDiubah}
            />
          ))
        )}
      </section>

      <DialogBatalTagihan
        open={tagihanDibatalkan !== null}
        onOpenChange={(next) => !next && setTagihanDibatalkan(null)}
        studentId={mhs.id}
        plan={tagihanDibatalkan}
      />

      <DialogBuatTagihan
        open={buatTerbuka}
        onOpenChange={setBuatTerbuka}
        studentId={studentId}
        studentTier={mhs.discountTier}
      />

      <DialogUbahNominal
        installment={cicilanDiubah}
        studentId={studentId}
        onClose={() => setCicilanDiubah(null)}
      />

      <DialogPenyesuaian
        open={penyesuaianTerbuka}
        onOpenChange={setPenyesuaianTerbuka}
        studentId={studentId}
        walletBalance={mhs.walletBalance}
        plans={plans.data ?? []}
      />

      <DialogUbahMahasiswa
        // key: isian dialog berangkat dari data terbaru tiap kali dibuka,
        // tanpa perlu effect penyetel ulang.
        key={`ubah-${mhs.name}-${mhs.phone}-${mhs.studyClassId}-${mhs.active}`}
        open={ubahTerbuka}
        onOpenChange={setUbahTerbuka}
        mahasiswa={mhs}
      />

      <DialogHapusMahasiswa
        open={hapusTerbuka}
        onOpenChange={setHapusTerbuka}
        mahasiswa={mhs}
      />

      <DialogResetSandi
        open={resetSandiTerbuka}
        onOpenChange={setResetSandiTerbuka}
        studentId={studentId}
        nim={mhs.nim}
        nama={mhs.name}
      />
    </div>
  );
}

function KartuTagihan({
  plan,
  onUbahCicilan,
  onBatalkan,
}: {
  plan: PaymentPlan;
  onUbahCicilan: (installment: Installment) => void;
  onBatalkan: (plan: PaymentPlan) => void;
}) {
  const lunas = Number(plan.remaining) === 0;
  const dibatalkan = plan.status === "CANCELLED";

  return (
    <div
      className={cn(
        "overflow-hidden rounded-lg border border-border bg-card",
        // Tagihan yang dibatalkan tetap ditampilkan, tapi diredupkan: ia bagian
        // dari riwayat mahasiswa ini dan menghilangkannya membuat admin bertanya
        // ke mana perginya tagihan yang tadi ada.
        dibatalkan && "opacity-60",
      )}
    >
      <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
        <div>
          <h4 className="font-heading text-sm font-semibold">
            {plan.categoryLabel}
            {plan.semesterNumber && (
              <span className="ml-2 font-normal text-muted-foreground">
                semester {plan.semesterNumber}
              </span>
            )}
          </h4>
          <p className="text-xs text-muted-foreground">
            {plan.academicYear} {plan.term === "GASAL" ? "Gasal" : "Genap"} ·
            dasar {formatRupiah(plan.baseAmount)}
            {Number(plan.discountPercent) > 0 && (
              <span className="text-success">
                {" "}
                · potongan {Number(plan.discountPercent)}%
              </span>
            )}
          </p>
        </div>

        <div className="text-right">
          <p className="font-heading text-base font-semibold tnum">
            {formatRupiah(plan.totalAmount)}
          </p>
          <p
            className={cn(
              "text-xs",
              lunas ? "text-success" : "text-muted-foreground",
            )}
          >
            {lunas ? "Lunas" : `sisa ${formatRupiah(plan.remaining)}`}
          </p>
        </div>

        {!dibatalkan && (
          <Tooltip>
            <TooltipTrigger
              render={
                <Button
                  variant="ghost"
                  size="icon-sm"
                  aria-label={`Batalkan tagihan ${plan.categoryLabel}`}
                  onClick={() => onBatalkan(plan)}
                >
                  <Ban className="size-4" />
                </Button>
              }
            />
            <TooltipContent>Batalkan tagihan ini</TooltipContent>
          </Tooltip>
        )}
      </div>

      {dibatalkan && (
        <p className="border-t border-border bg-muted/50 px-5 py-2 text-xs text-muted-foreground">
          <span className="font-medium text-foreground">Dibatalkan</span>
          {plan.cancelledAt ? ` ${formatTanggalJam(plan.cancelledAt)}` : ""} —{" "}
          {plan.cancelReason}
        </p>
      )}

      <div className="overflow-x-auto border-t border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-20">Cicilan</TableHead>
              <TableHead>Jatuh tempo</TableHead>
              <TableHead className="text-right">Nominal</TableHead>
              <TableHead className="text-right">Dibayar</TableHead>
              <TableHead className="text-right">Sisa</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-12" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {plan.installments.map((cicilan) => (
              <TableRow key={cicilan.id}>
                <TableCell className="tnum">
                  {cicilan.installmentNo}/{plan.installments.length}
                </TableCell>
                <TableCell className="whitespace-nowrap text-muted-foreground">
                  {formatTanggal(cicilan.dueDate)}
                </TableCell>
                <TableCell className="text-right font-medium">
                  {formatRupiah(cicilan.amount)}
                </TableCell>
                <TableCell className="text-right text-muted-foreground">
                  {formatRupiah(cicilan.amountPaid)}
                </TableCell>
                <TableCell className="text-right">
                  {formatRupiah(cicilan.outstanding)}
                </TableCell>
                <TableCell>
                  <InstallmentStatusBadge status={cicilan.status} />
                </TableCell>
                <TableCell>
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    aria-label={`Ubah nominal cicilan ${cicilan.installmentNo}`}
                    onClick={() => onUbahCicilan(cicilan)}
                  >
                    <PencilLine />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
