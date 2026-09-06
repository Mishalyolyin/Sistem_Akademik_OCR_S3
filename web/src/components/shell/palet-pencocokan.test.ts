import { describe, expect, it, vi } from "vitest";
import { cocok, itemHalaman, kataKunci } from "./palet-pencocokan";
import { navSections, navUntukPeran } from "./nav-config";

/**
 * Pencocokan dan penyusunan item command palette.
 *
 * Dua hal yang dijaga di sini. Pertama, penyaringan peran: palet mencari di
 * seluruh menu sekaligus, jadi kalau ia lupa menyaring, seorang DEVELOPER bisa
 * menemukan "Verifikasi Pembayaran" lewat Ctrl+K walau menu itu tidak pernah
 * tampak di rail — dan berakhir di halaman yang menolaknya. Kedua, pencocokan
 * yang terlalu longgar sama tidak bergunanya dengan yang terlalu ketat.
 */

const menuAdmin = navUntukPeran("ADMIN");

describe("kataKunci", () => {
  it("memecah ketikan jadi kata kecil, spasi berlebih diabaikan", () => {
    expect(kataKunci("  Verifikasi   UKT ")).toEqual(["verifikasi", "ukt"]);
  });

  it("ketikan kosong tidak menghasilkan kata apa pun", () => {
    expect(kataKunci("   ")).toEqual([]);
  });
});

describe("itemHalaman", () => {
  it("meratakan section beranak jadi satu item per anak", () => {
    const items = itemHalaman(menuAdmin, () => {});

    // Section "Verifikasi Pembayaran" punya tujuh anak: semua kategori plus
    // "Semua kategori". Yang masuk palet anaknya, bukan induknya.
    const verifikasi = items.filter((i) => i.id.startsWith("nav:verifikasi:"));
    expect(verifikasi).toHaveLength(7);
    expect(items.some((i) => i.id === "nav:verifikasi")).toBe(false);
  });

  it("section tanpa anak jadi satu item yang menuju href-nya", () => {
    const pergi = vi.fn();
    const items = itemHalaman(menuAdmin, pergi);

    items.find((i) => i.id === "nav:dashboard")?.jalankan();

    expect(pergi).toHaveBeenCalledWith("/dashboard");
  });

  it("tiap item punya id unik, supaya aria-activedescendant tidak ambigu", () => {
    const items = itemHalaman(menuAdmin, () => {});
    const id = items.map((i) => i.id);

    expect(new Set(id).size).toBe(id.length);
  });

  it("hanya menu yang boleh dilihat peran itu yang masuk palet", () => {
    const items = itemHalaman(navUntukPeran("DEVELOPER"), () => {});

    // DEVELOPER hanya punya forensik. Palet tidak boleh jadi pintu belakang
    // menuju halaman admin yang endpointnya menolaknya.
    expect(items.map((i) => i.id)).toEqual(["nav:forensik"]);
  });

  it("peran yang belum diketahui tidak mendapat item apa pun", () => {
    expect(itemHalaman(navUntukPeran(undefined), () => {})).toEqual([]);
  });
});

describe("cocok", () => {
  const items = itemHalaman(menuAdmin, () => {});
  const cari = (ketikan: string) =>
    items.filter((i) => cocok(i, kataKunci(ketikan))).map((i) => i.label);

  it("menemukan halaman lewat sebagian namanya", () => {
    expect(cari("kelas")).toContain("Kelas");
  });

  it("judul section ikut dicocokkan, bukan hanya judul anaknya", () => {
    // "UKT" adalah nama anak; "verifikasi" hanya ada di judul induknya. Tanpa
    // alias, ketikan ini tidak akan menemukan apa pun.
    expect(cari("verifikasi ukt")).toContain("UKT");
  });

  it("pencocokan substring bisa ikut menyeret kata lain, dan itu disadari", () => {
    // "b(ukt)i" ikut cocok dengan "ukt". Membuatnya cocok hanya di batas kata
    // akan menolak "ujian tertutup" saat orang mengetik "tutup", dan itu jauh
    // lebih mengganggu daripada satu baris tambahan yang tinggal dilewati.
    expect(cari("verifikasi ukt")).toEqual(["Semua kategori", "UKT"]);
  });

  it("urutan kata tidak berpengaruh", () => {
    expect(cari("ukt verifikasi")).toEqual(cari("verifikasi ukt"));
  });

  it("huruf besar-kecil tidak berpengaruh", () => {
    expect(cari("SEMINAR")).toEqual(cari("seminar"));
  });

  it("kata yang tidak ada membuang seluruh hasil, bukan sebagian", () => {
    // Semua kata harus ada. "verifikasi" cocok, "wisuda" tidak — hasilnya nol.
    expect(cari("verifikasi wisuda")).toEqual([]);
  });

  it("ketikan yang tak dikenal tidak menyeret apa pun", () => {
    expect(cari("zzzz")).toEqual([]);
  });

  it("keterangan ikut dicocokkan", () => {
    // Hint "5 cicilan bulanan" hanya ada di keterangan anak UKT.
    expect(cari("cicilan bulanan")).toEqual(["UKT"]);
  });

  it("alias mencocokkan tanpa ikut tampil di label", () => {
    const keluar = {
      id: "aksi:keluar",
      grup: "Tindakan" as const,
      label: "Keluar",
      icon: navSections[0].icon,
      alias: "logout sign out",
      jalankan: () => {},
    };

    expect(cocok(keluar, kataKunci("logout"))).toBe(true);
    expect(cocok(keluar, kataKunci("keluar"))).toBe(true);
    expect(cocok(keluar, kataKunci("hapus"))).toBe(false);
  });
});
