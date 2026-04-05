<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\TuitionRate;
use App\Models\InstallmentTemplate;
use App\Models\InstallmentTemplateItem;

class MunaqosahSeeder extends Seeder
{
    public function run()
    {
        $year = '2025/2026'; // Assuming current academic year context

        // 1. Create Tuition Rates for Munaqosah
        $programs = ['REGULER', 'RPL'];
        foreach ($programs as $program) {
            TuitionRate::updateOrCreate([
                'program_type' => $program,
                'academic_year' => $year,
                'category' => 'MUNAOSAH'
            ], [
                'amount' => 2500000, // 2.5jt
                'description' => "Biaya Munaqosah $program $year",
                'active' => true
            ]);
        }

        // 2. Create Installment Templates (1x Payment)
        $terms = ['GASAL', 'GENAP'];
        foreach ($programs as $program) {
            foreach ($terms as $term) {
                // Check if exists to avoid duplicates on re-run
                $exists = InstallmentTemplate::where('program_type', $program)
                    ->where('start_term', $term)
                    ->where('name', 'like', '%Munaqosah%')
                    ->exists();
                
                if (!$exists) {
                    $template = InstallmentTemplate::create([
                        'program_type' => $program,
                        'start_term' => $term,
                        'installments_count' => 1,
                        'name' => "Munaqosah 1x Bayar ($program - $term)",
                        'active' => true,
                    ]);

                    InstallmentTemplateItem::create([
                        'installment_template_id' => $template->id,
                        'installment_no' => 1,
                        'month_offset' => 0, // Due same month
                        'day_of_month' => 10, // Due on 10th
                    ]);
                }
            }
        }
    }
}
