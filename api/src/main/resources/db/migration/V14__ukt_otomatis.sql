-- Tagihan UKT dibuat penjadwal, bukan diketik admin per mahasiswa.
--
-- Sebelum ini, satu-satunya cara membuat tagihan UKT adalah membuka halaman
-- tiap mahasiswa lalu mengetik tahun akademiknya. Untuk enam semester dan
-- tiga puluh mahasiswa itu seratus delapan puluh kali — dan tahun akademik
-- yang diketik ulang sebanyak itu cepat atau lambat akan salah, tanpa satu pun
-- galat yang menegur, karena tahun apa pun tetap tersimpan dengan sah.
--
-- Yang menentukan tahun akademik sekarang adalah data mahasiswanya sendiri
-- (`start_academic_year` dan `start_term`, keduanya diisi admin lewat berkas
-- import), bukan tanggal hari ini. Angkatan Gasal dan angkatan Genap karena itu
-- berjalan di jalurnya masing-masing tanpa saling mengganggu.

INSERT INTO system_settings (key, value, description) VALUES
    ('ukt_auto_enabled', 'true',
     'Buat tagihan UKT otomatis tiap awal semester untuk mahasiswa aktif'),
    ('ukt_auto_last_run', '',
     'Kapan putaran pembuatan tagihan UKT terakhir dijalankan, beserta hasilnya');
