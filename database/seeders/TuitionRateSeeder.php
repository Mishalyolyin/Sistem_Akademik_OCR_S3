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
        // 2025/2026 Rates
        TuitionRate::create([
            'program_type' => 'REGULER',
            'academic_year' => '2025/2026',
            'amount' => 5000000,
            'description' => 'Biaya Kuliah Reguler 2025/2026',
            'active' => true,
        ]);

        TuitionRate::create([
            'program_type' => 'RPL',
            'academic_year' => '2025/2026',
            'amount' => 7500000,
            'description' => 'Biaya Kuliah RPL 2025/2026',
            'active' => true,
        ]);
    }
}
