<?php

namespace App\Exports;

use App\Models\Student;
use Maatwebsite\Excel\Concerns\WithMultipleSheets;

class BulkPaymentHistoryExport implements WithMultipleSheets
{
    protected $programType;

    public function __construct($programType)
    {
        $this->programType = $programType;
    }

    public function sheets(): array
    {
        $sheets = [];

        // Get all unique classes for this program type
        $classes = Student::where('program_type', $this->programType)
            ->whereNotNull('class')
            ->distinct()
            ->orderBy('class')
            ->pluck('class');

        // Add sheet for each class
        foreach ($classes as $className) {
            $sheets[] = new PerClassSheetExport($this->programType, $className);
        }

        // Check if there are students without class
        $hasNoClass = Student::where('program_type', $this->programType)
            ->whereNull('class')
            ->exists();

        if ($hasNoClass) {
            $sheets[] = new PerClassSheetExport($this->programType, null); // null for 'Tanpa Kelas'
        }

        // Fallback if no students at all, return empty sheet to avoid error
        if (empty($sheets)) {
            $sheets[] = new PerClassSheetExport($this->programType, null);
        }

        return $sheets;
    }
}
