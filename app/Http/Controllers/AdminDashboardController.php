<?php

namespace App\Http\Controllers;

use App\Models\Payment;
use App\Models\Student;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

use App\Exports\PaymentReportExport;
use App\Exports\StudentLedgerExport;
use Maatwebsite\Excel\Facades\Excel;

class AdminDashboardController extends Controller
{
    private function getProgramStats($programType)
    {
        // 1. Basic Counts
        $activeStudents = Student::where('program_type', $programType)->count();
        
        $waitingVerification = Payment::whereHas('student', function($q) use ($programType) {
            $q->where('program_type', $programType);
        })->where('status', 'NEEDS_REVIEW')->count();

        // 2. Financials (Complex calculation)
        // Get all students of this program with their Active Plan and Total Paid
        $students = Student::where('program_type', $programType)
            ->with(['paymentPlans' => function($q) {
                $q->where('status', 'ACTIVE');
            }])
            ->get();

        $totalTagihan = 0; // Total Bill (Invoice)
        $totalUang = 0; // Total Collected (Revenue)
        $countLunas = 0;
        $countMenunggu = 0; // Partial
        $countBelum = 0; // Zero

        $studentList = [];

        foreach ($students as $student) {
            $plan = $student->paymentPlans->first();
            $bill = $plan ? $plan->total_amount : 0;
            
            // Calculate total paid (verified only)
            $paid = $student->payments()->where('status', 'VERIFIED')->sum('amount');
            
            $totalTagihan += $bill;
            $totalUang += $paid;

            $status = 'BELUM';
            if ($bill > 0) {
                if ($paid >= $bill) {
                    $status = 'LUNAS';
                    $countLunas++;
                } elseif ($paid > 0) {
                    $status = 'MENUNGGU'; // Partially paid
                    $countMenunggu++;
                } else {
                    $status = 'BELUM'; // No payment
                    $countBelum++;
                }
            } else {
                // No active bill
                 if ($paid > 0) {
                    $status = 'LUNAS'; // Or Overpaid?
                 }
            }

            $studentList[] = [
                'name' => $student->name,
                'nim' => $student->nim,
                'status' => $status, // LUNAS, MENUNGGU, BELUM
                'bill' => $bill,
                'paid' => $paid
            ];
        }

        // New Logic: Eligible Munaqosah (Lunas SPP tapi belum Munaqosah)
        $eligibleMunaqosah = Student::where('program_type', $programType)
            ->whereHas('paymentPlans', function($q) {
                $q->where('category', 'SEMESTER');
            })
            ->whereDoesntHave('paymentPlans', function($q) {
                $q->where('category', 'MUNAOSAH');
            })
            ->whereDoesntHave('paymentPlans', function($q) {
                $q->where('category', 'SEMESTER')
                  ->whereHas('installments', function($sq) {
                      $sq->where('status', '!=', 'PAID');
                  });
            })
            ->count();

        return [
            'active_students' => $activeStudents,
            'total_tagihan' => $totalTagihan,
            'waiting_verification' => $waitingVerification,
            'sudah_lunas' => $countLunas,
            'eligible_munaqosah' => $eligibleMunaqosah,
            'ratio' => [
                'lunas' => $countLunas,
                'menunggu' => $countMenunggu,
                'belum' => $countBelum,
                'total_uang' => $totalUang
            ],
            'students' => $studentList
        ];
    }

    private function getOcrStatsByClass()
    {
        // Breakdown OCR / Payment Status by Class
        // Requirement: breakdown minimal “per kelas” (auto vs review)
        return Payment::join('students', 'payments.student_id', '=', 'students.id')
            ->select('students.class', 'payments.status', DB::raw('count(*) as count'))
            ->whereNotNull('students.class')
            ->groupBy('students.class', 'payments.status')
            ->get()
            ->groupBy('class');
    }

    private function getData()
    {
        // 1. Program Stats
        $rplStats = $this->getProgramStats('RPL');
        $regulerStats = $this->getProgramStats('REGULER');

        // 2. OCR Stats by Class
        $ocrStatsByClass = $this->getOcrStatsByClass();

        // 2. Trend Chart (6 Months)
        $months = [];
        $rplTrend = [];
        $regulerTrend = [];

        for ($i = 5; $i >= 0; $i--) {
            $date = now()->subMonths($i);
            $monthLabel = $date->format('F Y');
            $months[] = $monthLabel;

            $start = $date->copy()->startOfMonth();
            $end = $date->copy()->endOfMonth();

            $rplTrend[] = Payment::whereHas('student', fn($q) => $q->where('program_type', 'RPL'))
                ->where('status', 'VERIFIED')
                ->whereBetween('updated_at', [$start, $end])
                ->sum('amount');

            $regulerTrend[] = Payment::whereHas('student', fn($q) => $q->where('program_type', 'REGULER'))
                ->where('status', 'VERIFIED')
                ->whereBetween('updated_at', [$start, $end])
                ->sum('amount');
        }

        return compact(
            'rplStats',
            'regulerStats',
            'ocrStatsByClass',
            'months',
            'rplTrend',
            'regulerTrend'
        );
    }

    public function index()
    {
        $data = $this->getData();
        return view('admin.dashboard', $data);
    }

    public function stats()
    {
        $data = $this->getData();
        return response()->json($data);
    }

    public function exportExcel(Request $request)
    {
        $programType = $request->input('program_type');
        $status = $request->input('status');
        $format = $request->input('format', 'transaction'); // 'transaction' or 'ledger'
        
        $fileName = 'laporan_pembayaran_' . ($format === 'ledger' ? 'ledger_' : '') . date('Y-m-d_H-i') . '.xlsx';
        
        if ($format === 'ledger') {
            return Excel::download(new StudentLedgerExport($programType), $fileName);
        }

        return Excel::download(new PaymentReportExport($programType, $status), $fileName);
    }

    public function exportReport(Request $request)
    {
        // Simple CSV Export for Semester Report
        $year = $request->input('academic_year');
        $term = $request->input('term');

        $query = Payment::with(['student', 'paymentPlan'])
            ->where('status', 'VERIFIED');

        if ($year && $term) {
            $query->whereHas('paymentPlan', function($q) use ($year, $term) {
                $q->where('academic_year', $year)->where('term', $term);
            });
        }

        $payments = $query->latest()->get();

        $csvFileName = 'report_' . ($year ? Str::slug($year) . '_' : '') . ($term ?? 'all') . '.csv';
        
        $headers = [
            "Content-type"        => "text/csv",
            "Content-Disposition" => "attachment; filename=$csvFileName",
            "Pragma"              => "no-cache",
            "Cache-Control"       => "must-revalidate, post-check=0, pre-check=0",
            "Expires"             => "0"
        ];

        $callback = function() use ($payments) {
            $file = fopen('php://output', 'w');
            fputcsv($file, ['Date', 'NIM', 'Name', 'Class', 'Program', 'Amount', 'Method', 'Semester']);

            foreach ($payments as $payment) {
                fputcsv($file, [
                    $payment->created_at->format('Y-m-d H:i'),
                    $payment->student->nim,
                    $payment->student->name,
                    $payment->student->class ?? '-',
                    $payment->student->program_type,
                    $payment->amount,
                    $payment->payment_method,
                    $payment->paymentPlan ? $payment->paymentPlan->academic_year . ' ' . $payment->paymentPlan->term : 'N/A'
                ]);
            }

            fclose($file);
        };

        return response()->stream($callback, 200, $headers);
    }
}
