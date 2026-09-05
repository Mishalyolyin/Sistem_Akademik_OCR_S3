-- Pengingat jatuh tempo lewat WhatsApp.
--
-- Kirimannya dicatat, bukan sekadar dikirim: tanpa jejak ini penjadwal yang
-- berjalan tiap hari akan mengirimi mahasiswa pesan yang sama berulang-ulang,
-- dan pengingat yang mengganggu lebih buruk daripada tidak ada pengingat.

CREATE TYPE reminder_kind AS ENUM (
    -- Menjelang jatuh tempo, sekian hari sebelumnya.
    'JATUH_TEMPO',
    -- Sudah lewat jatuh tempo dan masih ada sisa.
    'LEWAT_TEMPO'
);

CREATE TYPE reminder_status AS ENUM (
    'SENT',
    -- Gateway menolak atau tidak bisa dihubungi; boleh dicoba lagi besok.
    'FAILED',
    -- Tidak jadi dikirim, misalnya mahasiswanya belum mengisi nomor telepon.
    'SKIPPED'
);

CREATE TABLE reminder_logs (
    id             BIGSERIAL       PRIMARY KEY,
    installment_id BIGINT          NOT NULL
        REFERENCES installments (id) ON DELETE CASCADE,
    student_id     BIGINT          NOT NULL
        REFERENCES students (id) ON DELETE CASCADE,
    kind           reminder_kind   NOT NULL,
    -- Nomor dan isi pesan disimpan apa adanya saat itu: nomor mahasiswa bisa
    -- berubah, dan saat menelusuri keluhan yang dicari justru pesan yang
    -- benar-benar terkirim, bukan pesan yang akan tersusun hari ini.
    phone          VARCHAR(25),
    message        TEXT,
    status         reminder_status NOT NULL,
    error          TEXT,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_reminder_logs_installment ON reminder_logs (installment_id, kind);
CREATE INDEX idx_reminder_logs_student ON reminder_logs (student_id, created_at DESC);

-- Satu pengingat yang BERHASIL per cicilan per jenis, selamanya. Yang gagal
-- sengaja tidak ikut terkunci supaya bisa dicoba lagi keesokan harinya.
--
-- Penjagaannya di sini, bukan hanya di kode: penjadwal bisa berjalan dua kali
-- kalau nanti ada lebih dari satu instance, dan pemeriksaan "sudah pernah
-- dikirim?" di aplikasi tidak menahan dua proses yang berjalan bersamaan.
CREATE UNIQUE INDEX uq_reminder_terkirim
    ON reminder_logs (installment_id, kind)
    WHERE status = 'SENT';

-- Pengaturan, semuanya bisa diubah admin tanpa deploy ulang.
INSERT INTO system_settings (key, value, description) VALUES
    ('reminder_enabled', 'false',
     'Nyalakan pengingat jatuh tempo lewat WhatsApp'),
    ('reminder_days_before', '3',
     'Berapa hari sebelum jatuh tempo pengingat dikirim'),
    ('reminder_overdue_enabled', 'true',
     'Ikut mengingatkan cicilan yang sudah lewat jatuh tempo'),
    ('whatsapp_gateway_url', 'https://api.fonnte.com/send',
     'Alamat gateway WhatsApp; Fonnte dan Wablas sama-sama menerima POST biasa'),
    ('whatsapp_gateway_token', '',
     'Token gateway WhatsApp. Selama kosong, pesan hanya dicatat di log dan tidak dikirim'),
    ('reminder_message_template', E'Halo {nama},\n\nCicilan {kategori} ke-{cicilan} sebesar {nominal} jatuh tempo {tanggal}.\nSisa yang perlu dibayar: {sisa}.\n\nTerima kasih.',
     'Isi pesan pengingat. Penanda yang tersedia: {nama} {nim} {kategori} {cicilan} {nominal} {sisa} {tanggal}');

-- Pengaturan mati sejak V5: tidak ada satu baris kode pun yang membacanya, dan
-- pengingat ini pun ditujukan ke mahasiswa, bukan ke admin. Dihapus supaya
-- halaman Pengaturan tidak menawarkan tombol yang tidak melakukan apa-apa.
DELETE FROM system_settings WHERE key = 'admin_phone_notification';
