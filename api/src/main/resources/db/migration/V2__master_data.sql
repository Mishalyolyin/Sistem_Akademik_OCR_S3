-- Master data: kelas, mahasiswa, tarif, dan template angsuran.
-- Tidak ada program_type di sini — S3 tidak mengenal pembagian Reguler/RPL.

CREATE TYPE discount_tier AS ENUM (
    'NON_ALUMNI', 'KERABAT_ALUMNI', 'ALUMNI', 'ALUMNI_PASUTRI', 'KERJASAMA'
);

CREATE TYPE academic_term AS ENUM ('GASAL', 'GENAP');

CREATE TYPE payment_category AS ENUM (
    'PENDAFTARAN', 'UKT', 'SEMINAR_PROPOSAL',
    'UJIAN_KELAYAKAN', 'UJIAN_TERTUTUP', 'UJIAN_TERBUKA'
);

-- Kelas dibuat otomatis saat import Excel; jumlahnya tidak dipatok.
CREATE TABLE study_classes (
    id            BIGSERIAL   PRIMARY KEY,
    name          VARCHAR(60) NOT NULL,
    kerjasama     BOOLEAN     NOT NULL DEFAULT FALSE,
    academic_year VARCHAR(9)  NOT NULL,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_study_classes_name_year UNIQUE (name, academic_year)
);

CREATE TABLE import_batches (
    id           BIGSERIAL    PRIMARY KEY,
    filename     VARCHAR(255) NOT NULL,
    total_rows   INTEGER      NOT NULL DEFAULT 0,
    success_rows INTEGER      NOT NULL DEFAULT 0,
    failed_rows  INTEGER      NOT NULL DEFAULT 0,
    -- Daftar galat per baris, supaya admin tahu baris mana yang gagal dan kenapa.
    errors       JSONB,
    admin_id     BIGINT       REFERENCES users (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE students (
    id                  BIGSERIAL     PRIMARY KEY,
    user_id             BIGINT        REFERENCES users (id) ON DELETE CASCADE,
    nim                 VARCHAR(30)   NOT NULL,
    name                VARCHAR(150)  NOT NULL,
    phone               VARCHAR(25),
    study_class_id      BIGINT        REFERENCES study_classes (id),

    discount_tier       discount_tier NOT NULL DEFAULT 'NON_ALUMNI',
    start_term          academic_term NOT NULL,
    start_academic_year VARCHAR(9)    NOT NULL,

    -- Dokumen wajib, diisi berurutan oleh mahasiswa sendiri.
    profile_picture     VARCHAR(255),
    nik                 VARCHAR(20),
    ktp_file_path       VARCHAR(255),
    kk_number           VARCHAR(20),
    kk_file_path        VARCHAR(255),
    ijazah_file_path    VARCHAR(255),
    address             TEXT,

    -- Kelebihan bayar yang belum dialokasikan ke cicilan manapun.
    wallet_balance      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    -- Membebaskan mahasiswa dari gate bayar Pendaftaran.
    pendaftaran_exempt  BOOLEAN       NOT NULL DEFAULT FALSE,

    import_batch_id     BIGINT        REFERENCES import_batches (id) ON DELETE SET NULL,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_students_nim UNIQUE (nim),
    CONSTRAINT uq_students_user UNIQUE (user_id),
    CONSTRAINT ck_students_wallet_non_negative CHECK (wallet_balance >= 0)
);

CREATE INDEX idx_students_class ON students (study_class_id);
CREATE INDEX idx_students_tier ON students (discount_tier);

-- Persentase potongan, bisa diubah admin tanpa deploy ulang.
CREATE TABLE discount_tier_rates (
    tier       discount_tier PRIMARY KEY,
    label      VARCHAR(60)   NOT NULL,
    percent    NUMERIC(5, 2) NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT ck_discount_percent_range CHECK (percent >= 0 AND percent <= 100)
);

-- Tarif dasar per kategori, sebelum potongan.
CREATE TABLE tuition_rates (
    id            BIGSERIAL        PRIMARY KEY,
    category      payment_category NOT NULL,
    academic_year VARCHAR(9)       NOT NULL,
    amount        NUMERIC(15, 2)   NOT NULL,
    active        BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),

    CONSTRAINT uq_tuition_rates UNIQUE (category, academic_year),
    CONSTRAINT ck_tuition_amount_positive CHECK (amount > 0)
);

-- Jadwal angsuran. Bulan disimpan sebagai offset dari awal term,
-- jadi satu template dipakai ulang untuk setiap tahun akademik.
CREATE TABLE installment_templates (
    id                 BIGSERIAL        PRIMARY KEY,
    name               VARCHAR(100)     NOT NULL,
    category           payment_category NOT NULL,
    term               academic_term    NOT NULL,
    installments_count INTEGER          NOT NULL,
    active             BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),

    CONSTRAINT uq_installment_templates UNIQUE (category, term),
    CONSTRAINT ck_installments_count CHECK (installments_count BETWEEN 1 AND 24)
);

CREATE TABLE installment_template_items (
    id                      BIGSERIAL   PRIMARY KEY,
    installment_template_id BIGINT      NOT NULL
        REFERENCES installment_templates (id) ON DELETE CASCADE,
    installment_no          INTEGER     NOT NULL,
    -- 0 = bulan pertama term (September untuk Gasal, Februari untuk Genap).
    month_offset            INTEGER     NOT NULL,
    due_day                 INTEGER     NOT NULL DEFAULT 10,

    CONSTRAINT uq_template_items UNIQUE (installment_template_id, installment_no),
    CONSTRAINT ck_month_offset CHECK (month_offset >= 0 AND month_offset <= 23),
    CONSTRAINT ck_due_day CHECK (due_day BETWEEN 1 AND 28)
);
