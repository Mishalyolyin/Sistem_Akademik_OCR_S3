import type { Metadata } from "next";
import { DetailMahasiswa } from "./detail-mahasiswa";

export const metadata: Metadata = { title: "Detail Mahasiswa" };

export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <DetailMahasiswa studentId={Number(id)} />;
}
