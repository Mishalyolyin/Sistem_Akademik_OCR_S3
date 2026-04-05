<?php

namespace Tests\Feature;

use App\Models\Adjustment;
use App\Models\ImportBatch;
use App\Models\InstallmentTemplate;
use App\Models\PaymentPlan;
use App\Models\Student;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AdjustmentTest extends TestCase
{
    use RefreshDatabase;

    protected $admin;
    protected $student;
    protected $installment;

    protected function setUp(): void
    {
        parent::setUp();

        $this->admin = User::factory()->create(['role' => 'admin']);
        $studentUser = User::factory()->create(['role' => 'mahasiswa']);

        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $this->admin->id]);
        $this->student = Student::create([
            'user_id' => $studentUser->id,
            'nim' => '123456',
            'name' => 'Test Student',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id,
            'wallet_balance' => 0,
        ]);

        $template = InstallmentTemplate::create([
             'name' => 'Template 1',
             'program_type' => 'REGULER',
             'start_term' => 'GASAL',
             'installments_count' => 1,
             'active' => true
        ]);

        $plan = PaymentPlan::create([
            'student_id' => $this->student->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'installment_template_id' => $template->id,
            'status' => 'ACTIVE',
        ]);
        
        $this->installment = $plan->installments()->create([
            'installment_no' => 1,
            'due_date' => now()->addMonth(),
            'amount' => 1000000,
            'amount_paid' => 500000, // Partial paid
            'status' => 'PARTIAL',
        ]);
    }

    public function test_admin_can_adjust_installment_balance()
    {
        // Credit (Add payment)
        $response = $this->actingAs($this->admin)
            ->postJson('/api/admin/adjustments', [
                'target_type' => 'installment',
                'target_id' => $this->installment->id,
                'amount' => 500000,
                'reason' => 'Manual correction for missed transfer',
            ]);

        $response->assertStatus(201);
        
        $this->installment->refresh();
        $this->assertEquals(1000000, $this->installment->amount_paid);
        $this->assertEquals('PAID', $this->installment->status);
        
        $this->assertDatabaseHas('adjustments', [
            'adjustable_type' => get_class($this->installment),
            'adjustable_id' => $this->installment->id,
            'amount' => 500000,
            'admin_id' => $this->admin->id,
        ]);
    }

    public function test_admin_can_refund_installment_balance()
    {
        // Debit (Refund)
        $response = $this->actingAs($this->admin)
            ->postJson('/api/admin/adjustments', [
                'target_type' => 'installment',
                'target_id' => $this->installment->id,
                'amount' => -500000,
                'reason' => 'Refund double payment',
            ]);

        $response->assertStatus(201);
        
        $this->installment->refresh();
        $this->assertEquals(0, $this->installment->amount_paid);
        $this->assertEquals('UNPAID', $this->installment->status);
    }

    public function test_admin_can_adjust_student_wallet()
    {
        $response = $this->actingAs($this->admin)
            ->postJson('/api/admin/adjustments', [
                'target_type' => 'student',
                'target_id' => $this->student->id,
                'amount' => 50000,
                'reason' => 'Bonus credit',
            ]);

        $response->assertStatus(201);
        
        $this->student->refresh();
        $this->assertEquals(50000, $this->student->wallet_balance);
        
        $this->assertDatabaseHas('adjustments', [
            'adjustable_type' => get_class($this->student),
            'adjustable_id' => $this->student->id,
            'amount' => 50000,
        ]);
    }

    public function test_non_admin_cannot_adjust()
    {
        $user = User::factory()->create(['role' => 'mahasiswa']);
        
        $response = $this->actingAs($user)
            ->postJson('/api/admin/adjustments', [
                'target_type' => 'student',
                'target_id' => $this->student->id,
                'amount' => 50000,
                'reason' => 'Hacking',
            ]);

        $response->assertStatus(403);
    }
}
