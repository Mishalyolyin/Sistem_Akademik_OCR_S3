<?php

namespace App\Exports;

use App\Models\Student;
use Maatwebsite\Excel\Concerns\FromCollection;
use Maatwebsite\Excel\Concerns\WithHeadings;
use Maatwebsite\Excel\Concerns\WithMapping;
use Maatwebsite\Excel\Concerns\WithStyles;
use Maatwebsite\Excel\Concerns\ShouldAutoSize;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;

class StudentPaymentHistoryExport implements FromCollection, WithHeadings, WithMapping, WithStyles, ShouldAutoSize
{
    protected $student;

    public function __construct(Student $student)
    {
        $this->student = $student;
    }

    public function collection()
    {
        $data = collect();
        
        // Load relationships to ensure efficient access if not already loaded
        $this->student->load(['paymentPlans.installments.payments']);

        foreach($this->student->paymentPlans as $plan) {
            foreach($plan->installments as $inst) {
                // Get verified payment or the latest attempt
                $payment = $inst->payments->where('status', 'VERIFIED')->first();
                // If no verified payment, get the latest one for info
                if (!$payment) {
                    $payment = $inst->payments->sortByDesc('created_at')->first();
                }

                $data->push((object)[
                    'plan' => $plan,
                    'installment' => $inst,
                    'payment' => $payment
                ]);
            }
        }
        
        return $data;
    }

    public function map($row): array
    {
        $plan = $row->plan;
        $inst = $row->installment;
        $payment = $row->payment;

        $statusLabel = 'BELUM LUNAS';
        if ($inst->status == 'PAID' || ($payment && $payment->status == 'VERIFIED')) {
            $statusLabel = 'LUNAS';
        } elseif ($payment && $payment->status == 'PENDING') {
            $statusLabel = 'MENUNGGU VERIFIKASI';
        }

        return [
            $plan->academic_year . ' ' . $plan->term,
            $inst->due_date->format('d M Y'),
            $inst->amount,
            $inst->status, // Internal status
            $payment && $payment->status == 'VERIFIED' ? $payment->amount : 0,
            $payment && $payment->status == 'VERIFIED' ? $payment->created_at->format('d M Y H:i') : '-',
            $statusLabel,
        ];
    }

    public function headings(): array
    {
        return [
            ['LAPORAN STATUS PEMBAYARAN MAHASISWA'],
            ['Nama', $this->student->name],
            ['NIM', $this->student->nim],
            ['Kelas', $this->student->class ?? '-'],
            ['Program', $this->student->program_type],
            [''], // Empty row
            [
                'Semester',
                'Jatuh Tempo',
                'Nominal Tagihan',
                'Status Sistem',
                'Nominal Terbayar',
                'Tanggal Bayar',
                'Keterangan'
            ]
        ];
    }

    public function styles(Worksheet $sheet)
    {
        return [
            1 => ['font' => ['bold' => true, 'size' => 14]],
            7 => ['font' => ['bold' => true]], // Header row
        ];
    }
}
