"use client";

import { useState } from "react";
import { AlertTriangle, Check, Eye, HelpCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PratinjauBerkas } from "@/components/pratinjau-berkas";
import { formatTanggal } from "@/lib/format";
import {
  dokumenLabels,
  useDokumenMahasiswa,
  usePemeriksaanDokumen,
  type HasilPeriksaDokumen,
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
  const periksa = usePemeriksaanDokumen(mahasiswa.id);

  const hasilPer = new Map(
    (periksa.data ?? []).map((hasil) => [hasil.jenis, hasil]),
  );

  return (
    <section className="flex flex-col gap-3">
      <h3 className="font-heading text-sm font-semibold">Dokumen wajib</h3>

      <div className="grid gap-3 sm:grid-cols-2">
        <div className="rounded-2xl border border-border/70 bg-card shadow-sm">
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
              <dt className="text-muted-foreground">Tempat, tanggal lahir</dt>
              <dd className="text-right">
                {mahasiswa.birthPlace || mahasiswa.birthDate ? (
                  <>
                    {mahasiswa.birthPlace ?? "—"}
                    {mahasiswa.birthDate
                      ? `, ${formatTanggal(mahasiswa.birthDate)}`
                      : ""}
                  </>
                ) : (
                  <span className="text-muted-foreground">
                    belum terbaca dari ijazah
                  </span>
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

        <ul className="flex flex-col divide-y divide-border rounded-2xl border border-border/70 bg-card shadow-sm">
          {URUTAN.map((jenis) => {
            const ada = mahasiswa.dokumenTersedia.includes(jenis);
            return (
              <li
                key={jenis}
                className="flex items-start justify-between gap-3 px-4 py-2"
              >
                <div className="min-w-0">
                  <span className={ada ? undefined : "text-muted-foreground"}>
                    {dokumenLabels[jenis]}
                  </span>
                  {ada && <Kesimpulan hasil={hasilPer.get(jenis)} />}
                </div>
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

/**
 * Kesimpulan pembacaan satu dokumen.
 *
 * <p>Tiga keadaan, bukan dua. "Tidak cocok" berarti mesin membaca sesuatu yang
 * berbeda dan dokumennya layak dilihat; "tidak bisa disimpulkan" berarti
 * mesinnya yang gagal membaca, dan itu bukan alasan mencurigai mahasiswanya.
 * Menyamakan keduanya membuat admin curiga pada dokumen yang baik-baik saja.
 */
function Kesimpulan({ hasil }: { hasil?: HasilPeriksaDokumen }) {
  if (!hasil || !hasil.sudahDibaca) {
    return (
      <span className="block text-xs text-muted-foreground">
        Belum dibaca mesin.
      </span>
    );
  }

  const nada =
    hasil.cocok === true
      ? "text-success"
      : hasil.cocok === false
        ? "text-warning"
        : "text-muted-foreground";

  const Ikon =
    hasil.cocok === true ? Check : hasil.cocok === false ? AlertTriangle : HelpCircle;

  return (
    <span className={`flex items-start gap-1.5 text-xs ${nada}`}>
      <Ikon className="mt-0.5 size-3 shrink-0" />
      <span>{hasil.keterangan}</span>
    </span>
  );
}
