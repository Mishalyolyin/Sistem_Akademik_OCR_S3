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
  Trash2,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
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
import { DialogHapusMassal } from "@/features/mahasiswa/dialog-hapus-massal";
import { useClasses } from "@/features/kelas/api";
import type { DiscountTier } from "@/features/tarif/konstanta";
import { useGolongan } from "@/features/tarif/api";
import { ApiError } from "@/lib/api";
import { formatRupiah } from "@/lib/format";

const SEMUA = "SEMUA";

export function HalamanMahasiswa() {
  const golongan = useGolongan();
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
            {golongan.aktif.map((tier) => (
              <SelectItem key={tier.tier} value={tier.tier}>
                {tier.label}
                {Number(tier.percent) > 0 ? ` (−${Number(tier.percent)}%)` : ""}
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
            error instanceof ApiError
              ? error.message
              : "Coba muat ulang halaman."
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
  const golongan = useGolongan();
  const ubahTier = useChangeDiscountTier();
  const [terpilih, setTerpilih] = useState<number[]>([]);
  const [hapusTerbuka, setHapusTerbuka] = useState(false);

  // Pilihan hanya berlaku untuk baris yang sedang tampil; berpindah halaman
  // atau menyaring ulang membuat daftar idnya tidak lagi ada di layar.
  const idHalamanIni = data.map((mhs) => mhs.id);
  const dipilihDiHalamanIni = terpilih.filter((id) =>
    idHalamanIni.includes(id),
  );
  const semuaTerpilih =
    data.length > 0 && dipilihDiHalamanIni.length === data.length;

  const mahasiswaTerpilih = data.filter((mhs) => terpilih.includes(mhs.id));

  function toggleSatu(id: number, pilih: boolean) {
    setTerpilih((sebelumnya) =>
      pilih
        ? [...sebelumnya, id]
        : sebelumnya.filter((terdaftar) => terdaftar !== id),
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {dipilihDiHalamanIni.length > 0 && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border bg-muted/40 px-4 py-2.5">
          <span className="text-sm">
            <strong>{dipilihDiHalamanIni.length}</strong> mahasiswa dipilih
          </span>
          <div className="flex gap-2">
            <Button variant="ghost" size="sm" onClick={() => setTerpilih([])}>
              Batalkan pilihan
            </Button>
            <Button
              variant="destructive"
              size="sm"
              onClick={() => setHapusTerbuka(true)}
            >
              <Trash2 />
              Hapus terpilih
            </Button>
          </div>
        </div>
      )}

      <div className="overflow-x-auto rounded-2xl border border-border/70 bg-card shadow-sm">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10">
                <Checkbox
                  checked={semuaTerpilih}
                  aria-label="Pilih semua mahasiswa di halaman ini"
                  onCheckedChange={(checked) =>
                    setTerpilih((sebelumnya) =>
                      checked
                        ? [
                            ...sebelumnya.filter(
                              (id) => !idHalamanIni.includes(id),
                            ),
                            ...idHalamanIni,
                          ]
                        : sebelumnya.filter((id) => !idHalamanIni.includes(id)),
                    )
                  }
                />
              </TableHead>
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
                  <Checkbox
                    checked={terpilih.includes(mhs.id)}
                    aria-label={`Pilih ${mhs.name}`}
                    onCheckedChange={(checked) => toggleSatu(mhs.id, !!checked)}
                  />
                </TableCell>

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
                            {golongan.label(mhs.discountTier)}
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
                                `${updated.name} kini ${golongan.label(updated.discountTier)}.`,
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
                        {golongan.aktif.map((tier) => (
                          <SelectItem key={tier.tier} value={tier.tier}>
                            {tier.label}
                            {Number(tier.percent) > 0
                              ? ` (−${Number(tier.percent)}%)`
                              : ""}
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

      <DialogHapusMassal
        open={hapusTerbuka}
        onOpenChange={setHapusTerbuka}
        terpilih={mahasiswaTerpilih}
        onSelesai={(idTerhapus) =>
          setTerpilih((sebelumnya) =>
            sebelumnya.filter((id) => !idTerhapus.includes(id)),
          )
        }
      />
    </div>
  );
}
