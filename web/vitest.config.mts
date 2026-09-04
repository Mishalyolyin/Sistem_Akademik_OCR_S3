import { defineConfig } from "vitest/config";
import { fileURLToPath } from "node:url";

/**
 * Uji satuan untuk logika murni: penguraian jawaban API, format angka, dan
 * hitungan tarif. Alur antar halaman sudah dijaga Playwright di `e2e/`, jadi
 * berkas di sana sengaja dikecualikan — pelarinya berbeda dan ia butuh server
 * yang benar-benar hidup.
 */
export default defineConfig({
  test: {
    environment: "node",
    include: ["src/**/*.test.ts"],
    exclude: ["e2e/**", "node_modules/**"],
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
