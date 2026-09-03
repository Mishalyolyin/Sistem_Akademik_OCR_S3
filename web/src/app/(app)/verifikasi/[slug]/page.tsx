import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { findView, verifikasiViews } from "@/features/verifikasi/kategori";
import { HalamanVerifikasi } from "./halaman-verifikasi";

export function generateStaticParams() {
  return verifikasiViews.map((view) => ({ slug: view.slug }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const view = findView(slug);
  return { title: view ? `Verifikasi ${view.title}` : "Verifikasi" };
}

export default async function Page({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const view = findView(slug);
  if (!view) notFound();

  return <HalamanVerifikasi view={view} />;
}
