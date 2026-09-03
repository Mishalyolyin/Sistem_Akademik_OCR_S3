# Sistem Akademik New TOCR

Sistem Akademik New TOCR adalah aplikasi pembayaran SKS berbasis Laravel yang mendukung pengelolaan tagihan mahasiswa, upload bukti bayar, verifikasi otomatis berbasis OCR, pengelolaan kelas, import data mahasiswa, laporan Excel, dan monitoring dashboard admin.

Project ini dirancang untuk dua peran utama:

- Admin kampus untuk mengelola mahasiswa, tarif, pembayaran, laporan, OCR, dan kelas
- Mahasiswa untuk melihat tagihan, memilih skema cicilan, upload bukti bayar, melihat histori, dan mengakses pembayaran munaqosah

## Fitur Utama

- Dashboard admin dengan ringkasan mahasiswa aktif, status pembayaran, tagihan, tren pembayaran, dan statistik OCR per kelas
- Import data mahasiswa dari file Excel/CSV lengkap dengan template import
- Pembuatan payment plan otomatis berdasarkan template angsuran
- Upload bukti pembayaran oleh mahasiswa
- Verifikasi pembayaran manual dan otomatis menggunakan OCR Python + Tesseract
- Validasi OCR terhadap nominal, rekening tujuan, tanggal transaksi, dan nama mahasiswa
- Export laporan pembayaran dan ledger mahasiswa ke Excel
- Cetak receipt pembayaran dalam format PDF
- Pengelolaan kelas, tarif kuliah, penyesuaian tagihan, dan pengaturan OCR dari panel admin
- Modul munaqosah untuk mahasiswa yang sudah melunasi tagihan semester
- Reminder pembayaran via WhatsApp melalui command terjadwal
- Export dataset pembayaran terverifikasi untuk kebutuhan pelatihan machine learning

## Teknologi yang Digunakan

- PHP 8.2
- Laravel 12
- MySQL
- Vite + Tailwind CSS
- Laravel Sanctum
- Laravel DomPDF
- Laravel Excel
- Python 3
- Tesseract OCR
- OpenCV, Pillow, NumPy, pytesseract

## Struktur Peran

### Admin

- Login ke dashboard admin
- Mengelola data mahasiswa reguler dan RPL
- Mengimpor data mahasiswa secara massal
- Mengatur tarif dan template pembayaran
- Memverifikasi pembayaran yang perlu review
- Mengekspor laporan transaksi dan ledger
- Mengelola kelas dan setting OCR

### Mahasiswa

- Login menggunakan email atau NIM
- Melihat rencana pembayaran aktif
- Mengunggah bukti transfer
- Melihat histori pembayaran
- Mengubah password
- Mengakses pendaftaran pembayaran munaqosah jika seluruh tagihan semester sudah lunas

## Alur Singkat Sistem

1. Admin menyiapkan tarif, template angsuran, dan data mahasiswa.
2. Mahasiswa login lalu memilih atau menerima payment plan.
3. Mahasiswa mengunggah bukti pembayaran.
4. Sistem memproses bukti bayar melalui queue dan OCR Python.
5. Jika hasil OCR valid, pembayaran dapat diverifikasi otomatis.
6. Jika hasil OCR meragukan atau gagal, status pembayaran masuk ke review admin.
7. Admin dapat melihat dashboard, memverifikasi pembayaran, lalu mengekspor laporan.

## Persyaratan Sistem

Sebelum menjalankan project ini, pastikan tersedia:

- PHP 8.2 atau lebih baru
- Composer
- Node.js dan npm
- MySQL
- Python 3
- Tesseract OCR

Untuk Windows, default path Tesseract yang digunakan script adalah:

```bash
C:\Program Files\Tesseract-OCR\tesseract.exe
```

Jika Tesseract terpasang di lokasi lain, sesuaikan path di `app/Scripts/ocr_processor.py` atau pastikan executable tersedia di PATH.

## Instalasi Lokal

### 1. Clone repository

```bash
git clone https://github.com/Mishalyolyin/Sistem_akademik_new_TOCR.git
cd Sistem_akademik_new_TOCR
```

### 2. Install dependency PHP

```bash
composer install
```

### 3. Siapkan environment

```bash
copy .env.example .env
php artisan key:generate
```

Lalu sesuaikan konfigurasi database di file `.env`.

### 4. Jalankan migrasi dan seeder

```bash
php artisan migrate --seed
```

Seeder akan membuat akun admin default dan data dummy awal.

### 5. Install dependency frontend

```bash
npm install
npm run build
```

Untuk pengembangan aktif, gunakan:

```bash
npm run dev
```

### 6. Install dependency OCR Python

```bash
pip install -r app/Scripts/requirements.txt
```

### 7. Siapkan storage link

```bash
php artisan storage:link
```

### 8. Jalankan aplikasi

Opsi paling praktis:

```bash
composer run dev
```

Script tersebut akan menjalankan:

- Laravel development server
- Queue listener
- Log viewer
- Vite dev server

Jika ingin manual, jalankan service terpisah:

```bash
php artisan serve
php artisan queue:listen --tries=1 --timeout=0
npm run dev
```

## Akun Default Seeder

Jika menjalankan `php artisan migrate --seed`, akun admin default yang dibuat adalah:

- Email: `admin@campus.ac.id`
- Password: `password`

Setelah login pertama, sebaiknya password langsung diganti.

## Command Penting

### Menjalankan test

```bash
composer test
```

### Menjalankan reminder pembayaran

```bash
php artisan payments:send-reminders
```

Command ini juga sudah dijadwalkan harian pukul `08:00`.

### Export dataset untuk ML

```bash
php artisan app:export-dataset --limit=100
```

## OCR dan Verifikasi Pembayaran

Sistem OCR menggunakan script Python di `app/Scripts/ocr_processor.py`.

Prosesnya sebagai berikut:

- Laravel menerima upload bukti pembayaran
- Job queue memproses file bukti bayar
- Python melakukan pre-processing gambar
- Tesseract membaca teks dari gambar
- Sistem mengekstrak nominal dan informasi penting
- Hasil OCR dibandingkan dengan data pembayaran yang seharusnya
- Pembayaran bisa otomatis diverifikasi atau masuk status review manual

Jika OCR gagal karena dependency belum lengkap atau hasil pembacaan tidak valid, pembayaran tidak langsung dianggap berhasil dan tetap memerlukan pengecekan admin.

## Fitur Import dan Export

### Import mahasiswa

- Mendukung file `csv`, `txt`, dan `xlsx`
- Memiliki template import bawaan
- Mendukung mode skip, update, dan cancel saat data bentrok

### Export laporan

- Export laporan pembayaran per program
- Export seluruh data pembayaran mahasiswa
- Export ledger mahasiswa
- Export receipt PDF
- Export dataset pembayaran terverifikasi untuk pelatihan model

## Struktur Folder Penting

```text
app/
  Console/Commands/     Command artisan custom
  Exports/              Export Excel
  Http/Controllers/     Controller admin dan mahasiswa
  Imports/              Logic import Excel mahasiswa
  Jobs/                 Job queue OCR
  Models/               Model Eloquent
  Scripts/              Script Python OCR
  Services/             Business logic utama
database/
  migrations/           Struktur database
  seeders/              Seeder akun admin dan data awal
resources/views/        Blade view admin, mahasiswa, auth, receipt
routes/                 Route web, api, dan console
tests/                  Feature test, unit test, dan python test
```

## Catatan Pengembangan

- Queue connection default menggunakan `database`
- Session dan cache default juga menggunakan database
- Project menyediakan banyak feature test untuk alur pembayaran, OCR, import, export, dashboard, dan keamanan pembayaran
- Dokumentasi tambahan tersedia di `DEPLOYMENT.md`, `ML_GUIDE.md`, dan `README_OCR.md`

## Lisensi

Project ini menggunakan lisensi MIT.
