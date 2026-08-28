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

    public function pendaftaranReguler()
    {
        return $this->getPaymentsView('Reguler', 'PENDAFTARAN');
    }

    public function pendaftaranRpl()
    {
        return $this->getPaymentsView('RPL', 'PENDAFTARAN');
    }

    private function getPaymentsView($programType, $category = 'SEMESTER')
    {
        $viewMode = request('view', 'group'); // Default to group view
        $search = request('search');
        $academicYear = request('academic_year'); // Format: "2025/2026-GASAL"
        $status = request('status'); // pending|rejected|verified|null (=semua)
        $paymentTolerance = (float) (\App\Models\SystemSetting::where('key', 'payment_tolerance_amount')->value('value') ?? 0);

        $statusGroups = [
            'pending' => [Payment::STATUS_PENDING, Payment::STATUS_NEEDS_REVIEW],
            'rejected' => [Payment::STATUS_REJECTED, Payment::STATUS_FAILED],
            'verified' => [Payment::STATUS_VERIFIED, Payment::STATUS_AUTO_VERIFIED],
        ];

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

        // Scope a PaymentPlan whereHas closure to the current category/academic year,
        // optionally also requiring at least one installment whose LATEST payment
        // (not just any payment ever) has a status in $statusList. Matching on any
        // historical payment made a student with an old rejected upload that was
        // later re-uploaded and verified show up under "Ditolak" forever, even
        // though every installment currently displays as fully paid.
        $planScope = function ($planQuery, $statusList = null) use ($category, $academicYear) {
            $planQuery->where('category', $category);
            if ($academicYear) {
                [$year, $term] = explode('-', $academicYear);
                $planQuery->where('academic_year', $year)->where('term', $term);
            }
            if ($statusList) {
                $planQuery->whereHas('installments', function ($q) use ($statusList) {
                    $q->whereExists(function ($sub) use ($statusList) {
                        $sub->select(\Illuminate\Support\Facades\DB::raw(1))
                            ->from('payments as p')
                            ->whereColumn('p.installment_id', 'installments.id')
                            ->whereIn('p.status', $statusList)
                            ->whereRaw('p.id = (SELECT MAX(p2.id) FROM payments p2 WHERE p2.installment_id = p.installment_id)');
                    });
                });
            }
        };

        // Count per status button, independent of which status filter is currently active,
        // so the buttons always show accurate totals for the current search/category/academic year.
        $statusCounts = [];
        foreach ($statusGroups as $key => $statusList) {
            $countQuery = \App\Models\Student::where('program_type', $programType)
                ->whereHas('paymentPlans', function ($q) use ($statusList, $planScope) {
                    $planScope($q, $statusList);
                });
            if ($search) {
                $countQuery->where(function ($q) use ($search) {
                    $q->where('nim', 'like', "%{$search}%")
                      ->orWhere('name', 'like', "%{$search}%")
                      ->orWhere('class', 'like', "%{$search}%");
                });
            }
            $statusCounts[$key] = $countQuery->count();
        }

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
                    },
                    'paymentPlans.installments.adjustments' => function($q) {
                        $q->latest();
                    },
                    'paymentPlans.installments.adjustments.admin',
                    'paymentPlans.munaqosahDetail',
                ]);

            // Filter students who have plans in the selected category (+ academic year, + status if chosen)
            $query->whereHas('paymentPlans', function ($q) use ($status, $statusGroups, $planScope) {
                $planScope($q, $status && isset($statusGroups[$status]) ? $statusGroups[$status] : null);
            });

            if ($search) {
                $query->where(function($q) use ($search) {
                    $q->where('nim', 'like', "%{$search}%")
                      ->orWhere('name', 'like', "%{$search}%")
                      ->orWhere('class', 'like', "%{$search}%");
                });
            }

            $students = $query->orderBy('name', 'asc')->paginate(10)->withQueryString();

            $pageTitle = "Verifikasi Tagihan " . $programType . " (" . ucfirst(strtolower($category)) . ")";
            return view('admin.payments.index', compact('students', 'pageTitle', 'programType', 'viewMode', 'search', 'availableAcademicYears', 'academicYear', 'status', 'statusCounts', 'category', 'paymentTolerance'));
        }

        // Flat view (original logic)
        $query = Payment::query();

        if ($status && isset($statusGroups[$status])) {
            $query->whereIn('status', $statusGroups[$status]);
        }

        $query->whereHas('student', function($q) use ($programType, $search) {
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

        $payments = $query->with(['student', 'installment.paymentPlan.munaqosahDetail'])
            ->orderBy('created_at', 'asc')->paginate(10)->withQueryString();

        $pageTitle = "Verifikasi Tagihan " . $programType . " (" . ucfirst(strtolower($category)) . ")";

        return view('admin.payments.index', compact('payments', 'pageTitle', 'programType', 'viewMode', 'search', 'availableAcademicYears', 'academicYear', 'status', 'statusCounts', 'category', 'paymentTolerance'));
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
                $message = $request->action === 'approve' ? 'Pembayaran berhasil disetujui' : 'Pembayaran berhasil ditolak';
                return response()->json(['message' => $message, 'payment' => $payment->fresh()]);
            }
            
            $successMessage = $request->action === 'approve' 
                ? 'Pembayaran berhasil disetujui (diverifikasi).' 
                : 'Pembayaran berhasil ditolak.';
                
            return redirect()->back()->with('success', $successMessage);
        } catch (\Exception $e) {
            if ($request->wantsJson()) {
                return response()->json(['message' => $e->getMessage()], 400);
            }
            return redirect()->back()->with('error', $e->getMessage());
        }
    }

    public function resetStatus(Request $request, Payment $payment)
    {
        try {
            if (in_array($payment->status, [Payment::STATUS_REJECTED, Payment::STATUS_VERIFIED, 'AUTO_REJECTED', 'AUTO_VERIFIED'])) {
                $oldStatus = $payment->status;
                
                \Illuminate\Support\Facades\DB::transaction(function () use ($payment, $oldStatus) {
                    $planId = $payment->payment_plan_id;
                    $student = $payment->student ?? $payment->paymentPlan->student;

                    if ($planId && $student) {
                        // 1. Hitung total uang yang sudah dialokasikan dari plan ini
                        $allocatedPayments = Payment::where('payment_plan_id', $planId)
                            ->whereNotNull('allocated_at')
                            ->get();
                        
                        $sumAllocated = $allocatedPayments->sum('amount');
                        
                        // 2. Hitung total tagihan dari plan ini
                        $totalBills = \App\Models\Installment::where('payment_plan_id', $planId)->sum('amount');
                        
                        // 3. Hitung kelebihan uang yang sebelumnya masuk ke wallet
                        $walletOverpayment = max(0, $sumAllocated - $totalBills);
                        
                        // 4. Tarik kembali saldo wallet mahasiswa jika ada kelebihan
                        if ($walletOverpayment > 0) {
                            $student->wallet_balance = max(0, $student->wallet_balance - $walletOverpayment);
                            $student->save();
                        }

                        // 5. Nol-kan semua angsuran di plan ini
                        \App\Models\Installment::where('payment_plan_id', $planId)->update([
                            'amount_paid' => 0,
                            'status' => 'UNPAID'
                        ]);

                        // 6. Hapus status alokasi dari semua pembayaran valid di plan ini
                        Payment::where('payment_plan_id', $planId)
                            ->whereNotNull('allocated_at')
                            ->update(['allocated_at' => null]);
                    }

                    // 7. Reset status pembayaran target
                    $payment->status = Payment::STATUS_NEEDS_REVIEW;
                    $payment->rejection_reason = null;
                    $payment->verified_at = null;
                    $payment->verified_by_user_id = null;
                    $payment->allocated_at = null;
                    $payment->save();

                    // 8. Hitung Ulang (Re-allocate) pembayaran valid yang tersisa
                    if ($planId) {
                        $remainingValidPayments = Payment::where('payment_plan_id', $planId)
                            ->whereIn('status', [Payment::STATUS_VERIFIED, 'AUTO_VERIFIED'])
                            ->orderBy('created_at', 'asc')
                            ->get();

                        $allocationService = app(\App\Services\PaymentAllocationService::class);
                        foreach ($remainingValidPayments as $vp) {
                            $allocationService->allocate($vp);
                        }
                    }

                    \App\Models\VerificationLog::create([
                        'payment_id' => $payment->id,
                        'admin_id' => \Illuminate\Support\Facades\Auth::id(),
                        'action' => 'RESET',
                        'notes' => 'Admin mereset status pembayaran (' . $oldStatus . ') menjadi Menunggu Verifikasi. Semua alokasi pada plan dihitung ulang.',
                    ]);
                });

                $successMessage = 'Status pembayaran berhasil di-reset. Alokasi tagihan dan saldo telah disesuaikan secara otomatis.';
                if ($request->wantsJson()) {
                    return response()->json(['message' => $successMessage, 'payment' => $payment->fresh()]);
                }
                return redirect()->back()->with('success', $successMessage);
            }

            $errorMessage = 'Hanya pembayaran dengan status Lunas atau Ditolak yang dapat di-reset.';
            if ($request->wantsJson()) {
                return response()->json(['message' => $errorMessage], 400);
            }
            return redirect()->back()->with('error', $errorMessage);
        } catch (\Exception $e) {
            \Illuminate\Support\Facades\Log::error('Error in resetStatus: ' . $e->getMessage());
            $errorMessage = 'Gagal mereset pembayaran: ' . $e->getMessage();
            if ($request->wantsJson()) {
                return response()->json(['message' => $errorMessage], 400);
            }
            return redirect()->back()->with('error', $errorMessage);
        }
    }

    public function resetStudentInstallments(Request $request, \App\Models\Student $student)
    {
        try {
            $deletedPaymentsCount = 0;
            $deletedPlansCount = 0;

            // 1. Temukan pembayaran yang belum diverifikasi (Pending, Needs Review, Rejected, Failed)
            $unverifiedPayments = Payment::where('student_id', $student->id)
                ->whereNotIn('status', [Payment::STATUS_VERIFIED, Payment::STATUS_AUTO_VERIFIED])
                ->get();

            if ($unverifiedPayments->isNotEmpty()) {
                $deletedPaymentsCount = $unverifiedPayments->count();
                $installmentIds = $unverifiedPayments->pluck('installment_id')->filter()->unique();

                // Hapus data pembayaran tersebut
                Payment::whereIn('id', $unverifiedPayments->pluck('id'))->delete();

                // Reset status Installment menjadi 'UNPAID' jika tidak ada lagi pembayaran yang verified
                foreach($installmentIds as $instId) {
                    $hasVerified = Payment::where('installment_id', $instId)
                        ->whereIn('status', [Payment::STATUS_VERIFIED, Payment::STATUS_AUTO_VERIFIED])
                        ->exists();
                    
                    if (!$hasVerified) {
                        \App\Models\Installment::where('id', $instId)->update([
                            'status' => 'UNPAID',
                            'amount_paid' => 0
                        ]);
                    }
                }
            }

            // 2. Cari Rencana Angsuran (PaymentPlan) yang TIDAK memiliki pembayaran Verified sama sekali
            $plansToDelete = \App\Models\PaymentPlan::where('student_id', $student->id)
                ->whereNotIn('id', function($query) {
                    $query->select('payment_plan_id')
                          ->from('payments')
                          ->whereIn('status', [Payment::STATUS_VERIFIED, Payment::STATUS_AUTO_VERIFIED])
                          ->whereNotNull('payment_plan_id');
                })
                ->get();

            if ($plansToDelete->isNotEmpty()) {
                $deletedPlansCount = $plansToDelete->count();
                foreach ($plansToDelete as $plan) {
                    \App\Models\Installment::where('payment_plan_id', $plan->id)->delete();
                    $plan->delete();
                }
            }

            // Jika tidak ada yang dihapus sama sekali
            if ($deletedPaymentsCount == 0 && $deletedPlansCount == 0) {
                $errorMessage = 'Tidak ada tagihan atau rencana angsuran yang dapat di-reset (hanya angsuran yang belum lunas/pending yang dapat di-reset).';
                if ($request->wantsJson()) {
                    return response()->json(['message' => $errorMessage], 400);
                }
                return redirect()->back()->with('error', $errorMessage);
            }

            $messages = [];
            if ($deletedPaymentsCount > 0) $messages[] = "$deletedPaymentsCount data upload bukti bayar";
            if ($deletedPlansCount > 0) $messages[] = "$deletedPlansCount rencana angsuran";

            $successMessage = "Berhasil mereset " . implode(" dan ", $messages) . " mahasiswa.";
            if ($request->wantsJson()) {
                return response()->json(['message' => $successMessage]);
            }
            return redirect()->back()->with('success', $successMessage);

        } catch (\Exception $e) {
            $errorMessage = 'Gagal mereset tagihan: ' . $e->getMessage();
            if ($request->wantsJson()) {
                return response()->json(['message' => $errorMessage], 400);
            }
            return redirect()->back()->with('error', $errorMessage);
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
