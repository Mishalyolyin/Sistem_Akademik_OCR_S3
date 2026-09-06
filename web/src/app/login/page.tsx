"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { LockKeyhole } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api";
import { berandaUntuk } from "@/components/shell/auth-guard";
import { useAuth } from "@/lib/auth";

const schema = z.object({
  email: z
    .string()
    .min(1, "Email wajib diisi.")
    .email("Format email tidak valid."),
  password: z.string().min(1, "Kata sandi wajib diisi."),
});

type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const { login, status, user } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  // Sudah punya sesi aktif — tidak perlu melihat halaman masuk lagi.
  useEffect(() => {
    if (status === "authenticated" && user) {
      router.replace(berandaUntuk(user.role));
    }
  }, [status, user, router]);

  async function onSubmit(values: FormValues) {
    setServerError(null);
    try {
      const masuk = await login(values.email, values.password);
      router.replace(berandaUntuk(masuk.role));
    } catch (error) {
      setServerError(
        error instanceof ApiError
          ? error.message
          : "Tidak bisa menghubungi server. Pastikan API sedang berjalan.",
      );
    }
  }

  return (
    <div className="flex min-h-dvh items-center justify-center bg-background px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center gap-3 text-center">
          <div className="flex size-11 items-center justify-center rounded-xl bg-primary font-heading text-base font-bold text-primary-foreground">
            SP
          </div>
          <div>
            <h1 className="font-heading text-lg font-semibold tracking-tight">
              Sistem Pembayaran Kampus
            </h1>
            <p className="text-sm text-muted-foreground">
              Masuk untuk mengelola tagihan dan verifikasi
            </p>
          </div>
        </div>

        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-4 rounded-xl border border-border bg-card p-6"
        >
          {serverError && (
            <p
              role="alert"
              className="rounded-md border border-danger/25 bg-danger-soft px-3 py-2 text-sm text-danger"
            >
              {serverError}
            </p>
          )}

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="admin@kampus.ac.id"
              aria-invalid={Boolean(errors.email)}
              {...register("email")}
            />
            {errors.email && (
              <p className="text-xs text-danger">{errors.email.message}</p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="password">Kata sandi</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              aria-invalid={Boolean(errors.password)}
              {...register("password")}
            />
            {errors.password && (
              <p className="text-xs text-danger">{errors.password.message}</p>
            )}
          </div>

          <Button type="submit" size="lg" loading={isSubmitting}>
            <LockKeyhole />
            {isSubmitting ? "Memeriksa…" : "Masuk"}
          </Button>
        </form>
      </div>
    </div>
  );
}
