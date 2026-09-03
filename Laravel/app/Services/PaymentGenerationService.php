<?php

namespace App\Services;

use App\Models\Installment;
use App\Models\InstallmentTemplate;
use App\Models\PaymentPlan;
use App\Models\Student;
use App\Models\TuitionRate;
use Carbon\Carbon;
use Illuminate\Support\Facades\DB;
use Exception;

class PaymentGenerationService
{
    protected SemesterService $semesterService;

    public function __construct(SemesterService $semesterService)
    {
        $this->semesterService = $semesterService;
    }

    /**
     * Generate Payment Plan and Installments for a student.
     * 
     * @param Student $student
     * @param InstallmentTemplate $template
     * @param string $category 'SEMESTER' or 'MUNAOSAH'
     * @return PaymentPlan
     * @throws Exception
     */
    public function generatePlan(Student $student, InstallmentTemplate $template, $category = 'SEMESTER')
    {
        // 1. Determine Active Semester
        $today = Carbon::now();
        $semesterInfo = $this->semesterService->getActiveSemester($today);
        $academicYear = $student->academic_year ?? $semesterInfo['academic_year'];
        $term = $student->start_term ?? $semesterInfo['term'];

        // 2. Check for existing ACTIVE plan
        $existingPlan = PaymentPlan::where('student_id', $student->id)
            ->where('academic_year', $academicYear)
            ->where('term', $term)
            ->where('category', $category)
            ->where('status', '!=', 'CANCELLED')
            ->first();

        if ($existingPlan) {
            throw new Exception("Student already has an active {$category} payment plan for {$academicYear} {$term}.");
        }

        // 3. Find Tuition Rate (Snapshot)
        $rate = TuitionRate::where('program_type', $student->program_type)
            ->where('academic_year', $academicYear)
            ->where('category', $category)
            ->where('start_term', $student->start_term)
            ->where('is_alumni', (bool)$student->is_alumni)
            ->where('active', true)
            ->first();

        // Fallback to the latest active rate if current academic year rate is not found
        if (!$rate) {
            $rate = TuitionRate::where('program_type', $student->program_type)
                ->where('category', $category)
                ->where('start_term', $student->start_term)
                ->where('is_alumni', (bool)$student->is_alumni)
                ->where('active', true)
                ->orderBy('academic_year', 'desc')
                ->first();
        }

        if (!$rate) {
            throw new Exception("Tuition rate not found for {$student->program_type} ({$category}). Please set it in Admin Panel.");
        }

        // 4. Validate Template compatibility (Optional but good practice)
        if ($template->program_type !== $student->program_type) {
             // In strict mode we might block, but flexible templates are okay if explicitly chosen.
             // For now, let's assume strictness or just warning.
             // throw new Exception("Template program type mismatch.");
        }

        return DB::transaction(function () use ($student, $template, $academicYear, $term, $rate, $today, $category) {
            // 5. Create Payment Plan
            $plan = PaymentPlan::create([
                'student_id' => $student->id,
                'installment_template_id' => $template->id,
                'academic_year' => $academicYear,
                'term' => $term,
                'category' => $category,
                'total_amount' => $rate->amount,
                'status' => 'ACTIVE',
            ]);

            // 6. Generate Schedule Dates
            // We use the start of the semester or today, usually semester start is better for fixed schedules.
            // But if student joins late, maybe today?
            // SemesterService logic uses a reference date.
            // Let's use today for now, or maybe the template's start month logic handles it relative to term start?
            // SemesterService::generateSchedule takes $startDate.
            // Ideally, $startDate should be the start of the semester (e.g., Sept 1st or Feb 1st).
            
            // Let's approximate start of term based on term name
            $termStartMonth = ($term === 'GASAL') ? 9 : 3; // Sept or Mar (Genap mulai Maret)
            $termStartYear = ($term === 'GASAL') ? substr($academicYear, 0, 4) : substr($academicYear, 5, 4);
            $startDate = Carbon::create($termStartYear, $termStartMonth, 1);

            $items = $template->items()->orderBy('installment_no')->get();
            $schedule = $this->semesterService->generateSchedule($startDate, $items);

            // 7. Calculate Amounts
            $totalAmount = $rate->amount;
            $count = $items->count();
            
            if ($count === 0) {
                 // Fallback for no items (should not happen if template is valid, but safe guard)
                 throw new Exception("Template has no installment items.");
            }
            
            $baseAmount = floor($totalAmount / $count);
            $remainder = $totalAmount - ($baseAmount * $count);

            // 8. Create Installments
            foreach ($schedule as $index => $item) {
                // Add remainder to the last installment
                $isLast = ($index === $count - 1);
                $amount = $baseAmount + ($isLast ? $remainder : 0);

                Installment::create([
                    'payment_plan_id' => $plan->id,
                    'installment_no' => $item['installment_no'],
                    'due_date' => $item['due_date'],
                    'amount' => $amount,
                    'status' => 'UNPAID',
                ]);
            }

            return $plan->load('installments');
        });
    }
}
