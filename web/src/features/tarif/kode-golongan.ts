/** Kode golongan dipakai apa adanya di berkas import, jadi bentuknya dibatasi. */
export const POLA_KODE = /^[A-Z][A-Z0-9_]*$/;

/**
 * Mengubah nama golongan jadi kode yang sah, supaya admin tidak perlu
 * memikirkan bentuknya sendiri.
 *
 * Aturannya harus sama persis dengan yang ditegakkan backend — di validasi
 * permintaan maupun di `CHECK` database. Kode yang dihasilkan di sini tapi
 * ditolak di sana berarti admin mengetik nama yang wajar lalu dijawab galat
 * yang tidak ia mengerti sebabnya.
 */
export function kodeDariNama(nama: string): string {
  return nama
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .replace(/^([0-9])/, "G$1");
}
