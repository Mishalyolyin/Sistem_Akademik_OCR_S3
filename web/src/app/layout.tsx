import type { Metadata } from "next";
import { Plus_Jakarta_Sans, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";

// Plus Jakarta Sans — dirancang di Indonesia, cocok untuk sistem kampus.
const sans = Plus_Jakarta_Sans({
  variable: "--font-sans",
  subsets: ["latin"],
  display: "swap",
});

// Mono dipakai untuk NIM dan nominal rupiah supaya digit rata kolom.
const mono = JetBrains_Mono({
  variable: "--font-mono",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "Sistem Pembayaran Kampus",
    template: "%s · Sistem Pembayaran Kampus",
  },
  description:
    "Pengelolaan tagihan, verifikasi bukti bayar berbasis OCR, dan pelaporan pembayaran mahasiswa.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="id"
      suppressHydrationWarning
      className={`${sans.variable} ${mono.variable} h-full antialiased`}
    >
      <body className="min-h-full">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
