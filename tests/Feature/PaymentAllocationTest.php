<?php

namespace Tests\Feature;

use Tests\TestCase;
use App\Models\User;
use App\Models\Student;
use App\Models\PaymentPlan;
use App\Models\Installment;
use App\Models\Payment;
use App\Models\ImportBatch;
use App\Models\InstallmentTemplate;
use App\Services\PaymentAllocationService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Carbon\Carbon;

class PaymentAllocationTest extends TestCase
{
    use RefreshDatabase;

    protected $user;
    protected $student;
    protected $plan;
    protected $allocationService;

    protected function setUp(): void
    {
        parent::setUp();

        $this->user = User::factory()->create(['role' => 'mahasiswa']);
        $admin = User::factory()->create(['role' => 'admin']);
        
        $batch = ImportBatch::create([
            'batch_name' => 'Test Batch',
            'file_name' => 'students.xlsx',
            'imported_by_admin_id' => $admin->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'status' => 'COMPLETED',
            'total_rows' => 1,
            'success_count' => 1,
            'failed_count' => 0
        ]);

        $this->student = Student::create([
            'user_id' => $this->user->id,
            'nim' => '12345678',
            'name' => 'Test Student',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id
        ]);

        $template = InstallmentTemplate::create([
            'name' => 'Test Template',
            'installments_count' => 3,
            'program_type' => 'REGULER',
            'is_active' => true
        ]);

        $this->plan = PaymentPlan::create([
            'student_id' => $this->student->id,
            'installment_template_id' => $template->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'status' => 'ACTIVE'
        ]);

        // Create 3 Installments: 1jt, 1jt, 1jt
        for ($i = 1; $i <= 3; $i++) {
            Installment::create([
                'payment_plan_id' => $this->plan->id,
                'installment_no' => $i,
                'due_date' => Carbon::now()->addMonths($i),
                'amount' => 1000000, // 1 Juta
                'status' => 'UNPAID'
            ]);
        }

        $this->allocationService = app(PaymentAllocationService::class);
    }

    public function test_partial_payment_allocation()
    {
        $inst1 = Installment::where('installment_no', 1)->first();

        $payment = Payment::create([
            'student_id' => $this->student->id,
            'payment_plan_id' => $this->plan->id,
            'installment_id' => $inst1->id,
            'amount' => 500000, // Half
            'proof_file_path' => 'dummy.jpg',
            'status' => 'VERIFIED', // Simulate Verified
            'verified_at' => now(),
            'file_hash' => 'hash1'
        ]);

        $this->allocationService->allocate($payment);

        $inst1->refresh();
        $this->assertEquals(500000, $inst1->amount_paid);
        $this->assertEquals('PARTIAL', $inst1->status);
        
        $payment->refresh();
        $this->assertNotNull($payment->allocated_at);
    }

    public function test_full_payment_allocation()
    {
        $inst1 = Installment::where('installment_no', 1)->first();

        $payment = Payment::create([
            'student_id' => $this->student->id,
            'payment_plan_id' => $this->plan->id,
            'installment_id' => $inst1->id,
            'amount' => 1000000, // Full
            'proof_file_path' => 'dummy.jpg',
            'status' => 'VERIFIED',
            'verified_at' => now(),
            'file_hash' => 'hash2'
        ]);

        $this->allocationService->allocate($payment);

        $inst1->refresh();
        $this->assertEquals(1000000, $inst1->amount_paid);
        $this->assertEquals('PAID', $inst1->status);
    }

    public function test_over_payment_allocation_to_wallet()
    {
        // Delete other installments to force overpayment to wallet
        Installment::where('installment_no', '>', 1)->delete();

        $inst1 = Installment::where('installment_no', 1)->first();

        $payment = Payment::create([
            'student_id' => $this->student->id,
            'payment_plan_id' => $this->plan->id,
            'installment_id' => $inst1->id,
            'amount' => 1500000, // Over 500k
            'proof_file_path' => 'dummy.jpg',
            'status' => 'VERIFIED',
            'verified_at' => now(),
            'file_hash' => 'hash3'
        ]);

        $this->allocationService->allocate($payment);

        $inst1->refresh();
        $this->assertEquals(1000000, $inst1->amount_paid);
        $this->assertEquals('PAID', $inst1->status);

        $this->student->refresh();
        $this->assertEquals(500000, $this->student->wallet_balance);
    }

    public function test_auto_allocation_fifo()
    {
        // No specific installment_id
        $payment = Payment::create([
            'student_id' => $this->student->id,
            'payment_plan_id' => $this->plan->id,
            'installment_id' => null,
            'amount' => 2500000, // Enough for Inst 1 (1jt), Inst 2 (1jt), and Half Inst 3 (500k)
            'proof_file_path' => 'dummy.jpg',
            'status' => 'VERIFIED',
            'verified_at' => now(),
            'file_hash' => 'hash4'
        ]);

        $this->allocationService->allocate($payment);

        $inst1 = Installment::where('installment_no', 1)->first();
        $inst2 = Installment::where('installment_no', 2)->first();
        $inst3 = Installment::where('installment_no', 3)->first();

        $this->assertEquals('PAID', $inst1->status);
        $this->assertEquals('PAID', $inst2->status);
        $this->assertEquals('PARTIAL', $inst3->status);
        $this->assertEquals(500000, $inst3->amount_paid);
    }
}
