<?php

namespace App\Exports;

use App\Models\Student;
use Carbon\Carbon;
use Maatwebsite\Excel\Concerns\FromCollection;
use Maatwebsite\Excel\Concerns\WithHeadings;
use Maatwebsite\Excel\Concerns\WithMapping;
use Maatwebsite\Excel\Concerns\WithStyles;
use Maatwebsite\Excel\Concerns\ShouldAutoSize;
use Maatwebsite\Excel\Concerns\WithEvents;
use Maatwebsite\Excel\Concerns\WithTitle;
use Maatwebsite\Excel\Events\AfterSheet;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;
use PhpOffice\PhpSpreadsheet\Style\Border;
use PhpOffice\PhpSpreadsheet\Style\Fill;
use PhpOffice\PhpSpreadsheet\Style\Alignment;

class StudentLedgerSheetExport implements FromCollection, WithHeadings, WithMapping, WithStyles, ShouldAutoSize, WithEvents, WithTitle
{
    protected $programType;
    protected $className;
    protected $maxInstallments = 6; // Cover up to 6 installments
    private $rowNumber = 0;

    public function __construct($programType = null, $className = null)
    {
        $this->programType = $programType;
        $this->className = $className;
        Carbon::setLocale('id');
    }

    public function title(): string
    {
        return $this->className ? substr($this->className, 0, 31) : 'Semua Kelas';
    }

    public function collection()
    {
        $query = Student::with(['paymentPlans' => function ($q) {
            $q->where('status', 'ACTIVE')->with(['installments.payments' => function($q) {
                $q->where('status', 'VERIFIED');
            }]);
        }, 'user']);

        if ($this->programType) {
            $query->where('program_type', $this->programType);
        }

        if ($this->className) {
            $query->where('class', $this->className);
        }

        return $query->orderBy('name')->get();
    }

    public function map($student): array
    {
        $this->rowNumber++;
        $plan = $student->paymentPlans->first();
        
        // Format Pendaftaran Date
        $pendaftaran = '';
        if ($plan) {
            $date = $plan->created_at->translatedFormat('d F Y');
            if ($student->program_type === 'RPL') {
                $pendaftaran = "RPL " . $date;
            } else {
                $pendaftaran = $date;
            }
        }

        $password = ''; 

        $row = [
            $this->rowNumber,           // No.
            $student->name,             // Nama
            $student->nim,              // NIM & Username SIM
            $password,                  // Password
            $pendaftaran,               // Pendaftaran
        ];

        // Add Installment Data columns
        for ($i = 1; $i <= $this->maxInstallments; $i++) {
            $date = '';
            $amount = '';
            
            if ($plan) {
                $installment = $plan->installments->where('installment_no', $i)->first();
                if ($installment) {
                    $paidAmount = $installment->payments->sum('amount');
                    $lastPayment = $installment->payments->sortByDesc('paid_at')->first();
                    
                    if ($paidAmount > 0) {
                        $amount = $paidAmount;
                        $date = $lastPayment ? $lastPayment->created_at->translatedFormat('d F Y') : '-';
                    }
                }
            }
            
            $row[] = $date;   // Tgl Termin N
            $row[] = $amount; // Jumlah Termin N
        }

        return $row;
    }

    public function headings(): array
    {
        // Row 1
        $row1 = [
            'No.',
            'Nama',
            'NIM & Username SIM',
            'Password',
            'Pembayaran',
        ];
        
        // Row 2
        $row2 = [
            '', // No.
            '', // Nama
            '', // NIM
            '', // Password
            'Pendaftaran',
        ];

        for ($i = 1; $i <= $this->maxInstallments; $i++) {
            $row2[] = "Tgl Termin $i";
            $row2[] = "Jumlah Termin $i";
            
            // Add empty cells to Row 1
            $row1[] = ''; 
            $row1[] = '';
        }
        
        return [
            $row1,
            $row2
        ];
    }

    public function styles(Worksheet $sheet)
    {
        $highestColumn = $sheet->getHighestColumn();
        
        // 1. Merge Header Cells
        // A1:A2 (No), B1:B2 (Nama), C1:C2 (NIM), D1:D2 (Password)
        $sheet->mergeCells('A1:A2');
        $sheet->mergeCells('B1:B2');
        $sheet->mergeCells('C1:C2');
        $sheet->mergeCells('D1:D2');
        
        // Merge "Pembayaran" across all remaining columns (E1 to End)
        $sheet->mergeCells('E1:' . $highestColumn . '1');

        // 2. Style Headers
        $sheet->getStyle('A1:' . $highestColumn . '2')->applyFromArray([
            'font' => ['bold' => true, 'size' => 11],
            'alignment' => [
                'horizontal' => Alignment::HORIZONTAL_CENTER,
                'vertical' => Alignment::VERTICAL_CENTER,
            ],
            'borders' => [
                'allBorders' => ['borderStyle' => Border::BORDER_THIN],
            ],
        ]);

        return [];
    }

    public function registerEvents(): array
    {
        return [
            AfterSheet::class => function(AfterSheet $event) {
                $sheet = $event->sheet;
                $highestRow = $sheet->getHighestRow();
                $highestColumn = $sheet->getHighestColumn();

                // Apply borders to all data cells (starting from row 3)
                if ($highestRow >= 3) {
                    $sheet->getStyle('A3:' . $highestColumn . $highestRow)->applyFromArray([
                        'borders' => [
                            'allBorders' => ['borderStyle' => Border::BORDER_THIN],
                        ],
                        'alignment' => [
                            'vertical' => Alignment::VERTICAL_CENTER,
                        ],
                    ]);
                    
                    // Left align Name (B)
                    $sheet->getStyle('B3:B' . $highestRow)->getAlignment()->setHorizontal(Alignment::HORIZONTAL_LEFT);
                    
                    // Center align No (A), NIM (C), Pass (D)
                    $sheet->getStyle('A3:A' . $highestRow)->getAlignment()->setHorizontal(Alignment::HORIZONTAL_CENTER);
                    $sheet->getStyle('C3:D' . $highestRow)->getAlignment()->setHorizontal(Alignment::HORIZONTAL_CENTER);
                }

                // Format Currency Columns
                // Pendaftaran is E (5).
                // Tgl 1 is F (6), Amt 1 is G (7).
                // Tgl 2 is H (8), Amt 2 is I (9).
                
                // Helper to get column letter from index
                for ($i = 1; $i <= $this->maxInstallments; $i++) {
                    // Index of Amount column:
                    // 1=A, 2=B, 3=C, 4=D, 5=E
                    // Installment 1: Tgl=6(F), Amt=7(G)
                    // Installment 2: Tgl=8(H), Amt=9(I)
                    // Formula: 5 + 2i.
                    
                    $colIndex = 5 + ($i * 2); 
                    $colLetter = \PhpOffice\PhpSpreadsheet\Cell\Coordinate::stringFromColumnIndex($colIndex);
                    
                    $sheet->getStyle($colLetter . '3:' . $colLetter . $highestRow)
                          ->getNumberFormat()
                          ->setFormatCode('"Rp " #,##0.00');
                }
            },
        ];
    }
}
