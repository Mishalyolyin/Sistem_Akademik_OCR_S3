"use client";

import { useRef, useState } from "react";
import { toast } from "sonner";
import { Download, Loader2, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { PageHeader } from "@/components/page-header";
import {
  downloadTemplate,
  useImportHistory,
  useImportStudents,
  type ImportResult,
} from "@/features/mahasiswa/api-import";
import { useGolongan } from "@/features/tarif/api";
import { ApiError } from "@/lib/api";

export function HalamanImport() {
  const fileInput = useRef<HTMLInputElement>(null);
  const [hasil, setHasil] = useState<ImportResult | null>(null);
  const unggah = useImportStudents();
  const riwayat = useImportHistory();

  function pilihFile(file: File | undefined) {
    if (!file) return;

    unggah.mutate(file, {
      onSuccess: (result) => {
        setHasil(result);
        if (result.failedRows === 0) {
          toast.success(`${result.successRows} mahasiswa berhasil ditambahkan.`);
        } else {
          toast.warning(
            `${result.successRows} berhasil, ${result.failedRows} gagal. Lihat rinciannya di bawah.`,
          );
        }
      },
      onError: (e) =>
        toast.error(
          e instanceof ApiError ? e.message : "Berkas gagal diunggah.",
        ),
    });

    if (fileInput.current) fileInput.current.value = "";
  }

  return (
    <div className="flex flex-col gap-5 p-6">
      <PageHeader
        title="Import Mahasiswa"
        description="Unggah data mahasiswa dari Excel. Kelas yang belum ada dibuat otomatis."
      />

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
        <section className="flex flex-col gap-4 rounded-lg border border-border bg-card p-5">
          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant="outline"
              onClick={() =>
                downloadTemplate().catch(() =>
                  toast.error("Gagal mengunduh template."),
                )
              }
            >
              <Download />
              Unduh template
            </Button>

            <input
              ref={fileInput}
              type="file"
              accept=".xlsx"
              className="hidden"
              onChange={(event) => pilihFile(event.target.files?.[0])}
            />
            <Button
              onClick={() => fileInput.current?.click()}
              disabled={unggah.isPending}
            >
              {unggah.isPending ? (
                <Loader2 className="animate-spin" />
              ) : (
                <Upload />
              )}
              {unggah.isPending ? "Mengunggah…" : "Pilih berkas .xlsx"}
            </Button>
          </div>

          <Separator />

          {hasil ? <HasilImport hasil={hasil} /> : <PetunjukKolom />}
        </section>

        <aside className="flex flex-col gap-3 rounded-lg border border-border bg-card p-5">
          <h3 className="font-heading text-sm font-semibold">Riwayat import</h3>
          {riwayat.isPending ? (
            <p className="text-sm text-muted-foreground">Memuat…</p>
          ) : riwayat.data && riwayat.data.length > 0 ? (
            <ul className="flex flex-col gap-2.5">
              {riwayat.data.slice(0, 8).map((item) => (
                <li key={item.batchId} className="text-sm">
                  <span className="block truncate font-medium">
                    {item.filename}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    <span className="text-success">{item.successRows} berhasil</span>
                    {item.failedRows > 0 && (
                      <>
                        {" · "}
                        <span className="text-danger">
                          {item.failedRows} gagal
                        </span>
                      </>
                    )}
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-muted-foreground">Belum pernah import.</p>
          )}
        </aside>
      </div>
    </div>
  );
}

function HasilImport({ hasil }: { hasil: ImportResult }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-3 divide-x divide-border overflow-hidden rounded-lg border border-border">
        <Angka label="Total baris" value={hasil.totalRows} />
        <Angka label="Berhasil" value={hasil.successRows} tone="success" />
        <Angka
          label="Gagal"
          value={hasil.failedRows}
          tone={hasil.failedRows > 0 ? "danger" : undefined}
        />
      </div>

      {hasil.errors.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-danger/25">
          <p className="bg-danger-soft px-4 py-2 text-sm font-medium text-danger">
            Baris yang gagal — baris lain tetap tersimpan
          </p>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-20">Baris</TableHead>
                  <TableHead className="w-40">NIM</TableHead>
                  <TableHead>Alasan</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {hasil.errors.map((galat) => (
                  <TableRow key={`${galat.row}-${galat.nim}`}>
                    <TableCell className="tnum">{galat.row}</TableCell>
                    <TableCell className="font-mono text-xs">
                      {galat.nim || "—"}
                    </TableCell>
                    <TableCell>{galat.message}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </div>
      )}
    </div>
  );
}

function Angka({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone?: "success" | "danger";
}) {
  return (
    <div className="px-4 py-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p
        className={
          "font-heading text-xl font-semibold tnum " +
          (tone === "success"
            ? "text-success"
            : tone === "danger"
              ? "text-danger"
              : "")
        }
      >
        {value}
      </p>
    </div>
  );
}

function PetunjukKolom() {
  const golongan = useGolongan();
  return (
    <div className="flex flex-col gap-3 text-sm">
      <p className="text-muted-foreground">
        Berkas harus .xlsx dengan kolom berikut pada baris pertama:
      </p>

      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Kolom</TableHead>
              <TableHead>Isi</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <Baris kolom="nim" isi="Wajib, tidak boleh sama dengan yang sudah ada" />
            <Baris kolom="name" isi="Wajib" />
            <Baris
              kolom="class"
              isi="Wajib. Bebas: A, B, atau Kerjasama A. Dibuat otomatis kalau belum ada"
            />
            <Baris
              kolom="discount_tier"
              isi={golongan.aktif
                .map((tier) =>
                  Number(tier.percent) > 0
                    ? `${tier.tier} (−${Number(tier.percent)}%)`
                    : tier.tier,
                )
                .join(", ")}
            />
            <Baris kolom="start_term" isi="GASAL atau GENAP" />
            <Baris kolom="academic_year" isi="Format 2026/2027" />
            <Baris kolom="phone" isi="Boleh dikosongkan" />
          </TableBody>
        </Table>
      </div>

      <p className="text-muted-foreground">
        Baris yang gagal dilaporkan satu per satu tanpa membatalkan baris lain.
      </p>
    </div>
  );
}

function Baris({ kolom, isi }: { kolom: string; isi: string }) {
  return (
    <TableRow>
      <TableCell className="w-40 font-mono text-xs">{kolom}</TableCell>
      <TableCell className="text-muted-foreground">{isi}</TableCell>
    </TableRow>
  );
}
