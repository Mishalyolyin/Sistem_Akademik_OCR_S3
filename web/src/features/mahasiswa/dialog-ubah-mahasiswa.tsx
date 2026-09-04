"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useClasses } from "@/features/kelas/api";
import { ApiError } from "@/lib/api";
import { useUpdateStudent, type StudentSummary } from "./api";

/**
 * Ubah data dasar mahasiswa.
 *
 * <p>Sebelum ada ini, satu huruf yang salah saat import hanya bisa dibetulkan
 * lewat basis data langsung — endpointnya sudah ada sejak lama, tapi tidak ada
 * satu pun tombol yang memanggilnya.
 *
 * <p>Golongan potongan sengaja tidak di sini: perubahannya punya aturan
 * tersendiri (terkunci setelah mahasiswa mengunggah bukti) dan sudah punya
 * jalurnya sendiri di halaman detail.
 */
export function DialogUbahMahasiswa({
  open,
  onOpenChange,
  mahasiswa,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mahasiswa: StudentSummary;
}) {
  const ubah = useUpdateStudent();
  const kelas = useClasses();

  const [nama, setNama] = useState(mahasiswa.name);
  const [telepon, setTelepon] = useState(mahasiswa.phone ?? "");
  const [kelasId, setKelasId] = useState<string>(
    mahasiswa.studyClassId ? String(mahasiswa.studyClassId) : "",
  );
  const [aktif, setAktif] = useState(mahasiswa.active);

  const bolehSimpan = nama.trim().length > 0 && !ubah.isPending;

  function simpan() {
    ubah.mutate(
      {
        id: mahasiswa.id,
        name: nama.trim(),
        phone: telepon.trim(),
        studyClassId: kelasId ? Number(kelasId) : undefined,
        active: aktif,
      },
      {
        onSuccess: (hasil) => {
          toast.success(`Data ${hasil.name} tersimpan.`);
          onOpenChange(false);
        },
        onError: (e) =>
          toast.error(
            e instanceof ApiError ? e.message : "Gagal menyimpan perubahan.",
          ),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Ubah data mahasiswa</DialogTitle>
          <DialogDescription>{mahasiswa.nim}</DialogDescription>
        </DialogHeader>

        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (bolehSimpan) simpan();
          }}
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="nama-mahasiswa">Nama</Label>
            <Input
              id="nama-mahasiswa"
              value={nama}
              aria-invalid={nama.trim().length === 0}
              onChange={(event) => setNama(event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="telepon-mahasiswa">Telepon</Label>
            <Input
              id="telepon-mahasiswa"
              placeholder="081234567890"
              value={telepon}
              onChange={(event) => setTelepon(event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="kelas-mahasiswa">Kelas</Label>
            <Select
              value={kelasId}
              onValueChange={(next) => setKelasId(next ?? "")}
            >
              <SelectTrigger id="kelas-mahasiswa">
                <SelectValue placeholder="Pilih kelas" />
              </SelectTrigger>
              <SelectContent>
                {(kelas.data ?? []).map((item) => (
                  <SelectItem key={item.id} value={String(item.id)}>
                    {item.displayName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <label className="flex items-start gap-2.5 rounded-md border border-border px-3 py-2.5 text-sm">
            <input
              type="checkbox"
              className="mt-0.5 size-4 accent-primary"
              checked={aktif}
              onChange={(event) => setAktif(event.target.checked)}
            />
            <span>
              Mahasiswa aktif
              <span className="block text-xs text-muted-foreground">
                Yang dinonaktifkan tetap tersimpan beserta seluruh riwayatnya,
                hanya tidak lagi ikut di daftar aktif.
              </span>
            </span>
          </label>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Batal
            </Button>
            <Button type="submit" disabled={!bolehSimpan}>
              {ubah.isPending && <Loader2 className="animate-spin" />}
              Simpan
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
