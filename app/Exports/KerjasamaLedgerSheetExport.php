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
use PhpOffice\PhpSpreadsheet\Style\Alignment;

/**
 * Ledger export for Kelas Kerjasama (RPL) students — same layout as
 * StudentLedgerSheetExport, but scoped to the KERJASAMA category (always
 * exactly 4 installments) and kept separate so its own class groupings
 * (e.g. "Kerjasama A/B/C") don't mix into the regular RPL semester report.
 */
class KerjasamaLedgerSheetExport implements FromCollection, WithHeadings, WithMapping, WithStyles, ShouldAutoSize, WithEvents, WithTitle
{
    protected $className;
    protected $maxInstallments = 4;
    private $rowNumber = 0;

    public function __construct($className = null)
    {
        $this->className = $className;
        Carbon::setLocale('id');
    }

    public function title(): string
    {
        return $this->className ? substr($this->className, 0, 31) : 'Kelas Kerjasama';
    }

    public function collection()
    {
        $query = Student::where('program_type', 'RPL')
            ->whereHas('paymentPlans', function ($q) {
                $q->where('category', 'KERJASAMA');
            })
            ->with(['paymentPlans' => function ($q) {
                $q->where('category', 'KERJASAMA')->with(['installments.payments' => function ($q) {
                    $q->whereIn('status', ['VERIFIED', 'AUTO_VERIFIED']);
                }]);
            }, 'user']);

        if ($this->className) {
            $query->where('class', $this->className);
        }

        return $query->orderBy('name')->get();
    }

    public function map($student): array
    {
        $this->rowNumber++;
        $plan = $student->paymentPlans->first();

        $pendaftaran = '';
        if ($plan) {
            $pendaftaran = 'Kerjasama ' . $plan->created_at->translatedFormat('d F Y');
        }

        $row = [
            $this->rowNumber,
            $student->name,
            $student->nim,
            '',
            $pendaftaran,
        ];

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

            $row[] = $date;
            $row[] = $amount;
        }

        return $row;
    }

    public function headings(): array
    {
        $row1 = ['No.', 'Nama', 'NIM & Username SIM', 'Password', 'Pembayaran'];
        $row2 = ['', '', '', '', 'Pendaftaran'];

        for ($i = 1; $i <= $this->maxInstallments; $i++) {
            $row2[] = "Tgl Termin $i";
            $row2[] = "Jumlah Termin $i";
            $row1[] = '';
            $row1[] = '';
        }

        return [$row1, $row2];
    }

    public function styles(Worksheet $sheet)
    {
        $highestColumn = $sheet->getHighestColumn();

        $sheet->mergeCells('A1:A2');
        $sheet->mergeCells('B1:B2');
        $sheet->mergeCells('C1:C2');
        $sheet->mergeCells('D1:D2');
        $sheet->mergeCells('E1:' . $highestColumn . '1');

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
            AfterSheet::class => function (AfterSheet $event) {
                $sheet = $event->sheet;
                $highestRow = $sheet->getHighestRow();
                $highestColumn = $sheet->getHighestColumn();

                if ($highestRow >= 3) {
                    $sheet->getStyle('A3:' . $highestColumn . $highestRow)->applyFromArray([
                        'borders' => [
                            'allBorders' => ['borderStyle' => Border::BORDER_THIN],
                        ],
                        'alignment' => [
                            'vertical' => Alignment::VERTICAL_CENTER,
                        ],
                    ]);

                    $sheet->getStyle('B3:B' . $highestRow)->getAlignment()->setHorizontal(Alignment::HORIZONTAL_LEFT);
                    $sheet->getStyle('A3:A' . $highestRow)->getAlignment()->setHorizontal(Alignment::HORIZONTAL_CENTER);
                    $sheet->getStyle('C3:D' . $highestRow)->getAlignment()->setHorizontal(Alignment::HORIZONTAL_CENTER);
                }

                for ($i = 1; $i <= $this->maxInstallments; $i++) {
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
