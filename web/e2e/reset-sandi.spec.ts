import { expect, test, type Page } from "@playwright/test";
import { ADMIN, MAHASISWA_RESET, idMahasiswaReset } from "./global-setup";

/**
 * Admin mengembalikan kata sandi mahasiswa ke NIM.
 *
 * <p>Mahasiswa tidak mengelola kata sandinya sendiri di sistem ini, jadi jalur
 * ini satu-satunya cara ia bisa masuk lagi setelah lupa. Yang dibuktikan di
 * sini bukan cuma bahwa tombolnya bekerja, tapi bahwa mahasiswa yang
 * bersangkutan benar-benar bisa masuk sesudahnya.
 *
 * <p>Memakai mahasiswa tersendiri: uji ini mengubah kata sandinya di basis
 * data, sementara penyemaian hanya berjalan sekali di awal.
 */

/**
 * Membuang sesi yang sedang berjalan sebelum masuk sebagai orang lain. Tanpa
 * ini halaman login memantul ke beranda peran yang masih aktif, dan kolom
 * emailnya tidak pernah muncul.
 */
async function masuk(page: Page, email: string, sandi: string) {
  await page.context().clearCookies();
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Kata sandi").fill(sandi);
  await page.getByRole("button", { name: "Masuk" }).click();
}

test("admin mengembalikan kata sandi, mahasiswa bisa masuk memakai NIM", async ({
  page,
}) => {
  await masuk(page, ADMIN.email, ADMIN.password);
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.goto(`/mahasiswa/${idMahasiswaReset()}`);
  await expect(
    page.getByRole("heading", { name: MAHASISWA_RESET.nama }),
  ).toBeVisible();

  await page.getByRole("button", { name: "Reset kata sandi" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();

  // NIM ditampilkan supaya admin bisa langsung menyampaikannya.
  await expect(page.getByRole("dialog")).toContainText(MAHASISWA_RESET.nim);

  await page.getByRole("button", { name: "Kembalikan ke NIM" }).click();
  await expect(page.getByText("Sudah dikembalikan")).toBeVisible();
  await page.getByRole("button", { name: "Tutup" }).click();

  // Kata sandi lamanya tidak berlaku lagi.
  await masuk(page, MAHASISWA_RESET.email, MAHASISWA_RESET.password);
  await expect(page.locator('form [role="alert"]')).toContainText("salah");

  // NIM-nya berlaku, dan mahasiswa mendarat di portalnya.
  await masuk(page, MAHASISWA_RESET.email, MAHASISWA_RESET.nim);
  await expect(page).toHaveURL(/\/portal$/);
});

test("mahasiswa tidak punya menu ganti kata sandi di portalnya", async ({ page }) => {
  await masuk(page, MAHASISWA_RESET.email, MAHASISWA_RESET.nim);
  await expect(page).toHaveURL(/\/portal$/);

  await page.goto("/portal/profil");

  // Halaman profil tetap tampil, tapi tanpa form ganti kata sandi.
  await expect(page.getByText(MAHASISWA_RESET.nim)).toBeVisible();
  await expect(page.getByText("Ganti kata sandi")).toHaveCount(0);
});
