"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Ban, Check, Info, PencilLine, Plus, Undo2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  ErrorState,
  PageHeader,
  TableSkeleton,
} from "@/components/page-header";
import {
  useTierRates,
  useTuitionRates,
  useUpdateTierPercent,
  useUpdateTuitionRate,
  type TierRate,
  type TuitionRate,
} from "@/features/tarif/api";
import { DialogTambahGolongan } from "@/features/tarif/dialog-tambah-golongan";
import {
  JUMLAH_SEMESTER_UKT,
  tarifDasar,
} from "@/features/tarif/konstanta";
import { ApiError } from "@/lib/api";
import { formatRupiah } from "@/lib/format";

export function HalamanTarif() {
  const [tambahTerbuka, setTambahTerbuka] = useState(false);
  const rates = useTuitionRates();
  const tiers = useTierRates();

  // Pratinjau di dialog memakai tarif dasar yang sungguhan, bukan angka brosur
  // di konstanta — tarif UKT bisa diubah admin di tabel tepat di atasnya.
  const tarifDasarUkt = Number(
    rates.data?.find((rate) => rate.category === "UKT" && rate.active)?.amount ??
      tarifDasar.UKT,
  );

  return (
    <div className="flex flex-col gap-6 p-6">
      <PageHeader
        title="Tarif & Potongan"
        description="Tarif dasar per kategori, dan potongan UKT per golongan mahasiswa"
      />

      <p className="flex gap-2 rounded-lg border border-info/25 bg-info-soft px-4 py-3 text-sm text-info">
        <Info className="mt-0.5 size-4 shrink-0" />
        <span>
          Perubahan tarif hanya berlaku untuk tagihan yang{" "}
          <strong>dibuat setelahnya</strong>. Tagihan mahasiswa yang sudah
          terbit tidak ikut berubah.
        </span>
      </p>

      <section className="flex flex-col gap-3">
        <h3 className="font-heading text-sm font-semibold">
          Tarif dasar per kategori
        </h3>
        {rates.isPending ? (
          <TableSkeleton rows={6} />
        ) : rates.error ? (
          <ErrorState
            message={
              rates.error instanceof ApiError
                ? rates.error.message
                : "Coba muat ulang halaman."
            }
          />
        ) : (
          <TabelTarif data={rates.data} />
        )}
      </section>

      <section className="flex flex-col gap-3">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h3 className="font-heading text-sm font-semibold">
              Golongan potongan
            </h3>
            <p className="text-sm text-muted-foreground">
              Potongan hanya berlaku untuk UKT. Pendaftaran dan biaya ujian sama
              untuk semua golongan.
            </p>
          </div>
          <Button variant="outline" onClick={() => setTambahTerbuka(true)}>
            <Plus />
            Tambah golongan
          </Button>
        </div>
        {tiers.isPending ? (
          <TableSkeleton rows={5} />
        ) : tiers.error ? (
          <ErrorState
            message={
              tiers.error instanceof ApiError
                ? tiers.error.message
                : "Coba muat ulang halaman."
            }
          />
        ) : (
          <TabelGolongan data={tiers.data} />
        )}
      </section>

      <DialogTambahGolongan
        open={tambahTerbuka}
        onOpenChange={setTambahTerbuka}
        tarifDasarUkt={tarifDasarUkt}
      />
    </div>
  );
}

function TabelTarif({ data }: { data: TuitionRate[] }) {
  const ubah = useUpdateTuitionRate();
  const [editId, setEditId] = useState<number | null>(null);
  const [nilai, setNilai] = useState("");

  function simpan(rate: TuitionRate) {
    const amount = Number(nilai);
    if (!Number.isFinite(amount) || amount <= 0) {
      toast.error("Nominal harus angka lebih besar dari nol.");
      return;
    }

    ubah.mutate(
      {
        id: rate.id,
        category: rate.category,
        academicYear: rate.academicYear,
        amount,
      },
      {
        onSuccess: () => {
          toast.success(`Tarif ${rate.categoryLabel} diperbarui.`);
          setEditId(null);
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal menyimpan tarif.",
          ),
      },
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Kategori</TableHead>
            <TableHead>Tahun akademik</TableHead>
            <TableHead className="text-right">Nominal</TableHead>
            <TableHead className="w-24" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((rate) => (
            <TableRow key={rate.id}>
              <TableCell className="font-medium">
                {rate.categoryLabel}
                {rate.category === "UKT" && (
                  <span className="ml-2 text-xs font-normal text-muted-foreground">
                    per semester × {JUMLAH_SEMESTER_UKT}
                  </span>
                )}
              </TableCell>
              <TableCell className="text-muted-foreground">
                {rate.academicYear}
              </TableCell>
              <TableCell className="text-right">
                {editId === rate.id ? (
                  <Input
                    autoFocus
                    type="number"
                    value={nilai}
                    onChange={(event) => setNilai(event.target.value)}
                    className="ml-auto h-8 w-40 text-right"
                    aria-label={`Nominal ${rate.categoryLabel}`}
                  />
                ) : (
                  <span className="font-medium">{formatRupiah(rate.amount)}</span>
                )}
              </TableCell>
              <TableCell>
                {editId === rate.id ? (
                  <div className="flex justify-end gap-1">
                    <Button
                      size="icon-sm"
                      onClick={() => simpan(rate)}
                      disabled={ubah.isPending}
                      aria-label="Simpan"
                    >
                      <Check />
                    </Button>
                    <Button
                      size="icon-sm"
                      variant="outline"
                      onClick={() => setEditId(null)}
                      aria-label="Batal"
                    >
                      <X />
                    </Button>
                  </div>
                ) : (
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    className="ml-auto flex"
                    aria-label={`Ubah tarif ${rate.categoryLabel}`}
                    onClick={() => {
                      setEditId(rate.id);
                      setNilai(String(Math.round(Number(rate.amount))));
                    }}
                  >
                    <PencilLine />
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function TabelGolongan({ data }: { data: TierRate[] }) {
  const ubah = useUpdateTierPercent();
  const [editTier, setEditTier] = useState<string | null>(null);
  const [persen, setPersen] = useState("");

  function simpan(tier: TierRate) {
    const nilai = Number(persen);
    if (!Number.isFinite(nilai) || nilai < 0 || nilai > 100) {
      toast.error("Potongan harus antara 0 dan 100 persen.");
      return;
    }

    ubah.mutate(
      { tier: tier.tier, percent: nilai },
      {
        onSuccess: (updated) => {
          toast.success(
            `Potongan ${updated.label} kini ${Number(updated.percent)}% — UKT ${formatRupiah(updated.uktPerSemester)} per semester.`,
          );
          setEditTier(null);
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal menyimpan potongan.",
          ),
      },
    );
  }

  /**
   * Golongan lama dinonaktifkan, bukan dihapus: mahasiswa dan tagihan yang
   * terlanjur memakainya tetap harus bisa dibaca. Backend menolak selama masih
   * ada mahasiswa di golongan itu, dan alasannya ditampilkan apa adanya.
   */
  function ubahAktif(tier: TierRate) {
    ubah.mutate(
      { tier: tier.tier, percent: Number(tier.percent), active: !tier.active },
      {
        onSuccess: (updated) =>
          toast.success(
            updated.active
              ? `Golongan ${updated.label} bisa dipilih lagi.`
              : `Golongan ${updated.label} dinonaktifkan — tidak lagi bisa dipilih untuk mahasiswa baru.`,
          ),
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal mengubah golongan.",
          ),
      },
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Golongan</TableHead>
            <TableHead className="text-right">Potongan</TableHead>
            <TableHead className="text-right">UKT per semester</TableHead>
            <TableHead className="text-right">Per cicilan (÷5)</TableHead>
            <TableHead className="text-right">
              Total {JUMLAH_SEMESTER_UKT} semester
            </TableHead>
            <TableHead className="w-28" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((tier) => (
            <TableRow key={tier.tier}>
              <TableCell className="font-medium">
                <span className={tier.active ? undefined : "text-muted-foreground"}>
                  {tier.label}
                </span>
                {!tier.active && (
                  <span className="ml-2 rounded border border-border px-1.5 py-0.5 text-xs font-normal text-muted-foreground">
                    nonaktif
                  </span>
                )}
              </TableCell>
              <TableCell className="text-right">
                {editTier === tier.tier ? (
                  <Input
                    autoFocus
                    type="number"
                    value={persen}
                    onChange={(event) => setPersen(event.target.value)}
                    className="ml-auto h-8 w-24 text-right"
                    aria-label={`Potongan ${tier.label}`}
                  />
                ) : Number(tier.percent) > 0 ? (
                  <span className="rounded border border-success/25 bg-success-soft px-1.5 py-0.5 text-xs font-medium text-success">
                    −{Number(tier.percent)}%
                  </span>
                ) : (
                  <span className="text-muted-foreground">—</span>
                )}
              </TableCell>
              <TableCell className="text-right font-medium">
                {formatRupiah(tier.uktPerSemester)}
              </TableCell>
              <TableCell className="text-right">
                {formatRupiah(tier.uktPerInstallment)}
              </TableCell>
              <TableCell className="text-right text-muted-foreground">
                {formatRupiah(
                  Number(tier.uktPerSemester) * JUMLAH_SEMESTER_UKT,
                )}
              </TableCell>
              <TableCell>
                {editTier === tier.tier ? (
                  <div className="flex justify-end gap-1">
                    <Button
                      size="icon-sm"
                      onClick={() => simpan(tier)}
                      disabled={ubah.isPending}
                      aria-label="Simpan"
                    >
                      <Check />
                    </Button>
                    <Button
                      size="icon-sm"
                      variant="outline"
                      onClick={() => setEditTier(null)}
                      aria-label="Batal"
                    >
                      <X />
                    </Button>
                  </div>
                ) : (
                  <div className="flex justify-end gap-1">
                    <Button
                      size="icon-sm"
                      variant="ghost"
                      aria-label={`Ubah potongan ${tier.label}`}
                      onClick={() => {
                        setEditTier(tier.tier);
                        setPersen(String(Number(tier.percent)));
                      }}
                    >
                      <PencilLine />
                    </Button>
                    <Button
                      size="icon-sm"
                      variant="ghost"
                      disabled={ubah.isPending}
                      aria-label={
                        tier.active
                          ? `Nonaktifkan golongan ${tier.label}`
                          : `Aktifkan lagi golongan ${tier.label}`
                      }
                      onClick={() => ubahAktif(tier)}
                    >
                      {tier.active ? <Ban /> : <Undo2 />}
                    </Button>
                  </div>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
