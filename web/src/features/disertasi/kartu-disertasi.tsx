"use client";

import { useState } from "react";
import { BookOpen, Eye, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PratinjauBerkas } from "@/components/pratinjau-berkas";
import {
  useDisertasiMahasiswa,
  useNaskahMahasiswa,
  type JenisBerkasDisertasi,
} from "./api";

/**
 * Disertasi seorang mahasiswa, dilihat admin.
 *
 * <p>Ini yang menjawab pertanyaan yang selama ini tidak bisa dijawab sistem
 * saat admin memverifikasi bukti bayar tahap ujian: disertasi mana yang sedang
 * diuji, dan siapa yang membimbingnya. Sebelumnya yang terlihat hanya nominal.
 */
export function KartuDisertasi({ studentId }: { studentId: number }) {
  const { data, isPending } = useDisertasiMahasiswa(studentId);
  const [naskah, setNaskah] = useState<JenisBerkasDisertasi | null>(null);

  if (isPending) return null;

  return (
    <section className="rounded-2xl border border-border/70 bg-card shadow-sm px-5 py-4">
      <div className="flex items-start gap-3">
        <BookOpen className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden />

        {!data ? (
          <div>
            <h3 className="font-heading text-sm font-semibold">Disertasi</h3>
            <p className="text-sm text-muted-foreground">
              Belum diisi. Mahasiswa mengisinya sendiri di portal, dan itu jadi
              syarat sebelum ia bisa mendaftar tahap ujian.
            </p>
          </div>
        ) : (
          <div className="min-w-0 flex-1">
            <h3 className="font-heading text-sm font-semibold">Disertasi</h3>
            <p className="mt-0.5 text-sm">{data.title}</p>

            <dl className="mt-2 grid gap-x-6 gap-y-1 text-sm sm:grid-cols-2">
              <div className="flex gap-2">
                <dt className="text-muted-foreground">Promotor</dt>
                <dd className="min-w-0 truncate font-medium">{data.promotor}</dd>
              </div>
              <div className="flex gap-2">
                <dt className="text-muted-foreground">Ko-promotor</dt>
                <dd className="min-w-0 truncate font-medium">
                  {data.copromotor}
                </dd>
              </div>
            </dl>

            <div className="mt-3 flex flex-wrap gap-2">
              <TombolNaskah
                ada={data.adaNaskah}
                label="Naskah disertasi"
                onBuka={() => setNaskah("DISERTASI")}
              />
              <TombolNaskah
                ada={data.adaArtikel}
                label="Artikel jurnal"
                onBuka={() => setNaskah("ARTIKEL")}
              />
            </div>
          </div>
        )}
      </div>

      {naskah && (
        <PratinjauNaskah
          studentId={studentId}
          jenis={naskah}
          onClose={() => setNaskah(null)}
        />
      )}
    </section>
  );
}

/**
 * Naskah yang belum diunggah tetap ditampilkan sebagai keterangan, bukan
 * disembunyikan — admin perlu tahu bedanya "belum ada" dan "tidak ada tombolnya".
 */
function TombolNaskah({
  ada,
  label,
  onBuka,
}: {
  ada: boolean;
  label: string;
  onBuka: () => void;
}) {
  if (!ada) {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-md border border-dashed border-border px-2.5 py-1 text-xs text-muted-foreground">
        <FileText className="size-3.5" aria-hidden />
        {label} belum diunggah
      </span>
    );
  }

  return (
    <Button variant="outline" size="sm" onClick={onBuka}>
      <Eye />
      {label}
    </Button>
  );
}

function PratinjauNaskah({
  studentId,
  jenis,
  onClose,
}: {
  studentId: number;
  jenis: JenisBerkasDisertasi;
  onClose: () => void;
}) {
  const berkas = useNaskahMahasiswa(studentId, jenis);

  return (
    <PratinjauBerkas
      judul={jenis === "DISERTASI" ? "Naskah disertasi" : "Artikel jurnal"}
      berkas={berkas}
      onClose={onClose}
    />
  );
}
