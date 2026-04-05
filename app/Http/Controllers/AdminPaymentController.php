<?php

namespace App\Http\Controllers;

use App\Models\Payment;
use App\Services\PaymentVerificationService;
use Illuminate\Http\Request;
use Maatwebsite\Excel\Facades\Excel;
use App\Exports\StudentPaymentHistoryExport;

class AdminPaymentController extends Controller
{
    protected $verificationService;

    public function __construct(PaymentVerificationService $verificationService)
    {
        $this->verificationService = $verificationService;
    }

    public function index()
    {
        $payments = Payment::where('status', Payment::STATUS_NEEDS_REVIEW)
            ->with(['paymentPlan.student'])
            ->orderBy('created_at', 'asc')
            ->get();

        return response()->json($payments);
    }

    public function indexView()
    {
        return redirect()->route('admin.payments.reguler');
    }

    public function reguler()
    {
        return $this->getPaymentsView('Reguler', 'SEMESTER');
    }

    public function rpl()
    {
        return $this->getPaymentsView('RPL', 'SEMESTER');
    }

    public function munaqosahReguler()
    {
        return $this->getPaymentsView('Reguler', 'MUNAOSAH');
    }

    public function munaqosahRpl()
    {
        return $this->getPaymentsView('RPL', 'MUNAOSAH');
    }

    private function getPaymentsView($programType, $category = 'SEMESTER')
    {
        $viewMode = request('view', 'group'); // Default to group view
        $search = request('search');
        $academicYear = request('academic_year'); // Format: "2025/2026-GASAL"

        // Get available academic years for dropdown
        $availableAcademicYears = \App\Models\PaymentPlan::select('academic_year', 'term')
            ->where('category', $category)
            ->distinct()
            ->orderBy('academic_year', 'desc')
            ->orderBy('term', 'desc')
            ->get()
            ->map(function ($plan) {
                return [
                    'value' => $plan->academic_year . '-' . $plan->term,
                    'label' => $plan->academic_year . ' ' . ucfirst(strtolower($plan->term))
                ];
            });

        if ($viewMode === 'group') {
            $query = \App\Models\Student::where('program_type', $programType)
                ->with([
                    'paymentPlans' => function($q) use ($academicYear, $category) {
                        $q->latest();
                        if ($academicYear) {
                            [$year, $term] = explode('-', $academicYear);
                            $q->where('academic_year', $year)
                              ->where('term', $term);
                        }
                        $q->where('category', $category);
                    },
                    'paymentPlans.installments' => function($q) {
                        $q->orderBy('due_date', 'asc');
                    },
                    'paymentPlans.installments.payments' => function($q) {
                        $q->latest(); // Get latest payment attempt first
                    }
                ]);
            
            // Filter students who have plans in the selected category
            $query->whereHas('paymentPlans', function($q) use ($category) {
                $q->where('category', $category);
            });

            // Filter students who have plans in the selected academic year if filter is applied
            if ($academicYear) {
                [$year, $term] = explode('-', $academicYear);
                $query->whereHas('paymentPlans', function($q) use ($year, $term, $category) {
                    $q->where('academic_year', $year)
                      ->where('term', $term)
                      ->where('category', $category);
                });
            }

            if ($search) {
                $query->where(function($q) use ($search) {
                    $q->where('nim', 'like', "%{$search}%")
                      ->orWhere('name', 'like', "%{$search}%")
                      ->orWhere('class', 'like', "%{$search}%");
                });
            }

            $students = $query->orderBy('name', 'asc')->paginate(10);

            $pageTitle = "Verifikasi Tagihan " . $programType . " (" . ucfirst(strtolower($category)) . ")";
            return view('admin.payments.index', compact('students', 'pageTitle', 'programType', 'viewMode', 'search', 'availableAcademicYears', 'academicYear'));
        }

        // Flat view (original logic)
        $query = Payment::whereIn('status', [Payment::STATUS_PENDING, Payment::STATUS_NEEDS_REVIEW, Payment::STATUS_FAILED])
            ->whereHas('student', function($q) use ($programType, $search) {
                $q->where('program_type', $programType);
                if ($search) {
                    $q->where(function($subQ) use ($search) {
                        $subQ->where('nim', 'like', "%{$search}%")
                             ->orWhere('name', 'like', "%{$search}%")
                             ->orWhere('class', 'like', "%{$search}%");
                    });
                }
            });

        // Filter by category
        $query->whereHas('paymentPlan', function($q) use ($category) {
            $q->where('category', $category);
        });

        // Filter by academic year in flat view
        if ($academicYear) {
            [$year, $term] = explode('-', $academicYear);
            $query->whereHas('paymentPlan', function($q) use ($year, $term, $category) {
                $q->where('academic_year', $year)
                  ->where('term', $term)
                  ->where('category', $category);
            });
        }
            
        $payments = $query->with(['student', 'installment.paymentPlan'])
            ->orderBy('created_at', 'asc')->paginate(10);

        $pageTitle = "Verifikasi Tagihan " . $programType . " (" . ucfirst(strtolower($category)) . ")";

        return view('admin.payments.index', compact('payments', 'pageTitle', 'programType', 'viewMode', 'search', 'availableAcademicYears', 'academicYear'));
    }

    public function verify(Request $request, Payment $payment)
    {
        $request->validate([
            'action' => 'required|in:approve,reject,request_reupload',
            'notes' => 'nullable|string',
        ]);

        try {
            $this->verificationService->verify(
                $payment,
                $request->user(),
                $request->action,
                $request->notes
            );

            if ($request->wantsJson()) {
                return response()->json(['message' => 'Payment processed successfully', 'payment' => $payment->fresh()]);
            }
            return redirect()->back()->with('success', 'Payment processed successfully.');
        } catch (\Exception $e) {
            if ($request->wantsJson()) {
                return response()->json(['message' => $e->getMessage()], 400);
            }
            return redirect()->back()->with('error', $e->getMessage());
        }
    }

    public function resetStudentInstallments(Request $request, \App\Models\Student $student)
    {
        try {
            // Find payments that are NOT verified (Pending, Needs Review, Rejected, Failed)
            // We specifically want to reset "in progress" or "failed" attempts so student can start over.
            $payments = Payment::where('student_id', $student->id)
                ->where('status', '!=', Payment::STATUS_VERIFIED)
                ->get();

            if ($payments->isEmpty()) {
                return redirect()->back()->with('error', 'Tidak ada tagihan yang dapat di-reset (hanya tagihan belum lunas/pending yang di-reset).');
            }

            $count = $payments->count();
            $installmentIds = $payments->pluck('installment_id')->unique();

            // Delete the payments
            Payment::where('student_id', $student->id)
                ->where('status', '!=', Payment::STATUS_VERIFIED)
                ->delete();

            // Reset Installments Status to 'UNPAID' if they don't have any other verified payments
            foreach($installmentIds as $instId) {
                if ($instId) {
                    $hasVerified = Payment::where('installment_id', $instId)
                        ->where('status', Payment::STATUS_VERIFIED)
                        ->exists();
                    
                    if (!$hasVerified) {
                        \App\Models\Installment::where('id', $instId)->update([
                            'status' => 'UNPAID', // Assuming 'UNPAID' is the default
                            'amount_paid' => 0
                        ]);
                    }
                }
            }

            return redirect()->back()->with('success', "Berhasil mereset $count data tagihan mahasiswa.");

        } catch (\Exception $e) {
            return redirect()->back()->with('error', 'Gagal mereset tagihan: ' . $e->getMessage());
        }
    }

    public function exportStudent(\App\Models\Student $student)
    {
        $filename = 'Tagihan_' . $student->nim . '_' . date('YmdHis') . '.xlsx';
        return Excel::download(new StudentPaymentHistoryExport($student), $filename);
    }

    public function exportAll($programType)
    {
        $programType = strtoupper($programType);
        if (!in_array($programType, ['REGULER', 'RPL'])) {
            abort(404);
        }
        
        $filename = 'Laporan_Tagihan_Semua_Mahasiswa_' . $programType . '_' . date('YmdHis') . '.xlsx';
        // Use StudentLedgerExport to ensure consistent template
        return Excel::download(new \App\Exports\StudentLedgerExport($programType), $filename);
    }
}
