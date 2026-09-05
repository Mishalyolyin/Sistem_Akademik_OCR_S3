import { expect, test } from "@playwright/test";
import { ADMIN, DEVELOPER } from "./global-setup";

/**
 * Peran DEVELOPER dan halaman forensik OCR.
 *
 * <p>Yang dibuktikan di sini tidak bisa dibuktikan di test backend: peran ini
 * pernah masuk lalu menggantung di "Memeriksa sesi…" selamanya, karena
 * diarahkan ke /dashboard yang justru menolaknya lalu dilempar ke sana lagi.
 * Tidak ada satu pun request yang gagal — jadi tidak ada yang bisa ditangkap
 * selain dengan membuka peramban.
 */

async function masuk(
  page: import("@playwright/test").Page,
  email: string,
  sandi: string,
) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Kata sandi").fill(sandi);
  await page.getByRole("button", { name: "Masuk" }).click();
}

test("developer mendarat di forensik, bukan menggantung di spinner", async ({
  page,
}) => {
  await masuk(page, DEVELOPER.email, DEVELOPER.password);

  await expect(page).toHaveURL(/\/forensik$/);
  await expect(
    page.getByRole("heading", { level: 2, name: "Forensik OCR" }),
  ).toBeVisible();
  await expect(page.getByText("Memeriksa sesi")).toHaveCount(0);
});

test("developer yang membuka halaman admin dikembalikan ke forensik", async ({
  page,
}) => {
  await masuk(page, DEVELOPER.email, DEVELOPER.password);
  await expect(page).toHaveURL(/\/forensik$/);

  await page.goto("/mahasiswa");

  await expect(page).toHaveURL(/\/forensik$/);
});

test("menu developer hanya berisi forensik, tanpa menu admin", async ({
  page,
}) => {
  await masuk(page, DEVELOPER.email, DEVELOPER.password);
  await expect(page).toHaveURL(/\/forensik$/);

  const navUtama = page.getByRole("navigation", { name: "Navigasi utama" });
  await expect(navUtama.getByRole("link", { name: "Forensik OCR" })).toBeVisible();
  await expect(navUtama.getByRole("link", { name: "Mahasiswa" })).toHaveCount(0);
  await expect(navUtama.getByRole("link", { name: "Dashboard" })).toHaveCount(0);
});

test("admin tetap bisa membuka forensik lewat menunya", async ({ page }) => {
  await masuk(page, ADMIN.email, ADMIN.password);
  await expect(page).toHaveURL(/\/dashboard$/);

  await page
    .getByRole("navigation", { name: "Navigasi utama" })
    .getByRole("link", { name: "Forensik OCR" })
    .click();

  await expect(page).toHaveURL(/\/forensik$/);
  await expect(
    page.getByRole("heading", { level: 2, name: "Forensik OCR" }),
  ).toBeVisible();
});
