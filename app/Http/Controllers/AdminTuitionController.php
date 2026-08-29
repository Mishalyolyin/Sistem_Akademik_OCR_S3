<?php

namespace App\Http\Controllers;

use App\Models\TuitionRate;
use App\Models\StudyClass;
use App\Models\PaymentPlan;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class AdminTuitionController extends Controller
{
    public function index()
    {
        // Reguler Rates
        $regulerSemesterRates = TuitionRate::where('program_type', 'REGULER')
            ->where('category', 'SEMESTER')
            ->orderBy('academic_year', 'desc')
            ->orderBy('start_term', 'asc')
            ->orderBy('is_alumni', 'asc') // Menampilkan Non-Alumni dulu, baru Alumni
            ->get();

        $regulerMunaqosahRates = TuitionRate::where('program_type', 'REGULER')
            ->where('category', 'MUNAOSAH')
            ->orderBy('academic_year', 'desc')
            ->orderBy('start_term', 'asc')
            ->orderBy('is_alumni', 'asc')
            ->get();

        // RPL Rates
        $rplSemesterRates = TuitionRate::where('program_type', 'RPL')
            ->where('category', 'SEMESTER')
            ->orderBy('academic_year', 'desc')
            ->orderBy('start_term', 'asc')
            ->get();

        $rplMunaqosahRates = TuitionRate::where('program_type', 'RPL')
            ->where('category', 'MUNAOSAH')
            ->orderBy('academic_year', 'desc')
            ->orderBy('start_term', 'asc')
            ->get();

        $rplKerjasamaRates = TuitionRate::where('program_type', 'RPL')
            ->where('category', 'KERJASAMA')
            ->orderBy('academic_year', 'desc')
            ->orderBy('start_term', 'asc')
            ->get();

        // Generate Academic Years based on Imported Data and Existing Records
        $years = [];

        // 1. From StudyClass (Generations of imported students)
        // Generation 2023 -> 2023/2024
        $generations = StudyClass::distinct()->pluck('generation')->filter()->toArray();
        foreach ($generations as $gen) {
            $years[] = $gen . '/' . ($gen + 1);
        }

        // 2. From existing PaymentPlans (if any)
        $planYears = PaymentPlan::distinct()->pluck('academic_year')->filter()->toArray();
        $years = array_merge($years, $planYears);

        // 3. From existing TuitionRates
        $rateYears = TuitionRate::distinct()->pluck('academic_year')->filter()->toArray();
        $years = array_merge($years, $rateYears);
        
        // 4. Always include Current and Next Academic Year (as fallback/convenience)
        $currentYear = date('Y');
        
        // Menambahkan rentang tahun dari 3 tahun ke belakang sampai 2 tahun ke depan
        // (Jadi tahun 2023/2024, 2024/2025 dst. selalu muncul)
        for ($i = -3; $i <= 2; $i++) {
            $y = $currentYear + $i;
            $years[] = $y . '/' . ($y + 1);
        }

        // Unique and Sort
        $availableAcademicYears = array_unique($years);
        rsort($availableAcademicYears);

        return view('admin.tuition.index', compact(
            'regulerSemesterRates',
            'regulerMunaqosahRates',
            'rplSemesterRates',
            'rplMunaqosahRates',
            'rplKerjasamaRates',
            'availableAcademicYears'
        ));
    }

    public function store(Request $request)
    {
        $request->validate([
            'program_type' => ['required', Rule::in(['REGULER', 'RPL'])],
            'academic_year' => ['required', 'string', 'regex:/^\d{4}\/\d{4}$/'],
            'category' => ['required', Rule::in(['SEMESTER', 'MUNAOSAH', 'KERJASAMA'])],
            'start_term' => ['required', Rule::in(['GASAL', 'GENAP'])],
            'amount' => 'required|numeric|min:0',
            'description' => 'nullable|string',
            'active' => 'boolean',
            'is_alumni' => 'nullable|in:1,0,true,false' // Fix: validasi value dari dropdown (string '1'/'0')
        ]);

        // Fix: pastikan nilai is_alumni dikonversi ke boolean yang benar jika ada is_alumni dari dropdown form
        $isAlumni = false;
        if ($request->program_type === 'REGULER' && $request->has('is_alumni')) {
            $isAlumni = filter_var($request->is_alumni, FILTER_VALIDATE_BOOLEAN);
        }

        TuitionRate::updateOrCreate(
            [
                'program_type' => $request->program_type,
                'academic_year' => $request->academic_year,
                'category' => $request->category,
                'start_term' => $request->start_term,
                'is_alumni' => $isAlumni,
            ],
            [
                'amount' => $request->amount,
                'description' => $request->description,
                'active' => $request->has('active')
            ]
        );

        return redirect()->back()->with('success', 'Biaya kuliah berhasil diperbarui.');
    }

    public function destroy(TuitionRate $tuitionRate)
    {
        $tuitionRate->delete();
        return redirect()->back()->with('success', 'Biaya kuliah berhasil dihapus.');
    }
}
