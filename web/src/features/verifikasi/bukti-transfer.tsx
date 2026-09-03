"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { FileText, ImageOff, Loader2, ZoomIn, ZoomOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useProofImage } from "./api";

const ZOOM_STEPS = [1, 1.5, 2, 3];

export function BuktiTransfer({ paymentId }: { paymentId: number }) {
  const { data, isPending, error } = useProofImage(paymentId);
  const [zoomIndex, setZoomIndex] = useState(0);

  // Object URL wajib dilepas, kalau tidak berkasnya menumpuk di memori browser.
  useEffect(() => {
    const url = data?.url;
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [data?.url]);

  useEffect(() => setZoomIndex(0), [paymentId]);

  const zoom = ZOOM_STEPS[zoomIndex];

  return (
    <div className="flex h-full flex-col gap-3">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium text-muted-foreground">
          Bukti transfer
        </span>

        {data && !data.isPdf && (
          <div className="flex items-center gap-1">
            <Button
              variant="outline"
              size="icon-xs"
              aria-label="Perkecil"
              disabled={zoomIndex === 0}
              onClick={() => setZoomIndex((i) => Math.max(i - 1, 0))}
            >
              <ZoomOut />
            </Button>
            <span className="w-10 text-center text-xs tabular-nums text-muted-foreground">
              {Math.round(zoom * 100)}%
            </span>
            <Button
              variant="outline"
              size="icon-xs"
              aria-label="Perbesar"
              disabled={zoomIndex === ZOOM_STEPS.length - 1}
              onClick={() =>
                setZoomIndex((i) => Math.min(i + 1, ZOOM_STEPS.length - 1))
              }
            >
              <ZoomIn />
            </Button>
          </div>
        )}
      </div>

      <div className="flex flex-1 items-start justify-center overflow-auto rounded-lg border border-border bg-muted/40 p-3">
        {isPending ? (
          <span className="flex items-center gap-2 self-center text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" />
            Memuat bukti…
          </span>
        ) : error || !data ? (
          <span className="flex flex-col items-center gap-2 self-center text-sm text-muted-foreground">
            <ImageOff className="size-6" />
            Gambar bukti tidak bisa dimuat.
          </span>
        ) : data.isPdf ? (
          <div className="flex flex-col items-center gap-3 self-center">
            <FileText className="size-8 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">Bukti berupa PDF.</p>
            <Button
              variant="outline"
              size="sm"
              nativeButton={false}
              render={<a href={data.url} target="_blank" rel="noreferrer" />}
            >
              Buka di tab baru
            </Button>
          </div>
        ) : (
          <Image
            src={data.url}
            alt="Bukti transfer"
            width={800}
            height={1200}
            unoptimized
            className="h-auto max-w-full origin-top transition-transform"
            style={{ transform: `scale(${zoom})` }}
          />
        )}
      </div>
    </div>
  );
}
