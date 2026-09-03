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

class MunaqosahController extends Controller
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
            return redirect()->route('student.dashboard')->with('error', 'Data mahasiswa tidak ditemukan.');
        }

        // 1. Check for Existing Munaqosah Plan
        $munaqosahPlan = PaymentPlan::where('student_id', $student->id)
            ->where('category', 'MUNAOSAH')
            ->where('status', '!=', 'CANCELLED')
            ->with([
                'installments' => function($q) {
                    $q->orderBy('due_date', 'asc')->with('payments');
                },
                'munaqosahDetail',
            ])
            ->latest()
            ->first();

        // 2. Check Eligibility (Must have paid all Semester SPP)
        // First check if they even have an active SEMESTER plan
        $hasActivePlan = PaymentPlan::where('student_id', $student->id)
            ->where('category', 'SEMESTER')
            ->where('status', 'ACTIVE')
            ->exists();

        $hasUnpaidSpp = \App\Models\Installment::whereHas('paymentPlan', function($q) use ($student) {
            $q->where('student_id', $student->id)
              ->where('category', 'SEMESTER');
        })->where('status', '!=', 'PAID')->exists();

        $eligible = $hasActivePlan && !$hasUnpaidSpp;

        // 3. Prepare Data for View
        if ($munaqosahPlan) {
            $totalAmount = $munaqosahPlan->total_amount;
            $paidAmount = $munaqosahPlan->installments->where('status', 'PAID')->sum('amount');
            $progressPercentage = $totalAmount > 0 ? ($paidAmount / $totalAmount) * 100 : 0;
            
            $nextInstallment = $munaqosahPlan->installments
                ->where('status', '!=', 'PAID')
                ->sortBy('due_date')
                ->first();

            return view('student.munaqosah.index', [
                'status' => 'has_plan',
                'plan' => $munaqosahPlan,
                'eligible' => $eligible,
                'progressPercentage' => $progressPercentage,
                'paidAmount' => $paidAmount,
                'remainingAmount' => $totalAmount - $paidAmount,
                'nextInstallment' => $nextInstallment
            ]);
        }

        // 4. If No Plan, Check for Templates
        $templates = [];
        if ($eligible) {
            $today = Carbon::now();
            $semesterInfo = $this->semesterService->getActiveSemester($today);
            $term = $semesterInfo['term'];

            $templates = InstallmentTemplate::where('program_type', $student->program_type)
                ->where('name', 'like', '%Munaqosah%')
                ->where('active', true)
                ->with('items')
                ->get();
        }

        return view('student.munaqosah.index', [
            'status' => 'no_plan',
            'plan' => null,
            'eligible' => $eligible,
            'templates' => $templates
        ]);
    }

    public function store(Request $request)
    {
        $user = Auth::user();
        $student = $user->student;
        
        if (!$student) {
             return redirect()->back()->with('error', 'Student not found.');
        }

        // Re-check eligibility
        $hasActivePlan = PaymentPlan::where('student_id', $student->id)
            ->where('category', 'SEMESTER')
            ->where('status', 'ACTIVE')
            ->exists();

        $hasUnpaidSpp = \App\Models\Installment::whereHas('paymentPlan', function($q) use ($student) {
            $q->where('student_id', $student->id)
              ->where('category', 'SEMESTER');
        })->where('status', '!=', 'PAID')->exists();
        
        if (!$hasActivePlan || $hasUnpaidSpp) {
             return redirect()->back()->with('error', 'Anda belum memenuhi syarat untuk Munaqosah. Pastikan SPP Semester sudah lunas.');
        }

        $request->validate([
            'installment_template_id' => 'required|exists:installment_templates,id',
        ]);

        $template = InstallmentTemplate::findOrFail($request->installment_template_id);
        
        // Security check: ensure template is for Munaqosah
        if (!str_contains($template->name, 'Munaqosah')) {
            return redirect()->back()->with('error', 'Template tidak valid.');
        }

        try {
            $this->paymentService->generatePlan($student, $template, 'MUNAOSAH');
            return redirect()->route('student.munaqosah')->with('success', 'Pendaftaran pembayaran Munaqosah berhasil.');
        } catch (Exception $e) {
            return redirect()->back()->with('error', $e->getMessage());
        }
    }

    public function storeThesisDetails(Request $request)
    {
        $student = Auth::user()->student;

        if (!$student) {
            return redirect()->back()->with('error', 'Data mahasiswa tidak ditemukan.');
        }

        $munaqosahPlan = PaymentPlan::where('student_id', $student->id)
            ->where('category', 'MUNAOSAH')
            ->where('status', '!=', 'CANCELLED')
            ->latest()
            ->first();

        if (!$munaqosahPlan) {
            return redirect()->back()->with('error', 'Rencana pembayaran Munaqosah tidak ditemukan.');
        }

        $validated = $request->validate([
            'supervisor_name' => 'required|string|max:255',
            'supervisor_name_2' => 'required|string|max:255',
            'thesis_title' => 'required|string|max:255',
            'thesis_file' => 'required|file|mimes:pdf|max:20480',
            'article_file' => 'required|file|mimes:pdf|max:20480',
        ]);

        $thesisPath = $request->file('thesis_file')->storeAs(
            'thesis/' . $student->nim,
            'skripsi_' . \Illuminate\Support\Str::slug($student->name) . '.pdf',
            'public'
        );

        $articlePath = $request->file('article_file')->storeAs(
            'thesis/' . $student->nim,
            'artikel_' . \Illuminate\Support\Str::slug($student->name) . '.pdf',
            'public'
        );

        \App\Models\MunaqosahDetail::updateOrCreate(
            ['payment_plan_id' => $munaqosahPlan->id],
            [
                'supervisor_name' => $validated['supervisor_name'],
                'supervisor_name_2' => $validated['supervisor_name_2'],
                'thesis_title' => $validated['thesis_title'],
                'thesis_file_path' => $thesisPath,
                'article_file_path' => $articlePath,
            ]
        );

        return redirect()->route('student.munaqosah')->with('success', 'Data skripsi berhasil disimpan. Silakan lanjutkan upload bukti pembayaran.');
    }
}
