-- Fondasi autentikasi. Tabel domain (mahasiswa, tagihan, pembayaran) menyusul di Fase 2.

CREATE TYPE user_role AS ENUM ('ADMIN', 'MAHASISWA', 'DEVELOPER');

CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          user_role    NOT NULL DEFAULT 'MAHASISWA',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_role ON users (role);
