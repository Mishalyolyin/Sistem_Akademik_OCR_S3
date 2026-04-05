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
                'GASAL',                // start_term (GASAL/GENAP)
                '081234567890',         // phone
            ],
            [
                '301230002',
                'Mahasiswa RPL Contoh',
                'B-2',
                'RPL',
                'GENAP',
                '081987654321',
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
            'start_term',
            'phone',
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
