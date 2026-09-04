-- Golongan potongan menjadi data, bukan tipe enum.
--
-- Sebelumnya golongan adalah enum native PostgreSQL dengan lima nilai tetap.
-- Menambah golongan baru berarti mengubah tipe di database, mengubah enum di
-- Java, lalu deploy ulang — pekerjaan pengembang untuk sesuatu yang sebenarnya
-- keputusan bagian keuangan.
--
-- Tabel discount_tier_rates sudah menyimpan label dan persentase, jadi ia
-- tinggal dijadikan sumber kebenaran: kuncinya berubah dari enum ke kode teks,
-- dan students menunjuk ke sana lewat kunci asing.

-- 1. Kolom tambahan supaya golongan bisa dinonaktifkan tanpa dihapus, dan
--    urutan tampilannya bisa diatur.
ALTER TABLE discount_tier_rates
    ADD COLUMN active     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN sort_order INT     NOT NULL DEFAULT 0;

-- 2. Lepas ketergantungan pada tipe enum. Nilai bawaan harus dilepas dulu
--    karena ia masih bertipe enum.
ALTER TABLE students ALTER COLUMN discount_tier DROP DEFAULT;

ALTER TABLE discount_tier_rates
    ALTER COLUMN tier TYPE VARCHAR(40) USING tier::text;

ALTER TABLE students
    ALTER COLUMN discount_tier TYPE VARCHAR(40) USING discount_tier::text;

ALTER TABLE students ALTER COLUMN discount_tier SET DEFAULT 'NON_ALUMNI';

-- 3. Golongan yang dipakai mahasiswa wajib benar-benar ada. Tanpa kunci asing,
--    salah ketik kode golongan baru menghasilkan mahasiswa yang tarifnya tidak
--    bisa dihitung sama sekali.
ALTER TABLE students
    ADD CONSTRAINT fk_students_discount_tier
        FOREIGN KEY (discount_tier) REFERENCES discount_tier_rates (tier)
        ON UPDATE CASCADE;

-- 4. Tipe enumnya sudah tidak dipakai kolom mana pun.
DROP TYPE discount_tier;

-- 5. Kode golongan hanya huruf kapital, angka, dan garis bawah — dipakai apa
--    adanya di API dan berkas import, jadi tidak boleh mengandung spasi.
ALTER TABLE discount_tier_rates
    ADD CONSTRAINT ck_discount_tier_code CHECK (tier ~ '^[A-Z][A-Z0-9_]*$');

-- 6. Urutan tampilan mengikuti besar potongan, sebagaimana brosur.
UPDATE discount_tier_rates SET sort_order = CASE tier
    WHEN 'NON_ALUMNI'     THEN 1
    WHEN 'KERABAT_ALUMNI' THEN 2
    WHEN 'ALUMNI'         THEN 3
    WHEN 'ALUMNI_PASUTRI' THEN 4
    WHEN 'KERJASAMA'      THEN 5
    ELSE 99
END;

CREATE INDEX idx_discount_tier_rates_urutan ON discount_tier_rates (sort_order, tier);
