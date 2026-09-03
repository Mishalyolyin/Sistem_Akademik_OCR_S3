<?php

namespace App\Exports;

use App\Models\Student;
use Maatwebsite\Excel\Concerns\WithMultipleSheets;

/**
 * Multi-sheet ledger export for Kelas Kerjasama (RPL), one sheet per class
 * (e.g. "Kerjasama A", "Kerjasama B") — kept separate from StudentLedgerExport
 * since only students with a KERJASAMA plan belong here.
 */
class KerjasamaLedgerExport implements WithMultipleSheets
{
    public function sheets(): array
    {
        $sheets = [];

        $baseQuery = fn () => Student::where('program_type', 'RPL')
            ->whereHas('paymentPlans', function ($q) {
                $q->where('category', 'KERJASAMA');
            });

        $classes = $baseQuery()->whereNotNull('class')->distinct()->pluck('class')->sort()->values();
        $hasNoClass = $baseQuery()->whereNull('class')->exists();

        foreach ($classes as $class) {
            $sheets[] = new KerjasamaLedgerSheetExport($class);
        }

        if ($hasNoClass) {
            $sheets[] = new KerjasamaLedgerSheetExport(null);
        }

        if (empty($sheets)) {
            $sheets[] = new KerjasamaLedgerSheetExport(null);
        }

        return $sheets;
    }
}
