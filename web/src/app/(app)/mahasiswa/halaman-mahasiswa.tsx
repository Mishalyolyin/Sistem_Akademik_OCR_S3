"use client";

import { useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import {
  ChevronLeft,
  ChevronRight,
  FileSpreadsheet,
  Lock,
  Search,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  EmptyState,
  ErrorState,
  PageHeader,
  TableSkeleton,
} from "@/components/page-header";
import {
  documentStepLabels,
  useChangeDiscountTier,
  useStudents,
  type StudentSummary,
} from "@/features/mahasiswa/api";
import { useClasses } from "@/features/kelas/api";
import { potongan, type DiscountTier } from "@/features/tarif/konstanta";
import { ApiError } from "@/lib/api";
import { formatRupiah } from "@/lib/format";

const SEMUA = "SEMUA";

export function HalamanMahasiswa() {
  const [search, setSearch] = useState("");
  const [classId, setClassId] = useState<string>(SEMUA);
  const [tier, setTier] = useState<string>(SEMUA);
  const [page, setPage] = useState(0);

  const kelas = useClasses();
  const { data, isPending, error } = useStudents({
    search: search.trim() || undefined,
    classId: classId === SEMUA ? undefined : Number(classId),
    tier: tier === SEMUA ? undefined : (tier as DiscountTier),
    page,
  });

  // Select dari Base UI mengirim string | null; null berarti pilihan dikosongkan.
  function pilih(setter: (value: string) => void) {
    return (value: string | null) => {
      setter(value ?? SEMUA);
      setPage(0);
    };
  }

  return (
    <div className="flex flex-col gap-5 p-6">
      <PageHeader
        title="Mahasiswa"
        description="Data mahasiswa Program Doktor PAI beserta golongan potongannya"
      >
        <Button
          variant="outline"
          nativeButton={false}
          render={<Link href="/mahasiswa/import" />}
        >
          <FileSpreadsheet />
          Import Excel
        </Button>
      </PageHeader>

      <div className="flex flex-wrap items-center gap-2">
        <div className="relative min-w-56 flex-1">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Cari nama atau NIM…"
            className="pl-8"
            aria-label="Cari mahasiswa"
          />
        </div>

        <Select value={classId} onValueChange={pilih(setClassId)}>
          <SelectTrigger className="w-52" aria-label="Saring kelas">
            <SelectValue placeholder="Semua kelas" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={SEMUA}>Semua kelas</SelectItem>
            {kelas.data?.map((item) => (
              <SelectItem key={item.id} value={String(item.id)}>
                {item.displayName}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={tier} onValueChange={pilih(setTier)}>
          <SelectTrigger className="w-52" aria-label="Saring golongan">
            <SelectValue placeholder="Semua golongan" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={SEMUA}>Semua golongan</SelectItem>
            {Object.entries(potongan).map(([key, value]) => (
              <SelectItem key={key} value={key}>
                {value.label}
                {value.persen > 0 ? ` (−${value.persen}%)` : ""}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isPending ? (
        <TableSkeleton />
      ) : error ? (
        <ErrorState
          message={
            error instanceof ApiError ? error.message : "Coba muat ulang halaman."
          }
        />
      ) : data.content.length === 0 ? (
        <EmptyState
          title="Tidak ada mahasiswa"
          description="Belum ada data yang cocok. Unggah Excel mahasiswa untuk mengisi data."
        />
      ) : (
        <>
          <TabelMahasiswa data={data.content} />

          <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-muted-foreground">
            <span>{data.totalElements} mahasiswa</span>
            <div className="flex items-center gap-2">
              <span>
                Halaman {data.number + 1} dari {Math.max(data.totalPages, 1)}
              </span>
              <Button
                variant="outline"
                size="icon-sm"
                onClick={() => setPage((p) => Math.max(p - 1, 0))}
                disabled={data.first}
                aria-label="Halaman sebelumnya"
              >
                <ChevronLeft />
              </Button>
              <Button
                variant="outline"
                size="icon-sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.last}
                aria-label="Halaman berikutnya"
              >
                <ChevronRight />
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function TabelMahasiswa({ data }: { data: StudentSummary[] }) {
  const ubahTier = useChangeDiscountTier();

  return (
    <div className="overflow-x-auto rounded-lg border border-border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Mahasiswa</TableHead>
            <TableHead>Kelas</TableHead>
            <TableHead>Golongan potongan</TableHead>
            <TableHead>Mulai</TableHead>
            <TableHead>Dokumen</TableHead>
            <TableHead className="text-right">Saldo</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((mhs) => (
            <TableRow key={mhs.id}>
              <TableCell>
                <Link
                  href={`/mahasiswa/${mhs.id}`}
                  className="block font-medium hover:underline focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
                >
                  {mhs.name}
                </Link>
                <span className="block font-mono text-xs text-muted-foreground">
                  {mhs.nim}
                </span>
              </TableCell>

              <TableCell className="whitespace-nowrap text-muted-foreground">
                {mhs.className ?? "—"}
              </TableCell>

              <TableCell>
                {mhs.discountTierLocked ? (
                  <Tooltip>
                    <TooltipTrigger
                      render={
                        <span className="inline-flex cursor-help items-center gap-1.5 text-sm">
                          <Lock className="size-3.5 text-muted-foreground" />
                          {potongan[mhs.discountTier].label}
                        </span>
                      }
                    />
                    <TooltipContent>
                      Terkunci: mahasiswa sudah pernah mengunggah bukti bayar
                    </TooltipContent>
                  </Tooltip>
                ) : (
                  <Select
                    value={mhs.discountTier}
                    onValueChange={(value) =>
                      value &&
                      ubahTier.mutate(
                        { id: mhs.id, tier: value as DiscountTier },
                        {
                          onSuccess: (updated) =>
                            toast.success(
                              `${updated.name} kini ${potongan[updated.discountTier].label}.`,
                            ),
                          onError: (e) =>
                            toast.error(
                              e instanceof ApiError
                                ? e.message
                                : "Gagal mengubah golongan.",
                            ),
                        },
                      )
                    }
                  >
                    <SelectTrigger
                      className="h-8 w-48"
                      aria-label={`Golongan ${mhs.name}`}
                    >
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {Object.entries(potongan).map(([key, value]) => (
                        <SelectItem key={key} value={key}>
                          {value.label}
                          {value.persen > 0 ? ` (−${value.persen}%)` : ""}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              </TableCell>

              <TableCell className="whitespace-nowrap text-muted-foreground">
                {mhs.startAcademicYear}{" "}
                {mhs.startTerm === "GASAL" ? "Gasal" : "Genap"}
              </TableCell>

              <TableCell>
                {mhs.documentsComplete ? (
                  <span className="text-sm text-success">Lengkap</span>
                ) : (
                  <span
                    className={cn(
                      "text-sm text-warning",
                      "whitespace-nowrap",
                    )}
                  >
                    Menunggu{" "}
                    {mhs.nextDocumentStep
                      ? documentStepLabels[mhs.nextDocumentStep]
                      : "—"}
                  </span>
                )}
              </TableCell>

              <TableCell className="text-right whitespace-nowrap">
                {formatRupiah(mhs.walletBalance)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
