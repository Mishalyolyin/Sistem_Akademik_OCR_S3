"use client";

import { useEffect } from "react";
import Image from "next/image";
import { FileText, ImageOff, Loader2 } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { BerkasTerlindungi } from "@/lib/berkas";

/**
 * Dialog pratinjau satu berkas yang diambil lewat hook berkas terlindungi.
 *
 * Dipakai panel admin maupun portal mahasiswa: keduanya membuka dokumen yang
 * sama, jadi tampilannya tidak boleh berbeda hanya karena tempat membukanya
 * berbeda.
 */
export function PratinjauBerkas({
  judul,
  keterangan,
  berkas,
  onClose,
}: {
  judul: string;
  keterangan?: string;
  berkas: {
    data?: BerkasTerlindungi;
    isPending: boolean;
    error: unknown;
  };
  onClose: () => void;
}) {
  const { data, isPending, error } = berkas;

  // Object URL wajib dilepas, kalau tidak berkasnya menumpuk di memori browser.
  useEffect(() => {
    const url = data?.url;
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [data?.url]);

  return (
    <Dialog open onOpenChange={(next) => (next ? undefined : onClose())}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{judul}</DialogTitle>
          {keterangan && <DialogDescription>{keterangan}</DialogDescription>}
        </DialogHeader>

        <div className="flex min-h-64 items-center justify-center overflow-auto rounded-lg border border-border bg-muted/30 p-2">
          {isPending ? (
            <Loader2 className="size-5 animate-spin text-muted-foreground" />
          ) : error || !data ? (
            <span className="flex flex-col items-center gap-2 py-8 text-sm text-muted-foreground">
              <ImageOff className="size-6" />
              Berkas tidak bisa dimuat.
            </span>
          ) : data.isPdf ? (
            <a
              href={data.url}
              target="_blank"
              rel="noreferrer"
              className="flex flex-col items-center gap-2 py-8 text-sm text-primary underline-offset-4 hover:underline"
            >
              <FileText className="size-6" />
              Buka berkas PDF
            </a>
          ) : (
            <Image
              src={data.url}
              alt={judul}
              width={800}
              height={600}
              unoptimized
              className="h-auto max-h-[65vh] w-auto object-contain"
            />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
