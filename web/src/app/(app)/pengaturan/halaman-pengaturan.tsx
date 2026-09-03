"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Check, PencilLine, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  ErrorState,
  PageHeader,
  TableSkeleton,
} from "@/components/page-header";
import {
  useSettings,
  useUpdateSetting,
  type SystemSetting,
} from "@/features/pengaturan/api";
import { ApiError } from "@/lib/api";

/** Nama yang lebih manusiawi daripada key mentah di database. */
const label: Record<string, string> = {
  ocr_confidence_threshold: "Ambang verifikasi otomatis (%)",
  ocr_auto_reject_threshold: "Ambang tolak otomatis (%)",
  payment_tolerance_amount: "Toleransi selisih nominal (Rp)",
  ocr_blacklist_keywords: "Kata kunci terlarang",
  ocr_date_validation_days: "Batas umur tanggal bukti (hari)",
  bank_account_number: "Rekening tujuan",
  admin_phone_notification: "WhatsApp admin",
};

const kelompok: Record<string, string[]> = {
  ocr: [
    "ocr_confidence_threshold",
    "ocr_auto_reject_threshold",
    "payment_tolerance_amount",
    "ocr_blacklist_keywords",
    "ocr_date_validation_days",
  ],
  sistem: ["bank_account_number", "admin_phone_notification"],
};

export function HalamanPengaturan({
  bagian,
  judul,
  keterangan,
}: {
  bagian: "ocr" | "sistem";
  judul: string;
  keterangan: string;
}) {
  const { data, isPending, error } = useSettings();

  const daftar = (data ?? []).filter((s) =>
    kelompok[bagian].includes(s.key),
  );

  return (
    <div className="flex flex-col gap-5 p-6">
      <PageHeader title={judul} description={keterangan} />

      {isPending ? (
        <TableSkeleton rows={5} />
      ) : error ? (
        <ErrorState
          message={
            error instanceof ApiError ? error.message : "Coba muat ulang halaman."
          }
        />
      ) : (
        <ul className="flex flex-col divide-y divide-border rounded-lg border border-border bg-card">
          {daftar.map((setting) => (
            <BarisPengaturan key={setting.key} setting={setting} />
          ))}
        </ul>
      )}

      {bagian === "ocr" && (
        <p className="rounded-lg border border-info/25 bg-info-soft px-4 py-3 text-sm text-info">
          Bukti dengan keyakinan di atas ambang verifikasi dan nominal cocok akan
          diterima otomatis. Di bawah ambang tolak, bukti langsung ditolak karena
          kemungkinan besar bukan bukti bayar. Di antara keduanya masuk antrean
          tinjauan admin.
        </p>
      )}
    </div>
  );
}

function BarisPengaturan({ setting }: { setting: SystemSetting }) {
  const ubah = useUpdateSetting();
  const [sedangEdit, setSedangEdit] = useState(false);
  const [nilai, setNilai] = useState(setting.value ?? "");

  function simpan() {
    ubah.mutate(
      { key: setting.key, value: nilai },
      {
        onSuccess: () => {
          toast.success(`${label[setting.key] ?? setting.key} disimpan.`);
          setSedangEdit(false);
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal menyimpan pengaturan.",
          ),
      },
    );
  }

  return (
    <li className="flex flex-wrap items-center justify-between gap-4 px-5 py-3.5">
      <div className="min-w-0 flex-1">
        <p className="font-medium">{label[setting.key] ?? setting.key}</p>
        {setting.description && (
          <p className="text-sm text-muted-foreground">{setting.description}</p>
        )}
      </div>

      <div className="flex items-center gap-2">
        {sedangEdit ? (
          <>
            <Input
              autoFocus
              value={nilai}
              onChange={(event) => setNilai(event.target.value)}
              className="h-8 w-64"
              aria-label={label[setting.key] ?? setting.key}
            />
            <Button
              size="icon-sm"
              onClick={simpan}
              disabled={ubah.isPending}
              aria-label="Simpan"
            >
              <Check />
            </Button>
            <Button
              size="icon-sm"
              variant="outline"
              onClick={() => {
                setNilai(setting.value ?? "");
                setSedangEdit(false);
              }}
              aria-label="Batal"
            >
              <X />
            </Button>
          </>
        ) : (
          <>
            <span className="max-w-64 truncate font-mono text-sm">
              {setting.value || (
                <span className="text-muted-foreground italic">kosong</span>
              )}
            </span>
            <Button
              size="icon-sm"
              variant="ghost"
              onClick={() => setSedangEdit(true)}
              aria-label={`Ubah ${label[setting.key] ?? setting.key}`}
            >
              <PencilLine />
            </Button>
          </>
        )}
      </div>
    </li>
  );
}
