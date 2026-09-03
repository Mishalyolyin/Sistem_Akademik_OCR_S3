<?php

namespace App\Http\Controllers;

use App\Models\InstallmentTemplate;
use App\Models\PaymentPlan;
use App\Services\PaymentGenerationService;
use App\Services\SemesterService;
use Carbon\Carbon;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Exception;

class PendaftaranController extends Controller
{
    protected PaymentGenerationService $paymentService;
    protected SemesterService $semesterService;

    public function __construct(PaymentGenerationService $paymentService, SemesterService $semesterService)
    {
        $this->paymentService = $paymentService;
        $this->semesterService = $semesterService;
    }

    public function index()
    {
        $user = Auth::user();
        $student = $user->student;

        if (!$student) {
            return redirect()->route('student.documents')->with('error', 'Data mahasiswa tidak ditemukan.');
        }

        // 1. Check for Existing Pendaftaran Plan
        $pendaftaranPlan = PaymentPlan::where('student_id', $student->id)
            ->where('category', 'PENDAFTARAN')
            ->where('status', '!=', 'CANCELLED')
            ->with([
                'installments' => function ($q) {
                    $q->orderBy('due_date', 'asc')->with('payments');
                },
            ])
            ->latest()
            ->first();

        if ($pendaftaranPlan) {
            $totalAmount = $pendaftaranPlan->total_amount;
            $paidAmount = $pendaftaranPlan->installments->where('status', 'PAID')->sum('amount');
            $progressPercentage = $totalAmount > 0 ? ($paidAmount / $totalAmount) * 100 : 0;

            $nextInstallment = $pendaftaranPlan->installments
                ->where('status', '!=', 'PAID')
                ->sortBy('due_date')
                ->first();

            return view('student.pendaftaran.index', [
                'status' => 'has_plan',
                'plan' => $pendaftaranPlan,
                'progressPercentage' => $progressPercentage,
                'paidAmount' => $paidAmount,
                'remainingAmount' => $totalAmount - $paidAmount,
                'nextInstallment' => $nextInstallment,
            ]);
        }

        // 2. Exempt (mahasiswa lama dari sebelum fitur ini aktif) & belum pernah
        // bikin plan Pendaftaran — gak perlu ditawari bayar sama sekali.
        if ($student->pendaftaran_exempt) {
            return view('student.pendaftaran.index', [
                'status' => 'exempt',
                'plan' => null,
            ]);
        }

        // 3. No Plan Yet — Offer Template
        $today = Carbon::now();
        $semesterInfo = $this->semesterService->getActiveSemester($today);
        $term = $student->start_term ?? $semesterInfo['term'];

        $templates = InstallmentTemplate::where('program_type', $student->program_type)
            ->where('start_term', $term)
            ->where('name', 'like', '%Pendaftaran%')
            ->where('active', true)
            ->with('items')
            ->get();

        return view('student.pendaftaran.index', [
            'status' => 'no_plan',
            'plan' => null,
            'templates' => $templates,
        ]);
    }

    public function store(Request $request)
    {
        $user = Auth::user();
        $student = $user->student;

        if (!$student) {
            return redirect()->back()->with('error', 'Student not found.');
        }

        $request->validate([
            'installment_template_id' => 'required|exists:installment_templates,id',
        ]);

        $template = InstallmentTemplate::findOrFail($request->installment_template_id);

        // Security check: ensure template is for Pendaftaran
        if (!str_contains($template->name, 'Pendaftaran')) {
            return redirect()->back()->with('error', 'Template tidak valid.');
        }

        try {
            $this->paymentService->generatePlan($student, $template, 'PENDAFTARAN');
            return redirect()->route('student.pendaftaran')->with('success', 'Rencana pembayaran pendaftaran berhasil dibuat.');
        } catch (Exception $e) {
            return redirect()->back()->with('error', $e->getMessage());
        }
    }
}
