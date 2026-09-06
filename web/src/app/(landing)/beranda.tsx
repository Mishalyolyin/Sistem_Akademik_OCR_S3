"use client";

import Link from "next/link";
import {
  ArrowRight,
  BookMarked,
  CalendarRange,
  FileCheck2,
  GraduationCap,
  LogIn,
  Landmark,
  HeartHandshake,
  ScrollText,
  Sparkles,
  Wallet,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  JUMLAH_CICILAN_UKT,
  JUMLAH_SEMESTER_UKT,
  kategoriLabel,
  tarifDasar,
  urutanUjian,
} from "@/features/tarif/konstanta";

const rupiah = (n: number) =>
  new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    maximumFractionDigits: 0,
  }).format(n);

const totalStudi =
  tarifDasar.PENDAFTARAN +
  tarifDasar.UKT * JUMLAH_SEMESTER_UKT +
  urutanUjian.reduce((jumlah, tahap) => jumlah + tarifDasar[tahap], 0);

const golongan = [
  { nama: "Non alumni", potongan: 0 },
  { nama: "Kerabat alumni", potongan: 20 },
  { nama: "Alumni", potongan: 25 },
  { nama: "Alumni + pasutri", potongan: 35 },
  { nama: "Kelas kerjasama", potongan: 40 },
];

const alur = [
  {
    icon: FileCheck2,
    judul: "Lengkapi dokumen",
    isi: "Foto, KTP, Kartu Keluarga, ijazah, dan alamat. Diisi berurutan, dibaca sistem untuk dicocokkan dengan data yang Anda ketik.",
  },
  {
    icon: Wallet,
    judul: "Bayar pendaftaran",
    isi: `${rupiah(tarifDasar.PENDAFTARAN)} sekali bayar. Setelah lunas, seluruh tagihan lain terbuka.`,
  },
  {
    icon: CalendarRange,
    judul: "Angsur UKT",
    isi: `${JUMLAH_SEMESTER_UKT} semester, masing-masing ${JUMLAH_CICILAN_UKT} angsuran bulanan. Tagihannya dibuat sistem sendiri tiap awal semester.`,
  },
  {
    icon: GraduationCap,
    judul: "Tempuh empat tahap ujian",
    isi: "Seminar Proposal, Ujian Kelayakan, Ujian Tertutup, Ujian Terbuka. Berurutan, masing-masing menunggu yang sebelumnya lunas.",
  },
];

export function Beranda() {
  return (
    <div className="relative min-h-dvh overflow-x-hidden bg-background">
      <Aurora />

      <Navbar />

      <main className="relative">
        <Hero />
        <Kelembagaan />
        <TentangProgram />
        <Biaya />
        <Alur />
        <Penutup />
      </main>

      <footer className="relative border-t border-border/60 bg-card/40 px-6 py-12 backdrop-blur-sm">
        <div className="mx-auto grid max-w-6xl gap-8 sm:grid-cols-3">
          <div>
            <Logo />
            <p className="mt-3 max-w-xs text-sm text-pretty text-muted-foreground">
              Sistem pembayaran Program Doktor Pendidikan Agama Islam,
              Universitas Islam Sultan Agung.
            </p>
          </div>

          <div>
            <p className="font-heading text-sm font-semibold">Halaman</p>
            <ul className="mt-3 flex flex-col gap-2 text-sm text-muted-foreground">
              <li>
                <a href="#biaya" className="transition-colors hover:text-foreground">
                  Rincian biaya
                </a>
              </li>
              <li>
                <a href="#alur" className="transition-colors hover:text-foreground">
                  Alur pembayaran
                </a>
              </li>
              <li>
                <Link href="/login" className="transition-colors hover:text-foreground">
                  Masuk ke akun
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <p className="font-heading text-sm font-semibold">Bantuan</p>
            <p className="mt-3 text-sm text-pretty text-muted-foreground">
              Pertanyaan seputar tagihan, potongan, atau bukti bayar disampaikan
              ke bagian keuangan program studi.
            </p>
          </div>
        </div>

        <p className="mx-auto mt-10 max-w-6xl border-t border-border/60 pt-6 text-xs text-muted-foreground/70">
          Sistem internal. Diakses mahasiswa dan pengelola program studi.
        </p>
      </footer>
    </div>
  );
}

/**
 * Latar gradien berlapis.
 *
 * <p>Tiga noda warna besar yang saling menimpa di balik lapisan buram. Ini yang
 * membuat halaman punya kedalaman tanpa satu gambar pun — penting karena tidak
 * ada aset foto kampus di repo ini, dan menaruh foto stok justru membuatnya
 * terlihat seperti templat.
 *
 * <p>Seluruhnya `pointer-events-none` dan `aria-hidden`: ini murni hiasan, dan
 * tidak boleh menghalangi klik maupun terbaca pembaca layar.
 */
function Aurora() {
  return (
    <div
      aria-hidden
      className="pointer-events-none fixed inset-0 overflow-hidden"
    >
      <div className="absolute -top-40 -left-32 size-[42rem] rounded-full bg-primary/20 blur-[120px] motion-safe:animate-[apung_18s_ease-in-out_infinite]" />
      <div className="absolute -top-24 right-0 size-[34rem] rounded-full bg-success/18 blur-[120px] motion-safe:animate-[apung_22s_ease-in-out_infinite_reverse]" />
      <div className="absolute top-[60%] left-1/3 size-[38rem] rounded-full bg-primary/14 blur-[130px] motion-safe:animate-[apung_26s_ease-in-out_infinite]" />
      {/* Kisi halus, supaya bidang kosong tidak terasa benar-benar kosong. */}
      <div className="absolute inset-0 bg-[linear-gradient(to_right,var(--border)_1px,transparent_1px),linear-gradient(to_bottom,var(--border)_1px,transparent_1px)] bg-[size:64px_64px] opacity-[0.35] [mask-image:radial-gradient(ellipse_at_center,black,transparent_75%)]" />
    </div>
  );
}

function Logo({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <span className="flex size-9 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-success font-heading text-sm font-bold text-primary-foreground shadow-lg shadow-primary/25">
        SP
      </span>
      <span className="font-heading text-sm font-semibold tracking-tight">
        Doktor PAI
      </span>
    </span>
  );
}

function Navbar() {
  return (
    <header className="sticky top-0 z-40 border-b border-border/60 bg-background/70 backdrop-blur-xl">
      <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-3.5">
        <Logo />

        <div className="hidden items-center gap-7 text-sm text-muted-foreground md:flex">
          <a href="#program" className="transition-colors hover:text-foreground">
            Tentang program
          </a>
          <a href="#biaya" className="transition-colors hover:text-foreground">
            Rincian biaya
          </a>
          <a href="#alur" className="transition-colors hover:text-foreground">
            Tata cara bayar
          </a>
        </div>

        <Link
          href="/login"
          className="group inline-flex items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-lg shadow-primary/25 transition-all hover:shadow-xl hover:shadow-primary/30 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
        >
          <LogIn className="size-4" />
          Masuk
          <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" />
        </Link>
      </nav>
    </header>
  );
}

function Hero() {
  return (
    <section className="mx-auto max-w-6xl px-6 pt-16 pb-20 sm:pt-20">
      <div className="grid items-center gap-12 lg:grid-cols-[1.05fr_0.95fr]">
        <div className="text-center lg:text-left">
          <span className="inline-flex items-center gap-2 rounded-full border border-primary/25 bg-primary/10 px-3.5 py-1.5 text-xs font-medium text-primary">
            <Sparkles className="size-3.5" />
            Penerimaan mahasiswa baru dibuka
          </span>

          <h1 className="mt-6 font-heading text-4xl leading-[1.05] font-bold tracking-tight text-balance sm:text-[3.4rem]">
            Program Doktor{" "}
            <span className="bg-gradient-to-r from-primary via-success to-primary bg-clip-text text-transparent">
              Pendidikan Agama Islam
            </span>
          </h1>

          <p className="mx-auto mt-5 max-w-xl text-base text-pretty text-muted-foreground sm:text-lg lg:mx-0">
            Jenjang tertinggi Pendidikan Agama Islam di Fakultas Agama Islam
            Universitas Islam Sultan Agung, Semarang. Melengkapi jenjang sarjana
            dan magister yang telah lama berjalan, dengan Budaya Akademik Islami
            sebagai dasar penyelenggaraannya.
          </p>

          <div className="mt-9 flex flex-wrap items-center justify-center gap-3 lg:justify-start">
            <Link
              href="/login"
              className="group inline-flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-sm font-medium text-primary-foreground shadow-xl shadow-primary/25 transition-all hover:-translate-y-0.5 hover:shadow-2xl hover:shadow-primary/30 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
            >
              Masuk ke akun mahasiswa
              <ArrowRight className="size-4 transition-transform group-hover:translate-x-1" />
            </Link>
            <a
              href="#program"
              className="inline-flex items-center gap-2 rounded-full border border-border bg-card/60 px-6 py-3 text-sm font-medium backdrop-blur transition-colors hover:bg-card focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
            >
              Kenali programnya
            </a>
          </div>
        </div>

        <KartuProgram />
      </div>

      <dl className="mt-16 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <AngkaBesar nilai={JUMLAH_SEMESTER_UKT} label="semester UKT" />
        <AngkaBesar nilai={JUMLAH_CICILAN_UKT} label="angsuran per semester" />
        <AngkaBesar nilai={urutanUjian.length} label="tahap ujian" />
        <AngkaBesar nilai={golongan.length} label="golongan potongan" />
      </dl>
    </section>
  );
}

/**
 * Kartu identitas program sebagai gambar utama.
 *
 * <p>Semula di sini ada pratinjau kartu tagihan beserta batang kemajuan
 * pembayaran. Itu keliru: yang membuka halaman depan adalah calon mahasiswa
 * yang belum punya tagihan apa pun, dan kemajuan pembayaran orang lain tidak
 * menjual apa-apa kepadanya. Yang dijual seharusnya programnya.
 *
 * <p>Yang ditampilkan sekarang silsilah jenjangnya — sarjana dan magister yang
 * sudah lama berjalan, lalu doktor yang baru dibuka. Itu jawaban paling
 * meyakinkan atas keraguan yang wajar terhadap program baru: fakultasnya sudah
 * mengelola jalur yang sama sejak lama.
 */
function KartuProgram() {
  const jenjang = [
    { kode: "S1", nama: "Pendidikan Agama Islam", ket: "Sarjana" },
    { kode: "S2", nama: "Pendidikan Agama Islam", ket: "Magister" },
    { kode: "S3", nama: "Pendidikan Agama Islam", ket: "Doktor", baru: true },
  ];

  return (
    <div className="relative mx-auto w-full max-w-md">
      <div
        aria-hidden
        className="absolute -inset-6 rounded-[2.5rem] bg-gradient-to-br from-primary/15 to-success/15 blur-2xl"
      />
      <div
        aria-hidden
        className="absolute inset-x-4 -top-3 h-full rotate-[-3deg] rounded-3xl border border-border/60 bg-card/70 shadow-lg"
      />

      <div className="relative rounded-3xl border border-border/70 bg-card p-7 shadow-2xl shadow-primary/10">
        <p className="text-xs font-semibold tracking-widest text-primary uppercase">
          Fakultas Agama Islam
        </p>
        <p className="mt-1 font-heading text-lg font-bold tracking-tight">
          Universitas Islam Sultan Agung
        </p>
        <p className="text-sm text-muted-foreground">Semarang</p>

        <ul className="mt-6 flex flex-col gap-2.5">
          {jenjang.map((j) => (
            <li
              key={j.kode}
              className={cn(
                "flex items-center gap-3 rounded-2xl border px-3.5 py-3 transition-colors",
                j.baru
                  ? "border-primary/30 bg-primary/[0.07]"
                  : "border-border/60",
              )}
            >
              <span
                className={cn(
                  "flex size-9 shrink-0 items-center justify-center rounded-xl font-heading text-sm font-bold",
                  j.baru
                    ? "bg-gradient-to-br from-primary to-success text-primary-foreground shadow-md shadow-primary/25"
                    : "bg-muted text-muted-foreground",
                )}
              >
                {j.kode}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-medium">
                  {j.nama}
                </span>
                <span className="block text-xs text-muted-foreground">
                  {j.ket}
                </span>
              </span>
              {j.baru && (
                <span className="shrink-0 rounded-full bg-primary/12 px-2.5 py-1 text-xs font-medium text-primary">
                  Baru
                </span>
              )}
            </li>
          ))}
        </ul>

        <p className="mt-5 border-t border-border/60 pt-4 text-xs text-muted-foreground">
          Program Doktor dibuka berdasarkan Keputusan Menteri Agama Republik
          Indonesia Nomor 822 Tahun 2025.
        </p>
      </div>
    </div>
  );
}

function AngkaBesar({ nilai, label }: { nilai: number; label: string }) {
  return (
    <div className="rounded-2xl border border-border/60 bg-card/50 px-4 py-5 backdrop-blur-sm transition-transform hover:-translate-y-0.5">
      <dt className="font-heading text-3xl font-bold tabular-nums">{nilai}</dt>
      <dd className="mt-0.5 text-xs text-muted-foreground">{label}</dd>
    </div>
  );
}

/**
 * Baris identitas kelembagaan.
 *
 * <p>Situs kampus lazim menaruh penanda kelembagaan tepat di bawah hero —
 * akreditasi, dasar hukum pendirian, identitas nilai. Pola itu dipinjam di
 * sini karena berguna: yang membuka halaman ini sebagian calon mahasiswa yang
 * belum yakin programnya resmi.
 *
 * <p>Status akreditasinya sengaja TIDAK dicantumkan. Program ini baru berdiri
 * 2025 dan angkanya belum terbit; menuliskannya berarti mengarang. Yang
 * dipakai adalah dasar hukum pendiriannya, yang bisa diperiksa siapa pun.
 */
function Kelembagaan() {
  const penanda = [
    {
      icon: ScrollText,
      judul: "SK Menag No. 822 / 2025",
      isi: "Dasar hukum pendirian program, terbit 28 Juli 2025",
    },
    {
      icon: Landmark,
      judul: "Universitas Islam Sultan Agung",
      isi: "Kampus Islam terkemuka untuk generasi global beradab",
    },
    {
      icon: BookMarked,
      judul: "Budaya Akademik Islami",
      isi: "Nilai yang menjadi dasar seluruh penyelenggaraan pendidikan",
    },
  ];

  return (
    <section className="border-y border-border/60 bg-card/40 backdrop-blur-sm">
      <div className="mx-auto grid max-w-6xl gap-6 px-6 py-8 sm:grid-cols-3">
        {penanda.map((p) => {
          const Icon = p.icon;
          return (
            <div key={p.judul} className="flex items-start gap-3">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <Icon className="size-5" />
              </span>
              <div className="min-w-0">
                <p className="font-heading text-sm font-semibold">{p.judul}</p>
                <p className="mt-0.5 text-xs text-pretty text-muted-foreground">
                  {p.isi}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function Judul({
  kecil,
  besar,
  keterangan,
}: {
  kecil: string;
  besar: string;
  keterangan: string;
}) {
  return (
    <div className="mx-auto max-w-2xl text-center">
      <p className="text-xs font-semibold tracking-widest text-primary uppercase">
        {kecil}
      </p>
      <h2 className="mt-2.5 font-heading text-3xl font-bold tracking-tight text-balance sm:text-4xl">
        {besar}
      </h2>
      <p className="mt-3 text-pretty text-muted-foreground">{keterangan}</p>
    </div>
  );
}

function Biaya() {
  const baris = [
    { nama: kategoriLabel.PENDAFTARAN, nominal: tarifDasar.PENDAFTARAN, sifat: "sekali bayar" },
    {
      nama: kategoriLabel.UKT,
      nominal: tarifDasar.UKT,
      sifat: `per semester × ${JUMLAH_SEMESTER_UKT}, ${JUMLAH_CICILAN_UKT} angsuran`,
      sorot: true,
    },
    ...urutanUjian.map((tahap) => ({
      nama: kategoriLabel[tahap],
      nominal: tarifDasar[tahap],
      sifat: "sekali bayar",
    })),
  ];

  return (
    <section id="biaya" className="mx-auto max-w-6xl scroll-mt-20 px-6 py-20">
      <Judul
        kecil="Rincian biaya"
        besar="Semuanya terbuka sejak awal"
        keterangan="Tidak ada biaya yang muncul belakangan. Angka di bawah adalah tarif dasar sebelum potongan golongan."
      />

      <div className="mt-12 grid gap-6 lg:grid-cols-5">
        <div className="overflow-hidden rounded-3xl border border-border/60 bg-card/70 backdrop-blur-sm lg:col-span-3">
          <ul className="divide-y divide-border/60">
            {baris.map((item) => (
              <li
                key={item.nama}
                className={cn(
                  "flex items-center justify-between gap-4 px-6 py-4 transition-colors hover:bg-accent/40",
                  item.sorot && "bg-primary/[0.06]",
                )}
              >
                <div className="min-w-0">
                  <p className="font-medium">{item.nama}</p>
                  <p className="text-xs text-muted-foreground">{item.sifat}</p>
                </div>
                <p className="shrink-0 font-heading text-lg font-semibold tabular-nums">
                  {rupiah(item.nominal)}
                </p>
              </li>
            ))}
          </ul>

          <div className="flex items-center justify-between gap-4 border-t border-border/60 bg-gradient-to-r from-primary/10 to-success/10 px-6 py-5">
            <div>
              <p className="font-heading font-semibold">Total masa studi</p>
              <p className="text-xs text-muted-foreground">
                Golongan non alumni, sebelum potongan
              </p>
            </div>
            <p className="font-heading text-2xl font-bold tabular-nums">
              {rupiah(totalStudi)}
            </p>
          </div>
        </div>

        <div className="rounded-3xl border border-border/60 bg-card/70 p-6 backdrop-blur-sm lg:col-span-2">
          <h3 className="font-heading font-semibold">Golongan potongan</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Potongan hanya berlaku untuk UKT. Biaya pendaftaran dan keempat tahap
            ujian sama untuk semua golongan.
          </p>

          <ul className="mt-5 flex flex-col gap-2.5">
            {golongan.map((g) => {
              const perAngsuran = Math.round(
                (tarifDasar.UKT * (1 - g.potongan / 100)) / JUMLAH_CICILAN_UKT,
              );
              return (
                <li key={g.nama} className="group">
                  <div className="flex items-baseline justify-between gap-3 text-sm">
                    <span className="font-medium">{g.nama}</span>
                    <span className="tabular-nums text-muted-foreground">
                      {rupiah(perAngsuran)}
                      <span className="text-xs">/angsuran</span>
                    </span>
                  </div>
                  {/*
                    Panjang batang menggambarkan sisa yang dibayar, bukan
                    potongannya — supaya yang terpanjang berarti paling mahal,
                    sesuai naluri membaca grafik.
                  */}
                  <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-primary to-success transition-[width] duration-700"
                      style={{ width: `${100 - g.potongan}%` }}
                    />
                  </div>
                  {g.potongan > 0 && (
                    <p className="mt-1 text-xs text-success">
                      hemat {g.potongan}%
                    </p>
                  )}
                </li>
              );
            })}
          </ul>
        </div>
      </div>
    </section>
  );
}

function Alur() {
  return (
    <section id="alur" className="mx-auto max-w-6xl scroll-mt-20 px-6 py-20">
      <Judul
        kecil="Cara bayar"
        besar="Tata cara pembayaran"
        keterangan="Tiap langkah membuka langkah berikutnya. Sistem menjaga urutannya, jadi tidak ada tahap yang bisa terlewat."
      />

      <ol className="mt-12 grid gap-5 md:grid-cols-2 lg:grid-cols-4">
        {alur.map((langkah, i) => {
          const Icon = langkah.icon;
          return (
            <li
              key={langkah.judul}
              className="group relative overflow-hidden rounded-3xl border border-border/60 bg-card/70 p-6 backdrop-blur-sm transition-all hover:-translate-y-1 hover:border-primary/30 hover:shadow-xl hover:shadow-primary/5"
            >
              {/* Nomor besar di latar: penanda urutan yang terbaca sekilas. */}
              <span
                aria-hidden
                className="absolute -top-3 -right-1 font-heading text-8xl font-bold text-foreground/[0.04] transition-colors group-hover:text-primary/10"
              >
                {i + 1}
              </span>

              <span className="relative flex size-11 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/15 to-success/15 text-primary">
                <Icon className="size-5" />
              </span>
              <h3 className="relative mt-4 font-heading font-semibold">
                {langkah.judul}
              </h3>
              <p className="relative mt-1.5 text-sm text-pretty text-muted-foreground">
                {langkah.isi}
              </p>
            </li>
          );
        })}
      </ol>
    </section>
  );
}

/**
 * Pengenalan program dan kampusnya.
 *
 * <p>Menggantikan bagian yang semula memaparkan cara kerja pembacaan OCR dan
 * jejak audit. Kemampuan itu nyata, tapi ia menjual sistemnya kepada pengelola
 * — sementara yang membaca halaman depan adalah calon mahasiswa, yang
 * pertanyaannya sama sekali berbeda: program apa ini, siapa yang
 * menyelenggarakan, dan kenapa di sini.
 *
 * <p>Seluruh keterangan di bawah bersumber dari pengumuman resmi pendirian
 * program. Tidak ada visi, misi, kurikulum, atau nama dosen yang dikarang —
 * ketiganya belum diumumkan, dan mengarangnya di halaman resmi jauh lebih
 * merugikan daripada membiarkannya kosong.
 */
function TentangProgram() {
  const alasan = [
    {
      icon: Landmark,
      judul: "Diselenggarakan Fakultas Agama Islam",
      isi: "Fakultas yang sama telah mengelola jenjang sarjana dan magister Pendidikan Agama Islam. Program doktor melanjutkan jalur yang sudah berjalan, bukan memulai dari nol.",
    },
    {
      icon: BookMarked,
      judul: "Berdasar Budaya Akademik Islami",
      isi: "Nilai yang menjadi dasar seluruh penyelenggaraan pendidikan di Universitas Islam Sultan Agung, dari ruang kuliah sampai pelayanan mahasiswa.",
    },
    {
      icon: HeartHandshake,
      judul: "Takrimul aulad",
      isi: "Memuliakan anak didik sebagai pusat pelayanan pendidikan — prinsip yang disebut fakultas saat program ini dibuka.",
    },
  ];

  return (
    <section id="program" className="mx-auto max-w-6xl scroll-mt-20 px-6 py-20">
      <Judul
        kecil="Tentang program"
        besar="Jenjang tertinggi, di fakultas yang sudah menempuhnya"
        keterangan="Program Doktor Pendidikan Agama Islam dibuka berdasarkan Keputusan Menteri Agama Republik Indonesia Nomor 822 Tahun 2025, tertanggal 28 Juli 2025."
      />

      <div className="mt-12 grid gap-5 md:grid-cols-3">
        {alasan.map((item) => {
          const Icon = item.icon;
          return (
            <article
              key={item.judul}
              className="group rounded-3xl border border-border/60 bg-card/70 p-7 backdrop-blur-sm transition-all hover:-translate-y-1 hover:shadow-xl hover:shadow-primary/5"
            >
              <span className="flex size-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-success text-primary-foreground shadow-lg shadow-primary/20 transition-transform group-hover:scale-105">
                <Icon className="size-5" />
              </span>
              <h3 className="mt-5 font-heading text-lg font-semibold text-balance">
                {item.judul}
              </h3>
              <p className="mt-2 text-sm text-pretty text-muted-foreground">
                {item.isi}
              </p>
            </article>
          );
        })}
      </div>

      <p className="mx-auto mt-8 max-w-2xl text-center text-sm text-pretty text-muted-foreground">
        Keterangan kurikulum, staf pengajar, dan jadwal penerimaan akan
        ditambahkan di halaman ini setelah diumumkan program studi.
      </p>
    </section>
  );
}

function Penutup() {
  return (
    <section className="mx-auto max-w-6xl px-6 pt-6 pb-24">
      <div className="relative overflow-hidden rounded-[2rem] border border-primary/20 bg-gradient-to-br from-primary/14 via-success/10 to-primary/10 px-8 py-14 text-center backdrop-blur-sm">
        <div
          aria-hidden
          className="absolute -top-24 left-1/2 size-72 -translate-x-1/2 rounded-full bg-primary/25 blur-[90px]"
        />

        <h2 className="relative font-heading text-3xl font-bold tracking-tight text-balance sm:text-4xl">
          Sudah punya akun mahasiswa?
        </h2>
        <p className="relative mx-auto mt-3 max-w-lg text-pretty text-muted-foreground">
          Masuk dengan NIM Anda. Kata sandi awalnya adalah NIM itu sendiri, dan
          bisa dikembalikan bagian keuangan bila lupa.
        </p>

        <Link
          href="/login"
          className="group relative mt-8 inline-flex items-center gap-2 rounded-full bg-primary px-7 py-3.5 text-sm font-medium text-primary-foreground shadow-xl shadow-primary/25 transition-all hover:-translate-y-0.5 hover:shadow-2xl hover:shadow-primary/30 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
        >
          Masuk sekarang
          <ArrowRight className="size-4 transition-transform group-hover:translate-x-1" />
        </Link>
      </div>
    </section>
  );
}
