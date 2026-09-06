"use client";

import { useRef, useState } from "react";
import { toast } from "sonner";
import { BookOpen, Check, Eye, Loader2, Upload } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ErrorState, TableSkeleton } from "@/components/page-header";
import { PratinjauBerkas } from "@/components/pratinjau-berkas";
import { ApiError } from "@/lib/api";
import {
  useDisertasiSaya,
  useNaskahSendiri,
  useSimpanDisertasi,
  useUnggahBerkasDisertasi,
  type JenisBerkasDisertasi,
} from "@/features/disertasi/api";

const JUDUL_MINIMAL = 10;

const berkas: {
  jenis: JenisBerkasDisertasi;
  judul: string;
  keterangan: string;
}[] = [
  {
    jenis: "DISERTASI",
    judul: "Naskah disertasi",
    keterangan:
      "Naskah lengkap dalam format PDF. Diperlukan menjelang Ujian Tertutup.",
  },
  {
    jenis: "ARTIKEL",
    judul: "Artikel jurnal",
    keterangan:
      "Artikel yang diterbitkan dari disertasi ini, format PDF. Syarat Ujian Terbuka.",
  },
];

/**
 * Identitas disertasi milik mahasiswa sendiri.
 *
 * <p>Judul dan kedua promotor dipisah dari unggahan naskah, dan itu bukan
 * sekadar tata letak. Judul ditetapkan jauh sebelum naskahnya jadi — itulah
 * yang dibawa ke Seminar Proposal. Menuntut keduanya sekaligus berarti tidak
 * ada yang bisa mendaftar tahap ujian pertama sampai disertasinya hampir
 * selesai, persis kebalikan dari urutan yang sebenarnya.
 */
export function HalamanDisertasi() {
  const disertasi = useDisertasiSaya();
  const simpan = useSimpanDisertasi();

  if (disertasi.isPending) return <TableSkeleton rows={4} />;
  if (disertasi.error) {
    return (
      <ErrorState
        message={
          disertasi.error instanceof ApiError
            ? disertasi.error.message
            : "Coba muat ulang halaman."
        }
      />
    );
  }

  const data = disertasi.data;

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h2 className="font-heading text-xl font-semibold tracking-tight">
          Disertasi
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Judul dan nama promotor wajib diisi sebelum mendaftar tahap ujian.
          Naskahnya menyusul belakangan.
        </p>
      </div>

      <FormIdentitas
        awal={data}
        sedangMenyimpan={simpan.isPending}
        onSimpan={(nilai) =>
          simpan.mutate(nilai, {
            onSuccess: () => toast.success("Identitas disertasi tersimpan."),
            onError: (e) =>
              toast.error(
                e instanceof ApiError ? e.message : "Gagal menyimpan.",
              ),
          })
        }
      />

      <section className="flex flex-col gap-3">
        <h3 className="font-heading text-sm font-semibold">Naskah</h3>

        {!data ? (
          <p className="rounded-lg border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground">
            Isi dulu judul dan promotor di atas. Naskah tanpa keterangan itu
            tidak bisa ditelusuri siapa pembimbingnya dan tentang apa isinya.
          </p>
        ) : (
          berkas.map((item) => (
            <KartuBerkas
              key={item.jenis}
              jenis={item.jenis}
              judul={item.judul}
              keterangan={item.keterangan}
              sudahAda={
                item.jenis === "DISERTASI" ? data.adaNaskah : data.adaArtikel
              }
            />
          ))
        )}
      </section>
    </div>
  );
}

function FormIdentitas({
  awal,
  sedangMenyimpan,
  onSimpan,
}: {
  awal: { title: string; promotor: string; copromotor: string } | null;
  sedangMenyimpan: boolean;
  onSimpan: (nilai: {
    title: string;
    promotor: string;
    copromotor: string;
  }) => void;
}) {
  const [judul, setJudul] = useState(awal?.title ?? "");
  const [promotor, setPromotor] = useState(awal?.promotor ?? "");
  const [copromotor, setCopromotor] = useState(awal?.copromotor ?? "");

  const judulPendek = judul.trim().length > 0 && judul.trim().length < JUDUL_MINIMAL;
  const promotorSama =
    promotor.trim().length > 0 &&
    promotor.trim().toLowerCase() === copromotor.trim().toLowerCase();

  const bolehSimpan =
    judul.trim().length >= JUDUL_MINIMAL &&
    promotor.trim().length >= 3 &&
    copromotor.trim().length >= 3 &&
    !promotorSama &&
    !sedangMenyimpan;

  return (
    <form
      className="flex flex-col gap-4 rounded-2xl border border-border/70 bg-card shadow-sm px-5 py-4"
      onSubmit={(event) => {
        event.preventDefault();
        if (bolehSimpan) {
          onSimpan({
            title: judul.trim(),
            promotor: promotor.trim(),
            copromotor: copromotor.trim(),
          });
        }
      }}
    >
      <div className="flex items-start gap-3">
        <BookOpen className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden />
        <div>
          <h3 className="font-heading text-sm font-semibold">
            Judul dan promotor
          </h3>
          <p className="text-sm text-muted-foreground">
            Boleh diperbarui kapan saja — judul disertasi memang lazim berubah
            selama penyusunan.
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="judul-disertasi">Judul disertasi</Label>
        <Textarea
          id="judul-disertasi"
          rows={2}
          value={judul}
          onChange={(e) => setJudul(e.target.value)}
          placeholder="Judul lengkap sesuai yang disetujui promotor"
          aria-invalid={judulPendek || undefined}
        />
        {judulPendek && (
          <p className="text-xs text-destructive">
            Minimal {JUDUL_MINIMAL} karakter.
          </p>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="promotor">Promotor</Label>
          <Input
            id="promotor"
            value={promotor}
            onChange={(e) => setPromotor(e.target.value)}
            placeholder="Nama lengkap beserta gelar"
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="copromotor">Ko-promotor</Label>
          <Input
            id="copromotor"
            value={copromotor}
            onChange={(e) => setCopromotor(e.target.value)}
            placeholder="Nama lengkap beserta gelar"
            aria-invalid={promotorSama || undefined}
          />
          {promotorSama && (
            <p className="text-xs text-destructive">
              Tidak boleh sama dengan promotor.
            </p>
          )}
        </div>
      </div>

      <div className="flex justify-end">
        <Button type="submit" disabled={!bolehSimpan}>
          {sedangMenyimpan && <Loader2 className="animate-spin" />}
          Simpan
        </Button>
      </div>
    </form>
  );
}

function KartuBerkas({
  jenis,
  judul,
  keterangan,
  sudahAda,
}: {
  jenis: JenisBerkasDisertasi;
  judul: string;
  keterangan: string;
  sudahAda: boolean;
}) {
  const unggah = useUnggahBerkasDisertasi();
  const inputRef = useRef<HTMLInputElement>(null);
  const [pratinjau, setPratinjau] = useState(false);

  function pilih(file: File | undefined) {
    if (!file) return;
    unggah.mutate(
      { jenis, file },
      {
        onSuccess: () => toast.success(`${judul} terunggah.`),
        onError: (e) =>
          toast.error(e instanceof ApiError ? e.message : "Gagal mengunggah."),
      },
    );
    if (inputRef.current) inputRef.current.value = "";
  }

  return (
    <div
      className={cn(
        "flex flex-wrap items-center justify-between gap-3 rounded-lg border px-5 py-4",
        sudahAda ? "border-success/30 bg-success-soft/40" : "border-border bg-card",
      )}
    >
      <div className="flex min-w-0 items-start gap-3">
        {sudahAda ? (
          <Check className="mt-0.5 size-5 shrink-0 text-success" aria-hidden />
        ) : (
          <Upload
            className="mt-0.5 size-5 shrink-0 text-muted-foreground"
            aria-hidden
          />
        )}
        <div className="min-w-0">
          <p className="font-medium">{judul}</p>
          <p className="max-w-lg text-sm text-muted-foreground">{keterangan}</p>
        </div>
      </div>

      <div className="flex items-center gap-2">
        {sudahAda && (
          <Button variant="outline" size="sm" onClick={() => setPratinjau(true)}>
            <Eye />
            Lihat
          </Button>
        )}

        <input
          ref={inputRef}
          type="file"
          accept="application/pdf"
          className="hidden"
          onChange={(e) => pilih(e.target.files?.[0])}
        />
        <Button
          variant={sudahAda ? "outline" : "default"}
          size="sm"
          disabled={unggah.isPending}
          onClick={() => inputRef.current?.click()}
        >
          {unggah.isPending ? <Loader2 className="animate-spin" /> : <Upload />}
          {sudahAda ? "Ganti" : "Unggah PDF"}
        </Button>
      </div>

      {pratinjau && (
        <PratinjauNaskah
          jenis={jenis}
          judul={judul}
          onClose={() => setPratinjau(false)}
        />
      )}
    </div>
  );
}

/**
 * Dipisah jadi komponen sendiri supaya berkasnya baru diambil saat pratinjau
 * dibuka — naskah disertasi berukuran puluhan megabyte, dan mengambilnya lebih
 * awal berarti tiap kunjungan ke halaman ini menyeretnya tanpa diminta.
 */
function PratinjauNaskah({
  jenis,
  judul,
  onClose,
}: {
  jenis: JenisBerkasDisertasi;
  judul: string;
  onClose: () => void;
}) {
  const berkas = useNaskahSendiri(jenis);

  return <PratinjauBerkas judul={judul} berkas={berkas} onClose={onClose} />;
}
