import { expect, test, type Page } from "@playwright/test";
import { ADMIN } from "./global-setup";

/**
 * Kartu status bukti bayar di dashboard.
 *
 * <p>Angkanya datang dari satu kueri agregat, sementara daftar yang dituju
 * disaring lewat parameter URL — dua jalur berbeda yang gampang berpisah diam
 * tanpa ada yang sadar. Yang diuji di sini justru sambungannya: kotak yang
 * diklik harus mendarat di daftar yang benar-benar tersaring.
 */

async function masukSebagaiAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(ADMIN.email);
  await page.getByLabel("Kata sandi").fill(ADMIN.password);
  await page.getByRole("button", { name: "Masuk" }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}

test.beforeEach(async ({ page }) => {
  await masukSebagaiAdmin(page);
});

test("keempat kartu status tampil", async ({ page }) => {
  const bagian = page.getByRole("heading", { name: "Status bukti bayar" });
  await expect(bagian).toBeVisible();

  for (const label of [
    "Menunggu dibaca",
    "Perlu ditinjau",
    "Ditolak",
    "Terverifikasi",
  ]) {
    await expect(page.getByText(label, { exact: true })).toBeVisible();
  }
});

test("kartu Ditolak membuka daftar yang tersaring ditolak", async ({ page }) => {
  await page.getByRole("link", { name: /Ditolak/ }).click();

  await expect(page).toHaveURL(/\/verifikasi\/semua\?status=REJECTED$/);
  // Saringan di daftar ikut memilih status yang sama, bukan kembali ke "Semua".
  await expect(page.getByRole("button", { name: "Ditolak" })).toBeVisible();
});

test("kartu Terverifikasi memakai saringan gabungan otomatis dan manual", async ({
  page,
}) => {
  await page.getByRole("link", { name: /Terverifikasi/ }).click();

  // Terverifikasi mencakup AUTO_VERIFIED dan VERIFIED sekaligus; menautkannya
  // ke salah satu saja membuat angka di kartu tidak cocok dengan isi daftar.
  await expect(page).toHaveURL(/\/verifikasi\/semua\?status=TERVERIFIKASI$/);
  await expect(page.getByRole("button", { name: "Terverifikasi" })).toBeVisible();
});

test("kartu Menunggu dibaca dan Perlu ditinjau mengarah ke saringannya sendiri", async ({
  page,
}) => {
  await page.getByRole("link", { name: /Menunggu dibaca/ }).click();
  await expect(page).toHaveURL(/status=PENDING$/);

  await page.goBack();
  await page.getByRole("link", { name: /Perlu ditinjau/ }).click();
  await expect(page).toHaveURL(/status=NEEDS_REVIEW$/);
});

test("halaman semua kategori tidak menyaring per kategori", async ({ page }) => {
  await page.goto("/verifikasi/semua");

  // Judul halaman muncul di dua tempat: bilah atas dan judul isi.
  await expect(
    page.getByRole("heading", { name: "Semua kategori", exact: true }),
  ).toBeVisible();
});
