# Deploy ke VPS

Seluruh sistem berjalan dari satu berkas compose. Yang dibutuhkan di server cuma
Docker; JDK, Node, dan Python tidak perlu dipasang karena semuanya dibangun di
dalam image.

## Kenapa harus VPS

Spring Boot butuh JVM yang berjalan terus, dan shared hosting cPanel tidak
menyediakannya. Sistem lama berbasis Laravel memang bisa di sana; yang ini tidak.

Ukuran minimal yang wajar: **2 vCPU, 4 GB RAM, 40 GB disk**. Penakarnya bukan
lalu lintas melainkan pembacaan bukti — OpenCV dan Tesseract memakan memori saat
memproses gambar, dan PostgreSQL serta RabbitMQ berbagi mesin yang sama.

## Sekali di awal

```bash
git clone <repo> pembayaran && cd pembayaran
cp .env.example .env
```

Isi `.env`. Empat yang wajib dan tidak punya nilai bawaan:

| Variabel | Cara mengisi |
|---|---|
| `DB_PASSWORD` | `openssl rand -base64 24` |
| `RABBITMQ_PASSWORD` | `openssl rand -base64 24` |
| `JWT_SECRET` | `openssl rand -base64 48`, minimal 32 karakter |
| `APP_ADMIN_PASSWORD` | kata sandi admin pertama |

Sisanya sudah masuk akal apa adanya, kecuali `APP_URL` dan `API_URL` yang harus
menunjuk domain sungguhan. Compose menolak menyala kalau yang wajib masih kosong,
jadi tidak ada risiko sistemnya terlanjur hidup dengan kata sandi kosong.

**`APP_ADMIN_PASSWORD` menentukan satu-satunya jalan masuk.** Sistem ini tidak
punya endpoint pembuat pengguna, jadi admin pertama hanya bisa lahir dari sini.
Ia dibuat sekali, saat akun dengan email itu belum ada; menyalakan ulang tidak
menimpanya.

## Menyalakan

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Build pertama memakan waktu beberapa menit karena Maven mengunduh dependensi dan
Tesseract dipasang di image OCR. Berikutnya jauh lebih cepat.

Memastikan semuanya hidup:

```bash
docker compose -f docker-compose.prod.yml ps
curl -s localhost:8080/api/actuator/health
```

Migrasi Flyway berjalan sendiri saat API menyala. Skema tidak pernah diubah
Hibernate — `ddl-auto` disetel `validate`, jadi ketidakcocokan antara kode dan
tabel membuat API menolak menyala alih-alih diam-diam merusak data.

## Reverse proxy dan HTTPS

Compose tidak menyertakan reverse proxy; pasang di depan sesuai kebiasaan
servernya. Yang penting:

- `COOKIE_SECURE=true` (bawaannya sudah begitu). Cookie sesi hanya boleh
  melewati HTTPS — tanpa itu refresh token ikut terkirim di koneksi biasa
- `APP_URL` harus sama persis dengan alamat yang dibuka pengguna, karena
  dipakai sebagai daftar CORS yang diizinkan
- Setel `API_BIND=127.0.0.1:8080` dan `WEB_BIND=127.0.0.1:3000` supaya kedua
  service hanya bisa dijangkau lewat proxy, bukan langsung dari internet

Contoh Caddy, yang sekaligus mengurus sertifikat:

```
pembayaran.kampus.ac.id {
    handle /api/* {
        reverse_proxy 127.0.0.1:8080
    }
    handle {
        reverse_proxy 127.0.0.1:3000
    }
}
```

## Memperbarui

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

`API_URL` ditanam ke bundel peramban saat build, jadi mengubahnya menuntut build
ulang service `web`, bukan sekadar menyalakan ulang.

## Cadangan

Yang tidak bisa dibuat ulang ada dua: basis data dan gambar bukti bayar.

```bash
# Basis data
docker compose -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U pembayaran pembayaran | gzip > cadangan-$(date +%F).sql.gz

# Gambar bukti bayar
docker run --rm -v pembayaran_bukti-bayar:/data -v "$PWD":/keluar alpine \
  tar czf /keluar/bukti-$(date +%F).tar.gz -C /data .
```

Bukti bayar adalah dokumen keuangan yang bisa diminta lagi saat ada sengketa,
jadi kehilangannya tidak tergantikan oleh basis data yang utuh.

## Yang belum ada

- **Admin belum bisa mengganti kata sandinya dari UI.** Yang punya halaman ganti
  kata sandi baru mahasiswa. Untuk admin, ganti lewat basis data, atau ubah
  `APP_ADMIN_PASSWORD` dan buat akun admin baru dengan email berbeda.
- **Belum ada reverse proxy dan TLS di dalam compose.** Sengaja, supaya bisa
  mengikuti kebiasaan server masing-masing — tapi berarti langkahnya manual.
- **Belum ada penjadwal cadangan.** Perintah di atas masih dijalankan tangan;
  pasang di cron.
