-- Bukti bayar dan pengaturan sistem.

CREATE TYPE payment_status AS ENUM (
    'PENDING',        -- baru diunggah, menunggu OCR
    'NEEDS_REVIEW',   -- OCR kurang yakin, perlu keputusan admin
    'AUTO_VERIFIED',  -- diverifikasi otomatis oleh OCR
    'VERIFIED',       -- diverifikasi manual oleh admin
    'REJECTED',
    'FAILED'          -- OCR gagal diproses setelah beberapa percobaan
);

CREATE TABLE payments (
    id                 BIGSERIAL      PRIMARY KEY,
    student_id         BIGINT         NOT NULL
        REFERENCES students (id) ON DELETE CASCADE,
    payment_plan_id    BIGINT         REFERENCES payment_plans (id) ON DELETE SET NULL,
    -- Cicilan yang dituju. Boleh kosong: pembayaran bebas dialokasikan
    -- ke cicilan terlama yang belum lunas.
    installment_id     BIGINT         REFERENCES installments (id) ON DELETE SET NULL,

    -- Nominal yang diklaim mahasiswa saat mengunggah.
    amount             NUMERIC(15, 2) NOT NULL,
    proof_file_path    VARCHAR(500)   NOT NULL,
    bank_name          VARCHAR(60),
    payment_proof_date DATE,

    status             payment_status NOT NULL DEFAULT 'PENDING',
    -- Hasil mentah dari service OCR, disimpan apa adanya untuk penelusuran.
    ocr_data           JSONB,
    ocr_confidence     NUMERIC(5, 4),

    verified_at        TIMESTAMPTZ,
    verified_by        BIGINT         REFERENCES users (id),
    reject_reason      VARCHAR(500),
    -- Terisi setelah uangnya dialokasikan ke cicilan (Fase 5).
    allocated_at       TIMESTAMPTZ,

    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT ck_payment_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_student ON payments (student_id);
CREATE INDEX idx_payments_plan ON payments (payment_plan_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created ON payments (created_at DESC);

-- Catatan tiap keputusan verifikasi, supaya bisa ditelusuri siapa memutuskan apa.
CREATE TABLE verification_logs (
    id           BIGSERIAL      PRIMARY KEY,
    payment_id   BIGINT         NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    from_status  payment_status,
    to_status    payment_status NOT NULL,
    -- Kosong berarti keputusan otomatis oleh sistem.
    admin_id     BIGINT         REFERENCES users (id),
    note         VARCHAR(500),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_verification_logs_payment ON verification_logs (payment_id);

-- Pengaturan yang bisa diubah admin tanpa deploy ulang.
CREATE TABLE system_settings (
    key         VARCHAR(80)  PRIMARY KEY,
    value       TEXT,
    description VARCHAR(300),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO system_settings (key, value, description) VALUES
    ('ocr_confidence_threshold', '80',
     'Keyakinan minimal (persen) agar pembayaran diverifikasi otomatis'),
    ('ocr_auto_reject_threshold', '10',
     'Di bawah keyakinan ini (persen) bukti langsung ditolak: kemungkinan bukan bukti bayar'),
    ('payment_tolerance_amount', '0',
     'Selisih nominal yang masih dianggap cocok, dalam rupiah (mis. kode unik bank)'),
    ('ocr_blacklist_keywords', 'judi,slot,togel,casino',
     'Kata kunci yang membuat bukti langsung ditolak, dipisah koma'),
    ('ocr_date_validation_days', '',
     'Batas umur tanggal transaksi dalam hari. Kosong berarti tidak dibatasi'),
    ('bank_account_number', '7 1234 5678 90',
     'Rekening tujuan yang harus muncul di bukti transfer'),
    ('admin_phone_notification', '',
     'Nomor WhatsApp admin untuk pemberitahuan pembayaran yang perlu ditinjau');
