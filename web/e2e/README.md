# Uji end-to-end

Berjalan terhadap sistem yang benar-benar hidup: Next.js, Spring Boot, dan
PostgreSQL sungguhan. Tidak ada yang ditiru — itulah gunanya, karena hal seperti
"cookie sesi benar-benar terpasang" dan "angka di layar ikut berubah setelah
disimpan" hanya bisa dibuktikan lewat peramban.

Port dan basis datanya sengaja terpisah dari yang dipakai sehari-hari, supaya
menjalankan uji ini tidak mematikan server pengembangan yang sedang berjalan dan
tidak mencemari datanya.

| Bagian | Sehari-hari | Uji |
|---|---|---|
| Web | 3000 | 3100 |
| API | 8080 | 8081 |
| Basis data | `pembayaran` | `pembayaran_e2e` |
| Folder build | `.next` | `.next-e2e` |

## Menyiapkan

Sekali saja:

```bash
docker compose up -d
docker exec pembayaran-postgres psql -U pembayaran -d postgres \
  -c "CREATE DATABASE pembayaran_e2e OWNER pembayaran;"

cd web && npx playwright install chromium
```

## Menjalankan

Tiga langkah, di tiga terminal atau berurutan.

**1. API menghadap basis data uji**

```bash
cd api
SERVER_PORT=8081 \
DB_URL="jdbc:postgresql://localhost:5432/pembayaran_e2e" \
CORS_ORIGINS="http://localhost:3100" \
JWT_SECRET="rahasia-e2e-minimal-32-karakter-untuk-hs256" \
STORAGE_ROOT="./storage-e2e" \
./mvnw spring-boot:run
```

**2. Bangun web yang menunjuk ke API itu**

Wajib dibangun ulang tiap kali kode frontend berubah: `NEXT_PUBLIC_API_URL`
ditanam saat build, bukan dibaca saat berjalan.

```bash
cd web
NEXT_DIST_DIR=.next-e2e NEXT_PUBLIC_API_URL=http://localhost:8081/api npm run build
```

**3. Jalankan ujinya**

Playwright menyalakan sendiri servernya di port 3100.

```bash
cd web && npm run e2e
```

`npm run e2e:ui` membuka mode telusur kalau ada yang perlu dilihat langkah demi
langkah.

## Data awal

`global-setup.ts` menghapus lalu menyemai ulang satu kelas dan satu mahasiswa
sebelum uji berjalan, jadi tiap kali berangkat dari keadaan yang sama.

Penyemaiannya lewat SQL, bukan API, karena sistem ini memang tidak punya
endpoint "buat mahasiswa" — satu-satunya jalan masuk adalah import Excel, dan
menyusun berkas `.xlsx` hanya untuk menyiapkan keadaan awal justru membuat
ujinya menguji si penyusun berkas.

Kata sandi mahasiswa uji disalin hash-nya dari baris admin, jadi sama dengan
kata sandi admin. Bcrypt tidak bisa disusun tanpa menjalankan penyandinya, dan
menambah pustaka bcrypt di sisi uji hanya untuk ini tidak sepadan.

## Catatan

Semua uji lolos. Kalau ada yang menambahkan menu tarik-turun berlabel, buka
menunya di `menu.spec.ts`: `DropdownMenuLabel` wajib berada di dalam
`DropdownMenuGroup`, dan kalau tidak, Base UI melempar galat yang mematikan
seluruh halaman begitu menunya dibuka — bukan sekadar menunya yang tidak muncul.
