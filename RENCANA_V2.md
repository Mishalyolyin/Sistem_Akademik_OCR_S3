# Sistem Pembayaran Program Doktor PAI — Spring Boot + Next.js + FastAPI

## Context

Ada sistem pembayaran SKS berbasis **Laravel 12 + PHP 8.2** yang sudah live dan dipakai di kampus
untuk program **Magister (S2)**. Salinan lokalnya sempat ada di repo ini sebagai pembanding, dan
kini tinggal di repo lama — sistem S3 tidak bergantung padanya sama sekali.

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

### Temuan dari sisir menyeluruh terhadap sistem S2

Sisirnya dilakukan atas salinan lengkap sistem S2 — keempat controller yang sempat hilang
(`StudentDocumentController`, `PendaftaranController`, `DevOcrController`,
`DevDocumentOcrController`) sudah ada, begitu pula middleware `documents.complete`,
`pendaftaran.complete`, dan `developer`. Ia kini bisa dipakai sebagai pembanding sungguhan, bukan
hanya sebagai peta rute.

Urutan alur dokumen wajibnya:

```
Foto → No.KTP + KTP → No.KK + KK → Ijazah → Alamat → bayar Pendaftaran → akses tagihan UKT
```

Alur ini dirancang ulang dari nol untuk S3, dengan gerbang yang sama tapi kategori dan urutan
biaya yang berbeda.

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
| `Storage::disk('public')` | Filesystem lokal (volume Docker); MinIO tidak jadi dipakai |
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
`payment_allocations`, `adjustments`, `verification_logs`, `installment_amount_changes`,
`discount_tier_rates`, `reminder_logs`, `system_settings`.

Dua di antaranya lahir belakangan, saat fiturnya dikerjakan: `payment_allocations` mencatat
uang mana masuk ke cicilan mana — tanpa itu pembagian FIFO tidak bisa ditarik kembali saat
verifikasi dibatalkan; `reminder_logs` mencegah satu mahasiswa dikirimi pengingat jatuh tempo
yang sama berulang kali.

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

### Pembatalan tagihan

```
payment_plans.cancelled_at, cancelled_by, cancel_reason
CHECK: status = 'CANCELLED' <-> cancel_reason dan cancelled_at terisi
```

### Disertasi

```
dissertation_details(student_id PK, title, promotor, copromotor,
                     dissertation_file_path, article_file_path)
```

**Satu baris per mahasiswa, bukan per tagihan.** Sistem S2 menyimpannya per tagihan
(`munaqosah_details.payment_plan_id`) karena di sana Munaqosah hanya satu tagihan. Di S3 keempat
tahap ujian mengacu ke satu disertasi yang sama, jadi menempelkannya ke tagihan berarti judul yang
sama tersimpan empat kali — dan keempat salinan itu bebas menyimpang tanpa ada yang menyadarinya.

### Pembebasan gerbang

```
students.pendaftaran_exempt  -- bebas syarat lunas Pendaftaran
students.ujian_exempt        -- bebas syarat lunas seluruh UKT sebelum tahap ujian
```

### Audit perubahan nominal

```
installment_amount_changes(id, installment_id, old_amount, new_amount, reason, admin_id, timestamps)
```

---

## Aturan Bisnis

**Ini bagian paling rawan salah — semuanya soal uang.**

### Pembuatan tagihan UKT

0. **Tagihan UKT dibuat sistem sendiri, bukan diketik admin per mahasiswa.** Penjadwal harian
   membuatkan semester yang sudah waktunya untuk tiap mahasiswa aktif. Tahun akademik dan termnya
   **diturunkan dari `students.start_academic_year` dan `start_term`** — data yang diisi admin lewat
   berkas import — bukan dari tanggal hari ini. Angkatan Gasal dan angkatan Genap karena itu
   berjalan di jalurnya masing-masing: pada bulan kalender yang sama, keduanya berada di semester
   yang berbeda, dan menurunkannya dari tanggal akan salah untuk salah satunya apa pun pilihannya.
0a. **Tidak ada jalur manual.** Route `POST /students/{id}/plans` dihapus, bukan disembunyikan.
   Sebelumnya admin membuka halaman tiap mahasiswa lalu mengetik tahun akademiknya — untuk enam
   semester dan tiga puluh mahasiswa itu 180 kali, dan tahun yang diketik sebanyak itu cepat atau
   lambat salah tanpa satu pun galat menegur, karena tahun apa pun tetap tersimpan dengan sah.
0b. **Putaran aman diulang.** Yang sudah ada tidak dibuat ulang; yang belum tiba waktunya tidak
   dibuat lebih dulu — kalau tidak, aturan snapshot tarif di nomor 4 dilanggar diam-diam, karena
   tarif hari ini ikut terkunci untuk semester yang baru dibayar dua tahun lagi.
0c. **Yang tertinggal dikejar.** Mahasiswa yang diimpor di tengah semester melewatkan putaran
   bulan itu; putaran berikutnya membuat semua semester yang waktunya sudah lewat. Tanpa ini,
   semester pertamanya tidak akan pernah terbentuk — jalur manualnya sudah tidak ada.
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
   (mengikuti pola `pendaftaran.complete` di sistem lama). Backend menolak pendaftaran kategori
   apa pun selain Pendaftaran selama biaya itu belum lunas.
11a. **Pengecualian gate itu adalah data, bukan tambalan kode.** `students.pendaftaran_exempt`
   membebaskan satu mahasiswa dari gate Pendaftaran — untuk yang biaya pendaftarannya memang
   ditanggung pihak lain atau sudah dibayar di luar sistem. Tanpa penanda ini, satu-satunya jalan
   keluar adalah membuatkan tagihan palsu lalu memverifikasinya, dan itu mengotori pembukuan.
12. **Empat tahap ujian harus lunas berurutan.** Mahasiswa hanya bisa mendaftar
   `UJIAN_KELAYAKAN` bila `SEMINAR_PROPOSAL` sudah lunas, dan seterusnya. Backend **wajib**
   memvalidasi urutan ini, bukan hanya menyembunyikan tombol di UI.
12a. **Seluruh UKT harus lunas sebelum tahap ujian mana pun bisa didaftarkan.** Keenam semester
   harus sudah **ditagihkan dan lunas** — bukan hanya yang kebetulan sudah dibuatkan tagihannya,
   sebab menagihkan semester berikutnya adalah pekerjaan admin dan mahasiswa tidak boleh
   diuntungkan karena pekerjaan itu belum dilakukan. Gerbang ini berdiri **sebelum** aturan urutan
   di nomor 12, dan pesan galatnya menyebut semester mana yang menghalangi.
12b. **Pengecualiannya data, bukan tambalan kode.** `students.ujian_exempt` membebaskan satu
   mahasiswa dari gerbang itu — untuk yang UKT-nya ditanggung beasiswa belakangan atau sedang
   menyicil di luar sistem. Polanya sama persis dengan `pendaftaran_exempt`.
12c. **Identitas disertasi wajib ada sebelum mendaftar tahap ujian.** Judul, promotor, dan
   ko-promotor. Gerbang ini di **portal**, bukan di `PaymentGenerationService` bersama dua aturan
   di atas: admin kadang membuatkan tagihan dari formulir kertas yang sudah ditandatangani
   promotor, dan menahannya di sana berarti pekerjaan admin ikut terhenti karena mahasiswa belum
   sempat mengetik judulnya. Yang dijaga adalah pendaftaran mandiri — satu-satunya jalur di mana
   tidak ada manusia lain yang memeriksa.
12d. **Naskah disertasi dan artikel jurnal dipisah dari identitasnya, dan tidak jadi syarat
   mendaftar.** Judul ditetapkan jauh sebelum naskahnya jadi; menuntut keduanya sekaligus berarti
   tidak ada yang bisa mendaftar Seminar Proposal sampai disertasinya hampir selesai.
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

### Pembatalan tagihan

20. **Tagihan yang salah dibuat bisa dibatalkan**, statusnya jadi `CANCELLED`. Ini bukan
    kemewahan: dua indeks unik (`uq_active_plan` dan `uq_one_time_plan`) membuat satu kekeliruan
    permanen. Seminar Proposal yang terlanjur dibuat untuk mahasiswa keliru akan membuat mahasiswa
    itu **tidak akan pernah** bisa punya Seminar Proposal lagi, dan UKT di tahun yang salah
    mengunci tahun itu sekaligus memakan jatah 6 semester.
21. **Alasan wajib**, minimal 5 karakter, tersimpan di `payment_plans.cancel_reason` beserta siapa
    dan kapan. `CHECK` di database menjaga ketiganya terisi bersamaan dengan status `CANCELLED`.
22. **Tagihan yang sudah menerima pembayaran terverifikasi ditolak.** Pembatalan tidak menyentuh
    uang sama sekali. Jalannya dua langkah dan itu disengaja: batalkan dulu tiap keputusan
    verifikasinya di panel verifikasi — masing-masing dengan alasannya sendiri — baru tagihannya
    bisa dibatalkan. Satu tombol yang membalik lima transaksi dengan satu kalimat alasan justru
    menghapus keterangan yang paling dicari ketika pembukuan diperiksa.
23. Pesan penolakannya **menyebut jumlah dan nominalnya**, bukan sekadar "tidak bisa dibatalkan".

### Kenapa tabel audit terpisah dari `adjustments`

`adjustments.amount` bermakna *delta terhadap uang yang sudah dibayar* (`amount_paid`), sedangkan
fitur ini *mengubah nominal yang harus dibayar* (`installments.amount`). Dua makna berbeda —
kalau digabung, riwayat "uang masuk" dan "tagihan berubah" bercampur di satu log.

---

## Modul & Fitur

### Admin
- Dashboard statistik: mahasiswa aktif, status bayar, tren, dan **statistik OCR per kelas** —
  berapa bukti terbaca, berapa perlu ditinjau, berapa gagal, dan keyakinan rata-ratanya. Per kelas
  karena masalah pembacaan hampir selalu berkelompok: satu kelas yang diajari memfoto struk dengan
  cara yang sama menghasilkan bukti yang sama sulitnya dibaca
- Verifikasi pembayaran **per kategori** (6 kategori), dengan penyaring per kelas
- Kelola mahasiswa: tingkat potongan, kelas, reset password, bulk delete, download foto
- Batalkan tagihan yang salah dibuat, dengan alasan wajib dan jejak audit
- Jalankan putaran pembuatan tagihan UKT lebih awal, tanpa menunggu jadwalnya
- Lihat disertasi tiap mahasiswa: judul, kedua promotor, dan naskahnya — dipakai saat
  memverifikasi bukti bayar tahap ujian
- Unduh foto profil satu kelas sekaligus dalam satu ZIP, dinamai NIM_Nama
- Dua pembebasan gerbang per mahasiswa: bebas biaya Pendaftaran, bebas syarat lunas UKT
- Kelola kelas berhuruf, termasuk penanda kelas kerjasama
- Atur tarif dasar dan persen potongan
- Edit nominal cicilan + alasan + audit trail
- Import mahasiswa dari Excel + template (kolom kelas dan tingkat potongan)
- Penyesuaian (adjustment), pengaturan OCR
- Export laporan pembayaran **dengan penyaring** kelas dan status, dalam dua format: daftar
  transaksi (tiga lembar rekap) atau **ledger termin** — satu lembar per semester UKT, satu baris
  per mahasiswa, sepasang kolom tanggal dan jumlah untuk tiap angsuran. Bentuk terakhir mengikuti
  ledger sistem S2 yang sudah dipakai bagian keuangan; di sana enam termin, di sini lima
- Export data mahasiswa beserta hasil pemeriksaan tiap dokumen wajib
- Receipt PDF per pembayaran

### Mahasiswa
- Gate dokumen wajib berurutan: Foto → No.KTP+KTP → No.KK+KK → Ijazah → Alamat
- Gate pembayaran Pendaftaran sebelum akses tagihan UKT
- Isi identitas disertasi (judul, promotor, ko-promotor) dan unggah naskahnya
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
- **Gambar hasil praproses OpenCV disimpan** (`payments.processed_file_path`) — yang benar-benar
  dibaca Tesseract, bukan berkas asli unggahan mahasiswa. Piksel yang tidak disimpan hari ini tidak
  bisa dipulihkan besok, beda dengan angka yang selalu bisa dihitung ulang
- Statistik pembelajaran di halaman pengaturan OCR: berapa contoh berlabel terkumpul, dan seberapa
  sering keputusan mesin sepakat dengan admin — satu-satunya ukuran yang menjawab apakah ambang
  keyakinan kekencangan atau kekendoran
- Export dataset **gambar** (ZIP: `gambar/` + `label.csv`) untuk melatih model penglihatan
- Export dataset CSV pembacaan bukti untuk ML — hanya bukti yang **diputuskan admin** yang ikut,
  disandingkan dengan apa yang dibaca mesin. Verifikasi otomatis yang belum disentuh manusia
  sengaja tidak masuk: itu tebakan mesin sendiri, dan melatih model darinya hanya mengukuhkan
  kesalahan yang sudah ada. Teks mentah OCR hanya ikut bila diminta, karena isinya data pribadi

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
- **Penanda semester berjalan**, diturunkan dari tanggal: chip di top bar admin (supaya tahun
  akademik tidak ditebak dari ingatan saat membuat tagihan) dan kartu di portal mahasiswa yang
  menyebut bulan jatuh temponya. Juli–Agustus ditandai **jeda**, bukan dipaksa masuk salah satu
  term — memang tidak ada angsuran di dua bulan itu
- Command palette (Ctrl+K): mencari halaman, mahasiswa, dan tindakan sekaligus. Daftar halamannya
  diturunkan dari konfigurasi menu dan ikut disaring per peran, supaya palet tidak jadi pintu
  belakang ke halaman yang endpointnya menolak

---

## Roadmap

| # | Fase | Isi | Status |
|---|---|---|---|
| 1 | Fondasi | Spring Boot + Flyway + JWT + RBAC + Swagger, Next.js shell + shadcn/ui, Docker Compose | **Selesai & terverifikasi** |
| 2 | Master data | Mahasiswa, kelas berhuruf, tingkat potongan, tarif, template angsuran, import Excel | **Selesai & terverifikasi** |
| 3 | Tagihan | Generate plan UKT (5x, potongan), 6 kategori, urutan wajib ujian, edit nominal + audit | **Selesai & terverifikasi** |
| 4 | OCR | FastAPI service, RabbitMQ, async + retry + dead-letter queue | **Selesai & terverifikasi** |
| 5 | Verifikasi & alokasi | Sambungkan UI split-view ke API, alokasi FIFO + wallet + toleransi | **Selesai & terverifikasi** |
| 6 | Laporan & rilis | Export Excel, receipt PDF, dashboard, reminder WhatsApp, Playwright E2E, deploy | **Selesai & terverifikasi** |
| + | Portal mahasiswa | Di luar rencana awal: gate dokumen, tagihan sendiri, unggah bukti, riwayat | **Selesai & terverifikasi** |

Rincian tiap fase, beserta apa yang diverifikasi dan kapan, ada di [STATUS.md](STATUS.md).

Frontend Fase 5 (tabel + split-view + dialog ubah nominal) dibuat lebih awal memakai data contoh,
lalu disambungkan ke API pada fasenya sendiri.

---

## Yang Masih Perlu Diputuskan

1. **Hosting.** Spring Boot butuh JVM (±512MB–1GB RAM) — shared hosting cPanel tidak mungkin.
   Perlu VPS. Paketnya belum dipastikan; ini satu-satunya keputusan yang masih menggantung.

### Sudah terjawab

2. ~~**Apakah peran `DEVELOPER` (forensik OCR)** tetap dibutuhkan.~~ **Dipertahankan.** Ia kini
   punya halaman forensik OCR sendiri beserta jalan membuat akunnya, dan menu untuk peran itu
   disaring supaya tidak mengantar ke halaman yang endpointnya menolaknya.
3. ~~*(Opsional)* Salin 4 controller yang hilang dari sistem Laravel live.~~ **Tidak diperlukan.**
   Alur dokumen wajib dirancang ulang dari nol dan sudah jalan; urutan di `routes/web.php` memang
   sudah cukup sebagai acuan.

---

## Verifikasi

- **Fase 1** ✅ — `docker compose up` menyalakan Postgres dan RabbitMQ; Flyway V1 diterapkan;
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
