"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Handshake, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  EmptyState,
  ErrorState,
  PageHeader,
  TableSkeleton,
} from "@/components/page-header";
import {
  useClasses,
  useCreateClass,
  useDeleteClass,
  type StudyClass,
} from "@/features/kelas/api";
import { ApiError } from "@/lib/api";

const schema = z.object({
  name: z.string().trim().min(1, "Nama kelas wajib diisi."),
  academicYear: z
    .string()
    .regex(/^\d{4}\/\d{4}$/, "Format tahun akademik harus 2026/2027."),
  kerjasama: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

export function HalamanKelas() {
  const { data, isPending, error } = useClasses();
  const [dialogTerbuka, setDialogTerbuka] = useState(false);

  return (
    <div className="flex flex-col gap-5 p-6">
      <PageHeader
        title="Kelas"
        description="Jumlah kelas tidak dipatok. Kelas juga dibuat otomatis saat import Excel."
      >
        <Button onClick={() => setDialogTerbuka(true)}>
          <Plus />
          Tambah kelas
        </Button>
      </PageHeader>

      {isPending ? (
        <TableSkeleton />
      ) : error ? (
        <ErrorState
          message={error instanceof ApiError ? error.message : "Coba muat ulang halaman."}
        />
      ) : data.length === 0 ? (
        <EmptyState
          title="Belum ada kelas"
          description="Tambah kelas di sini, atau unggah Excel mahasiswa — kelas yang belum ada akan dibuat otomatis."
        />
      ) : (
        <TabelKelas data={data} />
      )}

      <DialogTambahKelas open={dialogTerbuka} onOpenChange={setDialogTerbuka} />
    </div>
  );
}

function TabelKelas({ data }: { data: StudyClass[] }) {
  const hapus = useDeleteClass();

  return (
    <div className="overflow-x-auto rounded-lg border border-border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Kelas</TableHead>
            <TableHead>Jenis</TableHead>
            <TableHead>Tahun akademik</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="w-10" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((kelas) => (
            <TableRow key={kelas.id}>
              <TableCell className="font-medium">{kelas.displayName}</TableCell>
              <TableCell>
                {kelas.kerjasama ? (
                  <span className="inline-flex items-center gap-1.5 rounded-full border border-info/25 bg-info-soft px-2 py-0.5 text-xs font-medium text-info">
                    <Handshake className="size-3" />
                    Kerjasama
                  </span>
                ) : (
                  <span className="text-muted-foreground">Reguler</span>
                )}
              </TableCell>
              <TableCell className="text-muted-foreground">
                {kelas.academicYear}
              </TableCell>
              <TableCell>
                <span
                  className={
                    kelas.active
                      ? "text-success"
                      : "text-muted-foreground"
                  }
                >
                  {kelas.active ? "Aktif" : "Nonaktif"}
                </span>
              </TableCell>
              <TableCell>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  aria-label={`Hapus ${kelas.displayName}`}
                  disabled={hapus.isPending}
                  onClick={() =>
                    hapus.mutate(kelas.id, {
                      onSuccess: () =>
                        toast.success(`${kelas.displayName} dihapus.`),
                      onError: (e) =>
                        toast.error(
                          e instanceof ApiError
                            ? e.message
                            : "Gagal menghapus kelas.",
                        ),
                    })
                  }
                >
                  <Trash2 className="text-danger" />
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function DialogTambahKelas({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const buat = useCreateClass();
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", academicYear: "2026/2027", kerjasama: false },
  });

  const nama = watch("name");
  const kerjasama = watch("kerjasama");

  function onSubmit(values: FormValues) {
    buat.mutate(values, {
      onSuccess: (kelas) => {
        toast.success(`${kelas.displayName} ditambahkan.`);
        reset();
        onOpenChange(false);
      },
      onError: (e) =>
        toast.error(
          e instanceof ApiError ? e.message : "Gagal menambah kelas.",
        ),
    });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Tambah kelas</DialogTitle>
          <DialogDescription>
            Nama yang mengandung kata &quot;Kerjasama&quot; otomatis ditandai
            sebagai kelas kerjasama.
          </DialogDescription>
        </DialogHeader>

        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-4"
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="name">Nama kelas</Label>
            <Input
              id="name"
              placeholder="A, B, atau Kerjasama A"
              aria-invalid={Boolean(errors.name)}
              {...register("name")}
            />
            {errors.name && (
              <p className="text-xs text-danger">{errors.name.message}</p>
            )}
            {nama && (
              <p className="text-xs text-muted-foreground">
                Akan tampil sebagai <strong>Kelas {nama}</strong>
              </p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="academicYear">Tahun akademik</Label>
            <Input
              id="academicYear"
              placeholder="2026/2027"
              aria-invalid={Boolean(errors.academicYear)}
              {...register("academicYear")}
            />
            {errors.academicYear && (
              <p className="text-xs text-danger">
                {errors.academicYear.message}
              </p>
            )}
          </div>

          <label className="flex items-center gap-2 text-sm">
            <Checkbox
              checked={kerjasama}
              onCheckedChange={(value) =>
                setValue("kerjasama", Boolean(value))
              }
            />
            Tandai sebagai kelas kerjasama
          </label>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Batal
            </Button>
            <Button type="submit" disabled={buat.isPending}>
              Simpan
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
