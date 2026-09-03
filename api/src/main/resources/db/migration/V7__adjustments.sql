-- Penyesuaian saldo dan cicilan.
--
-- Dua kasus yang sebelumnya hanya bisa dibereskan lewat database langsung:
--   1. Kelebihan bayar yang muncul saat nominal cicilan diturunkan di bawah
--      yang sudah dibayar. Nominal berubah, amount_paid sengaja tidak disentuh,
--      jadi selisihnya perlu dipindahkan ke saldo mahasiswa.
--   2. Koreksi golongan potongan yang baru ketahuan setelah mahasiswa mengunggah
--      bukti. Golongan terkunci saat itu, jadi koreksinya lewat sini.
--
-- Sengaja terpisah dari installment_amount_changes: yang itu mencatat perubahan
-- BESAR TAGIHAN, yang ini mencatat perpindahan UANG. Kalau digabung, riwayat
-- "tagihan berubah" dan "uang masuk" bercampur dan sulit ditelusuri saat sengketa.

CREATE TABLE adjustments (
    id             BIGSERIAL      PRIMARY KEY,

    -- Pemiliknya selalu diketahui, termasuk saat sasarannya sebuah cicilan.
    -- Dengan begitu riwayat per mahasiswa bisa diambil dalam satu kueri.
    student_id     BIGINT         NOT NULL REFERENCES students (id) ON DELETE CASCADE,

    -- Kosong berarti penyesuaian mengenai saldo mahasiswa, bukan cicilan tertentu.
    installment_id BIGINT         REFERENCES installments (id) ON DELETE CASCADE,

    -- Bertanda: positif menambah, negatif mengurangi. Nol tidak ada artinya.
    amount         NUMERIC(15, 2) NOT NULL,

    -- Nilai sasaran sesudah penyesuaian, disimpan supaya riwayat bisa dibaca
    -- tanpa perlu memutar ulang seluruh mutasi dari awal.
    balance_after  NUMERIC(15, 2) NOT NULL,

    reason         TEXT           NOT NULL,
    admin_id       BIGINT         NOT NULL REFERENCES users (id),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT ck_adjustments_amount_not_zero CHECK (amount <> 0),
    CONSTRAINT ck_adjustments_reason_length   CHECK (length(btrim(reason)) >= 5)
);

CREATE INDEX idx_adjustments_student ON adjustments (student_id, created_at DESC);
CREATE INDEX idx_adjustments_installment ON adjustments (installment_id);
