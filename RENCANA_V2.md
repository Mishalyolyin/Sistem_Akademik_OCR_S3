# Sistem Pembayaran Program Doktor PAI — Spring Boot + Next.js + FastAPI

## Context

Ada sistem pembayaran SKS berbasis **Laravel 12 + PHP 8.2** yang sudah live dan dipakai di kampus
untuk program **Magister (S2)** (`Laravel/` di repo ini adalah salinan lokalnya).

**Rencana ini BUKAN mengganti sistem itu.** Ini proyek baru dan terpisah untuk
**Program Doktor (S3) PAI UNISSULA**, dibangun dengan bahasa dan framework yang berbeda dari
Laravel. Tujuannya karier dan pembelajaran: memperbanyak teknologi di CV dan belajar stack baru.
Target kerja yang diincar adalah **frontend developer**, dengan peluang sebesar-besarnya di semua
segmen (bank/BUMN, fintech, startup, software house).

Sistem Laravel dipakai sebagai **acuan alur dan logika**, bukan sebagai spesifikasi yang disalin —
struktur biaya S3 ternyata berbeda cukup jauh dari S2 (lihat di bawah).

Karena greenfield, tidak ada beban migrasi data dan tidak ada risiko produksi.

### Perbedaan penting dari sistem S2 lama

| Hal | S2 (Laravel lama) | **S3 (sistem ini)** |
|---|---|---|
| Pembagian program | REGULER vs RPL | **Tidak ada.** `program_type` dihapus dari akar |
| Kelas | Bebas | **Berhuruf**: Kelas A, B, C … dan Kelas Kerjasama A, B, … |
| Kategori tagihan | 4 (Semester, Munaqosah, Pendaftaran, Kerjasama) | **6** (lihat tabel biaya) |
| Ujian akhir | Munaqosah, satu tagihan | **4 tahap berurutan** |
| Angsuran UKT | 4x, tiap 3 bulan | **5x, bulanan** |
| Kerjasama | Kategori tagihan khusus, nominal diketik admin | **Salah satu dari 5 tingkat potongan**, nominal terhitung otomatis |

### Temuan dari salinan Laravel di repo ini

`Laravel/routes/web.php` memanggil 4 controller yang filenya tidak ada di salinan ini:
`StudentDocumentController`, `PendaftaranController`, `DevOcrController`, `DevDocumentOcrController`.
Middleware alias `documents.complete`, `pendaftaran.complete`, dan `developer` juga tidak terdaftar
di `Laravel/bootstrap/app.php`.

**Bukan penghambat.** Isi file-file itu hanya berguna sebagai contoh, dan `routes/web.php` sudah
memperlihatkan urutan alurnya dengan cukup jelas:

```
Foto → No.KTP + KTP → No.KK + KK → Ijazah → Alamat → bayar Pendaftaran → akses tagihan UKT
```

Alur ini dirancang ulang dari nol untuk S3. Kalau salinan lengkapnya mudah didapat, silakan
disalin sebagai pembanding; kalau tidak, tidak ada yang hilang.

---

## Struktur Biaya

Sumber: brosur resmi Pendaftaran Program Doktor PAI 2026.

| # | Kategori | Nominal | Sifat |
|---|---|---|---|
| 1 | Pendaftaran | Rp 1.000.000 | sekali bayar |
| 2 | UKT | Rp 10.000.000 × 6 semester | per semester, **5x angsuran** |
| 3 | Seminar Proposal | Rp 5.000.000 | sekali bayar |
| 4 | Ujian Kelayakan | Rp 5.000.000 | sekali bayar |
| 5 | Ujian Tertutup | Rp 10.000.000 | sekali bayar |
| 6 | Ujian Terbuka | Rp 10.000.000 | sekali bayar |
| | **Total (non alumni)** | **Rp 91.000.000** | |

### Tingkat potongan — hanya berlaku untuk UKT

Pendaftaran dan keempat biaya ujian (total Rp 30.000.000) **sama untuk semua golongan**.

| Golongan | Potongan | UKT/semester | Per cicilan (÷5) | 6 semester | Total keseluruhan |
|---|---|---|---|---|---|
| Non alumni | — | 10.000.000 | 2.000.000 | 60.000.000 | 91.000.000 |
| Kerabat alumni | 20% | 8.000.000 | 1.600.000 | 48.000.000 | 79.000.000 |
| Alumni | 25% | 7.500.000 | 1.500.000 | 45.000.000 | 76.000.000 |
| Alumni + pasutri | 35% | 6.500.000 | 1.300.000 | 39.000.000 | 70.000.000 |
| Kerjasama (1 kelas) | 40% | 6.000.000 | 1.200.000 | 36.000.000 | 67.000.000 |

Semua nominal habis dibagi 5 tanpa sisa, sehingga tidak perlu menempelkan sisa pembulatan ke
cicilan terakhir seperti di sistem S2 lama.

### Jadwal angsuran UKT

5 cicilan bulanan per semester:

- **Gasal**: September, Oktober, November, Desember, Januari
- **Genap**: Februari, Maret, April, Mei, Juni

### Urutan wajib biaya ujian

Keempat tahap ujian **harus lunas berurutan**. Mahasiswa tidak bisa mendaftar tahap berikutnya
sebelum tahap sebelumnya lunas:

```
Seminar Proposal → Ujian Kelayakan → Ujian Tertutup → Ujian Terbuka
```

---

## Keputusan Stack

### Inti

| Layer | Teknologi | Alasan |
|---|---|---|
| Frontend | **Next.js 16 (App Router) + TypeScript + React 19** | React+TS = kombinasi frontend paling banyak dicari di semua segmen |
| UI kit | **shadcn/ui + Tailwind 4 + TanStack Table v8** | Tabel data server-side, komponen modern, portofolio visual |
| Backend | **Java 21 + Spring Boot 3.5** | Bahasa dominan di bank/BUMN/enterprise Indonesia; ekosistem matang untuk sistem uang |
| ORM | **Spring Data JPA + Hibernate** | `@Transactional` + pessimistic locking bawaan |
| Migrasi DB | **Flyway** | Versi skema terkontrol, setara Laravel Migration |
| Database | **PostgreSQL 16** | JSONB untuk `ocr_data`, enum native |
| Message queue | **RabbitMQ (Spring AMQP)** | Job OCR async; broker pesan lazim di bank/enterprise |
| Cache | **Redis** | Cache dan rate limit |
| OCR | **Python 3 + FastAPI** (bungkus `ocr_processor.py`) | Model dimuat sekali, bukan spawn proses tiap upload |
| Auth | **Spring Security + JWT** | Access token 15 menit di memori, refresh token di cookie HttpOnly |
| Deploy | **Docker Compose** | Semua service dalam satu file |

### Pendukung

| Area | Teknologi |
|---|---|
| Build tool | Maven (lewat Maven Wrapper `mvnw`) |
| Boilerplate Java | Lombok |
| Mapping DTO | MapStruct |
| Dokumentasi API | springdoc-openapi (Swagger UI) |
| Test backend | JUnit 5 + Mockito + Testcontainers |
| State server FE | TanStack Query |
| Form FE | React Hook Form + Zod |
| Test FE | Vitest + Playwright |
| CI | GitHub Actions |

### Versi yang sengaja tidak dipakai versi terbaru

- **Spring Boot 3.5.16, bukan 4.x** — springdoc/Swagger belum mendukung Boot 4, materi belajar
  Boot 3 jauh lebih banyak, dan mayoritas lowongan masih Boot 3.
- **TanStack Table v8, bukan v9** — v9 mengubah API total (`useReactTable` → `useTable`,
  row model harus di-opt-in). Semua tutorial dan proyek nyata masih v8.

---

## Arsitektur

```
Browser
   |
   v
Next.js (UI + BFF)  --HTTP/JWT-->  Spring Boot (API, logika uang, Excel, PDF)
                                        |            |
                                        |            +--> PostgreSQL
                                        |            +--> Redis (cache)
                                        |            +--> RabbitMQ (job OCR)
                                        |
                                        +--HTTP-->  FastAPI (Tesseract + OpenCV)
```

Tiga modul terpisah: `web/` (Next.js), `api/` (Spring Boot), `ocr/` (FastAPI).

---

## Peta Konsep Laravel ke Spring Boot

| Laravel | Spring Boot |
|---|---|
| Controller | `@RestController` + `@RequestMapping` |
| Eloquent Model | JPA `@Entity` |
| Migration | Flyway SQL migration |
| FormRequest (validasi) | DTO + Bean Validation (`@Valid`, `@NotNull`) |
| Middleware `role:admin` | Spring Security `@PreAuthorize("hasRole('ADMIN')")` |
| Service class | `@Service` |
| `DB::transaction()` | `@Transactional` |
| `lockForUpdate()` | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |
| Queue Job + `tries`/`backoff` | RabbitMQ listener + `@Retryable` + dead-letter queue |
| Laravel Excel | Apache POI |
| DomPDF | OpenPDF / Flying Saucer |
| `Schedule::command()` | `@Scheduled(cron = "...")` |
| `Storage::disk('public')` | Filesystem lokal atau MinIO |
| Session auth | Spring Security + JWT |
| `Symfony\Process` ke python | `RestClient` ke FastAPI |
| PHPUnit | JUnit 5 + Mockito |

| Blade / Alpine | Next.js / React |
|---|---|
| `@extends('layouts.admin')` | Nested layout App Router |
| `x-data`, `x-show` | `useState` + conditional render |
| `@foreach` loop tabel | TanStack Table |
| `route('admin.payments')` | File-based routing App Router |
| Ambil data di controller | TanStack Query |
| Validasi FormRequest | Zod + React Hook Form |

---

## Skema Data

**Entitas inti:** `users`, `students`, `study_classes`, `import_batches`, `installment_templates`,
`installment_template_items`, `tuition_rates`, `payment_plans`, `installments`, `payments`,
`adjustments`, `verification_logs`, `installment_amount_changes`, `system_settings`.

### Enum (PostgreSQL native)

| Enum | Nilai |
|---|---|
| `payment_category` | `PENDAFTARAN`, `UKT`, `SEMINAR_PROPOSAL`, `UJIAN_KELAYAKAN`, `UJIAN_TERTUTUP`, `UJIAN_TERBUKA` |
| `term` | `GASAL`, `GENAP` |
| `installment_status` | `UNPAID`, `PARTIAL`, `PAID`, `OVERDUE` |
| `payment_status` | `PENDING`, `NEEDS_REVIEW`, `AUTO_VERIFIED`, `VERIFIED`, `REJECTED`, `FAILED` |
| `user_role` | `ADMIN`, `MAHASISWA`, `DEVELOPER` |

**Tidak ada `program_type`.** Ini perbedaan paling mendasar dari sistem S2.

### Kelas

```
study_classes(id, letter, kerjasama boolean, academic_year, active)
```

Nama tampilan diturunkan: `kerjasama ? "Kelas Kerjasama {letter}" : "Kelas {letter}"`.

**Jumlah kelas tidak dipatok.** Bisa 8, bisa kurang, bisa lebih, dan berubah tiap angkatan.
Kelas dibuat **otomatis saat import Excel mahasiswa** — sama seperti sistem lama: kalau nama kelas
di baris Excel belum ada di database, kelas itu dibuat. Admin juga bisa menambah dan menonaktifkan
kelas lewat menu Kelas. Tidak ada daftar kelas yang di-hardcode di kode manapun.

### Potongan

```
discount_tier_rates(tier, label, percent, active, sort_order)  -- daftar golongan
students.discount_tier  -- kode golongan mahasiswa, kunci asing ke tabel di atas
```

**Golongan adalah data, bukan tipe enum.** Sempat dibuat sebagai enum native PostgreSQL berisi
lima nilai tetap, tapi itu membuat penambahan golongan — keputusan bagian keuangan — jadi
pekerjaan pengembang: ubah tipe di database, ubah enum di Java, deploy ulang. Migrasi V8
mengubahnya jadi kode teks dengan kunci asing; golongan lama dinonaktifkan lewat `active`, tidak
dihapus, supaya mahasiswa dan tagihan yang memakainya tetap bisa dibaca.

Mahasiswa yang dimasukkan ke kelas kerjasama **default** ke tier `KERJASAMA` (40%), tapi tier
tetap disimpan per mahasiswa supaya pengecualian tetap mungkin tanpa memindahkan kelas.

### Audit perubahan nominal

```
installment_amount_changes(id, installment_id, old_amount, new_amount, reason, admin_id, timestamps)
```

---

## Aturan Bisnis

**Ini bagian paling rawan salah — semuanya soal uang.**

### Pembuatan tagihan UKT

1. **Nominal dihitung dari tarif dasar dikali potongan mahasiswa**, bukan diketik admin:
   `ukt = tarif_dasar × (1 − persen_potongan)`. Contoh: 10.000.000 × (1 − 0,40) = 6.000.000.
2. **Dibagi rata ke 5 cicilan.** Semua tarif habis dibagi 5, tapi kode tetap harus menempelkan
   sisa pembagian ke cicilan **terakhir** agar tahan bila tarif diubah jadi angka yang tidak bulat.
3. **Jatuh tempo bulanan** — Gasal: Sep, Okt, Nov, Des, Jan. Genap: Feb, Mar, Apr, Mei, Jun.
4. **Tarif adalah snapshot sekali saat generate.** Mengubah tarif dasar atau persen potongan
   **hanya berlaku untuk plan yang dibuat berikutnya** — plan yang sudah jadi tidak ditulis ulang.
5. **Satu plan aktif per (mahasiswa, tahun akademik, term, kategori).** Percobaan kedua ditolak.
6. **Maksimal 6 semester UKT** per mahasiswa. Percobaan membuat plan UKT ke-7 ditolak dengan
   pesan jelas.

### Perubahan golongan potongan

7. **Golongan boleh diubah admin**, tapi **terkunci begitu mahasiswa pernah mengunggah bukti
   bayar**. Yang masih bisa diubah hanya mahasiswa yang belum pernah upload sama sekali.
8. Backend **wajib** menolak perubahan golongan bila sudah ada baris `payments` milik mahasiswa itu,
   apa pun statusnya — termasuk yang `REJECTED`, karena unggahan tetap menandakan mahasiswa sudah
   melihat dan mengacu ke nominal lama.
9. UI menyembunyikan tombol ubah golongan untuk mahasiswa terkunci, **dan** menampilkan alasannya
   ("sudah pernah mengunggah bukti bayar"), bukan sekadar menonaktifkan tombol tanpa penjelasan.
10. **Konsekuensi yang harus diketahui admin:** koreksi golongan yang baru ketahuan belakangan
    (misal mahasiswa ternyata alumni) tidak bisa diterapkan lewat jalur ini. Jalan keluarnya
    memakai fitur Penyesuaian atau edit nominal cicilan per kasus, yang keduanya tercatat di
    jejak audit.

### Biaya sekali bayar

11. **Pendaftaran** Rp 1.000.000 — tagihan pertama, jadi gate sebelum akses tagihan UKT
   (mengikuti pola `pendaftaran.complete` di sistem lama).
12. **Empat tahap ujian harus lunas berurutan.** Mahasiswa hanya bisa mendaftar
   `UJIAN_KELAYAKAN` bila `SEMINAR_PROPOSAL` sudah lunas, dan seterusnya. Backend **wajib**
   memvalidasi urutan ini, bukan hanya menyembunyikan tombol di UI.
13. **Biaya ujian tidak kena potongan** dan tidak dicicil.

### Edit nominal cicilan

14. Tetap ada sebagai **jalan keluar untuk kasus khusus**, bukan alur utama — karena nominal
    sekarang terhitung otomatis dari tingkat potongan.
15. **`reason` wajib diisi**, tiap perubahan menulis satu baris `installment_amount_changes`.
16. **Status cicilan dihitung ulang** dari `amount_paid` dibanding `amount` BARU:
    `amount_paid >= amount` → PAID, `amount_paid > 0` → PARTIAL, selain itu UNPAID.
17. **`amount_paid` TIDAK pernah disentuh.** Bila nominal diturunkan di bawah yang sudah dibayar,
    status jadi PAID tapi **tidak ada auto-refund**. Kelebihan diurus lewat fitur Adjustment
    yang terpisah, agar jejak audit tidak campur aduk.
18. **`payment_plans.total_amount` dihitung ulang** = jumlah `amount` semua cicilan pada plan itu.
19. Seluruh operasi dalam **satu transaksi dengan pessimistic lock** pada baris cicilan.

### Kenapa tabel audit terpisah dari `adjustments`

`adjustments.amount` bermakna *delta terhadap uang yang sudah dibayar* (`amount_paid`), sedangkan
fitur ini *mengubah nominal yang harus dibayar* (`installments.amount`). Dua makna berbeda —
kalau digabung, riwayat "uang masuk" dan "tagihan berubah" bercampur di satu log.

---

## Modul & Fitur

### Admin
- Dashboard statistik: mahasiswa aktif, status bayar, tren, statistik OCR per kelas
- Verifikasi pembayaran **per kategori** (6 kategori), dengan penyaring per kelas
- Kelola mahasiswa: tingkat potongan, kelas, reset password, bulk delete, download foto
- Kelola kelas berhuruf, termasuk penanda kelas kerjasama
- Atur tarif dasar dan persen potongan
- Edit nominal cicilan + alasan + audit trail
- Import mahasiswa dari Excel + template (kolom kelas dan tingkat potongan)
- Penyesuaian (adjustment), pengaturan OCR
- Export laporan dan ledger, receipt PDF

### Mahasiswa
- Gate dokumen wajib berurutan: Foto → No.KTP+KTP → No.KK+KK → Ijazah → Alamat
- Gate pembayaran Pendaftaran sebelum akses tagihan UKT
- Lihat tagihan UKT per semester (maksimal 6) dan sisa cicilan
- Ajukan pembayaran tahap ujian **sesuai urutan wajib**
- Upload bukti transfer → OCR async → status
- Riwayat pembayaran, profil

### Developer
- Forensik OCR: hasil mentah OCR per pembayaran dan per dokumen

### Sistem
- OCR: preprocessing OpenCV → Tesseract → validasi nominal, rekening tujuan, tanggal,
  nama mahasiswa, blacklist keyword, confidence threshold
- Alokasi pembayaran: target cicilan → FIFO cicilan terlama → sisa ke wallet;
  leftover ≤ toleransi di-*discard* (kode unik bank)
- Guard race condition: keputusan manual admin tidak boleh ditimpa hasil OCR yang datang telat
- Reminder WhatsApp terjadwal
- Export dataset pembayaran terverifikasi untuk ML

---

## Rancangan UI

- **Layout dibalik**: icon rail tipis + top bar, bukan sidebar gelap 256px
- **Light-first + dark mode**, palet netral bias-dingin, aksen indigo
- **Warna semantik** (lunas / kurang / ditolak) dipisah dari warna aksen
- **TanStack Table**: sort, filter, pagination, sembunyikan kolom, aksi massal
- **Split-view verifikasi**: bukti transfer di kiri, hasil OCR di kanan dengan field mismatch
  ter-highlight
- **Dialog edit nominal** dengan React Hook Form + Zod, status cicilan dihitung ulang langsung
  saat diketik
- Command palette (Ctrl+K)

---

## Roadmap

| # | Fase | Isi | Status |
|---|---|---|---|
| 1 | Fondasi | Spring Boot + Flyway + JWT + RBAC + Swagger, Next.js shell + shadcn/ui, Docker Compose | **Selesai & terverifikasi** |
| 2 | Master data | Mahasiswa, kelas berhuruf, tingkat potongan, tarif, template angsuran, import Excel | 2 minggu |
| 3 | Tagihan | Generate plan UKT (5x, potongan), 6 kategori, urutan wajib ujian, edit nominal + audit | 2 minggu |
| 4 | OCR | FastAPI service, RabbitMQ, async + retry + dead-letter queue | 3 minggu |
| 5 | Verifikasi & alokasi | Sambungkan UI split-view ke API, alokasi FIFO + wallet + toleransi | 3 minggu |
| 6 | Laporan & rilis | Export Excel, receipt PDF, dashboard, reminder WhatsApp, Playwright E2E, deploy | 3 minggu |

Frontend Fase 5 (tabel + split-view + dialog ubah nominal) **sudah dibuat lebih awal** memakai data
contoh, tinggal disambungkan ke API.

---

## Yang Masih Perlu Diputuskan

1. **Hosting.** Spring Boot butuh JVM (±512MB–1GB RAM) — shared hosting cPanel tidak mungkin.
   Perlu VPS.
2. **Apakah peran `DEVELOPER` (forensik OCR)** tetap dibutuhkan.
3. *(Opsional, bukan penghambat)* Salin 4 controller yang hilang dari sistem Laravel live sebagai
   contoh alur dokumen wajib. Kalau tidak ada, alur itu dirancang ulang dari nol — routes yang ada
   sudah cukup menjelaskan urutannya.

---

## Verifikasi

- **Fase 1** ✅ — `docker compose up` menyalakan Postgres/Redis/RabbitMQ; Flyway V1 diterapkan;
  login mengembalikan access token + cookie HttpOnly; `/auth/me` tanpa token 401; refresh token
  ditolak bila dipakai sebagai access token; Swagger dan actuator 200.
- **Fase 2** — Import Excel campuran kelas biasa dan kerjasama; baris dengan kelas tidak dikenal
  masuk daftar gagal **tanpa** menggagalkan seluruh batch.
- **Fase 3 — wajib unit test (JUnit + Mockito):**
  - Plan UKT menghasilkan **5 cicilan** dengan bulan Sep–Jan (Gasal) dan Feb–Jun (Genap).
  - **Tiap tingkat potongan** menghasilkan nominal yang benar: 2.000.000 / 1.600.000 / 1.500.000 /
    1.300.000 / 1.200.000 per cicilan.
  - Jumlah 5 cicilan **sama persis** dengan UKT setelah potongan, tanpa selisih pembulatan.
  - **Snapshot tarif**: ubah tarif dasar atau persen potongan, plan lama TIDAK berubah.
  - **Plan UKT ke-7 ditolak** (maksimal 6 semester).
  - **Urutan ujian**: daftar `UJIAN_KELAYAKAN` sebelum `SEMINAR_PROPOSAL` lunas → ditolak 400.
  - **Edit nominal**: `installments.amount` berubah, `total_amount` dihitung ulang, satu baris
    `installment_amount_changes` tercatat, `reason` kosong ditolak 400.
  - **Nominal diturunkan di bawah `amount_paid`** → status PAID, `amount_paid` tidak berubah.
- **Fase 4** — Upload bukti → pesan masuk RabbitMQ → FastAPI mengembalikan JSON sebentuk output
  `ocr_processor.py`. Matikan FastAPI di tengah jalan untuk menguji retry dan dead-letter queue.
- **Fase 5 — wajib unit test alokasi:** (a) bayar pas, (b) bayar kurang → PARTIAL,
  (c) bayar lebih → sisa ke wallet, (d) leftover ≤ toleransi di-discard,
  (e) dua request bersamaan → pessimistic lock mencegah double-allocate,
  (f) verifikasi manual lebih dulu → hasil OCR telat tidak menimpa status.
- **Fase 6** — Bandingkan output Excel dan PDF dengan hitungan manual brosur.
- **Menyeluruh (Playwright E2E)** — Satu mahasiswa kelas kerjasama dites penuh: dokumen wajib →
  pendaftaran → UKT semester 1 → upload bukti → OCR → verifikasi → alokasi → receipt, lalu
  keempat tahap ujian berurutan.
