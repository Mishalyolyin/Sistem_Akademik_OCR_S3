import { defineConfig, devices } from "@playwright/test";

/**
 * Uji end-to-end berjalan terhadap sistem yang benar-benar hidup: Next.js,
 * Spring Boot, dan PostgreSQL sungguhan. Tidak ada yang ditiru.
 *
 * Port dan basis data sengaja dipisahkan dari yang dipakai sehari-hari
 * (3100 / 8081 / pembayaran_e2e), supaya menjalankan uji ini tidak mematikan
 * server pengembangan yang sedang berjalan dan tidak mencemari datanya.
 *
 * Prasyarat, lihat e2e/README.md:
 *   docker compose up -d
 *   API di 8081 dengan DB_URL menunjuk pembayaran_e2e
 */
const PORT = Number(process.env.E2E_WEB_PORT ?? 3100);
const BASE_URL = `http://localhost:${PORT}`;

export default defineConfig({
  testDir: "./e2e",
  globalSetup: "./e2e/global-setup.ts",
  // Alur ini berbagi satu basis data, jadi dijalankan berurutan supaya
  // kegagalan yang muncul benar-benar bug, bukan dua uji yang saling menimpa.
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? "github" : "list",
  timeout: 30_000,
  expect: { timeout: 10_000 },

  use: {
    baseURL: BASE_URL,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    locale: "id-ID",
    timezoneId: "Asia/Jakarta",
  },

  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
  ],

  webServer: {
    command: `npx next start -p ${PORT}`,
    url: BASE_URL,
    timeout: 120_000,
    reuseExistingServer: !process.env.CI,
    env: {
      NEXT_DIST_DIR: process.env.NEXT_DIST_DIR ?? ".next-e2e",
    },
  },
});
