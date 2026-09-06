-- Tiga hal yang saling tidak berhubungan tapi lahir dari satu putaran audit
-- terhadap sistem S2: pembatalan tagihan, syarat lunas UKT sebelum ujian, dan
-- penyimpanan gambar hasil praproses OCR.

-- 1. Pembatalan tagihan -------------------------------------------------------
--
-- `plan_status` sudah punya nilai CANCELLED sejak V4, dan kedua indeks unik di
-- bawah ini sudah mengecualikannya — tapi tidak ada satu baris kode pun yang
-- pernah menuliskannya. Akibatnya tagihan yang salah dibuat mengunci slotnya
-- selamanya: Seminar Proposal yang terlanjur dibuat untuk mahasiswa keliru
-- membuat mahasiswa itu tidak akan pernah bisa punya Seminar Proposal lagi,
-- dan satu-satunya jalan keluar adalah menghapus mahasiswanya.
--
-- Kolom di bawah melengkapi pembatalan dengan jejaknya: siapa, kapan, kenapa.
-- Alasan wajib, sama seperti penolakan pembayaran dan perubahan nominal —
-- yang dibatalkan di sini adalah sebuah tagihan yang pernah berlaku.
ALTER TABLE payment_plans
    ADD COLUMN cancelled_at     TIMESTAMPTZ,
    ADD COLUMN cancelled_by     BIGINT REFERENCES users (id),
    ADD COLUMN cancel_reason    VARCHAR(500);

-- Tagihan yang dibatalkan wajib punya alasan, dan yang tidak dibatalkan wajib
-- tidak punya. Tanpa ini, status dan alasannya bisa berjalan sendiri-sendiri.
ALTER TABLE payment_plans
    ADD CONSTRAINT ck_plan_cancel_reason CHECK (
        (status = 'CANCELLED' AND cancel_reason IS NOT NULL AND cancelled_at IS NOT NULL)
        OR (status <> 'CANCELLED' AND cancel_reason IS NULL AND cancelled_at IS NULL)
    );

-- 2. Syarat lunas UKT sebelum tahap ujian -------------------------------------
--
-- Keempat tahap ujian baru boleh diajukan setelah SELURUH enam semester UKT
-- ditagihkan dan lunas. Aturan itu ketat dengan sengaja: biaya ujian adalah
-- akhir masa studi, dan kampus tidak meluluskan mahasiswa yang masih menunggak.
--
-- Pengecualiannya data, bukan tambalan kode — persis seperti
-- `pendaftaran_exempt` yang sudah ada. Bagian keuangan sesekali memang
-- membolehkan mahasiswa tertentu maju lebih dulu, dan tanpa penanda ini
-- satu-satunya jalan adalah menyentuh database langsung.
ALTER TABLE students
    ADD COLUMN ujian_exempt BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN students.ujian_exempt IS
    'Membebaskan mahasiswa dari syarat lunas seluruh UKT sebelum mendaftar tahap ujian.';

-- 3. Gambar hasil praproses OCR -----------------------------------------------
--
-- Yang dibaca Tesseract bukan berkas asli yang diunggah mahasiswa, melainkan
-- hasil praproses OpenCV: grayscale, kontras dinaikkan, noise dibuang. Selama
-- ini gambar itu dibuang begitu pembacaan selesai.
--
-- Membuangnya berarti dataset gambar tidak akan pernah bisa dibuat, dan gambar
-- yang tidak disimpan hari ini tidak bisa dipulihkan besok — beda dengan angka,
-- yang selalu bisa dihitung ulang dari data yang ada.
ALTER TABLE payments
    ADD COLUMN processed_file_path VARCHAR(500);

COMMENT ON COLUMN payments.processed_file_path IS
    'Gambar hasil praproses OpenCV yang benar-benar dibaca Tesseract. '
    'Dipakai forensik dan ekspor dataset gambar.';

-- Dipakai ekspor dataset: hanya bukti berlabel manusia yang gambarnya tersimpan.
CREATE INDEX idx_payments_dataset_gambar
    ON payments (id)
    WHERE processed_file_path IS NOT NULL AND verified_by IS NOT NULL;
