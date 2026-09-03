<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\InstallmentTemplate;
use App\Models\Payment;
use App\Models\PaymentPlan;
use App\Models\Student;
use App\Models\User;
use App\Models\VerificationLog;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AdminPaymentVerificationTest extends TestCase
{
    use RefreshDatabase;

    protected $admin;
    protected $studentUser;
    protected $payment;

    protected function setUp(): void
    {
        parent::setUp();

        $this->admin = User::factory()->create(['role' => 'admin']);
        $this->studentUser = User::factory()->create(['role' => 'mahasiswa']);

        // Setup Payment Data
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $this->admin->id]);
        $student = Student::create([
            'user_id' => $this->studentUser->id,
            'nim' => '123456',
            'name' => 'Test Student',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id,
        ]);

        $template = InstallmentTemplate::create([
             'name' => 'Template 1',
             'program_type' => 'REGULER',
             'start_term' => 'GASAL',
             'installments_count' => 1,
             'active' => true
        ]);

        $plan = PaymentPlan::create([
            'student_id' => $student->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'installment_template_id' => $template->id,
            'status' => 'ACTIVE',
        ]);
        
        $installment = $plan->installments()->create([
            'installment_no' => 1,
            'due_date' => now()->addMonth(),
            'amount' => 1000000,
            'status' => 'UNPAID',
        ]);

        $this->payment = Payment::create([
            'student_id' => $student->id,
            'payment_plan_id' => $plan->id,
            'installment_id' => $installment->id,
            'amount' => 1000000,
            'proof_file_path' => 'payments/dummy.jpg',
            'status' => Payment::STATUS_NEEDS_REVIEW,
        ]);
    }

    public function test_admin_can_list_reviewable_payments()
    {
        $response = $this->actingAs($this->admin)
            ->getJson('/api/admin/payments/review');

        $response->assertStatus(200)
            ->assertJsonCount(1)
            ->assertJsonFragment(['id' => $this->payment->id]);
    }

    public function test_admin_can_approve_payment()
    {
        $response = $this->actingAs($this->admin)
            ->postJson("/api/admin/payments/{$this->payment->id}/verify", [
                'action' => 'approve',
                'notes' => 'Valid proof',
            ]);

        $response->assertStatus(200);

        $this->payment->refresh();
        $this->assertEquals(Payment::STATUS_VERIFIED, $this->payment->status);
        $this->assertNotNull($this->payment->verified_at);
        $this->assertEquals($this->admin->id, $this->payment->verified_by_user_id);
        
        // Check Log
        $this->assertDatabaseHas('verification_logs', [
            'payment_id' => $this->payment->id,
            'admin_id' => $this->admin->id,
            'action' => 'approve',
            'notes' => 'Valid proof',
        ]);

        // Check Allocation (Installment should be PAID)
        $this->assertEquals('PAID', $this->payment->installment->fresh()->status);
    }

    public function test_admin_can_reject_payment()
    {
        $response = $this->actingAs($this->admin)
            ->postJson("/api/admin/payments/{$this->payment->id}/verify", [
                'action' => 'reject',
                'notes' => 'Blurry image',
            ]);

        $response->assertStatus(200);

        $this->payment->refresh();
        $this->assertEquals(Payment::STATUS_REJECTED, $this->payment->status);
        
        // Check Log
        $this->assertDatabaseHas('verification_logs', [
            'payment_id' => $this->payment->id,
            'action' => 'reject',
            'notes' => 'Blurry image',
        ]);
        
        // Installment should remain UNPAID
        $this->assertEquals('UNPAID', $this->payment->installment->fresh()->status);
    }

    public function test_student_cannot_access_admin_routes()
    {
        $response = $this->actingAs($this->studentUser)
            ->getJson('/api/admin/payments/review');

        $response->assertStatus(403);
    }
}
