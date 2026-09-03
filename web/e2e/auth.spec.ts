import { expect, test } from "@playwright/test";
import { ADMIN, MAHASISWA } from "./global-setup";

/**
 * Masuk, keluar, dan pengarahan menurut peran.
 *
 * <p>Semuanya sudah punya test di sisi backend. Yang hanya bisa dibuktikan di
 * sini adalah sambungannya: token disimpan di memori dan cookie dipasang oleh
 * server, jadi apakah sesi benar-benar bertahan dan benar-benar berakhir cuma
 * kelihatan lewat peramban sungguhan.
 */

async function masuk(page: import("@playwright/test").Page, email: string, sandi: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Kata sandi").fill(sandi);
  await page.getByRole("button", { name: "Masuk" }).click();
}

test.describe("Masuk", () => {
  test("kata sandi salah menampilkan pesan, bukan halaman kosong", async ({ page }) => {
    await masuk(page, ADMIN.email, "jelas-salah");

    await expect(page.locator('form [role="alert"]')).toContainText(
      "Email atau kata sandi salah.",
    );
    await expect(page).toHaveURL(/\/login$/);
  });

  test("admin diarahkan ke dashboard", async ({ page }) => {
    await masuk(page, ADMIN.email, ADMIN.password);

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
  });

  test("mahasiswa diarahkan ke portal, bukan ke dashboard admin", async ({ page }) => {
    await masuk(page, MAHASISWA.email, MAHASISWA.password);

    await expect(page).toHaveURL(/\/portal$/);
  });
});

test.describe("Batas peran di peramban", () => {
  test("mahasiswa yang membuka halaman admin dikembalikan ke portalnya", async ({ page }) => {
    await masuk(page, MAHASISWA.email, MAHASISWA.password);
    await expect(page).toHaveURL(/\/portal$/);

    await page.goto("/mahasiswa");

    await expect(page).toHaveURL(/\/portal$/);
  });

  test("tanpa sesi, halaman admin mengarah ke login", async ({ page }) => {
    await page.goto("/dashboard");

    await expect(page).toHaveURL(/\/login$/);
  });
});

test.describe("Keluar", () => {
  // BUG TERBUKA: membuka menu pengguna melempar "Base UI error #31" di build
  // produksi, dan seluruh halaman ikut mati. Akibatnya admin tidak punya jalan
  // keluar dari UI sama sekali. Pencabutan sesinya sendiri sudah benar dan
  // terkunci di AuthServiceTest serta AuthControllerTest; yang rusak hanya
  // pemicunya di layar. Uji ini sengaja dibiarkan sebagai penanda, bukan
  // dihapus, supaya tidak hilang dari pandangan.
  test.fixme("setelah keluar, halaman admin tidak bisa dibuka lagi", async ({ page }) => {
    await masuk(page, ADMIN.email, ADMIN.password);
    await expect(page).toHaveURL(/\/dashboard$/);

    await page.getByRole("button", { name: "Menu pengguna" }).click();
    await page.getByRole("menuitem", { name: "Keluar" }).click();

    await expect(page).toHaveURL(/\/login$/);

    // Sesi benar-benar berakhir: memuat ulang halaman admin tidak
    // mengembalikannya, karena cookie refresh sudah dicabut di server.
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/login$/);
  });
});
