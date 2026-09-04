import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch, setAccessToken } from "./api";

/**
 * Penguraian jawaban dari Spring Boot.
 *
 * Yang paling penting di sini: galat datang sebagai
 * `application/problem+json`, bukan `application/json`. Mencocokkan tipe
 * lengkapnya pernah membuat SELURUH pesan galat di aplikasi tampil sebagai JSON
 * mentah di layar pengguna — bukan cuma di satu halaman. Bug itu baru ketahuan
 * lewat peramban sungguhan, jadi sekarang dikunci di sini.
 */
describe("apiFetch", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    setAccessToken(null);
  });

  function jawaban(
    body: unknown,
    { status = 200, contentType = "application/json" } = {},
  ) {
    const fetchPalsu = vi.fn().mockResolvedValue(
      new Response(typeof body === "string" ? body : JSON.stringify(body), {
        status,
        headers: { "content-type": contentType },
      }),
    );
    vi.stubGlobal("fetch", fetchPalsu);
    return fetchPalsu;
  }

  it("membaca ProblemDetail yang bertipe application/problem+json", async () => {
    jawaban(
      { type: "about:blank", title: "Gagal masuk", status: 401, detail: "Email atau kata sandi salah." },
      { status: 401, contentType: "application/problem+json" },
    );

    await expect(apiFetch("/auth/login")).rejects.toMatchObject({
      status: 401,
      message: "Email atau kata sandi salah.",
    });
  });

  it("galat tanpa field detail tetap memberi pesan, bukan 'undefined'", async () => {
    jawaban({ title: "Kacau" }, { status: 500, contentType: "application/problem+json" });

    await expect(apiFetch("/apa-saja")).rejects.toThrow("Permintaan gagal (500)");
  });

  it("badan bukan JSON dipakai apa adanya sebagai pesan", async () => {
    jawaban("Gateway sedang tumbang", { status: 502, contentType: "text/plain" });

    await expect(apiFetch("/apa-saja")).rejects.toThrow("Gateway sedang tumbang");
  });

  it("melempar ApiError yang membawa status dan badan aslinya", async () => {
    jawaban(
      { detail: "Golongan masih dipakai.", status: 409 },
      { status: 409, contentType: "application/problem+json" },
    );

    const galat: unknown = await apiFetch("/tuition/tiers/ALUMNI").catch(
      (e: unknown) => e,
    );

    expect(galat).toBeInstanceOf(ApiError);
    expect((galat as ApiError).status).toBe(409);
    expect((galat as ApiError).detail).toMatchObject({ status: 409 });
  });

  it("204 tidak diurai sebagai JSON, karena badannya memang kosong", async () => {
    const fetchPalsu = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchPalsu);

    await expect(apiFetch("/students/1")).resolves.toBeUndefined();
  });

  it("params kosong dibuang supaya tidak jadi saringan yang tidak diminta", async () => {
    const fetchPalsu = jawaban({ content: [] });

    await apiFetch("/students", {
      params: { search: "", classId: 3, tier: undefined, active: null },
    });

    const url = fetchPalsu.mock.calls[0][0] as URL;
    expect(url.searchParams.get("classId")).toBe("3");
    expect(url.searchParams.has("search")).toBe(false);
    expect(url.searchParams.has("tier")).toBe(false);
    expect(url.searchParams.has("active")).toBe(false);
  });

  it("token dipasang di header hanya kalau memang ada", async () => {
    const tanpaToken = jawaban({});
    await apiFetch("/auth/me");
    expect(
      (tanpaToken.mock.calls[0][1] as RequestInit & { headers: Headers }).headers.has(
        "Authorization",
      ),
    ).toBe(false);

    setAccessToken("token-uji");
    const denganToken = jawaban({});
    await apiFetch("/auth/me");
    expect(
      (denganToken.mock.calls[0][1] as RequestInit & { headers: Headers }).headers.get(
        "Authorization",
      ),
    ).toBe("Bearer token-uji");
  });

  it("FormData dikirim apa adanya, tanpa dipaksa jadi JSON", async () => {
    const fetchPalsu = jawaban({});
    const form = new FormData();
    form.set("file", new Blob(["isi"]), "bukti.png");

    await apiFetch("/me/pembayaran", { method: "POST", body: form });

    const init = fetchPalsu.mock.calls[0][1] as RequestInit & { headers: Headers };
    expect(init.body).toBe(form);
    // Content-Type dibiarkan kosong supaya peramban menuliskan boundary-nya.
    expect(init.headers.has("Content-Type")).toBe(false);
  });
});
