import { execFileSync } from "node:child_process";

/**
 * Menyiapkan keadaan awal sebelum uji end-to-end berjalan.
 *
 * <p>Data mahasiswa disemai lewat SQL, bukan lewat API, karena sistem ini
 * memang tidak punya endpoint "buat mahasiswa": satu-satunya jalan masuk adalah
 * import Excel, dan menyusun berkas .xlsx hanya untuk menyiapkan keadaan awal
 * justru membuat ujinya menguji si penyusun berkas, bukan aplikasinya.
 *
 * <p>Basis datanya terpisah dari yang dipakai sehari-hari, jadi menghapus dan
 * menyemai ulang di sini tidak menyentuh data pengembangan.
 */

export const API_URL = process.env.E2E_API_URL ?? "http://localhost:8081/api";

export const ADMIN = {
  email: "admin@kampus.ac.id",
  password: "admin123",
};

/**
 * Admin kedua, dipakai khusus oleh uji ganti kata sandi.
 *
 * Uji itu benar-benar mengubah kata sandi di basis data, dan penyemaian hanya
 * berjalan sekali di awal — bukan sebelum tiap uji. Kalau ia memakai akun admin
 * utama, seluruh uji sesudahnya gagal masuk.
 */
export const ADMIN_SANDI = {
  email: "admin.sandi@kampus.ac.id",
  password: ADMIN.password,
};

/**
 * Mahasiswa yang disemai; dokumennya sengaja sudah lengkap.
 *
 * Kata sandinya sama dengan admin karena hash-nya memang disalin dari baris
 * admin — lihat catatan di penyemaian. Ini hanya berlaku di basis data uji.
 */
export const MAHASISWA = {
  nim: "2612690001",
  nama: "Uji Endtoend",
  email: "uji.e2e@kampus.ac.id",
  password: ADMIN.password,
};

/**
 * Hash bcrypt dari kata sandi admin di atas, diambil dari baris yang dibuat
 * seeder. Ditanam sebagai tetapan supaya tiap kali uji berjalan admin kembali
 * ke kata sandi yang diketahui — uji ganti kata sandi mengubahnya, dan tanpa
 * pengembalian ini seluruh uji berikutnya gagal masuk.
 */
const HASH_ADMIN = "$2a$12$1qmfdlxDhAdE21HNTY.oQeHZX4NClr6uSQfYrlmuPU70OSAgaLDu2";

/**
 * Mahasiswa kedua, dipakai khusus oleh uji reset kata sandi. Uji itu mengubah
 * kata sandinya di basis data, sementara penyemaian hanya berjalan sekali di
 * awal — memakai mahasiswa utama akan merusak uji lain yang ikut masuk sebagai
 * mahasiswa.
 */
export const MAHASISWA_RESET = {
  nim: "2612690002",
  nama: "Uji Reset Sandi",
  email: "uji.reset@kampus.ac.id",
  password: ADMIN.password,
};

const CONTAINER = process.env.E2E_PG_CONTAINER ?? "pembayaran-postgres";
const DATABASE = process.env.E2E_PG_DATABASE ?? "pembayaran_e2e";

function psql(sql: string): string {
  return execFileSync(
    "docker",
    ["exec", "-i", CONTAINER, "psql", "-U", "pembayaran", "-d", DATABASE, "-t", "-A", "-c", sql],
    { encoding: "utf8" },
  ).trim();
}

async function apiSehat(): Promise<void> {
  const res = await fetch(`${API_URL}/actuator/health`);
  if (!res.ok) {
    throw new Error(
      `API di ${API_URL} tidak menjawab (${res.status}). ` +
        "Jalankan dulu API-nya, lihat e2e/README.md.",
    );
  }
}

export default async function globalSetup() {
  await apiSehat();

  // Kembalikan admin ke keadaan yang diketahui, termasuk pencabutan sesinya.
  psql(`
    UPDATE users
       SET password_hash = '${HASH_ADMIN}', tokens_valid_from = NULL
     WHERE email = '${ADMIN.email}';
  `);

  // Bersihkan jejak uji sebelumnya supaya tiap kali jalan berangkat dari
  // keadaan yang sama. Urutannya mengikuti ketergantungan kunci asing.
  psql(`
    DELETE FROM adjustments WHERE student_id IN (SELECT id FROM students WHERE nim = '${MAHASISWA.nim}');
    DELETE FROM payments WHERE student_id IN (SELECT id FROM students WHERE nim = '${MAHASISWA.nim}');
    DELETE FROM installments WHERE payment_plan_id IN (
      SELECT id FROM payment_plans WHERE student_id IN (SELECT id FROM students WHERE nim = '${MAHASISWA.nim}'));
    DELETE FROM payment_plans WHERE student_id IN (SELECT id FROM students WHERE nim = '${MAHASISWA.nim}');
    DELETE FROM students WHERE nim = '${MAHASISWA.nim}';
    DELETE FROM users WHERE email = '${MAHASISWA.email}';
    DELETE FROM users WHERE email = '${ADMIN_SANDI.email}';
    DELETE FROM students WHERE nim = '${MAHASISWA_RESET.nim}';
    DELETE FROM users WHERE email = '${MAHASISWA_RESET.email}';
    DELETE FROM study_classes WHERE name = 'E2E' AND academic_year = '2026/2027';
  `);

  psql(`
    INSERT INTO study_classes (name, kerjasama, academic_year)
    VALUES ('E2E', FALSE, '2026/2027');

    -- Memakai hash yang sama dengan admin; lihat catatan pada HASH_ADMIN.
    INSERT INTO users (name, email, password_hash, role, active)
    VALUES ('${MAHASISWA.nama}', '${MAHASISWA.email}', '${HASH_ADMIN}',
            'MAHASISWA', TRUE);

    INSERT INTO users (name, email, password_hash, role, active)
    VALUES ('Admin Uji Sandi', '${ADMIN_SANDI.email}', '${HASH_ADMIN}', 'ADMIN', TRUE);

    INSERT INTO students (
      user_id, nim, name, study_class_id, discount_tier,
      start_term, start_academic_year,
      profile_picture, nik, ktp_file_path, kk_number, kk_file_path,
      ijazah_file_path, address, wallet_balance, active)
    VALUES (
      (SELECT id FROM users WHERE email = '${MAHASISWA.email}'),
      '${MAHASISWA.nim}', '${MAHASISWA.nama}',
      (SELECT id FROM study_classes WHERE name = 'E2E'),
      'NON_ALUMNI', 'GASAL', '2026/2027',
      'foto.png', '3201010101010001', 'ktp.png', '3201010101010002', 'kk.png',
      'ijazah.png', 'Jalan Uji Nomor Satu, Bandung', 0, TRUE);
  `);

  psql(`
    INSERT INTO users (name, email, password_hash, role, active)
    VALUES ('${MAHASISWA_RESET.nama}', '${MAHASISWA_RESET.email}', '${HASH_ADMIN}',
            'MAHASISWA', TRUE);

    INSERT INTO students (
      user_id, nim, name, study_class_id, discount_tier,
      start_term, start_academic_year, wallet_balance, active)
    VALUES (
      (SELECT id FROM users WHERE email = '${MAHASISWA_RESET.email}'),
      '${MAHASISWA_RESET.nim}', '${MAHASISWA_RESET.nama}',
      (SELECT id FROM study_classes WHERE name = 'E2E'),
      'NON_ALUMNI', 'GASAL', '2026/2027', 0, TRUE);
  `);

  const jumlah = psql(
    `SELECT count(*) FROM students WHERE nim = '${MAHASISWA.nim}'`,
  );
  if (jumlah !== "1") {
    throw new Error(`Penyemaian mahasiswa gagal, ditemukan ${jumlah} baris.`);
  }
}

/** Id mahasiswa kedua, yang dipakai uji reset kata sandi. */
export function idMahasiswaReset(): number {
  return Number(
    psql(`SELECT id FROM students WHERE nim = '${MAHASISWA_RESET.nim}'`),
  );
}

/** Id mahasiswa yang disemai, dibaca ulang saat uji membutuhkannya. */
export function idMahasiswaUji(): number {
  return Number(psql(`SELECT id FROM students WHERE nim = '${MAHASISWA.nim}'`));
}

/** Saldo mahasiswa uji langsung dari basis data, untuk memeriksa hasil di UI. */
export function saldoMahasiswaUji(): number {
  return Number(
    psql(`SELECT wallet_balance FROM students WHERE nim = '${MAHASISWA.nim}'`),
  );
}
