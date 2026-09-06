"use client";

import { useMemo, useState } from "react";
import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type SortingState,
  type VisibilityState,
} from "@tanstack/react-table";
import {
  ArrowDown,
  ArrowUp,
  ChevronLeft,
  ChevronRight,
  ChevronsUpDown,
  Search,
  SlidersHorizontal,
  TriangleAlert,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  PaymentStatusBadge,
  type PaymentStatus,
} from "@/components/status-badge";
import { formatRupiah, formatTanggalJam } from "@/lib/format";
import { KeyakinanOcr } from "./keyakinan-ocr";
import type { PaymentRow } from "./api";

const statusFilters = [
  { value: "SEMUA", label: "Semua" },
  { value: "NEEDS_REVIEW", label: "Perlu ditinjau" },
  { value: "PENDING", label: "Menunggu OCR" },
  // Gabungan otomatis dan manual: keduanya sama-sama berarti uangnya sudah
  // masuk, dan itu yang biasanya ingin dilihat sekaligus.
  { value: "TERVERIFIKASI", label: "Terverifikasi" },
  { value: "AUTO_VERIFIED", label: "Otomatis" },
  { value: "VERIFIED", label: "Manual" },
  { value: "REJECTED", label: "Ditolak" },
  { value: "FAILED", label: "Gagal" },
] as const;

const columnLabels: Record<string, string> = {
  student: "Mahasiswa",
  className: "Kelas",
  installment: "Cicilan",
  amount: "Nominal",
  ocrAmount: "Terbaca OCR",
  confidence: "Keyakinan",
  status: "Status",
  createdAt: "Diunggah",
};

export function TabelVerifikasi({
  data,
  statusFilter,
  onStatusFilterChange,
  search,
  onSearchChange,
  onReview,
}: {
  data: PaymentRow[];
  statusFilter: string;
  onStatusFilterChange: (value: string) => void;
  search: string;
  onSearchChange: (value: string) => void;
  onReview: (payment: PaymentRow) => void;
}) {
  const [sorting, setSorting] = useState<SortingState>([
    { id: "createdAt", desc: true },
  ]);
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({});

  const columns = useMemo<ColumnDef<PaymentRow>[]>(
    () => [
      {
        id: "student",
        accessorFn: (row) => `${row.studentName} ${row.studentNim}`,
        header: "Mahasiswa",
        cell: ({ row }) => (
          <div className="min-w-44">
            <span className="block font-medium">
              {row.original.studentName}
            </span>
            <span className="block font-mono text-xs text-muted-foreground">
              {row.original.studentNim}
            </span>
          </div>
        ),
      },
      {
        id: "className",
        accessorFn: (row) => row.className ?? "—",
        header: "Kelas",
        cell: ({ getValue }) => (
          <span className="whitespace-nowrap text-muted-foreground">
            {String(getValue())}
          </span>
        ),
      },
      {
        id: "installment",
        accessorFn: (row) => row.installmentNo ?? 0,
        header: "Cicilan",
        cell: ({ row }) => {
          const { installmentNo, semesterNumber } = row.original;
          if (installmentNo === null) {
            return <span className="text-muted-foreground">Sekali bayar</span>;
          }
          return (
            <span className="whitespace-nowrap">
              {semesterNumber ? `Sem ${semesterNumber} · ` : ""}
              Cicilan {installmentNo}/5
            </span>
          );
        },
      },
      {
        id: "amount",
        accessorFn: (row) => Number(row.amount),
        header: "Nominal",
        cell: ({ getValue }) => (
          <span className="block text-right font-medium whitespace-nowrap">
            {formatRupiah(Number(getValue()))}
          </span>
        ),
      },
      {
        id: "ocrAmount",
        accessorFn: (row) => row.ocrData?.extracted_amount ?? null,
        header: "Terbaca OCR",
        cell: ({ row }) => {
          const terbaca = row.original.ocrData?.extracted_amount;
          if (terbaca === null || terbaca === undefined) {
            return (
              <span className="block text-right text-muted-foreground">—</span>
            );
          }
          const beda = terbaca !== Number(row.original.amount);
          return (
            <span
              className={cn(
                "flex items-center justify-end gap-1.5 font-medium whitespace-nowrap",
                beda && "text-warning",
              )}
            >
              {beda && <TriangleAlert className="size-3.5 shrink-0" />}
              {formatRupiah(terbaca)}
            </span>
          );
        },
      },
      {
        id: "confidence",
        accessorFn: (row) =>
          row.ocrConfidence ? Number(row.ocrConfidence) : -1,
        header: "Keyakinan",
        cell: ({ row }) => (
          <KeyakinanOcr
            value={
              row.original.ocrConfidence === null
                ? null
                : Number(row.original.ocrConfidence)
            }
          />
        ),
      },
      {
        id: "status",
        accessorFn: (row) => row.status,
        header: "Status",
        cell: ({ row }) => <PaymentStatusBadge status={row.original.status} />,
      },
      {
        id: "createdAt",
        accessorFn: (row) => row.createdAt,
        header: "Diunggah",
        cell: ({ getValue }) => (
          <span className="whitespace-nowrap text-muted-foreground">
            {formatTanggalJam(String(getValue()))}
          </span>
        ),
      },
      {
        id: "aksi",
        enableSorting: false,
        enableHiding: false,
        header: () => <span className="sr-only">Aksi</span>,
        cell: ({ row }) => (
          <Button
            variant="outline"
            size="sm"
            onClick={(event) => {
              event.stopPropagation();
              onReview(row.original);
            }}
          >
            Tinjau
          </Button>
        ),
      },
    ],
    [onReview],
  );

  const table = useReactTable({
    data,
    columns,
    state: { sorting, columnVisibility },
    onSortingChange: setSorting,
    onColumnVisibilityChange: setColumnVisibility,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    initialState: { pagination: { pageSize: 10 } },
  });

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <div className="relative min-w-56 flex-1">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Cari nama atau NIM…"
            className="pl-8"
            aria-label="Cari mahasiswa"
          />
        </div>

        <div className="flex items-center gap-1 overflow-x-auto">
          {statusFilters.map((filter) => (
            <Button
              key={filter.value}
              variant={statusFilter === filter.value ? "secondary" : "ghost"}
              size="sm"
              onClick={() => onStatusFilterChange(filter.value)}
              className={cn(
                "whitespace-nowrap",
                statusFilter === filter.value && "font-semibold",
              )}
            >
              {filter.label}
            </Button>
          ))}
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger
            render={
              <Button variant="outline" size="sm" className="ml-auto">
                <SlidersHorizontal />
                Kolom
              </Button>
            }
          />
          <DropdownMenuContent align="end">
            {/* Label wajib di dalam Group, lihat catatan di user-menu.tsx. */}
            <DropdownMenuGroup>
              <DropdownMenuLabel>Tampilkan kolom</DropdownMenuLabel>
              {table
                .getAllColumns()
                .filter((column) => column.getCanHide())
                .map((column) => (
                  <DropdownMenuCheckboxItem
                    key={column.id}
                    checked={column.getIsVisible()}
                    onCheckedChange={(value) =>
                      column.toggleVisibility(Boolean(value))
                    }
                  >
                    {columnLabels[column.id] ?? column.id}
                  </DropdownMenuCheckboxItem>
                ))}
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-border/70 bg-card shadow-sm">
        <Table>
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  const canSort = header.column.getCanSort();
                  const sorted = header.column.getIsSorted();
                  return (
                    <TableHead key={header.id}>
                      {header.isPlaceholder ? null : canSort ? (
                        <button
                          type="button"
                          onClick={header.column.getToggleSortingHandler()}
                          className="inline-flex items-center gap-1 rounded-sm hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
                        >
                          {flexRender(
                            header.column.columnDef.header,
                            header.getContext(),
                          )}
                          {sorted === "asc" ? (
                            <ArrowUp className="size-3" />
                          ) : sorted === "desc" ? (
                            <ArrowDown className="size-3" />
                          ) : (
                            <ChevronsUpDown className="size-3 opacity-40" />
                          )}
                        </button>
                      ) : (
                        flexRender(
                          header.column.columnDef.header,
                          header.getContext(),
                        )
                      )}
                    </TableHead>
                  );
                })}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {table.getRowModel().rows.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={columns.length}
                  className="h-28 text-center text-muted-foreground"
                >
                  Belum ada bukti bayar untuk kategori ini.
                </TableCell>
              </TableRow>
            ) : (
              table.getRowModel().rows.map((row) => (
                <TableRow
                  key={row.id}
                  onClick={() => onReview(row.original)}
                  className="cursor-pointer"
                >
                  {row.getVisibleCells().map((cell) => (
                    <TableCell key={cell.id}>
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext(),
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-muted-foreground">
        <span>{data.length} pembayaran</span>
        <div className="flex items-center gap-2">
          <span>
            Halaman {table.getState().pagination.pageIndex + 1} dari{" "}
            {table.getPageCount() || 1}
          </span>
          <Button
            variant="outline"
            size="icon-sm"
            onClick={() => table.previousPage()}
            disabled={!table.getCanPreviousPage()}
            aria-label="Halaman sebelumnya"
          >
            <ChevronLeft />
          </Button>
          <Button
            variant="outline"
            size="icon-sm"
            onClick={() => table.nextPage()}
            disabled={!table.getCanNextPage()}
            aria-label="Halaman berikutnya"
          >
            <ChevronRight />
          </Button>
        </div>
      </div>
    </div>
  );
}

export type { PaymentStatus };
