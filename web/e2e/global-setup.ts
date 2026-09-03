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
    DELETE FROM study_classes WHERE name = 'E2E' AND academic_year = '2026/2027';
  `);

  psql(`
    INSERT INTO study_classes (name, kerjasama, academic_year)
    VALUES ('E2E', FALSE, '2026/2027');

    -- Hash kata sandi disalin dari baris admin yang dibuat seeder, bukan
    -- ditulis tangan: bcrypt tidak bisa disusun tanpa menjalankan penyandinya,
    -- dan menambah pustaka bcrypt di sisi uji hanya untuk ini tidak sepadan.
    INSERT INTO users (name, email, password_hash, role, active)
    VALUES ('${MAHASISWA.nama}', '${MAHASISWA.email}',
            (SELECT password_hash FROM users WHERE email = '${ADMIN.email}'),
            'MAHASISWA', TRUE);

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

  const jumlah = psql(
    `SELECT count(*) FROM students WHERE nim = '${MAHASISWA.nim}'`,
  );
  if (jumlah !== "1") {
    throw new Error(`Penyemaian mahasiswa gagal, ditemukan ${jumlah} baris.`);
  }
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
