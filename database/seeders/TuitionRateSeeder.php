<?php

namespace Database\Seeders;

use App\Models\TuitionRate;
use Illuminate\Database\Seeder;

class TuitionRateSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        // 2025/2026 Rates (GASAL)
        TuitionRate::create([
            'program_type' => 'REGULER',
            'academic_year' => '2025/2026',
            'start_term' => 'GASAL',
            'amount' => 5000000,
            'description' => 'Biaya Kuliah Reguler 2025/2026 (Gasal)',
            'active' => true,
        ]);

        TuitionRate::create([
            'program_type' => 'RPL',
            'academic_year' => '2025/2026',
            'start_term' => 'GASAL',
            'amount' => 7500000,
            'description' => 'Biaya Kuliah RPL 2025/2026 (Gasal)',
            'active' => true,
        ]);

        // 2025/2026 Rates (GENAP)
        TuitionRate::create([
            'program_type' => 'REGULER',
            'academic_year' => '2025/2026',
            'start_term' => 'GENAP',
            'amount' => 5500000,
            'description' => 'Biaya Kuliah Reguler 2025/2026 (Genap)',
            'active' => true,
        ]);

        TuitionRate::create([
            'program_type' => 'RPL',
            'academic_year' => '2025/2026',
            'start_term' => 'GENAP',
            'amount' => 8000000,
            'description' => 'Biaya Kuliah RPL 2025/2026 (Genap)',
            'active' => true,
        ]);

        // Kelas Kerjasama (RPL only) — nominal negosiasi per partner, belum ditentukan.
        // Sengaja dibuat non-aktif sampai admin isi nominal & aktifkan lewat "Atur Biaya Kuliah",
        // supaya tidak ada tagihan salah nominal ter-generate sebelum tarif riil di-set.
        TuitionRate::create([
            'program_type' => 'RPL',
            'academic_year' => '2025/2026',
            'category' => 'KERJASAMA',
            'start_term' => 'GASAL',
            'amount' => 0,
            'description' => 'Biaya Kelas Kerjasama RPL 2025/2026 (Gasal) — belum diisi',
            'active' => false,
        ]);

        TuitionRate::create([
            'program_type' => 'RPL',
            'academic_year' => '2025/2026',
            'category' => 'KERJASAMA',
            'start_term' => 'GENAP',
            'amount' => 0,
            'description' => 'Biaya Kelas Kerjasama RPL 2025/2026 (Genap) — belum diisi',
            'active' => false,
        ]);
    }
}
