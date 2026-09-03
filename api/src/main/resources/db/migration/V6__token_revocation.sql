-- Pencabutan sesi saat logout.
--
-- Refresh token berumur tujuh hari dan tidak menyimpan state di server, jadi
-- sebelum ini "keluar" hanya menghapus cookie di sisi peramban: tokennya sendiri
-- tetap sah sampai kedaluwarsa. Kolom ini menandai sejak kapan token milik
-- seorang pengguna dianggap sah; token yang diterbitkan sebelumnya ditolak.
--
-- NULL berarti belum pernah ada pencabutan, jadi baris lama tidak perlu diisi.

ALTER TABLE users ADD COLUMN tokens_valid_from TIMESTAMPTZ;

COMMENT ON COLUMN users.tokens_valid_from IS
    'Refresh token yang diterbitkan sebelum waktu ini ditolak. NULL = belum pernah dicabut.';
