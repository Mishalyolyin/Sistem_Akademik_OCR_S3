import { expect, test, type Page } from "@playwright/test";
import { ADMIN, MAHASISWA, idMahasiswaUji, saldoMahasiswaUji } from "./global-setup";

/**
 * Penyesuaian saldo, dari klik admin sampai angka di basis data.
 *
 * <p>Aturannya sudah dikunci di test satuan dan test controller. Yang hanya
 * bisa dibuktikan di sini adalah bahwa ketiganya benar-benar tersambung:
 * tombol memanggil endpoint yang benar, hasilnya tersimpan, dan angka di layar
 * ikut berubah tanpa perlu memuat ulang halaman.
 */

async function masukSebagaiAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(ADMIN.email);
  await page.getByLabel("Kata sandi").fill(ADMIN.password);
  await page.getByRole("button", { name: "Masuk" }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}

async function bukaDetailMahasiswa(page: Page) {
  await page.goto(`/mahasiswa/${idMahasiswaUji()}`);
  await expect(page.getByRole("heading", { name: MAHASISWA.nama })).toBeVisible();
}

test.beforeEach(async ({ page }) => {
  await masukSebagaiAdmin(page);
  await bukaDetailMahasiswa(page);
});

test("menambah saldo lewat dialog, angkanya berubah di layar dan di basis data", async ({
  page,
}) => {
  const sebelum = saldoMahasiswaUji();

  await page.getByRole("button", { name: "Penyesuaian" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();

  await page.getByLabel("Nominal").fill("250000");
  await page
    .getByLabel("Alasan")
    .fill("pengembalian kelebihan bayar cicilan pertama");
  await page.getByRole("button", { name: "Simpan penyesuaian" }).click();

  await expect(page.getByRole("dialog")).toBeHidden();

  // Kotak ringkasan menyegarkan diri sendiri setelah penyesuaian tersimpan.
  // Nominalnya dicocokkan dengan pola, bukan teks persis: Intl id-ID menyisipkan
  // spasi tanpa-pemenggal setelah "Rp" yang tidak sama dengan spasi biasa.
  const kotakSaldo = page
    .locator("div")
    .filter({ hasText: /^Saldo mahasiswa/ })
    .first();
  await expect(kotakSaldo).toContainText(/250[.\s ]?000/);

  expect(saldoMahasiswaUji()).toBe(sebelum + 250000);
});

test("saldo tidak boleh jadi minus, tombol simpan tertahan sebelum dikirim", async ({
  page,
}) => {
  const sebelum = saldoMahasiswaUji();

  await page.getByRole("button", { name: "Penyesuaian" }).click();
  await page.getByRole("button", { name: "Kurangi" }).click();
  await page.getByLabel("Nominal").fill(String(sebelum + 1_000_000));
  await page.getByLabel("Alasan").fill("mengurangi lebih dari yang ada");

  await expect(page.getByText("tidak boleh minus")).toBeVisible();
  await expect(page.getByRole("button", { name: "Simpan penyesuaian" })).toBeDisabled();

  expect(saldoMahasiswaUji()).toBe(sebelum);
});

test("alasan yang terlalu pendek menahan penyimpanan", async ({ page }) => {
  await page.getByRole("button", { name: "Penyesuaian" }).click();
  await page.getByLabel("Nominal").fill("50000");
  await page.getByLabel("Alasan").fill("ok");

  await expect(page.getByText("minimal 5 karakter")).toBeVisible();
  await expect(page.getByRole("button", { name: "Simpan penyesuaian" })).toBeDisabled();
});

test("riwayat penyesuaian muncul di dialog berikutnya", async ({ page }) => {
  await page.getByRole("button", { name: "Penyesuaian" }).click();
  await page.getByLabel("Nominal").fill("75000");
  await page.getByLabel("Alasan").fill("koreksi pembulatan bank");
  await page.getByRole("button", { name: "Simpan penyesuaian" }).click();
  await expect(page.getByRole("dialog")).toBeHidden();

  await page.getByRole("button", { name: "Penyesuaian" }).click();

  await expect(page.getByText("Riwayat penyesuaian")).toBeVisible();
  await expect(page.getByText("koreksi pembulatan bank")).toBeVisible();
});
