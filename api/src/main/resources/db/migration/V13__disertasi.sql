-- Identitas disertasi: judul, promotor, dan berkasnya.
--
-- Sistem ini sejak awal hanya mengurus UANG keempat tahap ujian. Ia tahu
-- seorang mahasiswa membayar Ujian Tertutup, tapi tidak tahu disertasi mana
-- yang sedang diuji maupun siapa yang membimbingnya — sehingga admin yang
-- memverifikasi bukti bayarnya tidak punya cara memastikan ia sedang melihat
-- berkas orang yang benar.
--
-- Sistem S2 menyimpannya per tagihan (`munaqosah_details.payment_plan_id`).
-- Di sini SENGAJA per mahasiswa. Alasannya struktur S3: keempat tahap ujian —
-- Seminar Proposal, Ujian Kelayakan, Ujian Tertutup, Ujian Terbuka — semuanya
-- mengacu ke satu disertasi yang sama. Menempelkannya ke tagihan berarti judul
-- yang sama tersimpan empat kali, dan keempat salinan itu bebas menyimpang satu
-- sama lain tanpa ada yang menyadarinya.

CREATE TABLE dissertation_details (
    -- Kunci primernya student_id itu sendiri: satu mahasiswa satu disertasi,
    -- dijamin skema, bukan hanya oleh kode yang mengingat untuk memeriksanya.
    student_id             BIGINT       PRIMARY KEY
        REFERENCES students (id) ON DELETE CASCADE,

    title                  VARCHAR(300) NOT NULL,
    -- Promotor utama dan ko-promotor. Keduanya wajib: di Program Doktor
    -- pembimbingnya memang selalu dua.
    promotor               VARCHAR(150) NOT NULL,
    copromotor             VARCHAR(150) NOT NULL,

    -- Naskah disertasi dan artikel jurnalnya. Boleh kosong lebih dulu:
    -- judul dan promotor sudah ditetapkan jauh sebelum naskahnya siap, dan
    -- memaksa keduanya diunggah bersamaan berarti mahasiswa tidak bisa
    -- mendaftar Seminar Proposal sampai disertasinya hampir selesai.
    dissertation_file_path VARCHAR(500),
    article_file_path      VARCHAR(500),

    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_dissertation_title CHECK (length(trim(title)) >= 10),
    CONSTRAINT ck_dissertation_promotor CHECK (
        length(trim(promotor)) >= 3 AND length(trim(copromotor)) >= 3
    )
);

COMMENT ON TABLE dissertation_details IS
    'Satu baris per mahasiswa. Keempat tahap ujian mengacu ke baris yang sama.';
