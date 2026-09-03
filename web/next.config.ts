import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Bundel mandiri untuk image Docker yang ramping.
  output: "standalone",
  // Folder keluaran bisa dipindah lewat environment variable supaya uji
  // end-to-end bisa membangun salinannya sendiri tanpa menabrak `next dev`
  // yang mungkin sedang berjalan memakai .next yang sama.
  distDir: process.env.NEXT_DIST_DIR || ".next",
};

export default nextConfig;
