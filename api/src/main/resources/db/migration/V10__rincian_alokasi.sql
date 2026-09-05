-- Rincian ke mana tiap rupiah satu pembayaran mengalir.
--
-- Sampai sekarang alokasi hanya meninggalkan jejak berupa `payments.allocated_at`
-- dan perubahan `installments.amount_paid`. Dari situ tidak ada cara mengetahui
-- pembayaran mana yang melunasi cicilan mana: begitu dua pembayaran masuk ke
-- tagihan yang sama, angkanya bercampur dan tidak bisa diurai lagi.
--
-- Akibatnya dua hal tidak bisa dikerjakan sama sekali:
--   1. Membatalkan verifikasi yang salah — uangnya tidak bisa ditarik kembali
--      dengan tepat, karena tidak diketahui ke mana perginya.
--   2. Menjawab "cicilan mana yang dibayar bukti ini" saat mahasiswa bertanya.

CREATE TYPE allocation_kind AS ENUM (
    -- Masuk ke satu cicilan.
    'INSTALLMENT',
    -- Kelebihan yang masuk saldo mahasiswa.
    'WALLET',
    -- Sisa kecil dalam toleransi, dibuang karena memang tidak pernah diterima
    -- kampus (kode unik bank). Ikut dicatat supaya jumlah seluruh baris selalu
    -- sama persis dengan nominal pembayarannya.
    'DISCARDED'
);

CREATE TABLE payment_allocations (
    id             BIGSERIAL       PRIMARY KEY,
    payment_id     BIGINT          NOT NULL
        REFERENCES payments (id) ON DELETE CASCADE,
    -- Terisi hanya untuk INSTALLMENT.
    installment_id BIGINT
        REFERENCES installments (id) ON DELETE CASCADE,
    kind           allocation_kind NOT NULL,
    amount         NUMERIC(15, 2)  NOT NULL CHECK (amount > 0),
    -- Terisi saat alokasinya dibatalkan. Barisnya sengaja TIDAK dihapus:
    -- pembatalan adalah peristiwa yang justru paling perlu bisa ditelusuri.
    reversed_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT ck_allocation_installment CHECK (
        (kind = 'INSTALLMENT' AND installment_id IS NOT NULL)
        OR (kind <> 'INSTALLMENT' AND installment_id IS NULL)
    )
);

CREATE INDEX idx_payment_allocations_payment ON payment_allocations (payment_id);
CREATE INDEX idx_payment_allocations_installment ON payment_allocations (installment_id);
