<?php

namespace App\Exports;

use App\Models\Payment;
use Maatwebsite\Excel\Concerns\FromCollection;
use Maatwebsite\Excel\Concerns\WithHeadings;
use Maatwebsite\Excel\Concerns\WithMapping;
use Maatwebsite\Excel\Concerns\WithStyles;
use Maatwebsite\Excel\Concerns\ShouldAutoSize;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;

class PaymentReportExport implements FromCollection, WithHeadings, WithMapping, WithStyles, ShouldAutoSize
{
    protected $programType;
    protected $status;

    public function __construct($programType = null, $status = null)
    {
        $this->programType = $programType;
        $this->status = $status;
    }

    public function collection()
    {
        $query = Payment::with(['student', 'paymentPlan', 'installment'])->latest();

        if ($this->programType) {
            $query->whereHas('student', function ($q) {
                $q->where('program_type', $this->programType);
            });
        }

        if ($this->status) {
            $query->where('status', $this->status);
        }

        return $query->get();
    }

    public function map($payment): array
    {
        return [
            $payment->id,
            $payment->created_at->format('d/m/Y H:i'),
            $payment->student->nim,
            $payment->student->name,
            $payment->student->program_type,
            'Cicilan ke-' . ($payment->installment ? $payment->installment->installment_order : '-'),
            $payment->amount,
            $payment->payment_method,
            $payment->status,
        ];
    }

    public function headings(): array
    {
        return [
            'ID Transaksi',
            'Tanggal',
            'NIM',
            'Nama Mahasiswa',
            'Kelas',
            'Program Studi',
            'Keterangan',
            'Jumlah (Rp)',
            'Metode Pembayaran',
            'Status',
        ];
    }

    public function styles(Worksheet $sheet)
    {
        return [
            // Style the first row as bold text.
            1    => ['font' => ['bold' => true, 'color' => ['rgb' => 'FFFFFF']], 'fill' => ['fillType' => 'solid', 'startColor' => ['rgb' => '022c22']]], // Primary Color
        ];
    }
}
