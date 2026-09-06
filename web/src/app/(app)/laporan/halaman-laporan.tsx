"use client";

import { useState } from "react";
import { toast } from "sonner";
import {
  Download,
  FileSpreadsheet,
  Images,
  ImageDown,
  Loader2,
  ShieldAlert,
  Table2,
  Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PageHeader } from "@/components/page-header";
import { unduhBerkas } from "@/features/pengaturan/api";
import { useClasses } from "@/features/kelas/api";
import { useGolongan } from "@/features/tarif/api";

const SEMUA = "semua";

/** Status pembayaran yang masuk akal dipakai sebagai penyaring laporan. */
const statusPembayaran = [
  { nilai: "VERIFIED", label: "Terverifikasi admin" },
  { nilai: "AUTO_VERIFIED", label: "Terverifikasi otomatis" },
  { nilai: "NEEDS_REVIEW", label: "Perlu ditinjau" },
  { nilai: "PENDING", label: "Menunggu dibaca" },
  { nilai: "REJECTED", label: "Ditolak" },
  { nilai: "FAILED", label: "Gagal dibaca" },
];

function query(params: Record<string, string | number | undefined>) {
  const sp = new URLSearchParams();
  for (const [kunci, nilai] of Object.entries(params)) {
    if (nilai !== undefined && nilai !== "" && nilai !== SEMUA) {
      sp.set(kunci, String(nilai));
    }
  }
  const teks = sp.toString();
  return teks ? `?${teks}` : "";
}

export function HalamanLaporan() {
  const kelas = useClasses();
  const golongan = useGolongan();
  const [mengunduh, setMengunduh] = useState<string | null>(null);

  // Penyaring laporan pembayaran.
  const [kelasBayar, setKelasBayar] = useState(SEMUA);
  const [status, setStatus] = useState(SEMUA);
  const [format, setFormat] = useState<"TRANSAKSI" | "TERMIN">("TRANSAKSI");

  // Penyaring data mahasiswa.
  const [kelasMhs, setKelasMhs] = useState(SEMUA);
  const [tier, setTier] = useState(SEMUA);

  // Penyaring foto.
  const [kelasFoto, setKelasFoto] = useState(SEMUA);

  // Mati secara bawaan, dan sengaja tidak diingat antar kunjungan: menyertakan
  // teks struk harus jadi keputusan sadar tiap kali, bukan setelan yang
  // terlanjur menyala.
  const [sertakanTeks, setSertakanTeks] = useState(false);

  async function unduh(id: string, path: string, berkas: string, nama: string) {
    setMengunduh(id);
    try {
      await unduhBerkas(path, berkas);
      toast.success(`${nama} diunduh.`);
    } catch {
      toast.error("Berkas gagal diunduh.");
    } finally {
      setMengunduh(null);
    }
  }

  const daftarKelas = (kelas.data ?? []).filter((k) => k.active);

  return (
    <div className="flex flex-col gap-5 p-6">
      <PageHeader
        title="Laporan"
        description="Unduh rekap tagihan, data mahasiswa, dan dataset pembacaan bukti"
      />

      <div className="flex flex-col gap-3">
        {/* --- Laporan pembayaran --- */}
        <KartuLaporan
          icon={FileSpreadsheet}
          warnaIkon="text-success"
          nama="Laporan Pembayaran"
          keterangan={
            format === "TERMIN"
              ? "Ledger termin: satu lembar per semester UKT, satu baris per mahasiswa, dengan sepasang kolom tanggal dan jumlah untuk tiap angsuran. Yang dihitung hanya pembayaran yang sudah diverifikasi."
              : "Tiga lembar: ringkasan per kelas, tagihan per mahasiswa, dan riwayat seluruh pembayaran."
          }
          tombol="Unduh Excel"
          sedang={mengunduh === "pembayaran"}
          onUnduh={() =>
            unduh(
              "pembayaran",
              `reports/pembayaran.xlsx${query({ classId: kelasBayar, status, format })}`,
              "laporan-pembayaran.xlsx",
              "Laporan Pembayaran",
            )
          }
        >
          <Penyaring>
            <Pilihan
              id="format-laporan"
              label="Format"
              nilai={format}
              onGanti={(v) => setFormat(v as "TRANSAKSI" | "TERMIN")}
              pilihan={[
                { nilai: "TRANSAKSI", label: "Daftar transaksi" },
                { nilai: "TERMIN", label: "Ledger termin" },
              ]}
            />
            <Pilihan
              id="kelas-laporan"
              label="Kelas"
              nilai={kelasBayar}
              onGanti={setKelasBayar}
              pilihan={[
                { nilai: SEMUA, label: "Semua kelas" },
                ...daftarKelas.map((k) => ({
                  nilai: String(k.id),
                  label: k.displayName,
                })),
              ]}
            />
            {/*
              Status hanya menyaring lembar riwayat pembayaran, dan pada format
              termin tidak berlaku sama sekali — ledger memang hanya menghitung
              yang terverifikasi. Menampilkannya dalam keadaan mati lebih jujur
              daripada membiarkannya tampak berpengaruh padahal tidak.
            */}
            <Pilihan
              id="status-laporan"
              label="Status pembayaran"
              nilai={status}
              onGanti={setStatus}
              nonaktif={format === "TERMIN"}
              hint={
                format === "TERMIN"
                  ? "Ledger termin selalu hanya menghitung yang terverifikasi"
                  : "Menyaring lembar riwayat pembayaran"
              }
              pilihan={[
                { nilai: SEMUA, label: "Semua status" },
                ...statusPembayaran.map((s) => ({
                  nilai: s.nilai,
                  label: s.label,
                })),
              ]}
            />
          </Penyaring>
        </KartuLaporan>

        {/* --- Data mahasiswa --- */}
        <KartuLaporan
          icon={Users}
          warnaIkon="text-primary"
          nama="Data Mahasiswa"
          keterangan="Identitas, kelas, golongan, dan hasil pemeriksaan tiap dokumen wajib — apakah NIK yang terbaca di KTP cocok dengan yang diketik, nama di ijazah cocok, latar foto merah. Kolom yang berguna untuk mencari berkas siapa yang perlu dilihat manusia."
          tombol="Unduh Excel"
          sedang={mengunduh === "mahasiswa"}
          onUnduh={() =>
            unduh(
              "mahasiswa",
              `reports/mahasiswa.xlsx${query({ classId: kelasMhs, tier })}`,
              "data-mahasiswa.xlsx",
              "Data Mahasiswa",
            )
          }
        >
          <Penyaring>
            <Pilihan
              id="kelas-mahasiswa"
              label="Kelas"
              nilai={kelasMhs}
              onGanti={setKelasMhs}
              pilihan={[
                { nilai: SEMUA, label: "Semua kelas" },
                ...daftarKelas.map((k) => ({
                  nilai: String(k.id),
                  label: k.displayName,
                })),
              ]}
            />
            <Pilihan
              id="golongan-mahasiswa"
              label="Golongan"
              nilai={tier}
              onGanti={setTier}
              pilihan={[
                { nilai: SEMUA, label: "Semua golongan" },
                ...golongan.daftar.map((g) => ({
                  nilai: g.tier,
                  label: g.label,
                })),
              ]}
            />
          </Penyaring>
        </KartuLaporan>

        {/* --- Foto mahasiswa --- */}
        <KartuLaporan
          icon={ImageDown}
          warnaIkon="text-primary"
          nama="Foto Mahasiswa"
          keterangan="ZIP foto profil, dinamai NIM_Nama dan dikelompokkan per folder kelas. Untuk mencetak kartu mahasiswa dan berkas wisuda. Mahasiswa yang fotonya tercatat tapi berkasnya raib ikut didaftar di dalam ZIP."
          tombol="Unduh ZIP"
          sedang={mengunduh === "foto"}
          onUnduh={() =>
            unduh(
              "foto",
              `reports/foto-mahasiswa.zip${query({ classId: kelasFoto })}`,
              "foto-mahasiswa.zip",
              "Foto Mahasiswa",
            )
          }
        >
          <Penyaring>
            <Pilihan
              id="kelas-foto"
              label="Kelas"
              nilai={kelasFoto}
              onGanti={setKelasFoto}
              pilihan={[
                { nilai: SEMUA, label: "Semua kelas" },
                ...daftarKelas.map((k) => ({
                  nilai: String(k.id),
                  label: k.displayName,
                })),
              ]}
            />
          </Penyaring>
        </KartuLaporan>

        {/* --- Dataset angka --- */}
        <KartuLaporan
          icon={Table2}
          warnaIkon="text-primary"
          nama="Dataset Pembacaan Bukti (angka)"
          keterangan="Satu baris per bukti bayar yang sudah diputuskan admin: apa yang dibaca mesin disandingkan dengan keputusan akhirnya. Bahan untuk mengukur ambang keyakinan. Verifikasi otomatis yang belum disentuh admin tidak ikut — itu tebakan mesin sendiri, bukan jawaban."
          tombol="Unduh CSV"
          sedang={mengunduh === "dataset-csv"}
          onUnduh={() =>
            unduh(
              "dataset-csv",
              `reports/dataset-ocr.csv${query({ sertakanTeks: sertakanTeks ? "true" : undefined })}`,
              "dataset-ocr.csv",
              "Dataset Pembacaan Bukti",
            )
          }
        >
          <div className="mt-3 flex max-w-xl items-start gap-2.5 rounded-md border border-warning/40 bg-warning/10 px-3 py-2">
            <Checkbox
              id="sertakan-teks"
              checked={sertakanTeks}
              onCheckedChange={(nilai) => setSertakanTeks(nilai === true)}
              className="mt-0.5"
            />
            <Label
              htmlFor="sertakan-teks"
              className="flex flex-col items-start gap-0.5 font-normal"
            >
              <span className="flex items-center gap-1.5 font-medium">
                <ShieldAlert className="size-3.5 text-warning" />
                Sertakan teks mentah hasil OCR
              </span>
              <span className="text-xs text-muted-foreground">
                Fitur paling berguna untuk model, tapi isinya seluruh tulisan di
                struk — nama, nomor rekening, dan saldo. Jangan sebar berkasnya.
              </span>
            </Label>
          </div>
        </KartuLaporan>

        {/* --- Dataset gambar --- */}
        <KartuLaporan
          icon={Images}
          warnaIkon="text-primary"
          nama="Dataset Pembacaan Bukti (gambar)"
          keterangan="ZIP berisi gambar hasil praproses OpenCV — yang benar-benar dibaca Tesseract — beserta label.csv. Bahan untuk melatih model penglihatan. Bukti yang masuk sebelum penyimpanan gambar dinyalakan tidak punya gambarnya dan dilewati."
          tombol="Unduh ZIP"
          sedang={mengunduh === "dataset-zip"}
          onUnduh={() =>
            unduh(
              "dataset-zip",
              "reports/dataset-ocr.zip",
              "dataset-ocr-gambar.zip",
              "Dataset Gambar",
            )
          }
        >
          <p className="mt-3 max-w-xl text-xs text-muted-foreground">
            Berapa banyak gambar yang sudah terkumpul bisa dilihat di{" "}
            <a
              href="/pengaturan/ocr"
              className="underline underline-offset-2 hover:text-foreground"
            >
              Pengaturan → OCR &amp; Verifikasi
            </a>
            .
          </p>
        </KartuLaporan>
      </div>

      <p className="text-sm text-muted-foreground">
        Kuitansi PDF per pembayaran diunduh dari panel verifikasi, pada
        pembayaran yang sudah diverifikasi.
      </p>
    </div>
  );
}

function KartuLaporan({
  icon: Icon,
  warnaIkon,
  nama,
  keterangan,
  tombol,
  sedang,
  onUnduh,
  children,
}: {
  icon: React.ComponentType<{ className?: string }>;
  warnaIkon: string;
  nama: string;
  keterangan: string;
  tombol: string;
  sedang: boolean;
  onUnduh: () => void;
  children?: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-border/70 bg-card shadow-sm px-5 py-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex min-w-0 items-start gap-3">
          <Icon className={`mt-0.5 size-5 shrink-0 ${warnaIkon}`} />
          <div className="min-w-0">
            <p className="font-medium">{nama}</p>
            <p className="max-w-2xl text-sm text-muted-foreground">
              {keterangan}
            </p>
          </div>
        </div>

        <Button variant="outline" disabled={sedang} onClick={onUnduh}>
          {sedang ? <Loader2 className="animate-spin" /> : <Download />}
          {tombol}
        </Button>
      </div>

      {children && <div className="pl-8">{children}</div>}
    </section>
  );
}

function Penyaring({ children }: { children: React.ReactNode }) {
  return (
    <div className="mt-3 flex flex-wrap gap-3">{children}</div>
  );
}

function Pilihan({
  id,
  label,
  nilai,
  onGanti,
  pilihan,
  nonaktif,
  hint,
}: {
  id: string;
  label: string;
  nilai: string;
  onGanti: (nilai: string) => void;
  pilihan: { nilai: string; label: string }[];
  nonaktif?: boolean;
  hint?: string;
}) {
  return (
    <div className="flex min-w-48 flex-col gap-1.5">
      <Label htmlFor={id} className="text-xs">
        {label}
      </Label>
      <Select
        value={nilai}
        onValueChange={(v) => onGanti(String(v))}
        disabled={nonaktif}
      >
        <SelectTrigger id={id} size="sm">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {pilihan.map((p) => (
            <SelectItem key={p.nilai} value={p.nilai}>
              {p.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
    </div>
  );
}
