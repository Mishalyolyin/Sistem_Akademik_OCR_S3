<?php

namespace App\Exports;

use Maatwebsite\Excel\Concerns\FromArray;
use Maatwebsite\Excel\Concerns\WithHeadings;
use Maatwebsite\Excel\Concerns\WithStyles;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;
use Maatwebsite\Excel\Concerns\ShouldAutoSize;

class StudentTemplateExport implements FromArray, WithHeadings, WithStyles, ShouldAutoSize
{
    public function array(): array
    {
        return [
            [
                '301230001',           // nim
                'Contoh Mahasiswa',     // name
                'A-1',                  // class
                'REGULER',              // program_type (REGULER/RPL)
                'TRUE',                 // is_alumni (TRUE/FALSE)
                'GASAL',                // start_term (GASAL/GENAP)
                '2025/2026',            // academic_year
                '081234567890',         // phone
                'FALSE',                // kelas_kerjasama (TRUE/FALSE) — hanya berlaku untuk program_type RPL
            ],
            [
                '301230002',
                'Mahasiswa RPL Contoh',
                'B-2',
                'RPL',
                'FALSE',
                'GENAP',
                '2025/2026',
                '081987654321',
                'TRUE',
            ],
        ];
    }

    public function headings(): array
    {
        return [
            'nim',
            'name',
            'class',
            'program_type',
            'is_alumni',
            'start_term',
            'academic_year',
            'phone',
            'kelas_kerjasama',
        ];
    }

    public function styles(Worksheet $sheet)
    {
        return [
            // Style the first row as bold text
            1    => ['font' => ['bold' => true, 'color' => ['rgb' => 'FFFFFF']], 'fill' => ['fillType' => 'solid', 'startColor' => ['rgb' => '022c22']]],
        ];
    }
}
