-- Hasil pembacaan OCR atas dokumen wajib mahasiswa.
--
-- Service OCR sudah bisa membaca KTP, Kartu Keluarga, ijazah, dan menganalisis
-- foto sejak awal — `ocr/ocr_processor.py` punya process_ktp, process_kk,
-- process_ijazah, dan process_photo. Yang tidak pernah ada adalah sisi Spring
-- yang mengirim berkasnya ke sana, jadi kemampuan itu menganggur sepenuhnya
-- dan admin memeriksa setiap dokumen dengan mata.
--
-- Hasilnya disimpan apa adanya sebagai JSONB, sama seperti `payments.ocr_data`:
-- bentuk jawaban service OCR bisa berubah, dan yang berguna saat menelusuri
-- justru field yang tidak diduga ada.

ALTER TABLE students
    ADD COLUMN ktp_ocr_data             JSONB,
    ADD COLUMN kk_ocr_data              JSONB,
    ADD COLUMN ijazah_ocr_data          JSONB,
    ADD COLUMN profile_picture_analysis JSONB,
    -- Ijazah adalah sumber kebenaran untuk keduanya: tidak ada isian manualnya
    -- di mana pun, jadi nilainya memang datang dari pembacaan.
    ADD COLUMN birth_place              VARCHAR(100),
    ADD COLUMN birth_date               DATE;

-- Dipakai halaman forensik untuk mencari dokumen yang belum pernah terbaca.
CREATE INDEX idx_students_ktp_belum_terbaca
    ON students (id) WHERE ktp_file_path IS NOT NULL AND ktp_ocr_data IS NULL;
