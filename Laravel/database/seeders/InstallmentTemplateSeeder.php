<?php

namespace Database\Seeders;

use App\Models\InstallmentTemplate;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class InstallmentTemplateSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        // Clear existing templates to avoid duplicates/conflicts with old schemes
        DB::statement('SET FOREIGN_KEY_CHECKS=0;');
        InstallmentTemplate::truncate();
        DB::table('installment_template_items')->truncate();
        DB::statement('SET FOREIGN_KEY_CHECKS=1;');

        $this->seedReguler();
        $this->seedRPL();
    }

    private function seedReguler()
    {
        // 0. Reguler 1x Angsuran (Bayar Penuh) - Gasal
        $this->createTemplate('REGULER', 'GASAL', 1, 'Reguler 1x Angsuran (Bayar Penuh - Gasal)', [0]);

        // 1. Reguler 8x Angsuran (Gasal)
        // Sep, Jan, Mar, Jul, Okt, Jan, Mar, Jul
        $this->createTemplate('REGULER', 'GASAL', 8, 'Reguler 8x Angsuran (Gasal)', [
            0,  // September
            4,  // Januari
            6,  // Maret
            10, // Juli
            13, // Oktober
            16, // Januari
            18, // Maret
            22  // Juli
        ]);

        // 2. Reguler 20x Angsuran (Gasal)
        // September urut kebawah (consecutive 20 months)
        $offsets20 = range(0, 19);
        $this->createTemplate('REGULER', 'GASAL', 20, 'Reguler 20x Angsuran (Gasal)', $offsets20);

        // --- GENAP ---

        // 0. Reguler 1x Angsuran (Bayar Penuh) - Genap
        $this->createTemplate('REGULER', 'GENAP', 1, 'Reguler 1x Angsuran (Bayar Penuh - Genap)', [0]);

        // 3. Reguler 8x Angsuran (Genap)
        // Feb, Jul, Sep, Nov, Jan, Mar, Jul, Nov
        // Offsets from Feb (Month 0):
        // Feb (0), Jul (5), Sep (7), Nov (9), Jan (11), Mar (13), Jul (17), Nov (21)
        $this->createTemplate('REGULER', 'GENAP', 8, 'Reguler 8x Angsuran (Genap)', [
            0,  // Feb
            5,  // Jul
            7,  // Sep
            9,  // Nov
            11, // Jan
            13, // Mar
            17, // Jul
            21  // Nov
        ]);
        
        // 4. Reguler 20x Angsuran (Genap) - Added per user request for more options
        // Assuming consecutive months starting from Feb
        $this->createTemplate('REGULER', 'GENAP', 20, 'Reguler 20x Angsuran (Genap)', range(0, 19));
    }

    private function seedRPL()
    {
        // RPL Gasal (Start September)
        
        // 0. RPL 1x Angsuran (Bayar Penuh) - Gasal
        $this->createTemplate('RPL', 'GASAL', 1, 'RPL 1x Angsuran (Bayar Penuh - Gasal)', [0]);

        // 4x: Sept, Dec, Mar, Jun
        $this->createTemplate('RPL', 'GASAL', 4, 'RPL 4x Angsuran (Gasal)', [
            0, // Sept
            3, // Dec
            6, // Mar
            9  // Jun
        ]);

        // 6x: Sept, Nov, Jan, Mar, Mei, Jun
        $this->createTemplate('RPL', 'GASAL', 6, 'RPL 6x Angsuran (Gasal)', [
            0, // Sept
            2, // Nov
            4, // Jan
            6, // Mar
            8, // Mei
            9  // Jun
        ]);

        // 10x: Sept to Jun (consecutive)
        $this->createTemplate('RPL', 'GASAL', 10, 'RPL 10x Angsuran (Gasal)', range(0, 9));


        // RPL Genap (Start February)

        // 0. RPL 1x Angsuran (Bayar Penuh) - Genap
        $this->createTemplate('RPL', 'GENAP', 1, 'RPL 1x Angsuran (Bayar Penuh - Genap)', [0]);

        // 4x: Feb, Mei, Aug, Nov
        $this->createTemplate('RPL', 'GENAP', 4, 'RPL 4x Angsuran (Genap)', [
            0, // Feb
            3, // Mei
            6, // Aug
            9  // Nov
        ]);

        // 6x: Feb, Apr, Jun, Aug, Oct, Dec
        $this->createTemplate('RPL', 'GENAP', 6, 'RPL 6x Angsuran (Genap)', [
            0,  // Feb
            2,  // Apr
            4,  // Jun
            6,  // Aug
            8,  // Oct
            10  // Dec
        ]);

        // 10x: Feb to Nov (consecutive)
        $this->createTemplate('RPL', 'GENAP', 10, 'RPL 10x Angsuran (Genap)', range(0, 9));
    }

    private function createTemplate($program, $term, $count, $name, $offsets)
    {
        $template = InstallmentTemplate::create([
            'program_type' => $program,
            'start_term' => $term,
            'installments_count' => $count,
            'name' => $name,
            'active' => true,
        ]);

        foreach ($offsets as $index => $offset) {
            $template->items()->create([
                'installment_no' => $index + 1,
                'month_offset' => $offset,
                'day_of_month' => 20, // Default to 20th of the month
            ]);
        }
    }
}
