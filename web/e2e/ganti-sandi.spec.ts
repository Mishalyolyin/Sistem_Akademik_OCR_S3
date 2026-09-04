import { expect, test, type Page } from "@playwright/test";
import { ADMIN_SANDI } from "./global-setup";

/**
 * Ganti kata sandi sendiri, dari menu pengguna sampai masuk lagi.
 *
 * <p>Alur ini punya satu bagian yang tidak kelihatan di test satuan: mengganti
 * kata sandi mencabut sesi di server, termasuk sesi yang sedang dipakai. Hanya
 * di peramban sungguhan bisa dipastikan pengguna benar-benar terlempar ke
 * halaman masuk dan kata sandi lamanya tidak lagi berlaku.
 *
 * <p>Memakai akun admin tersendiri. Uji ini benar-benar mengubah kata sandi di
 * basis data, sementara penyemaian hanya berjalan sekali di awal — memakai akun
 * admin utama akan membuat seluruh uji sesudahnya gagal masuk.
 */

const SANDI_BARU = "sandi-baru-yang-kuat";

async function masuk(page: Page, sandi: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(ADMIN_SANDI.email);
  await page.getByLabel("Kata sandi").fill(sandi);
  await page.getByRole("button", { name: "Masuk" }).click();
}

async function bukaDialogGantiSandi(page: Page) {
  await page.getByRole("button", { name: "Menu pengguna" }).click();
  await page.getByRole("menuitem", { name: "Ganti kata sandi" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
}

test("admin mengganti kata sandi, dikeluarkan, lalu masuk dengan yang baru", async ({
  page,
}) => {
  await masuk(page, ADMIN_SANDI.password);
  await expect(page).toHaveURL(/\/dashboard$/);

  await bukaDialogGantiSandi(page);
  await page.getByLabel("Kata sandi sekarang").fill(ADMIN_SANDI.password);
  await page.getByLabel("Kata sandi baru", { exact: true }).fill(SANDI_BARU);
  await page.getByLabel("Ulangi kata sandi baru").fill(SANDI_BARU);
  await page.getByRole("button", { name: "Ganti kata sandi" }).click();

  // Sesi dicabut di server, jadi pengguna diantar ke halaman masuk.
  await expect(page).toHaveURL(/\/login$/);

  // Kata sandi lama tidak berlaku lagi.
  await masuk(page, ADMIN_SANDI.password);
  await expect(page.locator('form [role="alert"]')).toContainText("salah");

  // Yang baru berlaku.
  await masuk(page, SANDI_BARU);
  await expect(page).toHaveURL(/\/dashboard$/);

  // Dikembalikan supaya uji berikutnya di berkas ini tetap bisa masuk;
  // penyemaian hanya berjalan sekali di awal, bukan sebelum tiap uji.
  await bukaDialogGantiSandi(page);
  await page.getByLabel("Kata sandi sekarang").fill(SANDI_BARU);
  await page.getByLabel("Kata sandi baru", { exact: true }).fill(ADMIN_SANDI.password);
  await page.getByLabel("Ulangi kata sandi baru").fill(ADMIN_SANDI.password);
  await page.getByRole("button", { name: "Ganti kata sandi" }).click();
  await expect(page).toHaveURL(/\/login$/);
});

test("kata sandi lama yang salah ditolak dengan pesan, sesi tetap hidup", async ({
  page,
}) => {
  await masuk(page, ADMIN_SANDI.password);
  await expect(page).toHaveURL(/\/dashboard$/);

  await bukaDialogGantiSandi(page);
  await page.getByLabel("Kata sandi sekarang").fill("tebakan-ngawur");
  await page.getByLabel("Kata sandi baru", { exact: true }).fill(SANDI_BARU);
  await page.getByLabel("Ulangi kata sandi baru").fill(SANDI_BARU);
  await page.getByRole("button", { name: "Ganti kata sandi" }).click();

  await expect(page.getByText("Kata sandi lama salah.")).toBeVisible();
  await expect(page).toHaveURL(/\/dashboard$/);
});

test("tombol simpan tertahan sampai isiannya masuk akal", async ({ page }) => {
  await masuk(page, ADMIN_SANDI.password);
  await bukaDialogGantiSandi(page);

  const simpan = page.getByRole("button", { name: "Ganti kata sandi" });
  await expect(simpan).toBeDisabled();

  await page.getByLabel("Kata sandi sekarang").fill(ADMIN_SANDI.password);
  await page.getByLabel("Kata sandi baru", { exact: true }).fill("pendek7");
  // Teks yang sama juga muncul di keterangan dialog, jadi dicocokkan persis
  // supaya yang diuji benar-benar pesan galat di bawah isian.
  await expect(
    page.getByText("Minimal 8 karakter.", { exact: true }),
  ).toBeVisible();
  await expect(simpan).toBeDisabled();

  await page.getByLabel("Kata sandi baru", { exact: true }).fill(SANDI_BARU);
  await page.getByLabel("Ulangi kata sandi baru").fill("beda-sendiri");
  await expect(page.getByText("Belum sama dengan yang di atas")).toBeVisible();
  await expect(simpan).toBeDisabled();

  await page.getByLabel("Ulangi kata sandi baru").fill(SANDI_BARU);
  await expect(simpan).toBeEnabled();
});
