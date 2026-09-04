# Status Pengerjaan

Daftar apa yang sudah jadi dan apa yang belum. Diperbarui tiap kali ada bagian
yang selesai. Rencana lengkapnya ada di [RENCANA_V2.md](RENCANA_V2.md).

Terakhir diperbarui: 4 September 2026, 04.00

---

## Verifikasi terakhir

Dijalankan 4 September 2026 pukul 04.00 di mesin pengembangan, semuanya lolos:

| Yang dicek | Perintah | Hasil |
|---|---|---|
| Test backend | `api/mvnw clean verify` | ✅ 172 test lolos, BUILD SUCCESS |
| Ketikan frontend | `npx tsc --noEmit` | ✅ tanpa galat |
| Lint frontend | `npx eslint src/` | ✅ 0 error, 1 warning yang memang tak bisa diperbaiki |
| Build frontend | `npm run build` | ✅ 23 rute terbentuk |
| Service OCR (mesin) | impor `cv2`, `pytesseract`, `main` | ✅ OpenCV 5.0.0 di Python 3.14.7 |
| Service OCR (container) | `docker build` lalu `GET /health` | ✅ Python 3.14.7, OpenCV 5.0.0, Tesseract 5.5.0 |
| Susunan compose | `docker compose config` | ✅ dev dan prod terbaca |
| Stack produksi | `up -d --build` lalu login | ✅ 5 service hidup, admin bisa masuk |

Toolchain yang terpasang: JDK 21.0.12.1, Node 24.19.0, Python 3.14.7, Docker 29.7.2.

> **Catatan version control.** Seluruh V2 sempat tidak pernah masuk git dan nyaris
> hilang karena sebuah auto-stash menyapu working tree. Sudah dipulihkan dan
> dikomit ke branch `migrasi-v2`. Commit tiap kali ada bagian yang selesai.

---

## Ringkasan

| Fase | Isi | Status |
|---|---|---|
| 1 | Fondasi: auth JWT, RBAC, Swagger, Docker, shell UI | ✅ Selesai |
| 2 | Master data: kelas, mahasiswa, tarif, potongan, import Excel | ✅ Selesai (backend + frontend) |
| 3 | Tagihan: generate plan UKT, urutan ujian, edit nominal | ✅ Selesai (backend + UI admin) |
| 4 | OCR: FastAPI service, RabbitMQ, retry | ✅ Selesai (backend) |
| 5 | Verifikasi & alokasi pembayaran | ✅ Selesai |
| 6 | Laporan, export Excel, PDF, deploy | ✅ Selesai |
| + | Portal mahasiswa (di luar rencana awal) | ✅ Selesai |

---

## Hasil audit 3 September 2026

Seluruh isi berkas ini ditelusuri ulang ke kode. Klaimnya cocok, kecuali hal-hal
di bawah. Yang sudah diperbaiki punya test yang menguncinya; tiap test itu sudah
dibuktikan gagal terhadap kode sebelum perbaikan.

### Diperbaiki

**1. Nominal pembayaran bisa berubah setelah uangnya dibagikan.** Hasil OCR yang
datang setelah admin memutuskan manual memang tidak mengubah statusnya — tapi
nominalnya ikut ditulis ulang, dan alokasi tidak pernah dihitung ulang. Cicilan
terlanjur menerima angka lama sementara kuitansi mencetak angka baru.

Urutannya: mahasiswa mengaku bayar Rp 1.000.000 → admin memverifikasi manual →
uang masuk cicilan → pekerjaan OCR baru jalan dan membaca Rp 1.200.000 → nominal
tersimpan 1.200.000 padahal yang masuk 1.000.000. Tidak ada galat, tidak ada
peringatan. Sekarang nominal hanya boleh disesuaikan selama statusnya masih
`PENDING`/`NEEDS_REVIEW` **dan** uangnya belum dialokasikan; selisihnya tetap
dicatat sebagai catatan supaya admin tahu ada yang perlu ditinjau.

Dari keluarga yang sama: `verified_at` juga ikut ditimpa, sehingga jejak audit
mencatat pembayaran diverifikasi admin pada jam saat OCR selesai, bukan saat
admin menekan tombolnya. Ikut diperbaiki.

**2. Keluar sekarang benar-benar mencabut sesi.** Sebelumnya `/auth/logout` hanya
menghapus cookie di peramban; refresh tokennya tetap sah tujuh hari penuh.
Migrasi V6 menambah `users.tokens_valid_from`, dan refresh token yang terbit
sebelum waktu itu ditolak. Access token yang sudah beredar tidak ikut dicabut —
pemeriksaannya harus tetap tanpa akses database — tapi umurnya lima belas menit
dan tidak bisa diperbarui lagi.

**3. Penolakan 401 kini berformat ProblemDetail.** Request tanpa token dijawab
401 bertubuh kosong, padahal frontend membaca field `detail`, sehingga di layar
muncul galat tanpa keterangan apa pun. Penolakan di rantai filter sekarang
memakai format yang sama dengan penolakan dari controller.

**4. Baris contoh di template Excel dipindah ke sheet sendiri.** Importer hanya
membaca sheet pertama, jadi tiga baris contoh yang lupa dihapus akan masuk
sebagai mahasiswa sungguhan — dan karena datanya valid, tidak ada galat apa pun
yang memberi tahu. Sheet data kini berisi judul kolom saja.

**5. Redis dihapus dari `docker compose`.** Tidak ada satu pun dependency atau
baris kode yang memakainya, di `api/` maupun `web/`. Containernya nyala dan makan
memori tanpa fungsi.

**6. Container OCR disamakan ke Python 3.14.** Sebelumnya `ocr/Dockerfile` memakai
3.12 sementara pengembangan memakai 3.14. Beda minor version membuat bug
pembacaan sulit direproduksi.

### Ditemukan, belum diputuskan

**Peran DEVELOPER adalah peran yatim.** Dikecualikan dari seluruh endpoint admin;
satu-satunya kemampuannya membuka gambar bukti. Login sebagai DEVELOPER berarti
mendapat dashboard yang gagal memuat. Ini menyambung ke keputusan menggantung
nomor 2 di bawah — sengaja tidak disentuh sampai ada keputusannya.

---

## Daftar kerja berikutnya

Dikerjakan berurutan, dari yang paling mendesak.

| # | Pekerjaan | Kenapa urutannya begini | Status |
|---|---|---|---|
| 1 | Penyesuaian saldo mahasiswa | Satu-satunya fitur yang belum ada. Tanpa ini, kelebihan bayar dan koreksi golongan hanya bisa dibereskan lewat database langsung | ✅ Selesai |
| 2 | Test controller per-endpoint | Aturan peran sudah dikunci `SecurityLayerTest`, tapi validasi masukan dan bentuk jawaban tiap endpoint belum | ✅ Selesai |
| 3 | Playwright end-to-end | Menguji sambungan antar bagian yang tidak terlihat di test satuan: login, unggah, verifikasi, kuitansi | ✅ Selesai |
| 4 | Persiapan deploy VPS | Paling akhir karena butuh keputusan paket hosting, dan lebih aman dilakukan setelah tiga hal di atas beres | ✅ Selesai |

---

## Yang BELUM dikerjakan

### Peran DEVELOPER

Halaman forensik OCR belum dibuat, dan **belum diputuskan** apakah peran ini
masih dibutuhkan di sistem S3.

### Hal teknis yang ditandai untuk dikerjakan nanti

| Berkas | Yang perlu dilakukan | Fase |
|---|---|---|
| ~~`.env` di server~~ | ~~Isi rahasia sebelum deploy~~ Sudah dipaksa: compose menolak menyala kalau kosong | ✅ |
| ~~`COOKIE_SECURE=true`~~ | Sudah jadi bawaan di compose produksi | ✅ |
| `web/src/features/tarif/konstanta.ts` | Label dan pratinjau saja; sumber kebenaran ada di API | — |

### Test otomatis

Sudah ada **172 test** dan semuanya lolos:

- `PaymentGenerationServiceTest` — 17 test aturan hitungan tagihan
- `InstallmentBillingServiceTest` — 9 test aturan ubah nominal
- `PaymentAllocationServiceTest` — 11 test aturan pembagian uang ke cicilan
- `ApiApplicationTests` — 1 test yang menyalakan PostgreSQL asli lewat
  Testcontainers, sekaligus memverifikasi keenam migrasi Flyway
- `OcrJobConsumerTest` — 10 test keputusan otomatis atas hasil pembacaan bukti,
  termasuk kapan nominal boleh ditulis ulang
- `SecurityLayerTest` — 8 test lapisan HTTP: peran mana yang diterima di
  endpoint admin, dan bentuk badan jawaban penolakan
- `AuthServiceTest` — 7 test aturan sesi dan pencabutan token
- `StudentExcelTemplateTest` — 4 test bentuk berkas template import
- `AdjustmentServiceTest` — 17 test aturan penyesuaian saldo dan cicilan
- `InitialAdminSeederTest` — 4 test pembuatan admin pertama
- `PasswordServiceTest` — 6 test aturan penggantian kata sandi
- `StudentServiceTest` — 5 test pengembalian kata sandi mahasiswa ke NIM

**Lapisan controller**, memakai rantai filter keamanan yang sesungguhnya —
bukan dimatikan seperti kebiasaan pada uji controller, karena justru di sanalah
aturan peran dan bentuk jawaban penolakan ditegakkan:

- `AuthControllerTest` — 9 test; termasuk penjagaan bahwa refresh token tidak
  pernah ikut ke badan jawaban
- `ProofFileControllerTest` — 8 test kepemilikan gambar bukti
- `AdjustmentControllerTest` — 11 test
- `PaymentControllerTest` — 10 test
- `BillingControllerTest` — 9 test
- `StudentControllerTest` — 9 test
- `StudentSelfControllerTest` — 8 test batas wewenang portal
- `SecurityLayerTest` — 8 test

Yang belum:

- Lima controller belum punya test sendiri: `StudyClassController`,
  `TuitionController`, `SystemSettingController`, `StudentImportController`,
  dan `ReportController`. Semuanya sudah tercakup aturan perannya lewat
  `@PreAuthorize` yang sama, tapi validasi masukannya belum dikunci

### Keputusan yang masih menggantung

1. **Hosting.** Spring Boot butuh JVM, shared hosting cPanel tidak mungkin.
   Perlu VPS. Belum dipastikan paket Rumahweb yang dipakai.
2. **Peran DEVELOPER** masih dibutuhkan atau tidak.
3. ~~**Python 3.14** dan wheel `opencv-python`.~~ Terjawab: OpenCV 5.0.0 dan
   `pytesseract` terpasang normal di Python 3.14.7, `ocr/main.py` bisa diimpor
   tanpa galat. Tidak perlu Python 3.12 khusus untuk service OCR.
4. ~~**Kata sandi awal mahasiswa = NIM.**~~ Terjawab: mahasiswa memang tidak
   mengelola kata sandinya sendiri. NIM adalah kata sandinya, dan kalau lupa,
   admin mengembalikannya lewat tombol di halaman detail mahasiswa.

---

## Yang SUDAH selesai dan terverifikasi

### Fase 1 — Fondasi

- Spring Boot 3.5.16 + Java 21, dijalankan lewat Maven Wrapper
- PostgreSQL 16 dan RabbitMQ jalan lewat `docker compose up -d`
- Login JWT: access token 15 menit di memori, refresh token di cookie HttpOnly
- Refresh token ditolak bila dipakai sebagai access token (diuji)
- RBAC: endpoint admin menolak token mahasiswa dengan 403 (diuji)
- Semua error memakai format RFC 7807 ProblemDetail berbahasa Indonesia
- Swagger UI di `/api/swagger-ui.html`
- Next.js 16 + React 19 + Tailwind 4 + shadcn/ui, tema terang dan gelap
- Shell: icon rail + panel sub-navigasi + top bar

### Portal mahasiswa

Di luar enam fase rencana awal, tapi dibutuhkan agar sistem bisa dipakai.

**Backend** — semua endpoint di `/api/me/*` bekerja pada pemilik token, tidak
pernah menerima id mahasiswa dari klien:

- Wizard dokumen wajib lima langkah, urutannya ditegakkan di backend
- Validasi NIK dan nomor KK harus 16 digit
- Mahasiswa boleh mendaftarkan sendiri Pendaftaran dan empat tahap ujian;
  UKT tetap dibuat admin
- Gate Pendaftaran: kategori lain terkunci sampai Pendaftaran lunas
- Unggah bukti bayar sendiri, dengan pemeriksaan kepemilikan cicilan
- Gambar bukti hanya bisa dibuka pemiliknya

**Frontend** — kerangka terpisah dari panel admin: navigasi mendatar, lebar
terbatas, dirancang untuk ponsel.

- `/portal` tagihan beserta tombol bayar per cicilan
- `/portal/dokumen` wizard bertahap dengan penanda langkah selesai
- `/portal/riwayat` menyegarkan sendiri sambil menunggu OCR
- `/portal/profil`
- Pengarahan setelah masuk mengikuti peran: admin ke `/dashboard`,
  mahasiswa ke `/portal`

**Diuji langsung, satu alur penuh:** lompat ke Ijazah ditolak → isi lima dokumen
berurutan → daftar Seminar Proposal ditolak karena Pendaftaran belum lunas →
daftar UKT sendiri ditolak karena wewenang admin → daftar Pendaftaran berhasil →
unggah bukti → bayar cicilan mahasiswa lain ditolak 409 → buka bukti mahasiswa
lain ditolak 403 → OCR membaca Rp 1.200.000 sementara diklaim Rp 1.000.000 →
NEEDS_REVIEW → admin verifikasi → cicilan lunas, kelebihan Rp 200.000 masuk
saldo → Seminar Proposal terbuka.

### Penyesuaian saldo dan cicilan

Menutup dua kasus yang sebelumnya hanya bisa dibereskan lewat database langsung:
kelebihan bayar saat nominal cicilan diturunkan, dan koreksi golongan yang baru
ketahuan setelah mahasiswa mengunggah bukti.

- Migrasi V7: tabel `adjustments`, terpisah dari `installment_amount_changes`
  karena yang satu mencatat perpindahan UANG dan yang lain perubahan BESAR
  TAGIHAN — kalau digabung, riwayatnya bercampur dan sulit ditelusuri
- Sasarannya saldo mahasiswa atau satu cicilan tertentu; pemiliknya diambil dari
  jalur URL, tidak pernah dari badan permintaan
- Cicilan milik mahasiswa lain ditolak
- Alasan wajib minimal 5 karakter, nominal nol ditolak supaya audit tidak terisi
  baris kosong
- Saldo maupun uang yang tercatat masuk tidak boleh jadi minus
- Kelebihan tidak boleh menumpuk di cicilan; admin diarahkan memasukkannya ke
  saldo, karena di cicilan uang itu tidak kelihatan di mana pun
- Status cicilan dihitung ulang, dan tagihan ikut ditutup atau dibuka lagi
- Tiap baris audit menyimpan nilai sesudahnya, jadi riwayat bisa dibaca tanpa
  memutar ulang seluruh mutasi
- Semua mutasi memakai kunci baris

**Ditemukan saat mengerjakan ini:** `PaymentAllocationService` mengubah saldo
mahasiswa **tanpa** kunci baris, padahal komentar kelasnya menyatakan seluruh
proses terkunci. Dua pembayaran yang diverifikasi bersamaan bisa membaca saldo
yang sama lalu saling menimpa, dan salah satunya hilang tanpa jejak. Sudah
diperbaiki memakai `findByIdForUpdate` yang sama.

**Di UI:** tombol Penyesuaian di halaman detail mahasiswa. Arah dipilih lewat
tombol Tambah/Kurangi, bukan dengan mengetik tanda minus — salah tanda di sini
berarti uang bergerak ke arah sebaliknya. Pratinjau menampilkan nilai sebelum
dan sesudah, dan penolakan aturan bisnis muncul sebelum tombol simpan aktif.

### Lint frontend dibersihkan

`npm run build` tidak menjalankan lint, jadi lima error React Hooks menumpuk
tanpa pernah terlihat. Semuanya diperbaiki di sumbernya, bukan dibungkam:

- **Tiga `setState` di dalam effect.** Pada tombol tema diganti
  `useSyncExternalStore` untuk membedakan server dari peramban. Pada panel
  tinjau dan gambar bukti, pengaturan ulang keadaan saat berpindah pembayaran
  kini lewat `key` dari induknya, sehingga komponennya terpasang ulang dan
  keadaannya kosong dengan sendirinya — tanpa render berantai.
- **`renew` diakses sebelum dideklarasikan di `lib/auth.tsx`.** Penjadwal
  perpanjangan token dan fungsi perpanjangannya saling membutuhkan. Lingkarannya
  diputus dengan ref, jadi penjadwal memakai versi terbaru saat waktunya tiba
  alih-alih menyebut fungsi yang belum ada.
- **Dua `watch()` dari React Hook Form** diganti `useWatch`, yang aman
  dimemoisasi. Sebelumnya React Compiler melewatkan seluruh komponen itu.

Satu peringatan sengaja dibiarkan: `useReactTable` dari TanStack Table
mengembalikan fungsi yang tidak bisa dimemoisasi dengan aman, dan tidak ada
alternatifnya. Dibiarkan terlihat, bukan dibungkam, supaya ketahuan kalau suatu
saat pustakanya memperbaikinya.

Satu `eslint-disable` yang tersisa ada di effect pemulihan sesi: itu justru
pemakaian effect yang tepat — menyelaraskan React dengan cookie HttpOnly yang
hanya dipegang peramban — dan perubahan state-nya terjadi setelah permintaan
jaringan, bukan seketika. Alasannya ditulis di sebelahnya.

### Pengelolaan kata sandi

Pembagiannya mengikuti cara kerja bagian keuangan: **staf mengurus kata sandinya
sendiri, mahasiswa tidak.**

**Staf** — `POST /auth/kata-sandi`, khusus peran ADMIN dan DEVELOPER. Menu
"Profil saya" yang selama ini tidak melakukan apa-apa diganti jadi "Ganti kata
sandi" yang benar-benar bekerja. Aturannya di `PasswordService`: kata sandi lama
wajib cocok, yang baru minimal 8 karakter dan tidak boleh sama dengan yang lama.

**Mahasiswa** — kata sandinya adalah NIM-nya, disetel saat import. Ia tidak
punya jalur untuk menggantinya; kalau lupa, ia datang ke bagian keuangan dan
admin menekan **Reset kata sandi** di halaman detail mahasiswa, yang
mengembalikannya ke NIM. Jalur ganti kata sandi mandiri di portal sudah dicabut.

**Celah yang ikut tertutup:** mengganti kata sandi sebelumnya tidak mencabut
sesi mana pun, jadi refresh token lama tetap sah tujuh hari penuh. Padahal orang
mengganti kata sandi justru karena curiga ada yang tahu. Sekarang seluruh sesi
dicabut — pada reset mahasiswa ini penting sekali, karena NIM diketahui banyak
orang. Untuk staf, karena sesi yang sedang dipakai ikut terkena, penggantinya
langsung diantar ke halaman masuk alih-alih terlempar sendiri beberapa menit
kemudian tanpa penjelasan.

**Ditemukan saat mengerjakan ini:** alamat yang tidak dikenal dijawab **500**,
bukan 404 — penangkap serba-guna di `GlobalExceptionHandler` menelan exception
routing Spring, sekaligus mencatatnya sebagai ERROR di log padahal itu kesalahan
pemanggil. Metode HTTP yang salah juga begitu. Keduanya sudah ditangani dan
dikunci uji.

### Persiapan deploy

Petunjuk lengkapnya di [DEPLOY.md](DEPLOY.md). Seluruh stack produksi sudah
dijalankan sungguhan sekali dari nol — lima service menyala sehat, dan admin
berhasil masuk memakai kata sandi dari `.env`.

**Dua hal yang membuat deploy sebenarnya tidak akan berhasil, sudah diperbaiki:**

**1. Pemasangan baru tidak punya jalan masuk sama sekali.** `docker-compose.prod.yml`
menyetel `APP_SEED_ADMIN: "false"`, sementara sistem ini tidak menyediakan
endpoint pembuat pengguna. Deploy bersih akan menghasilkan sistem yang rapi tapi
tidak bisa dibuka siapa pun. Sekarang penyemaian menyala di produksi dan
kredensialnya diambil dari `APP_ADMIN_EMAIL` serta `APP_ADMIN_PASSWORD` yang
wajib diisi; compose menolak menyala kalau keduanya kosong. Terbukti: login
dengan kata sandi dari `.env` dijawab 200, sementara kata sandi bawaan lama
`admin123` dijawab 401.

**2. Cookie sesi terkirim tanpa flag `Secure`.** `COOKIE_SECURE` tidak pernah
disebut di compose produksi, jadi nilainya jatuh ke bawaan `false` dan refresh
token ikut lewat koneksi biasa. Sekarang bawaannya `true` di produksi. Terbukti:
jawabannya kini `Secure; HttpOnly; SameSite=Lax`.

Selain itu, port yang dipublikasikan kini bisa diatur lewat `API_BIND` dan
`WEB_BIND`, supaya di VPS keduanya bisa dikunci ke `127.0.0.1` dan hanya
dijangkau lewat reverse proxy.

### Uji end-to-end

Tujuh belas uji Playwright berjalan terhadap sistem yang benar-benar hidup: Next.js,
Spring Boot, dan PostgreSQL sungguhan, tanpa satu pun bagian yang ditiru. Port
dan basis datanya terpisah dari yang dipakai sehari-hari (3100 / 8081 /
`pembayaran_e2e`), jadi menjalankannya tidak mematikan server pengembangan dan
tidak mencemari datanya. Petunjuknya di `web/e2e/README.md`.

Yang dibuktikan: masuk dan pesan galatnya, pengarahan menurut peran, penjagaan
halaman admin dari mahasiswa, dan alur penyesuaian saldo dari klik admin sampai
angka di basis data.

**Dua bug ditemukan justru karena ujinya lewat peramban sungguhan.**

**1. Seluruh pesan galat tampil sebagai JSON mentah.** `ProblemDetail` dikirim
dengan tipe `application/problem+json`, sementara `web/src/lib/api.ts` hanya
mengenali `application/json`. Badan jawaban dibaca sebagai teks, ekstraksi field
`detail` dilewati, dan yang sampai ke layar pengguna adalah
`{"type":"about:blank","title":"Gagal masuk",...}`. Ini berlaku untuk **setiap**
galat di seluruh aplikasi, bukan cuma halaman login. Sudah diperbaiki.

**2. Menu pengguna mematikan halaman.** Membuka menu di pojok kiri bawah
melempar `Base UI error #31` di build produksi dan seluruh halaman ikut mati,
sehingga **admin tidak punya jalan keluar dari UI sama sekali**. Menu "Tampilkan
kolom" di halaman verifikasi rusak dengan sebab yang persis sama.

Kode #31 ternyata berarti *"MenuGroupContext is missing. Menu group parts must be
used within `<Menu.Group>`"* — pemetaannya tidak ikut dipaketkan, tapi ada di
sumber `@base-ui/react/menu/group/MenuGroupContext.mjs`. `DropdownMenuLabel`
memakai `Menu.GroupLabel`, dan di kedua tempat itu ia dipakai telanjang tanpa
`Group` di atasnya. Sudah diperbaiki, dan kedua menu kini dibuka sungguhan di
`menu.spec.ts`.

Sambil menelusurinya, lima tempat kedapatan melanggar kontrak yang sama: `Button`
dengan `render={<Link/>}` menghasilkan `<a>` padahal Base UI menganggapnya harus
`<button>`. Di mode pengembangan itu peringatan, di produksi bisa dilempar
sebagai galat. Semuanya sudah diberi `nativeButton={false}`.

### Test lapisan controller

Sebelumnya lapisan ini tidak punya test sama sekali. Satu anotasi
`@PreAuthorize` yang hilang tidak membuat test mana pun gagal, tidak membuat
kompilasi gagal, dan tidak kelihatan di layar — endpointnya diam-diam terbuka.

- Anotasi `@ControllerTest` menyalakan satu controller beserta rantai filter
  keamanan yang sesungguhnya, dan `ControllerTestSupport` menyediakan penyusun
  token beserta penulis badan JSON
- Delapan controller terkunci: siapa yang boleh masuk, ke mana permintaan
  diarahkan, dan bagaimana penolakan sampai ke layar
- Pemeriksaan kepemilikan gambar bukti dibuktikan menangkap regresi: dengan
  pemeriksaannya dilepas, dua test langsung gagal dengan 200 alih-alih 403

**Ditemukan saat mengerjakan ini:** penyaring `?status=` pada daftar pembayaran
menerima lebih dari satu nilai, sehingga tipenya `List<PaymentStatus>`. Pesan
galat untuk nilai yang tidak dikenal tidak menyertakan daftar pilihan — beda
dari enum tunggal yang sudah diperbaiki dulu — karena `getRequiredType()`
mengembalikan `List`, bukan enumnya. `GlobalExceptionHandler` kini membuka
pembungkusnya.

### Fase 6 — Laporan & rilis

- Endpoint statistik dashboard memakai SQL agregat, bukan memuat entitas
- Dashboard kini memakai data asli: ringkasan, rincian per kategori dan per
  kelas dengan bar kemajuan, serta antrean verifikasi
- Halaman Pengaturan OCR dan Rekening tersambung ke `system_settings`
- Laporan Excel tiga lembar (Apache POI): ringkasan per kelas, tagihan per
  mahasiswa, riwayat pembayaran
- Kuitansi PDF (OpenPDF) lengkap dengan nomor kuitansi dan nominal terbilang;
  hanya bisa dicetak untuk pembayaran yang sudah diverifikasi
- Dockerfile untuk ketiga service, semuanya berjalan sebagai user non-root
- `docker-compose.prod.yml` menyalakan seluruh sistem dalam satu perintah
- Seeder admin dan cookie Secure kini diatur lewat environment variable

**Diuji langsung:** dashboard mengembalikan angka yang cocok dengan database;
laporan Excel 6 KB terunduh; kuitansi PDF terbaca dengan benar termasuk
terbilang "Satu juta dua ratus ribu rupiah"; kuitansi untuk pembayaran yang
ditolak dijawab 409.

### Fase 5 — Verifikasi & alokasi

- `PaymentAllocationService`: cicilan tujuan dulu, lalu FIFO jatuh tempo terlama,
  sisanya ke saldo mahasiswa — semuanya dengan pessimistic lock
- Sisa kecil dalam toleransi (kode unik bank) dibuang, tidak nyangkut di cicilan
  berikutnya maupun di saldo
- Cicilan tujuan yang sudah lunas **tidak** dialihkan diam-diam; ditandai perlu
  ditinjau supaya aliran uang tetap bisa ditelusuri
- Tagihan otomatis jadi COMPLETED ketika seluruh cicilannya lunas
- Alokasi dipanggil setelah verifikasi otomatis maupun manual
- Endpoint penyaji gambar bukti dengan pemeriksaan hak akses; berkas tidak
  ditaruh di folder publik
- Penyaringan pembayaran per kategori, status, mahasiswa, dan pencarian
- Halaman Verifikasi kini memakai data asli: tabel, split-view dengan gambar
  bukti sungguhan beserta zoom, riwayat status, tombol baca ulang, dan
  verifikasi/tolak yang benar-benar menyentuh database

**Diuji langsung:** unggah Rp 1.200.000 ke cicilan bernominal Rp 900.000 →
cicilan 1 lunas, sisa Rp 300.000 mengalir ke cicilan 2 jadi PARTIAL, saldo tetap
nol karena masih ada cicilan yang belum lunas.

### Fase 4 — OCR

- Service FastAPI di `ocr/` membungkus `ocr_processor.py`; model dimuat sekali
  saat menyala, bukan spawn proses Python tiap unggahan seperti sistem lama
- Python 3.14 + OpenCV 5.0 + Tesseract 5.5 dengan bahasa `ind` dan `eng`,
  versinya sama antara mesin pengembangan dan container
- Berkas bahasa dibundel di `ocr/tessdata/`, jadi tidak bergantung pada
  instalasi Tesseract yang menyertakan bahasa Indonesia
- Migrasi V5: `payments`, `verification_logs`, `system_settings`
- RabbitMQ dengan dead-letter queue; pesan dikirim setelah transaksi commit
  supaya pekerja tidak membaca baris yang belum tersimpan
- Keputusan otomatis: terverifikasi bila keyakinan ≥ ambang dan nominal cocok,
  ditolak bila keyakinan di bawah ambang bawah, selain itu perlu ditinjau
- Nominal disesuaikan ke angka yang benar-benar terbaca di bukti bila berbeda
- Keputusan manual admin tidak pernah ditimpa hasil OCR yang datang belakangan
- Ambang keyakinan, toleransi selisih, blacklist, dan rekening tujuan disimpan
  di `system_settings`, bisa diubah tanpa deploy ulang
- Kunci golongan potongan kini aktif sungguhan (bukan lagi penampung sementara)

**Diuji langsung:** unggah bukti Rp 1.200.000 → terbaca 100% → AUTO_VERIFIED;
bukti dengan nominal diklaim berbeda → NEEDS_REVIEW dengan catatan selisih;
tolak manual tanpa alasan → 409; ubah golongan mahasiswa yang sudah upload → 409;
service OCR dimatikan → 3 percobaan ulang → dead-letter queue → status FAILED →
"baca ulang" setelah OCR hidup → AUTO_VERIFIED.

### Fase 3 — Tagihan (backend)

- Migrasi V4: `payment_plans`, `installments`, `installment_amount_changes`
- Nominal UKT dihitung dari tarif dasar dikali potongan golongan, terbukti sesuai
  brosur untuk kelima golongan
- Sisa pembagian menempel di cicilan terakhir; jumlah cicilan selalu sama persis
  dengan total
- Jatuh tempo Gasal Sep–Jan dan Genap Feb–Jun, terbukti termasuk pergantian tahun
- Tarif dan persen potongan dibekukan di plan saat dibuat
- Maksimal 6 semester UKT, plan ganda ditolak, kategori sekali bayar hanya sekali
- Urutan wajib empat tahap ujian ditegakkan di backend (bukan cuma di UI)
- Biaya ujian tidak kena potongan walau mahasiswanya golongan kerjasama
- Ubah nominal cicilan: alasan wajib, audit tercatat, `total_amount` plan
  dihitung ulang, `amount_paid` tidak pernah disentuh, memakai pessimistic lock

### Fase 3 — UI admin tagihan

- Halaman detail mahasiswa `/mahasiswa/{id}`: ringkasan total ditagih, sudah
  dibayar, sisa, dan saldo; peringatan bila dokumen wajib belum lengkap
- Kartu per tagihan dengan tabel cicilan: jatuh tempo, nominal, dibayar, sisa,
  status
- Dialog "Buat tagihan" dengan pratinjau nominal sebelum disimpan
- Dialog ubah nominal tersambung API, lengkap dengan riwayat audit dan
  peringatan bila nominal turun di bawah yang sudah dibayar
- Nama mahasiswa di tabel jadi tautan ke halaman detail

### Fase 2 — Master data

- Migrasi V2 dan V3: kelas, mahasiswa, tarif, golongan potongan, template angsuran
- Hitungan potongan terbukti sesuai brosur: 2.000.000 / 1.600.000 / 1.500.000 /
  1.300.000 / 1.200.000 per cicilan
- Import Excel dengan Apache POI: kelas dibuat otomatis, baris gagal dilaporkan
  per baris tanpa membatalkan yang lain (diuji)
- Halaman Mahasiswa, Import, Kelas, Tarif & Potongan tersambung ke API asli

### Bug yang sudah ditemukan dan diperbaiki

1. Baris keterangan di template Excel ikut terbaca sebagai data mahasiswa
2. `GET /students` balas 500 — pola `(:param IS NULL OR ...)` gagal di
   PostgreSQL, diganti JPA Specification
3. Nilai enum tidak dikenal balas 500, sekarang 400 dengan daftar pilihan
4. `LazyInitializationException` akibat `open-in-view: false`, diperbaiki
   dengan `@EntityGraph`
5. `Select` Base UI mengirim `string | null`, tertangkap TypeScript saat build
6. TanStack Table v9 API-nya berubah total, diturunkan ke v8
7. Literal enum di JPQL (`... = PaymentCategory.UKT`) membuat Hibernate mengecast
   ke nama kelas Java `::PaymentCategory`, padahal tipe PostgreSQL-nya bernama
   `payment_category`. Diperbaiki dengan mengirim enum sebagai parameter query.
