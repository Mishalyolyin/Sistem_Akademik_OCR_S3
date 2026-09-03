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

class PaymentPlanController extends Controller
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
        if (!$user->student) {
            // For now, if no student record, maybe redirect or show error view
            return view('welcome')->with('error', 'User is not linked to a student record.');
        }

        $student = $user->student;
        $today = Carbon::now();
        // Dapatkan informasi semester global saat ini
        $semesterInfo = $this->semesterService->getActiveSemester($today);
        
        // Siklus tagihan mahasiswa didasarkan pada periode awal masuk mereka (start_term)
        // dan tahun akademik yang terdaftar pada profil mereka (dari data import admin)
        $academicYear = $student->academic_year ?? $semesterInfo['academic_year'];
        $term = $student->start_term ?? $semesterInfo['term'];

        // Check for Semester plan
        $semesterPlan = PaymentPlan::where('student_id', $student->id)
            ->where('academic_year', $academicYear)
            ->where('term', $term)
            ->where('category', 'SEMESTER')
            ->where('status', '!=', 'CANCELLED')
            ->with(['installments' => function($q) {
                $q->orderBy('due_date', 'asc')->with('payments');
            }])
            ->first();

        $activePlan = $semesterPlan;

        if ($activePlan) {
            // Find next unpaid installment
            $nextInstallment = $activePlan->installments
                ->where('status', '!=', 'PAID')
                ->sortBy('due_date')
                ->first();

            // Calculate progress
            $totalAmount = $activePlan->total_amount;
            $paidAmount = $activePlan->installments->where('status', 'PAID')->sum('amount');
            $progressPercentage = $totalAmount > 0 ? ($paidAmount / $totalAmount) * 100 : 0;

            // Mock Broadcasts
            $broadcasts = [
                ['title' => 'Perubahan Rekening', 'content' => 'Mulai 1 November, pembayaran via BNI menggunakan Virtual Account baru.', 'date' => '2025-10-20'],
                ['title' => 'Jadwal UAS', 'content' => 'Pelunasan biaya kuliah wajib dilakukan sebelum UAS dimulai.', 'date' => '2025-10-15'],
            ];

            return view('student.dashboard', [
                'status' => 'has_plan',
                'student' => $student,
                'plan' => $activePlan,
                'semesterInfo' => $semesterInfo,
                'billingYear' => $academicYear,
                'billingTerm' => $term,
                'nextInstallment' => $nextInstallment,
                'progressPercentage' => $progressPercentage,
                'paidAmount' => $paidAmount,
                'remainingAmount' => $totalAmount - $paidAmount,
                'broadcasts' => $broadcasts,
                'templates' => []
            ]);
        }

        // Available Templates (exclude Munaqosah)
        $templates = InstallmentTemplate::where('program_type', $student->program_type)
            ->where('start_term', $term)
            ->where('name', 'not like', '%Munaqosah%')
            ->where('active', true)
            ->with('items')
            ->get();

        $tuitionRate = \App\Models\TuitionRate::where('program_type', $student->program_type)
            ->where('academic_year', $academicYear)
            ->where('category', 'SEMESTER')
            ->where('start_term', $student->start_term)
            ->where('is_alumni', (bool)$student->is_alumni)
            ->where('active', true)
            ->first();

        // Fallback to the latest active rate if current academic year rate is not found
        if (!$tuitionRate) {
            $tuitionRate = \App\Models\TuitionRate::where('program_type', $student->program_type)
                ->where('category', 'SEMESTER')
                ->where('start_term', $student->start_term)
                ->where('is_alumni', (bool)$student->is_alumni)
                ->where('active', true)
                ->orderBy('academic_year', 'desc')
                ->first();
        }
            
        $tuitionAmount = $tuitionRate ? $tuitionRate->amount : 0;

        return view('student.dashboard', [
            'status' => 'choose_plan',
            'student' => $student,
            'templates' => $templates,
            'tuitionAmount' => $tuitionAmount,
            'semesterInfo' => $semesterInfo,
            'billingYear' => $academicYear,
            'billingTerm' => $term,
            'plan' => null,
            'nextInstallment' => null
        ]);
    }

    public function createMunaqosahPlan(Request $request)
    {
        // Deprecated: Logic moved to MunaqosahController
        abort(404);
    }

    public function store(Request $request)
    {
        $request->validate([
            'installment_template_id' => 'required|exists:installment_templates,id',
        ]);

        $user = Auth::user();
        $student = $user->student;

        if (!$student) {
            return redirect()->back()->with('error', 'Student not found.');
        }

        $template = InstallmentTemplate::findOrFail($request->installment_template_id);

        try {
            $plan = $this->paymentService->generatePlan($student, $template);
            return redirect()->route('student.dashboard')->with('success', 'Rencana pembayaran berhasil dibuat.');
        } catch (Exception $e) {
            return redirect()->back()->with('error', $e->getMessage());
        }
    }
}

