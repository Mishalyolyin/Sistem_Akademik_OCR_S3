"use client";

import { useState } from "react";
import { Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PratinjauBerkas } from "@/components/pratinjau-berkas";
import {
  dokumenLabels,
  useDokumenMahasiswa,
  type JenisDokumen,
  type StudentSummary,
} from "./api";

const URUTAN: JenisDokumen[] = ["foto", "ktp", "kk", "ijazah"];

/**
 * Dokumen wajib mahasiswa beserta tombol untuk membukanya.
 *
 * Sebelum ada ini, berkasnya hanya bisa diunggah dan tidak pernah bisa dilihat
 * lagi oleh siapa pun — jadi kelengkapan dokumen tercatat tanpa ada yang benar-
 * benar memeriksa isinya.
 */
export function KartuDokumen({ mahasiswa }: { mahasiswa: StudentSummary }) {
  const [dibuka, setDibuka] = useState<JenisDokumen | null>(null);

  return (
    <section className="flex flex-col gap-3">
      <h3 className="font-heading text-sm font-semibold">Dokumen wajib</h3>

      <div className="grid gap-3 sm:grid-cols-2">
        <div className="rounded-lg border border-border bg-card">
          <dl className="flex flex-col divide-y divide-border text-sm">
            <div className="flex items-center justify-between gap-3 px-4 py-2.5">
              <dt className="text-muted-foreground">NIK</dt>
              <dd className="tnum">
                {mahasiswa.nik ?? (
                  <span className="text-muted-foreground">belum diisi</span>
                )}
              </dd>
            </div>
            <div className="flex items-center justify-between gap-3 px-4 py-2.5">
              <dt className="text-muted-foreground">Nomor Kartu Keluarga</dt>
              <dd className="tnum">
                {mahasiswa.kkNumber ?? (
                  <span className="text-muted-foreground">belum diisi</span>
                )}
              </dd>
            </div>
            <div className="flex items-center justify-between gap-3 px-4 py-2.5">
              <dt className="text-muted-foreground">Alamat</dt>
              <dd className="text-right">
                {mahasiswa.documentsComplete ? (
                  "Sudah diisi"
                ) : (
                  <span className="text-muted-foreground">belum lengkap</span>
                )}
              </dd>
            </div>
          </dl>
        </div>

        <ul className="flex flex-col divide-y divide-border rounded-lg border border-border bg-card">
          {URUTAN.map((jenis) => {
            const ada = mahasiswa.dokumenTersedia.includes(jenis);
            return (
              <li
                key={jenis}
                className="flex items-center justify-between gap-3 px-4 py-2"
              >
                <span className={ada ? undefined : "text-muted-foreground"}>
                  {dokumenLabels[jenis]}
                </span>
                {ada ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setDibuka(jenis)}
                  >
                    <Eye />
                    Lihat
                  </Button>
                ) : (
                  <span className="text-xs text-muted-foreground">
                    belum diunggah
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      </div>

      {/* key: berkasnya terpasang ulang saat berpindah dokumen, jadi tidak ada
          sisa gambar sebelumnya yang sempat terlihat. */}
      {dibuka && (
        <PratinjauDokumen
          key={dibuka}
          studentId={mahasiswa.id}
          nama={mahasiswa.name}
          jenis={dibuka}
          onClose={() => setDibuka(null)}
        />
      )}
    </section>
  );
}

function PratinjauDokumen({
  studentId,
  nama,
  jenis,
  onClose,
}: {
  studentId: number;
  nama: string;
  jenis: JenisDokumen;
  onClose: () => void;
}) {
  const berkas = useDokumenMahasiswa(studentId, jenis);

  return (
    <PratinjauBerkas
      judul={dokumenLabels[jenis]}
      keterangan={nama}
      berkas={berkas}
      onClose={onClose}
    />
  );
}
