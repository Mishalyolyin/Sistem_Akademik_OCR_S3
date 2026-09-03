-- Data acuan sesuai brosur Pendaftaran Program Doktor PAI 2026.
-- Semuanya bisa diubah admin lewat menu, jadi ini hanya nilai awal.

INSERT INTO discount_tier_rates (tier, label, percent) VALUES
    ('NON_ALUMNI',     'Non alumni',        0.00),
    ('KERABAT_ALUMNI', 'Kerabat alumni',   20.00),
    ('ALUMNI',         'Alumni',           25.00),
    ('ALUMNI_PASUTRI', 'Alumni + pasutri', 35.00),
    ('KERJASAMA',      'Kerjasama',        40.00);

-- Tarif dasar sebelum potongan. Total non alumni:
-- 1.000.000 + (10.000.000 x 6) + 5.000.000 + 5.000.000 + 10.000.000 + 10.000.000 = 91.000.000
INSERT INTO tuition_rates (category, academic_year, amount) VALUES
    ('PENDAFTARAN',      '2026/2027',  1000000),
    ('UKT',              '2026/2027', 10000000),
    ('SEMINAR_PROPOSAL', '2026/2027',  5000000),
    ('UJIAN_KELAYAKAN',  '2026/2027',  5000000),
    ('UJIAN_TERTUTUP',   '2026/2027', 10000000),
    ('UJIAN_TERBUKA',    '2026/2027', 10000000);

-- UKT dicicil 5x bulanan.
-- Gasal  : September, Oktober, November, Desember, Januari
-- Genap  : Februari, Maret, April, Mei, Juni
INSERT INTO installment_templates (name, category, term, installments_count) VALUES
    ('UKT 5x Angsuran (Gasal)', 'UKT', 'GASAL', 5),
    ('UKT 5x Angsuran (Genap)', 'UKT', 'GENAP', 5);

INSERT INTO installment_template_items (installment_template_id, installment_no, month_offset)
SELECT t.id, i.installment_no, i.month_offset
FROM installment_templates t
CROSS JOIN (VALUES (1, 0), (2, 1), (3, 2), (4, 3), (5, 4))
    AS i (installment_no, month_offset)
WHERE t.category = 'UKT';

-- Kategori sekali bayar: satu "cicilan" jatuh tempo di bulan pertama term.
INSERT INTO installment_templates (name, category, term, installments_count) VALUES
    ('Pendaftaran (Gasal)',      'PENDAFTARAN',      'GASAL', 1),
    ('Pendaftaran (Genap)',      'PENDAFTARAN',      'GENAP', 1),
    ('Seminar Proposal (Gasal)', 'SEMINAR_PROPOSAL', 'GASAL', 1),
    ('Seminar Proposal (Genap)', 'SEMINAR_PROPOSAL', 'GENAP', 1),
    ('Ujian Kelayakan (Gasal)',  'UJIAN_KELAYAKAN',  'GASAL', 1),
    ('Ujian Kelayakan (Genap)',  'UJIAN_KELAYAKAN',  'GENAP', 1),
    ('Ujian Tertutup (Gasal)',   'UJIAN_TERTUTUP',   'GASAL', 1),
    ('Ujian Tertutup (Genap)',   'UJIAN_TERTUTUP',   'GENAP', 1),
    ('Ujian Terbuka (Gasal)',    'UJIAN_TERBUKA',    'GASAL', 1),
    ('Ujian Terbuka (Genap)',    'UJIAN_TERBUKA',    'GENAP', 1);

INSERT INTO installment_template_items (installment_template_id, installment_no, month_offset)
SELECT id, 1, 0 FROM installment_templates WHERE installments_count = 1;
