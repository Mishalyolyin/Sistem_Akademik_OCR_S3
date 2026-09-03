import { expect, test, type Page } from "@playwright/test";
import { ADMIN } from "./global-setup";

/**
 * Menu tarik-turun yang memakai label.
 *
 * <p>Label di Base UI wajib berada di dalam Group. Dipakai telanjang, Base UI
 * melempar galat begitu menunya dibuka dan seluruh halaman ikut mati — bukan
 * sekadar menunya yang tidak muncul. Kegagalan sebesar itu tidak tertangkap
 * kompilasi maupun test satuan, jadi tiap menu berlabel dibuka sungguhan di sini.
 */

async function masukSebagaiAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(ADMIN.email);
  await page.getByLabel("Kata sandi").fill(ADMIN.password);
  await page.getByRole("button", { name: "Masuk" }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}

test("menu pengguna terbuka dan halamannya tetap hidup", async ({ page }) => {
  const galat: string[] = [];
  page.on("pageerror", (e) => galat.push(e.message));

  await masukSebagaiAdmin(page);
  await page.getByRole("button", { name: "Menu pengguna" }).click();

  await expect(page.getByRole("menuitem", { name: "Keluar" })).toBeVisible();
  expect(galat).toEqual([]);
});

test("menu pilih kolom di halaman verifikasi terbuka", async ({ page }) => {
  const galat: string[] = [];
  page.on("pageerror", (e) => galat.push(e.message));

  await masukSebagaiAdmin(page);
  await page.goto("/verifikasi/pendaftaran");

  await page.getByRole("button", { name: "Kolom" }).click();

  await expect(page.getByText("Tampilkan kolom")).toBeVisible();
  expect(galat).toEqual([]);
});
