<?php

namespace App\Exports;

use App\Models\Student;
use Maatwebsite\Excel\Concerns\FromCollection;
use Maatwebsite\Excel\Concerns\WithHeadings;
use Maatwebsite\Excel\Concerns\WithMapping;
use Maatwebsite\Excel\Concerns\WithStyles;
use Maatwebsite\Excel\Concerns\ShouldAutoSize;
use Maatwebsite\Excel\Concerns\WithTitle;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;

class PerClassSheetExport implements FromCollection, WithHeadings, WithMapping, WithStyles, ShouldAutoSize, WithTitle
{
    protected $programType;
    protected $className;
    protected $maxInstallments = 0;
    protected $rowNumber = 0;

    public function __construct($programType, $className)
    {
        $this->programType = $programType;
        $this->className = $className;
    }

    public function title(): string
    {
        return $this->className ? substr($this->className, 0, 31) : 'Tanpa Kelas';
    }

    public function collection()
    {
        $query = Student::where('program_type', $this->programType)
            ->with(['paymentPlans.installments.payments']);

        if ($this->className) {
            $query->where('class', $this->className);
        } else {
            $query->whereNull('class');
        }

        $students = $query->orderBy('name')->get();

        // Calculate max installments to set headers dynamically
        foreach ($students as $student) {
            $count = 0;
            foreach ($student->paymentPlans as $plan) {
                $count += $plan->installments->count();
            }
            if ($count > $this->maxInstallments) {
                $this->maxInstallments = $count;
            }
        }

        return $students;
    }

    public function map($student): array
    {
        $this->rowNumber++;
        
        $row = [
            $this->rowNumber,
            $student->name,
            $student->nim,
            $student->program_type,
        ];

        // Collect all installments across all plans
        $allInstallments = collect();
        foreach ($student->paymentPlans as $plan) {
            foreach ($plan->installments as $inst) {
                $allInstallments->push((object)[
                    'plan' => $plan,
                    'installment' => $inst,
                    'due_date' => $inst->due_date
                ]);
            }
        }

        // Sort by due date
        $sortedInstallments = $allInstallments->sortBy('due_date');

        foreach ($sortedInstallments as $item) {
            $inst = $item->installment;
            
            // Get verified payment or the latest attempt
            $payment = $inst->payments->where('status', 'VERIFIED')->first();
            if (!$payment) {
                $payment = $inst->payments->sortByDesc('created_at')->first();
            }

            $statusLabel = 'BELUM LUNAS';
            if ($inst->status == 'PAID' || ($payment && $payment->status == 'VERIFIED')) {
                $statusLabel = 'LUNAS';
            } elseif ($payment && $payment->status == 'PENDING') {
                $statusLabel = 'MENUNGGU VERIFIKASI';
            } elseif ($payment && $payment->status == 'NEEDS_REVIEW') {
                $statusLabel = 'PERLU REVIEW';
            } elseif ($payment && $payment->status == 'REJECTED') {
                $statusLabel = 'DITOLAK';
            }

            // Append installment details to row
            $row[] = $item->plan->academic_year . ' ' . $item->plan->term; // Semester
            $row[] = $inst->due_date->format('d M Y'); // Jatuh Tempo
            $row[] = $inst->amount; // Nominal Tagihan
            $row[] = $inst->status; // Status Sistem
            $row[] = $payment && $payment->status == 'VERIFIED' ? $payment->amount : 0; // Nominal Terbayar
            $row[] = $payment && $payment->status == 'VERIFIED' ? $payment->created_at->format('d M Y H:i') : '-'; // Tanggal Bayar
            $row[] = $statusLabel; // Keterangan
        }

        return $row;
    }

    public function headings(): array
    {
        $headers = [
            'No',
            'Nama Mahasiswa',
            'NIM',
            'Program',
        ];

        for ($i = 1; $i <= $this->maxInstallments; $i++) {
            $headers[] = "Semester ($i)";
            $headers[] = "Jatuh Tempo ($i)";
            $headers[] = "Nominal Tagihan ($i)";
            $headers[] = "Status Sistem ($i)";
            $headers[] = "Nominal Terbayar ($i)";
            $headers[] = "Tanggal Bayar ($i)";
            $headers[] = "Keterangan ($i)";
        }

        return $headers;
    }

    public function styles(Worksheet $sheet)
    {
        return [
            1 => ['font' => ['bold' => true]],
        ];
    }
}
