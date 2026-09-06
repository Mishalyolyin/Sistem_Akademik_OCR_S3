# Status Pengerjaan

Daftar apa yang sudah jadi dan apa yang belum. Diperbarui tiap kali ada bagian
yang selesai. Rencana lengkapnya ada di [RENCANA_V2.md](RENCANA_V2.md).

Terakhir diperbarui: 6 September 2026, 08.40

---

## Verifikasi terakhir

Dijalankan 6 September 2026 pukul 08.40 di mesin pengembangan, semuanya lolos:

| Yang dicek | Perintah | Hasil |
|---|---|---|
| Test backend | `api/mvnw test` | ✅ 331 test lolos, BUILD SUCCESS |
| Ketikan frontend | `npx tsc --noEmit` | ✅ tanpa galat |
| Test unit frontend | `npm test` | ✅ 49 test lolos di 5 berkas |
| Lint frontend | `npx eslint src/` | ✅ 0 error, 1 warning yang memang tak bisa diperbaiki |
| Build frontend | `npm run build` | ✅ 23 rute terbentuk |
| Service OCR (mesin) | impor `cv2`, `pytesseract`, `main` | ✅ OpenCV 5.0.0 di Python 3.14.7 |
| Service OCR (container) | `docker build` lalu `GET /health` | ✅ Python 3.14.7, OpenCV 5.0.0, Tesseract 5.5.0 |
| Susunan compose | `docker compose config` | ✅ dev terbaca; prod menolak tanpa `.env` terisi — memang penjagaannya |
| Stack produksi | `up -d --build` lalu login | ✅ 5 service hidup, admin bisa masuk |
| Endpoint dataset OCR | `GET /reports/dataset-ocr.csv` di stack produksi | ✅ 200 `text/csv`, header kolom benar, tanpa token 401 |

Toolchain yang terpasang: JDK 21.0.12.1, Node 24.19.0, Python 3.14.7, Docker 29.7.2.

> **Catatan cara menjalankan verifikasi stack produksi.** Port 8080 dan volume
> `postgres-data` biasanya sudah dipakai lingkungan pengembangan. Jalankan
> verifikasi dengan nama project dan port tersendiri supaya keduanya tidak
> saling menimpa:
> `docker compose -p verif --env-file <env> -f docker-compose.prod.yml up -d --build`
> dengan `API_BIND=8081` dan `WEB_BIND=3001`, lalu `down -v` setelah selesai.
> Tanpa `-p`, compose produksi memakai nama container yang sama dengan compose
> dev dan akan me-recreate lalu menghapus container dev — datanya selamat karena
> ada di volume, tapi containernya harus dinyalakan ulang.

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

~~**Peran DEVELOPER adalah peran yatim.**~~ Sudah dibereskan — lihat bagian
"Peran DEVELOPER akhirnya punya halaman" di bawah. Catatan lama ini juga keliru
menyebut akibatnya "dashboard yang gagal memuat"; kenyataannya login berakhir di
spinner selamanya.

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

### Hal teknis yang ditandai untuk dikerjakan nanti

| Berkas | Yang perlu dilakukan | Fase |
|---|---|---|
| ~~`.env` di server~~ | ~~Isi rahasia sebelum deploy~~ Sudah dipaksa: compose menolak menyala kalau kosong | ✅ |
| ~~`COOKIE_SECURE=true`~~ | Sudah jadi bawaan di compose produksi | ✅ |
| `web/src/features/tarif/konstanta.ts` | Tinggal label kategori dan rumus tampilan; angka tarifnya hanya cadangan saat API belum termuat | — |

### Test otomatis

Sudah ada **331 test backend** dan semuanya lolos:

- `PaymentGenerationServiceTest` — 17 test aturan hitungan tagihan
- `InstallmentBillingServiceTest` — 9 test aturan ubah nominal
- `PaymentAllocationServiceTest` — 17 test aturan pembagian uang ke cicilan dan
  penarikannya kembali
- `PaymentServiceBatalTest` — 7 test pembatalan keputusan verifikasi
- `StudentDocumentCheckTest` — 12 test ringkasan pembacaan dokumen
- `ApiApplicationTests` — 1 test yang menyalakan PostgreSQL asli lewat
  Testcontainers, sekaligus memverifikasi kesebelas migrasi Flyway
- `OcrJobConsumerTest` — 10 test keputusan otomatis atas hasil pembacaan bukti,
  termasuk kapan nominal boleh ditulis ulang
- `SecurityLayerTest` — 8 test lapisan HTTP: peran mana yang diterima di
  endpoint admin, dan bentuk badan jawaban penolakan
- `AuthServiceTest` — 7 test aturan sesi dan pencabutan token
- `StudentExcelTemplateTest` — 7 test bentuk berkas template import, termasuk
  daftar golongan yang mengikuti isi database
- `AdjustmentServiceTest` — 17 test aturan penyesuaian saldo dan cicilan
- `InitialAdminSeederTest` — 4 test pembuatan admin pertama
- `PasswordServiceTest` — 6 test aturan penggantian kata sandi
- `StudentServiceTest` — 14 test pengembalian kata sandi mahasiswa ke NIM,
  pemeriksaan golongan saat mahasiswa dipindah, dan penjagaan penghapusan
- `StudentImportServiceTest` — 5 test pemeriksaan golongan pada berkas import
- `OcrDatasetExporterTest` — 5 test pengutipan sel CSV dataset
- `OcrDatasetExporterIntegrationTest` — 6 test query dataset di PostgreSQL asli
  lewat Testcontainers: penyaringan label manusia, kolom turunan dari flags, dan
  `ocr_data` yang tidak punya kunci `flags` sama sekali

**Lapisan controller**, memakai rantai filter keamanan yang sesungguhnya —
bukan dimatikan seperti kebiasaan pada uji controller, karena justru di sanalah
aturan peran dan bentuk jawaban penolakan ditegakkan:

- `AuthControllerTest` — 14 test; termasuk penjagaan bahwa refresh token tidak
  pernah ikut ke badan jawaban
- `ProofFileControllerTest` — 8 test kepemilikan gambar bukti
- `AdjustmentControllerTest` — 11 test
- `PaymentControllerTest` — 10 test
- `BillingControllerTest` — 9 test
- `StudentControllerTest` — 16 test
- `StudentSelfControllerTest` — 9 test batas wewenang portal
- `StudentDocumentControllerTest` — 10 test pembukaan dokumen wajib
- `StudyClassControllerTest` — 8 test pengelolaan kelas
- `SystemSettingControllerTest` — 6 test pengaturan sistem
- `StudentImportControllerTest` — 7 test unggahan Excel
- `ReportControllerTest` — 12 test laporan, kuitansi, dan dataset OCR
- `DashboardControllerTest` — 5 test bentuk angka ringkasan
- `ForensicControllerTest` — 10 test forensik OCR dan batas perannya
- `ReminderServiceTest` — 16 test aturan pengingat jatuh tempo
- `GatewayWhatsAppSenderTest` — 8 test pengiriman ke gateway
- `ReminderControllerTest` — 5 test halaman pengingat
- `DeveloperSeederTest` — 4 test pembuatan akun forensik
- `TuitionControllerTest` — 8 test pengelolaan golongan potongan
- `SecurityLayerTest` — 8 test

**Seluruh controller kini punya test sendiri.** Empat belas dari empat belas.

### Keputusan yang masih menggantung

1. **Hosting.** Spring Boot butuh JVM, shared hosting cPanel tidak mungkin.
   Perlu VPS. Belum dipastikan paket Rumahweb yang dipakai.
2. ~~**Peran DEVELOPER** masih dibutuhkan atau tidak.~~ Terjawab: dipertahankan
   dan dilengkapi halaman forensik OCR beserta jalan membuat akunnya.
3. ~~**Python 3.14** dan wheel `opencv-python`.~~ Terjawab: OpenCV 5.0.0 dan
   `pytesseract` terpasang normal di Python 3.14.7, `ocr/main.py` bisa diimpor
   tanpa galat. Tidak perlu Python 3.12 khusus untuk service OCR.
4. ~~**Kata sandi awal mahasiswa = NIM.**~~ Terjawab: mahasiswa memang tidak
   mengelola kata sandinya sendiri. NIM adalah kata sandinya, dan kalau lupa,
   admin mengembalikannya lewat tombol di halaman detail mahasiswa.

---

## Yang SUDAH selesai dan terverifikasi

### Dua fitur yang tertulis di rencana tapi tidak pernah dibangun

Pemeriksaan RENCANA_V2.md terhadap kode menemukan dua fitur yang tercantum di
rencana namun tidak ada satu baris pun yang mengerjakannya, dan keduanya juga
tidak pernah tercatat di sini — jadi bukan "dikerjakan lalu dicoret", memang
luput. Sekaligus rencananya diselaraskan dengan kenyataan.

**1. Command palette (Ctrl+K).** Tombol "Cari · Ctrl K" sudah lama duduk di top
bar dalam keadaan `disabled`. Sekarang ia hidup, dan pintasannya bekerja dari
halaman mana pun di shell admin.

Yang bisa dicari: halaman, mahasiswa (nama atau NIM, dicari ke server dengan
jeda ketik), dan tindakan seperti ganti tema atau keluar. Daftar halamannya
**diturunkan dari `nav-config`, bukan didaftar ulang** — menu baru otomatis ikut
tercari, dan yang lebih penting, penyaringan perannya memakai fungsi yang sama
persis dengan rail ikon. Tanpa itu, seorang DEVELOPER bisa menemukan
"Verifikasi Pembayaran" lewat Ctrl+K walau menu itu tak pernah tampak olehnya,
lalu mendarat di halaman yang endpointnya menolaknya. Pencarian mahasiswa juga
tidak dikirim sama sekali untuk peran selain ADMIN, karena `/students` memang
menjawabnya 403.

Isi paletnya dipisah ke komponen yang hanya dirender saat palet terbuka, jadi
ketikan dan sorotan sesi sebelumnya hilang karena komponennya memang mati —
bukan karena ada effect yang membersihkannya. Repo ini melarang `setState` di
dalam effect lewat aturan React Compiler, dan pengaturan ulang sorotan saat
daftar berubah memakai penyesuaian state saat render, pola yang memang
disarankan React untuk keadaan turunan.

**2. Export dataset pembacaan bukti untuk ML.** `GET /reports/dataset-ocr.csv`,
dan tombolnya ada di halaman Laporan. Satu baris per bukti bayar: apa yang
dibaca mesin (keyakinan, nominal, tanggal, bank, flag, panjang teks) disandingkan
dengan keputusan akhir admin. Ambang keyakinan yang dipakai hari ini ditebak,
bukan diukur; data inilah yang nanti mengukurnya.

Keputusan yang paling menentukan isinya: **hanya baris yang diputuskan manusia
yang ikut** (`verified_by IS NOT NULL`). Pembayaran `AUTO_VERIFIED` adalah
tebakan mesin itu sendiri, dan memasukkannya sebagai label berarti melatih model
dari jawabannya sendiri — kesalahan yang sudah ada justru dikukuhkan, bukan
diperbaiki. Verifikasi yang dibatalkan admin ikut keluar dengan sendirinya,
karena pembatalan mengosongkan `verified_by`.

Teks mentah OCR adalah fitur paling berguna sekaligus paling sensitif: di
dalamnya ada nama, nomor rekening, dan saldo. Ia hanya ikut kalau diminta lewat
`?sertakanTeks=true`, dan di layar centangnya diberi peringatan dan tidak
diingat antar kunjungan. Nama mahasiswa tidak pernah jadi kolom tersendiri —
yang ada hanya `student_id`.

Query-nya diuji ke PostgreSQL sungguhan lewat Testcontainers, bukan ke JdbcClient
yang di-mock. Ia bertumpu pada operator `jsonb`, `jsonb_array_elements_text`,
`LATERAL`, dan cast enum ke teks; query yang salah ketik akan lolos mulus di
test yang me-mock dan baru meledak di layar admin. Satu kasus yang khusus
diuji: `ocr_data` yang tidak punya kunci `flags` sama sekali — bentuk yang
mungkin datang dari bukti lama — karena `jsonb_array_elements_text` melempar
galat kalau yang diberikan bukan array.

**Rencana ikut diselaraskan.** RENCANA_V2.md masih mencantumkan Redis di tabel
stack dan di diagram arsitektur, padahal audit 3 September membuangnya dan tidak
ada satu pun dependency maupun baris kode yang memakainya. Roadmapnya juga masih
berisi estimasi mingguan untuk Fase 2–6 yang sebenarnya sudah selesai, dan dua
keputusan yang sudah terjawab di sini masih menggantung di sana. Ditambahkan
pula `payment_allocations`, `reminder_logs`, dan `students.pendaftaran_exempt`
yang selama ini ada di kode tanpa pernah disebut di rencana.

### OCR dokumen mahasiswa akhirnya disambungkan

Service OCR sudah bisa membaca KTP, Kartu Keluarga, ijazah, dan menganalisis
foto **sejak awal** — `ocr/ocr_processor.py` punya `process_ktp`, `process_kk`,
`process_ijazah`, dan `process_photo`, dan `main.py` menerima keempat jenisnya.
Yang tidak pernah ada adalah sisi Spring yang mengirim berkasnya ke sana. Jadi
kemampuan itu menganggur sepenuhnya, dan admin memeriksa tiap dokumen dengan
mata tanpa satu pun petunjuk.

- Migrasi V11 menambah empat kolom JSONB untuk hasil pembacaan, plus
  `birth_place` dan `birth_date` — keduanya **tidak punya isian manual di mana
  pun**, jadi nilainya memang hanya bisa datang dari ijazah
- Antrean dokumen **dipisah** dari antrean bukti bayar. Keduanya punya
  kepentingan berbeda: bukti bayar menahan uang dan harus segera diputuskan,
  sementara pembacaan dokumen hanya membantu. Menumpuknya di satu antrean
  membuat unggahan dokumen massal saat pendaftaran menunda pembacaan bukti bayar
- Pembacaan berjalan di belakang layar dan **tidak menahan unggahan**: mahasiswa
  tidak menunggu Tesseract, dan service OCR yang sedang mati tidak membuat
  unggahannya gagal. Gate dokumen tidak bergantung padanya sama sekali
- Unggah ulang **membuang hasil lama** lebih dulu; hasil yang tertinggal akan
  dibaca admin sebagai hasil berkas yang baru
- Tempat dan tanggal lahir hanya diisi bila masih kosong. Sekali admin
  membetulkannya dengan mata sendiri, pembacaan ulang tidak boleh menimpanya
  lagi dengan tebakan mesin

**Tidak ada dokumen yang diterima atau ditolak otomatis.** Tesseract tidak bisa
memeriksa hologram, stempel, atau tanda tangan; berpura-pura bisa justru
berbahaya. Yang disajikan adalah **kecocokan** — NIK yang terbaca sama atau
tidak dengan yang diketik, nama di ijazah sama atau tidak dengan nama mahasiswa.

Kesimpulannya punya **tiga** keadaan, bukan dua: cocok, tidak cocok, dan tidak
bisa disimpulkan. Yang ketiga berarti mesinnya gagal membaca — bukan alasan
mencurigai mahasiswanya. Menyamakan keduanya membuat admin curiga pada dokumen
yang sebenarnya baik-baik saja.

**Diuji langsung terhadap sistem yang hidup**, dengan service OCR sungguhan
(Tesseract 5.4, OpenCV 5.0): mahasiswa mengunggah KTP → NIK terbaca
`3374010101990001` dan disimpulkan cocok → mengunggah KTP yang sama tapi
mengetik NIK berbeda → **"NIK berbeda: terbaca 3374010101990001, diketik
3374019999999999"** → mengunggah ijazah → nama cocok, dan tempat/tanggal lahir
**SEMARANG, 1995-05-10** terisi sendiri dari pembacaan, data yang sebelumnya
tidak bisa ditangkap sistem ini sama sekali. Berkas dan data ujinya dikembalikan
seperti semula.

### Verifikasi yang keliru akhirnya bisa dibatalkan

Ditemukan saat membandingkan ulang dengan sistem Laravel: di sana ada
`resetStatus` yang mengembalikan pembayaran ke antrean **sekaligus membongkar
alokasinya**. Di sini tidak ada padanannya sama sekali — `requeue` justru
menolak menyentuh yang sudah diverifikasi.

Artinya bukti palsu yang telanjur diverifikasi, atau tombol yang salah pencet,
hanya bisa dibetulkan lewat basis data langsung. Penyesuaian saldo tidak
setara: uangnya bisa ditarik, tapi barisnya tetap berbunyi `VERIFIED`,
kuitansinya tetap bisa dicetak, dan jejak auditnya tetap menyatakan uang itu
masuk.

**Yang menghalangi selama ini: tidak ada catatan ke mana uangnya pergi.**
Alokasi hanya meninggalkan `allocated_at` dan perubahan `amount_paid`. Begitu
dua pembayaran masuk ke tagihan yang sama, angkanya bercampur dan tidak bisa
diurai lagi. Migrasi V10 menambah `payment_allocations`: satu baris per aliran
uang, termasuk sisa yang dibuang karena toleransi, sehingga jumlah seluruh
barisnya selalu sama persis dengan nominal pembayarannya.

- Pembatalan membaca rincian itu dan membalikkannya **persis**, bukan
  menghitung ulang seisi tagihan
- Sistem lama menarik saldo dengan menerka: `max(0, totalDialokasikan −
  totalTagihan)`. Terkaan itu meleset begitu saldo juga pernah diisi lewat
  Penyesuaian — uang yang tidak ada hubungannya ikut lenyap. Di sini yang
  ditarik hanya baris `WALLET` milik pembayaran itu sendiri
- Kelebihan yang **sudah telanjur terpakai** menahan pembatalan, bukan
  dipangkas jadi nol. Sistem lama memangkasnya dengan `max(0, ...)`, dan
  selisihnya hilang tanpa ada yang tahu
- Yang **ditolak** juga bisa dibatalkan; penolakan pun bisa keliru — bukti sah
  bisa terlanjur ditolak karena gambarnya kurang jelas
- Uang ditarik **lebih dulu**, baru statusnya diubah. Kalau urutannya terbalik
  dan penarikannya gagal, pembayaran berakhir di antrean sementara uangnya
  tetap tercatat masuk
- Alokasi lama yang dibuat sebelum rinciannya dicatat **ditolak dengan
  penjelasan**, bukan ditebak — dan diarahkan ke Penyesuaian

**Di UI:** tombol "Batalkan keputusan" di panel tinjau, muncul hanya untuk yang
sudah diputuskan, dengan dialog konfirmasi yang menyebutkan bahwa uangnya ikut
ditarik dan kuitansinya tidak bisa dicetak lagi. Alasan wajib, minimal 5
karakter, sama seperti penolakan dan perubahan nominal.

**Diuji langsung terhadap sistem yang hidup:** bayar 3.000.000 ke cicilan yang
sisanya 900.000 → rinciannya tercatat 900.000 + 1.200.000 + 900.000, berjumlah
tepat 3.000.000 → dibatalkan, ketiga cicilan kembali persis ke angka semula →
bayar 20.000.000 sehingga 15.500.000 masuk saldo, lalu admin menambah 2.000.000
lewat Penyesuaian → dibatalkan, saldo tersisa **2.000.000**, bukan nol → bayar
30.000.000 lalu saldonya dipakai 25.000.000 → pembatalan **ditolak 409** dengan
menyebut berapa yang kurang. Seluruh data uji dikembalikan seperti semula.

### Pengingat jatuh tempo lewat WhatsApp

Disebut di rencana sejak awal, nol implementasi sampai sekarang: tidak ada satu
pun `@Scheduled` di seluruh backend, dan `@EnableScheduling` belum pernah
dinyalakan — jadi anotasi jadwal apa pun akan diabaikan tanpa peringatan.

Gateway-nya Fonnte/Wablas: POST form biasa dengan token di header. Alamat dan
tokennya disimpan di pengaturan sistem, bukan di berkas konfigurasi, supaya bisa
diganti tanpa deploy ulang — termasuk saat nomor pengirim perlu dipindah karena
diblokir.

**Aturannya berpihak pada tidak mengganggu.** Satu pengingat per cicilan per
jenis, selamanya — mahasiswa yang menunggak dua bulan tidak menerima enam puluh
pesan. Pengingat "menjelang" hanya dikirim pada hari yang tepat, bukan tiap hari
sepanjang rentangnya. Penjagaannya ada di database juga, bukan cuma di kode:
indeks unik parsial pada `(installment_id, kind) WHERE status = 'SENT'`, karena
pemeriksaan di aplikasi tidak menahan dua proses yang berjalan bersamaan.

Yang gagal terkirim **tidak** dianggap sudah dikirim, jadi dicoba lagi keesokan
harinya — tapi jejaknya tetap dicatat supaya kegagalan berulang kelihatan.

**Dua cacat yang ketahuan justru karena mengujinya sungguhan:**

**1. Uji coba tanpa gateway menghabiskan jatah pengingat.** Dengan token kosong,
pesan hanya dicatat di log — dan semula tetap tercatat `SENT`. Karena satu
cicilan hanya diingatkan sekali, satu putaran percobaan berarti mahasiswa itu
tidak akan pernah diingatkan lagi begitu gateway benar-benar dipasang. Sekarang
dicatat `SKIPPED` beserta alasannya. Terbukti: setelah token diisi, kedua
pengingat yang tadinya dilewati benar-benar dikirim ulang.

**2. Gateway menolak sambil menjawab HTTP 200.** Dicoba dengan token yang salah,
Fonnte membalas **200** berisi `{"status":false}`. Tanpa membaca badan
jawabannya, token yang keliru membuat seluruh pengingat tercatat terkirim
padahal tidak satu pun sampai — dan kekeliruannya permanen karena jatahnya
telanjur habis. Sekarang badan jawaban ikut diperiksa.

**Di UI:** halaman Pengaturan → Pengingat WhatsApp, berisi keenam pengaturannya,
peringatan bila token belum diisi, riwayat 50 kiriman terakhir beserta alasan
gagalnya, dan tombol **jalankan sekarang** dengan konfirmasi. Tombolnya ada
karena penjadwal cuma berjalan sekali sehari: tanpa itu admin baru tahu
pengaturannya salah keesokan harinya — atau tidak tahu sama sekali.

**Diuji langsung terhadap sistem yang hidup:** migrasi V9 terpasang → putaran
pertama menemukan 0 cicilan (memang belum ada yang jatuh tempo dalam tiga hari)
→ ambangnya digeser ke H-5, 2 cicilan ketemu → nomor `081234567892` dirapikan
jadi `6281234567892` → keduanya `SKIPPED` karena gateway belum diatur, dengan
isi pesan lengkap terlihat di log → token diisi, keduanya benar-benar terkirim.
Data uji dan pengaturannya dikembalikan seperti semula.

**Sekalian dibersihkan:** `admin_phone_notification` dihapus. Ia disemai sejak
V5, tampil di halaman Pengaturan, dan tidak pernah dibaca satu baris kode pun.

### Peran DEVELOPER akhirnya punya halaman, dan bisa masuk

Peran ini yatim sejak awal: dikecualikan dari seluruh endpoint admin, satu-satunya
kemampuannya membuka gambar bukti, dan **tidak ada cara membuat akunnya** selain
INSERT langsung ke database.

**Bug yang lebih parah dari yang tercatat.** Catatan lama menyebut login sebagai
DEVELOPER berujung "dashboard yang gagal memuat". Kenyataannya lebih buruk:
`berandaUntuk()` mengarahkan setiap non-mahasiswa ke `/dashboard`, sementara
seluruh grup `(app)` dijaga `role="ADMIN"` — jadi ia dilempar ke halaman yang
justru menolaknya, berulang tanpa henti. Yang terlihat hanya **"Memeriksa
sesi…" selamanya**. Tidak ada satu pun request yang gagal, jadi tidak ada yang
bisa ditangkap kecuali dengan membuka peramban.

- `AuthGuard` kini menerima daftar peran, dan `berandaUntuk()` mengembalikan
  `/forensik` untuk DEVELOPER
- `GerbangForensik` menahannya di halaman itu: tanpa penjagaan ini ia bisa
  membuka `/mahasiswa` dan mendapat halaman gagal memuat tanpa penjelasan
- Navigasi jadi sadar peran. Bawaannya **hanya ADMIN**, jadi menu baru tidak
  diam-diam muncul untuk peran yang endpointnya justru menolaknya
- `DeveloperSeeder`: akun forensik lewat `APP_SEED_DEVELOPER`. Mati secara
  bawaan, dan **tidak punya kata sandi bawaan** — akun ini bisa membaca hasil
  pembacaan bukti bayar seluruh mahasiswa, jadi kata sandi yang tertulis di kode
  sama saja membukanya untuk umum

**Halaman forensiknya** menyajikan hasil OCR **mentah apa adanya** sebagai JSON,
bukan dirapikan jadi beberapa field yang dikenal — bentuk jawaban service OCR
bisa berubah, dan yang dicari saat menelusuri justru field yang tidak diduga ada.
Ditambah catatan mesin, dan riwayat keputusan yang **membedakan keputusan mesin
dari keputusan admin**. Ada saringan "pembacaan meragukan" yang juga memuat bukti
yang belum pernah terbaca sama sekali — justru itu kasus yang paling perlu
ditelusuri.

**Dua hal yang ketahuan justru karena mengujinya sungguhan:**

**1. Keyakinan OCR tersimpan sebagai pecahan 0–1, bukan persen.** Halaman
forensik semula memperlakukannya sebagai persen: `1.0` tampil sebagai "1%", dan
saringan "di bawah 80" akan mencocokkan **seluruh** baris karena tidak ada nilai
yang lebih besar dari 1. Sekarang memakai komponen `KeyakinanOcr` yang sudah ada,
dan satuannya ditulis jelas di DTO maupun di tipe TypeScript-nya.

**2. Seluruh menu utama tidak punya nama aksesibel.** Rail navigasi hanya berisi
ikon; tooltipnya cuma muncul saat disorot tetikus. Bagi pembaca layar, tiap menu
terbaca sebagai "link" tanpa nama — sejak awal, bukan akibat perubahan ini.
Sudah diberi `aria-label`.

**Diuji langsung terhadap sistem yang hidup:** seeder menyalakan akun developer
(tercatat di log) → login berhasil → forensik terbuka berisi 6 bukti → kelima
endpoint admin (`/students`, `/classes`, `/dashboard/summary`, `/reports`,
`/settings`) menjawab 403 → saringan pecahan 0.8 mengembalikan 0 dari 6, sesuai
karena semua pembacaannya yakin → detail memuat `raw_text` dan `extracted_amount`
yang tidak ada di DTO mana pun → riwayat menunjukkan dua keputusan otomatis
disusul satu keputusan admin. Akun ujinya dihapus lagi setelahnya.

**Empat uji Playwright baru** mengunci bug loop-nya di peramban: developer
mendarat di forensik dan bukan di spinner, dikembalikan ke forensik saat membuka
halaman admin, menunya hanya berisi forensik, dan admin tetap bisa membukanya
lewat menu.

### CI, test unit frontend, dan lima controller terakhir

Tiga hal yang tertulis di rencana sejak awal tapi tidak pernah ada: GitHub
Actions, Vitest, dan test untuk lima controller terakhir.

**CI.** `.github/workflows/ci.yml` menjalankan dua pekerjaan paralel — backend
(`mvnw verify`, Testcontainers menyalakan PostgreSQL sungguhan di runner) dan
frontend (ketikan, lint, test unit, build). Lint sengaja dijalankan sebagai
langkah terpisah karena `next build` **tidak** menjalankannya; justru begitulah
dulu lima galat React Hooks sempat menumpuk tanpa terlihat. Laporan surefire
diunggah sebagai artifact kalau backend gagal.

> **Belum menyala.** Branch `migrasi-v2` belum pernah didorong ke `origin`, jadi
> workflow-nya belum pernah dijalankan GitHub. Ia baru hidup setelah branch ini
> di-push.

**Vitest.** 33 test unit di empat berkas, semuanya logika murni yang memang
pernah salah atau memang penting:

- `lib/api.test.ts` — penguraian `application/problem+json`. Bug yang dulu
  membuat **seluruh** pesan galat di aplikasi tampil sebagai JSON mentah di
  layar; sekarang dikunci, bersama pembuangan parameter kosong dan pengiriman
  FormData tanpa dipaksa jadi JSON
- `features/tarif/konstanta.test.ts` — kelima angka brosur, dan bahwa lima
  cicilan berjumlah tepat sama dengan UKT satu semester
- `features/tarif/kode-golongan.test.ts` — kode golongan yang diturunkan dari
  namanya diuji terhadap pola yang sama persis dengan yang ditegakkan backend
- `lib/format.test.ts` — nominal yang datang sebagai string BigDecimal, dan
  nilai kosong yang tidak boleh berubah jadi "Rp NaN"

`@types/node` ikut dinaikkan dari 20 ke 24, mengikuti Node yang benar-benar
dipakai (24.19.0). Sebelumnya tipe dan runtime-nya memang sudah tidak cocok.

**Lima controller terakhir** kini punya test sendiri: `StudyClass`,
`SystemSetting`, `StudentImport`, `Report`, dan `Dashboard`. Yang terakhir luput
dari catatan sebelumnya — endpointnya cuma tersentuh `SecurityLayerTest` untuk
aturan peran, sementara bentuk jawaban suksesnya tidak dikunci sama sekali.

**Bug yang ketahuan justru karena menulis testnya:** unggah import **tanpa
berkas** dijawab **500**. `MissingServletRequestPartException` tidak tertangani
dan jatuh ke penangkap serba-guna — keluarga yang persis sama dengan alamat
tidak dikenal dan metode HTTP salah yang sudah diperbaiki lebih dulu. Sekarang
400 dengan menyebut bagian mana yang kurang. Terbukti di sistem hidup: dulu 500,
sekarang `Bagian "file" wajib disertakan dalam permintaan.`

### Mahasiswa dan kelas akhirnya bisa dikelola dari layar

`useUpdateStudent`, `useDeleteStudent`, dan `useUpdateClass` sudah ditulis
lengkap sejak lama — dan tidak pernah dipanggil dari mana pun. Akibatnya satu
huruf yang salah pada nama saat import hanya bisa dibetulkan lewat basis data
langsung, dan kelas cuma bisa dihapus permanen, bukan dinonaktifkan seperti yang
direncanakan.

**Temuan yang menghentikan pekerjaan sejenak:** `payment_plans`, `payments`, dan
`adjustments` semuanya `ON DELETE CASCADE` ke `students`. Artinya `DELETE
/students/{id}` yang sudah ada sejak awal akan menghapus seluruh tagihan,
pembayaran yang sudah diverifikasi beserta kuitansinya, dan jejak audit
penyesuaian — dalam satu perintah, tanpa satu pun galat. Memasang tombol hapus
di layar tanpa membereskan ini sama saja menyediakan tombol penghapus riwayat
keuangan.

- Penghapusan kini ditolak bila mahasiswanya sudah punya bukti bayar, tagihan,
  atau penyesuaian saldo, dan penolakannya menunjuk jalan keluar yang benar:
  nonaktifkan saja
- Akun penggunanya ikut terhapus. Akun tanpa data mahasiswa masih bisa masuk,
  tapi tiap halaman portal menjawab "tidak terhubung ke data mahasiswa"
- Hapus massal untuk membereskan salah import, dilaporkan **per baris** seperti
  import Excel: satu mahasiswa yang ditolak tidak membatalkan yang lain
- Menghapus kelas yang masih berisi mahasiswa dulunya dijawab **500** — kunci
  asingnya tidak punya `ON DELETE`, jadi penolakan database jatuh ke penangkap
  serba-guna. Sekarang 409 dengan jumlah penghuninya disebut

**Di UI:** dialog Ubah data mahasiswa (nama, telepon, kelas, aktif) di halaman
detail, tombol hapus dengan konfirmasi, kolom pilih beserta hapus massal di
tabel mahasiswa, dan di halaman Kelas tombol nonaktifkan/aktifkan sebagai aksi
utama — hapus permanen dipindah ke belakang konfirmasi.

Kolom pilih ditambahkan ke tabel biasa, **bukan** dengan memindahkannya ke
TanStack Table seperti rencana awal. Yang dibutuhkan hanya aksi massal; menyeret
seluruh tabel ke pustaka lain hanya untuk itu menambah satu peringatan lint yang
tidak bisa diperbaiki, tanpa memberi apa pun yang dipakai.

**Diuji langsung terhadap sistem yang hidup:** hapus mahasiswa yang punya bukti
bayar ditolak 409 → hapus kelas berisi 1 mahasiswa ditolak 409 → hapus massal
tiga id (satu bersih, satu punya bukti bayar, satu tidak ada) menjawab 1
berhasil dan 2 ditolak beserta alasan masing-masing → baris mahasiswa dan akun
penggunanya benar-benar hilang, sementara bukti bayar mahasiswa lain tetap utuh
→ ubah nama dan telepon tersimpan → kelas dinonaktifkan lalu diaktifkan lagi.
Data ujinya dikembalikan seperti semula.

### Dokumen wajib akhirnya bisa dibuka

Keempat berkas dokumen wajib — foto, KTP, Kartu Keluarga, ijazah — selama ini
hanya punya jalur unggah. Ada empat `POST`, dan **nol** `GET` di seluruh
backend. Berkasnya tersimpan rapi di disk lalu tidak pernah bisa dibuka lagi
oleh siapa pun, termasuk pemiliknya sendiri.

Akibatnya gate dokumen wajib berjalan sebagai formalitas: mahasiswa mengunggah
KTP, gate-nya terbuka, dan tidak ada seorang pun yang bisa memeriksa apakah yang
diunggah memang KTP. Bagian keuangan hanya melihat tulisan "Lengkap".

- `GET /students/{id}/dokumen/{jenis}` untuk admin, dan `GET /me/dokumen/{jenis}`
  untuk mahasiswa. Jalur portal **tidak menerima id sama sekali** — pemiliknya
  diambil dari token, jadi membuka dokumen orang lain bukan sekadar ditolak,
  melainkan tidak ada alamatnya
- Berkasnya tetap di luar folder publik. KTP dan Kartu Keluarga memuat NIK dan
  alamat; kalau ditaruh di folder statis, siapa pun yang menebak URL bisa
  membukanya tanpa pernah masuk
- Penyaji berkas dipindah ke `StoredFileResponse` yang dipakai bersama bukti
  bayar, supaya aturan cache dan tipe berkasnya tidak berbeda antar tempat
- **Belum diunggah** dibedakan dari **berkas hilang di disk**: yang pertama 409,
  yang kedua 404. Bedanya penting — yang satu berarti mahasiswanya belum
  mengerjakan, yang satu lagi berarti ada yang salah di penyimpanan
- Alamat ditulis huruf kecil (`/dokumen/ktp`), tapi huruf besar tetap diterima;
  konversi enum bawaan Spring justru menolak bentuk yang benar
- NIK dan nomor Kartu Keluarga ikut ditampilkan. Keduanya sudah tersimpan sejak
  awal tapi tidak pernah muncul di layar mana pun

**Di UI:** kartu Dokumen di halaman detail mahasiswa dengan tombol lihat per
berkas, dan pratinjau yang sama dipakai portal mahasiswa pada langkah yang sudah
selesai. Gambar diambil sebagai blob karena `<img src>` tidak bisa membawa
header `Authorization`; object URL-nya dilepas saat dialognya ditutup.

**Diuji langsung terhadap sistem yang hidup:** admin membuka KTP mahasiswa → PNG
700×900 sebesar 48 KB benar-benar keluar → jenis `rapor` ditolak 400 beserta
daftar pilihannya → dokumen yang belum diunggah 409 → mahasiswa membuka miliknya
sendiri lewat `/me` dapat berkas yang byte-nya identik dengan yang dilihat admin
→ mahasiswa yang sama menembak jalur admin ditolak 403 → mahasiswa lain lewat
`/me` hanya mendapat miliknya sendiri → tanpa token 401.

### Golongan potongan jadi data, bukan enum

Menambah satu golongan baru — misalnya kerja sama dengan satu instansi yang
potongannya 30% — sebelumnya berarti mengubah tipe enum di PostgreSQL, mengubah
enum di Java, mengubah union di TypeScript, lalu deploy ulang. Itu pekerjaan
pengembang untuk sesuatu yang sebenarnya keputusan bagian keuangan, dan di sistem
yang sudah jalan artinya golongan baru harus menunggu.

Sekarang golongan adalah baris di `discount_tier_rates`, dan `students` menunjuk
ke sana lewat kunci asing. Migrasi V8 melepas tipe enumnya dan menggantinya
dengan kode teks, ditambah kolom `active` dan `sort_order`.

- **Kodenya dibatasi `^[A-Z][A-Z0-9_]*$`**, di validasi permintaan maupun di
  `CHECK` database. Kode itu dipakai apa adanya di kolom `discount_tier` berkas
  import, jadi spasi atau huruf kecil di sana akan menghasilkan berkas yang
  ditolak tanpa admin tahu sebabnya
- **Golongan lama dinonaktifkan, bukan dihapus** — mahasiswa dan tagihan yang
  terlanjur memakainya tetap harus bisa dibaca. Menonaktifkan golongan yang
  masih dipakai ditolak, dan jumlah mahasiswanya ikut disebut supaya admin tahu
  seberapa besar pekerjaan memindahkan mereka
- **Kode golongan diperiksa di service**, bukan hanya diserahkan ke kunci asing.
  Tanpa itu, salah ketik sampai ke pengguna sebagai kegagalan teknis; sekarang
  jawabannya menyebut pilihan yang ada
- **Template Excel menyusun daftar golongannya dari database.** Petunjuk yang
  menyebut daftar lama justru menuntun orang mengisi kode yang ditolak importer,
  dan baris contohnya kini memakai kode yang benar-benar aktif
- **Portal mahasiswa ikut mengirim nama golongan**, karena portal tidak boleh
  memanggil endpoint admin untuk sekadar menerjemahkan kode jadi label

**Di UI:** tombol "Tambah golongan" di halaman Tarif & Potongan. Kodenya terisi
otomatis dari nama supaya admin tidak perlu memikirkan bentuknya, tapi tetap
bisa disunting. Pratinjau memakai tarif dasar UKT yang sungguhan dari API, bukan
angka brosur di konstanta. Golongan nonaktif ditandai di tabel dan bisa
diaktifkan lagi.

**Diuji langsung terhadap sistem yang hidup:** tambah golongan MITRA_INSTANSI 30%
→ 201 → kode ganda ditolak 409 → kode berspasi ditolak 400 → mahasiswa dipindah
ke golongan itu → salah ketik satu huruf ditolak 409 dengan daftar pilihan →
nonaktifkan golongan yang dipakai 1 mahasiswa ditolak 409 → buat tagihan UKT:
10.000.000 − 30% = 7.000.000 terbagi 5 × 1.400.000 → template import terunduh
sudah memuat MITRA_INSTANSI di petunjuk dan di baris contoh. Data uji dibersihkan
lagi setelahnya.

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

### Ringkasan status di dashboard

Empat kotak baru menghitung bukti bayar per status — menunggu dibaca, perlu
ditinjau, ditolak, dan terverifikasi — dan **tiap kotak bisa diklik** menuju
daftar yang sudah tersaring statusnya.

- Hitungannya satu kueri `GROUP BY`, bukan lima kueri terpisah untuk pertanyaan
  yang sebenarnya sama
- Halaman verifikasi selama ini selalu per kategori, jadi kartu yang menghitung
  lintas kategori tidak punya tujuan yang benar. Ditambahkan tampilan
  **Semua kategori** (`/verifikasi/semua`), yang juga masuk ke menu samping
- Saringan status kini bisa datang dari URL, sehingga tautannya mendarat dalam
  keadaan tersaring, bukan di daftar penuh
- "Terverifikasi" mencakup otomatis dan manual sekaligus. Menautkannya ke salah
  satu saja akan membuat angka di kartu tidak cocok dengan isi daftarnya, jadi
  ditambahkan pilihan saringan gabungan

Saldo mahasiswa naik jadi kotak tersendiri di baris ringkasan, dan barisnya yang
lama di kartu "Per kelas" dihapus supaya tidak tampil dua kali.

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

Dua puluh enam uji Playwright berjalan terhadap sistem yang benar-benar hidup: Next.js,
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
