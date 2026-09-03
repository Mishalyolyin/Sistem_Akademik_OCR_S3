<?php

namespace App\Exports;

use App\Models\Student;
use Maatwebsite\Excel\Concerns\WithMultipleSheets;

class StudentLedgerExport implements WithMultipleSheets
{
    protected $programType;

    public function __construct($programType = null)
    {
        $this->programType = $programType;
    }

    public function sheets(): array
    {
        $sheets = [];
        
        $query = Student::query();
        if ($this->programType) {
            $query->where('program_type', $this->programType);
        }
        
        // Get distinct classes
        $classes = $query->whereNotNull('class')
                         ->distinct()
                         ->pluck('class')
                         ->sort()
                         ->values();
                         
        // Check if there are students without class
        $noClassQuery = Student::query();
        if ($this->programType) {
            $noClassQuery->where('program_type', $this->programType);
        }
        $hasNoClass = $noClassQuery->whereNull('class')->exists();
        
        foreach ($classes as $class) {
            $sheets[] = new StudentLedgerSheetExport($this->programType, $class);
        }
        
        if ($hasNoClass) {
             $sheets[] = new StudentLedgerSheetExport($this->programType, null);
        }
        
        // If no students found, create a default sheet
        if (empty($sheets)) {
            $sheets[] = new StudentLedgerSheetExport($this->programType, null);
        }

        return $sheets;
    }
}

