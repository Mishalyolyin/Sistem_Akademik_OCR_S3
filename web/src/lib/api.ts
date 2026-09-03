/**
 * Klien HTTP ke API Spring Boot.
 *
 * Token disimpan di memori proses browser, bukan localStorage — refresh token
 * nantinya dikirim lewat httpOnly cookie dari backend (Fase 1, bagian auth).
 */

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly detail?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

let accessToken: string | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

type RequestOptions = Omit<RequestInit, "body"> & {
  /** Objek biasa akan di-JSON.stringify; FormData dikirim apa adanya. */
  body?: unknown;
  /** Query string, nilai null/undefined dibuang. */
  params?: Record<string, string | number | boolean | null | undefined>;
};

export async function apiFetch<T>(
  path: string,
  { body, params, headers, ...init }: RequestOptions = {},
): Promise<T> {
  const url = new URL(
    path.startsWith("/") ? path.slice(1) : path,
    API_BASE_URL.endsWith("/") ? API_BASE_URL : `${API_BASE_URL}/`,
  );

  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== null && value !== undefined && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }

  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;
  const finalHeaders = new Headers(headers);

  if (accessToken) {
    finalHeaders.set("Authorization", `Bearer ${accessToken}`);
  }
  if (body !== undefined && !isFormData) {
    finalHeaders.set("Content-Type", "application/json");
  }

  const response = await fetch(url, {
    ...init,
    headers: finalHeaders,
    credentials: "include",
    body:
      body === undefined
        ? undefined
        : isFormData
          ? (body as FormData)
          : JSON.stringify(body),
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  // Jawaban galat datang sebagai "application/problem+json", bukan
  // "application/json". Mencocokkan tipe lengkapnya membuat seluruh
  // ProblemDetail terbaca sebagai teks biasa, dan pesan yang sampai ke layar
  // pengguna jadi JSON mentah alih-alih kalimat di field detail.
  const payload = contentType.includes("json")
    ? await response.json().catch(() => null)
    : await response.text();

  if (!response.ok) {
    // Spring Boot mengembalikan RFC 7807 ProblemDetail: { title, detail, status }
    const message =
      (payload && typeof payload === "object" && "detail" in payload
        ? String((payload as { detail: unknown }).detail)
        : null) ??
      (typeof payload === "string" && payload ? payload : null) ??
      `Permintaan gagal (${response.status})`;
    throw new ApiError(response.status, message, payload);
  }

  return payload as T;
}

/** Bentuk halaman dari Spring Data Pageable. */
export type Page<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
