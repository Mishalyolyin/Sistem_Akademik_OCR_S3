"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Check, Eye, Upload } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ErrorState, TableSkeleton } from "@/components/page-header";
import { PratinjauBerkas } from "@/components/pratinjau-berkas";
import {
  jenisDokumenPerLangkah,
  useDokumenSendiri,
  useProfil,
  useSimpanAlamat,
  useUnggahDokumen,
  type DocumentStep,
  type JenisDokumen,
} from "@/features/portal/api";
import { ApiError } from "@/lib/api";

const langkah: {
  id: DocumentStep;
  judul: string;
  keterangan: string;
  nomor?: { label: string; placeholder: string };
}[] = [
  {
    id: "PHOTO",
    judul: "Foto profil",
    keterangan: "Foto diri terbaru, wajah terlihat jelas.",
  },
  {
    id: "KTP",
    judul: "KTP",
    keterangan: "Foto atau pindaian KTP yang masih berlaku.",
    nomor: { label: "Nomor KTP (NIK)", placeholder: "16 digit angka" },
  },
  {
    id: "KK",
    judul: "Kartu Keluarga",
    keterangan: "Foto atau pindaian Kartu Keluarga.",
    nomor: { label: "Nomor Kartu Keluarga", placeholder: "16 digit angka" },
  },
  {
    id: "IJAZAH",
    judul: "Ijazah",
    keterangan: "Ijazah pendidikan terakhir.",
  },
  {
    id: "ADDRESS",
    judul: "Alamat",
    keterangan: "Alamat tempat tinggal saat ini.",
  },
];

export function HalamanDokumen() {
  const { data: profil, isPending, error } = useProfil();
  const router = useRouter();
  const [dilihat, setDilihat] = useState<JenisDokumen | null>(null);

  if (isPending) return <TableSkeleton rows={5} />;
  if (error) {
    return (
      <ErrorState
        message={
          error instanceof ApiError ? error.message : "Coba muat ulang halaman."
        }
      />
    );
  }

  const aktifIndex = profil.langkahDokumenBerikutnya
    ? langkah.findIndex((l) => l.id === profil.langkahDokumenBerikutnya)
    : langkah.length;

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h2 className="font-heading text-xl font-semibold tracking-tight">
          Dokumen wajib
        </h2>
        <p className="text-sm text-muted-foreground">
          Diisi berurutan. Tagihan baru bisa diakses setelah semuanya lengkap.
        </p>
      </div>

      {profil.dokumenLengkap && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-success/25 bg-success-soft px-4 py-3">
          <p className="flex items-center gap-2 text-sm text-success">
            <Check className="size-4" />
            Semua dokumen sudah lengkap.
          </p>
          <Button size="sm" onClick={() => router.push("/portal")}>
            Lihat tagihan
          </Button>
        </div>
      )}

      <ol className="flex flex-col gap-3">
        {langkah.map((item, index) => {
          const selesai = index < aktifIndex;
          const aktif = index === aktifIndex;

          return (
            <li
              key={item.id}
              className={cn(
                "rounded-lg border p-4",
                aktif
                  ? "border-primary/40 bg-card"
                  : selesai
                    ? "border-border bg-card"
                    : "border-dashed border-border bg-muted/30",
              )}
            >
              <div className="flex items-start gap-3">
                <span
                  className={cn(
                    "flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold",
                    selesai
                      ? "bg-success text-success-foreground"
                      : aktif
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground",
                  )}
                >
                  {selesai ? <Check className="size-3.5" /> : index + 1}
                </span>

                <div className="min-w-0 flex-1">
                  <p className="font-medium">{item.judul}</p>
                  <p className="text-sm text-muted-foreground">
                    {item.keterangan}
                  </p>

                  {aktif && item.id !== "ADDRESS" && (
                    <FormUnggah
                      langkah={item.id as Exclude<DocumentStep, "ADDRESS">}
                      nomor={item.nomor}
                    />
                  )}
                  {aktif && item.id === "ADDRESS" && <FormAlamat />}
                </div>

                {selesai && (
                  <div className="flex shrink-0 items-center gap-2">
                    {item.id !== "ADDRESS" &&
                      profil.dokumenTersedia.includes(
                        jenisDokumenPerLangkah[
                          item.id as Exclude<DocumentStep, "ADDRESS">
                        ],
                      ) && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setDilihat(
                              jenisDokumenPerLangkah[
                                item.id as Exclude<DocumentStep, "ADDRESS">
                              ],
                            )
                          }
                        >
                          <Eye />
                          Lihat
                        </Button>
                      )}
                    <span className="text-xs font-medium text-success">
                      Selesai
                    </span>
                  </div>
                )}
              </div>
            </li>
          );
        })}
      </ol>

      {/* key: berkasnya terpasang ulang saat berpindah dokumen. */}
      {dilihat && (
        <PratinjauDokumenSendiri
          key={dilihat}
          jenis={dilihat}
          onClose={() => setDilihat(null)}
        />
      )}
    </div>
  );
}

function PratinjauDokumenSendiri({
  jenis,
  onClose,
}: {
  jenis: JenisDokumen;
  onClose: () => void;
}) {
  const berkas = useDokumenSendiri(jenis);

  return (
    <PratinjauBerkas
      judul={labelDokumen[jenis]}
      berkas={berkas}
      onClose={onClose}
    />
  );
}

const labelDokumen: Record<JenisDokumen, string> = {
  foto: "Foto profil",
  ktp: "KTP",
  kk: "Kartu Keluarga",
  ijazah: "Ijazah",
};

function FormUnggah({
  langkah,
  nomor,
}: {
  langkah: Exclude<DocumentStep, "ADDRESS">;
  nomor?: { label: string; placeholder: string };
}) {
  const unggah = useUnggahDokumen();
  const fileInput = useRef<HTMLInputElement>(null);
  const [nilaiNomor, setNilaiNomor] = useState("");
  const [namaBerkas, setNamaBerkas] = useState<string | null>(null);

  function kirim() {
    const file = fileInput.current?.files?.[0];
    if (!file) {
      toast.error("Pilih berkasnya dulu.");
      return;
    }

    unggah.mutate(
      { langkah, file, nomor: nilaiNomor },
      {
        onSuccess: () => toast.success("Tersimpan."),
        onError: (e) =>
          toast.error(e instanceof ApiError ? e.message : "Gagal mengunggah."),
      },
    );
  }

  return (
    <div className="mt-3 flex flex-col gap-3">
      {nomor && (
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`nomor-${langkah}`}>{nomor.label}</Label>
          <Input
            id={`nomor-${langkah}`}
            inputMode="numeric"
            placeholder={nomor.placeholder}
            value={nilaiNomor}
            onChange={(event) => setNilaiNomor(event.target.value)}
            className="max-w-xs"
          />
        </div>
      )}

      <div className="flex flex-wrap items-center gap-2">
        <input
          ref={fileInput}
          type="file"
          accept=".jpg,.jpeg,.png,.webp,.pdf"
          className="hidden"
          onChange={(event) =>
            setNamaBerkas(event.target.files?.[0]?.name ?? null)
          }
        />
        <Button
          variant="outline"
          size="sm"
          onClick={() => fileInput.current?.click()}
        >
          <Upload />
          Pilih berkas
        </Button>
        {namaBerkas && (
          <span className="truncate text-sm text-muted-foreground">
            {namaBerkas}
          </span>
        )}
        <Button size="sm" onClick={kirim} loading={unggah.isPending}>
          Simpan
        </Button>
      </div>

      <p className="text-xs text-muted-foreground">
        JPG, PNG, WEBP, atau PDF. Maksimal 10 MB.
      </p>
    </div>
  );
}

function FormAlamat() {
  const simpan = useSimpanAlamat();
  const [alamat, setAlamat] = useState("");
  const [telepon, setTelepon] = useState("");

  return (
    <div className="mt-3 flex flex-col gap-3">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="alamat">Alamat lengkap</Label>
        <Textarea
          id="alamat"
          rows={3}
          value={alamat}
          onChange={(event) => setAlamat(event.target.value)}
          placeholder="Jalan, nomor, kelurahan, kecamatan, kota, kode pos"
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="telepon">Nomor WhatsApp</Label>
        <Input
          id="telepon"
          inputMode="tel"
          value={telepon}
          onChange={(event) => setTelepon(event.target.value)}
          placeholder="08…"
          className="max-w-xs"
        />
      </div>

      <Button
        size="sm"
        className="w-fit"
        loading={simpan.isPending}
        onClick={() =>
          simpan.mutate(
            { alamat, telepon: telepon || undefined },
            {
              onSuccess: () => toast.success("Dokumen wajib sudah lengkap."),
              onError: (e) =>
                toast.error(
                  e instanceof ApiError ? e.message : "Gagal menyimpan.",
                ),
            },
          )
        }
      >
        Simpan
      </Button>
    </div>
  );
}
