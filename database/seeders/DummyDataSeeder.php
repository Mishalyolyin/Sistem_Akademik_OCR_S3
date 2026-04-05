<?php

namespace Database\Seeders;

use App\Models\User;
use App\Models\Student;
use App\Models\PaymentPlan;
use App\Models\InstallmentTemplate;
use App\Models\Payment;
use App\Models\ImportBatch;
use App\Models\Installment;
use Illuminate\Database\Seeder;
use Illuminate\Support\Str;
use Carbon\Carbon;

class DummyDataSeeder extends Seeder
{
    public function run(): void
    {
        $admin = User::where('role', 'admin')->first();
        $batch = ImportBatch::create(['file_name' => 'dummy_seeder.xlsx', 'imported_by_admin_id' => $admin->id ?? 1]);
        
        $templateReguler = InstallmentTemplate::where('program_type', 'REGULER')->first();
        
        // 1. Student with Full Payment (Verified)
        $user1 = User::create([
            'name' => 'Abdul Rahman',
            'email' => 'abdul@mhs.com',
            'password' => bcrypt('password'),
            'role' => 'mahasiswa'
        ]);
        
        $student1 = Student::create([
            'user_id' => $user1->id,
            'nim' => '21502500764',
            'name' => 'Abdul Rahman',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id
        ]);

        $plan1 = PaymentPlan::create([
            'student_id' => $student1->id,
            'installment_template_id' => $templateReguler->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'status' => 'ACTIVE'
        ]);

        $this->generateInstallments($plan1, $templateReguler);

        // Pay Installment 1 & 2
        foreach($plan1->installments()->orderBy('installment_no')->take(2)->get() as $inst) {
            Payment::create([
                'student_id' => $student1->id,
                'payment_plan_id' => $plan1->id,
                'installment_id' => $inst->id,
                'amount' => $inst->amount,
                'status' => 'VERIFIED',
                'proof_file_path' => 'dummy/proof.jpg',
                'verified_at' => now(),
            ]);
            $inst->update(['amount_paid' => $inst->amount, 'status' => 'PAID']);
        }


        // 2. Student with Partial Payment
        $user2 = User::create([
            'name' => 'Siti Aminah',
            'email' => 'siti@mhs.com',
            'password' => bcrypt('password'),
            'role' => 'mahasiswa'
        ]);
        
        $student2 = Student::create([
            'user_id' => $user2->id,
            'nim' => '21502500766',
            'name' => 'Siti Aminah',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id
        ]);

        $plan2 = PaymentPlan::create([
            'student_id' => $student2->id,
            'installment_template_id' => $templateReguler->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'status' => 'ACTIVE'
        ]);
        $this->generateInstallments($plan2, $templateReguler);

        // Pay Installment 1 only
        $inst2_1 = $plan2->installments()->orderBy('installment_no')->first();
        Payment::create([
            'student_id' => $student2->id,
            'payment_plan_id' => $plan2->id,
            'installment_id' => $inst2_1->id,
            'amount' => $inst2_1->amount,
            'status' => 'VERIFIED',
            'proof_file_path' => 'dummy/proof_siti.jpg',
            'verified_at' => now(),
        ]);
        $inst2_1->update(['amount_paid' => $inst2_1->amount, 'status' => 'PAID']);
        
        // 3. Student with Pending Payment
        $user3 = User::create([
            'name' => 'Budi Santoso',
            'email' => 'budi@mhs.com',
            'password' => bcrypt('password'),
            'role' => 'mahasiswa'
        ]);
        
        $student3 = Student::create([
            'user_id' => $user3->id,
            'nim' => '21502500767',
            'name' => 'Budi Santoso',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id
        ]);

        $plan3 = PaymentPlan::create([
            'student_id' => $student3->id,
            'installment_template_id' => $templateReguler->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'status' => 'ACTIVE'
        ]);
        $this->generateInstallments($plan3, $templateReguler);

        $inst3_1 = $plan3->installments()->orderBy('installment_no')->first();
        Payment::create([
            'student_id' => $student3->id,
            'payment_plan_id' => $plan3->id,
            'installment_id' => $inst3_1->id,
            'amount' => $inst3_1->amount,
            'status' => 'PENDING',
            'proof_file_path' => 'dummy/proof.jpg',
        ]);
    }

    private function generateInstallments($plan, $template) {
        $items = $template->items()->orderBy('installment_no')->get();
        $totalAmount = $plan->total_amount;
        $count = $items->count();
        if ($count == 0) return;
        
        $baseAmount = floor($totalAmount / $count);
        $remainder = $totalAmount - ($baseAmount * $count);
        
        // Start date: Sept 1st 2025 for GASAL 2025/2026
        $startDate = Carbon::create(2025, 9, 1);

        foreach ($items as $index => $item) {
            $isLast = ($index === $count - 1);
            $amount = $baseAmount + ($isLast ? $remainder : 0);
            
            $dueDate = $startDate->copy()->addMonths($item->month_offset)->setDay($item->day_of_month);

            Installment::create([
                'payment_plan_id' => $plan->id,
                'installment_no' => $item->installment_no,
                'due_date' => $dueDate,
                'amount' => $amount,
                'status' => 'UNPAID',
            ]);
        }
    }
}
