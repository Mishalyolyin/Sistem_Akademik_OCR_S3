-- Tagihan: rencana pembayaran, cicilan, dan audit perubahan nominal.

CREATE TYPE plan_status AS ENUM ('ACTIVE', 'COMPLETED', 'CANCELLED');

CREATE TYPE installment_status AS ENUM ('UNPAID', 'PARTIAL', 'PAID', 'OVERDUE');

CREATE TABLE payment_plans (
    id                      BIGSERIAL        PRIMARY KEY,
    student_id              BIGINT           NOT NULL
        REFERENCES students (id) ON DELETE CASCADE,
    installment_template_id BIGINT           REFERENCES installment_templates (id),
    category                payment_category NOT NULL,
    academic_year           VARCHAR(9)       NOT NULL,
    term                    academic_term    NOT NULL,

    -- Semester ke berapa; hanya terisi untuk kategori UKT (1 sampai 6).
    semester_number         INTEGER,

    -- Tarif dasar sebelum potongan, dibekukan saat plan dibuat.
    base_amount             NUMERIC(15, 2)   NOT NULL,
    -- Persen potongan yang berlaku saat plan dibuat, juga dibekukan.
    discount_percent        NUMERIC(5, 2)    NOT NULL DEFAULT 0,
    -- Jumlah yang harus dibayar = base_amount setelah potongan.
    -- Dihitung ulang bila ada cicilan yang diubah nominalnya.
    total_amount            NUMERIC(15, 2)   NOT NULL,

    status                  plan_status      NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ      NOT NULL DEFAULT now(),

    CONSTRAINT ck_plan_amounts CHECK (base_amount > 0 AND total_amount >= 0),
    CONSTRAINT ck_plan_discount CHECK (discount_percent >= 0 AND discount_percent <= 100),
    CONSTRAINT ck_plan_semester CHECK (
        (category = 'UKT' AND semester_number BETWEEN 1 AND 6)
        OR (category <> 'UKT' AND semester_number IS NULL)
    )
);

-- Satu plan aktif per mahasiswa, tahun, term, dan kategori.
-- Plan yang dibatalkan tidak menghalangi pembuatan plan baru.
CREATE UNIQUE INDEX uq_active_plan
    ON payment_plans (student_id, category, academic_year, term)
    WHERE status <> 'CANCELLED';

-- Kategori sekali bayar hanya boleh muncul satu kali seumur studi.
CREATE UNIQUE INDEX uq_one_time_plan
    ON payment_plans (student_id, category)
    WHERE category <> 'UKT' AND status <> 'CANCELLED';

CREATE INDEX idx_plans_student ON payment_plans (student_id);

CREATE TABLE installments (
    id              BIGSERIAL          PRIMARY KEY,
    payment_plan_id BIGINT             NOT NULL
        REFERENCES payment_plans (id) ON DELETE CASCADE,
    installment_no  INTEGER            NOT NULL,
    due_date        DATE               NOT NULL,
    amount          NUMERIC(15, 2)     NOT NULL,
    amount_paid     NUMERIC(15, 2)     NOT NULL DEFAULT 0,
    status          installment_status NOT NULL DEFAULT 'UNPAID',
    created_at      TIMESTAMPTZ        NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ        NOT NULL DEFAULT now(),

    CONSTRAINT uq_installment_no UNIQUE (payment_plan_id, installment_no),
    CONSTRAINT ck_installment_amount CHECK (amount > 0),
    CONSTRAINT ck_installment_paid CHECK (amount_paid >= 0)
);

CREATE INDEX idx_installments_plan ON installments (payment_plan_id);
CREATE INDEX idx_installments_due ON installments (due_date) WHERE status <> 'PAID';

-- Audit perubahan nominal cicilan.
-- Sengaja terpisah dari tabel adjustments: yang ini mengubah "berapa yang harus
-- dibayar", sedangkan adjustment mengubah "berapa yang sudah dibayar".
CREATE TABLE installment_amount_changes (
    id             BIGSERIAL      PRIMARY KEY,
    installment_id BIGINT         NOT NULL
        REFERENCES installments (id) ON DELETE CASCADE,
    old_amount     NUMERIC(15, 2) NOT NULL,
    new_amount     NUMERIC(15, 2) NOT NULL,
    reason         VARCHAR(500)   NOT NULL,
    admin_id       BIGINT         NOT NULL REFERENCES users (id),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT ck_change_reason_not_blank CHECK (length(trim(reason)) >= 5)
);

CREATE INDEX idx_amount_changes_installment
    ON installment_amount_changes (installment_id);
