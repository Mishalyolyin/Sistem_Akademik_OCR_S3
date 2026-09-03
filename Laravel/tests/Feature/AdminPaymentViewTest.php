<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\Payment;
use App\Models\Student;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AdminPaymentViewTest extends TestCase
{
    use RefreshDatabase;

    public function test_admin_can_view_verification_page_with_class_column()
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);
        
        $student = Student::create([
            'nim' => '12345678',
            'name' => 'Test Student',
            'class' => 'TI-2B-VIEW',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id
        ]);

        Payment::create([
            'student_id' => $student->id,
            'amount' => 500000,
            'status' => 'NEEDS_REVIEW',
            'proof_file_path' => 'dummy.jpg'
        ]);

        $response = $this->actingAs($admin)->get(route('admin.payments'));

        $response->assertStatus(200);
        $response->assertViewIs('admin.payments.index');
        $response->assertSee('TI-2B-VIEW');
        $response->assertSee('Verifikasi Pembayaran');
    }

    public function test_admin_can_verify_payment_via_web_form()
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);
        
        $student = Student::create([
            'nim' => '87654321',
            'name' => 'Verify Student',
            'class' => 'TI-3C',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id
        ]);

        $template = \App\Models\InstallmentTemplate::create([
             'name' => 'Template Test',
             'program_type' => 'REGULER',
             'start_term' => 'GASAL',
             'installments_count' => 1,
             'active' => true
        ]);

        $plan = \App\Models\PaymentPlan::create([
            'student_id' => $student->id,
            'installment_template_id' => $template->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'status' => 'ACTIVE'
        ]);

        $payment = Payment::create([
            'student_id' => $student->id,
            'payment_plan_id' => $plan->id,
            'amount' => 500000,
            'status' => 'NEEDS_REVIEW',
            'proof_file_path' => 'dummy.jpg'
        ]);

        // Mock Service if needed, or rely on integration test
        // Here we rely on integration, assuming PaymentVerificationService works as tested in AdminPaymentVerificationTest
        
        $response = $this->actingAs($admin)->post(route('admin.payments.verify', $payment->id), [
            'action' => 'approve'
        ]);

        $response->assertRedirect();
        $response->assertSessionHas('success');
        
        $this->assertDatabaseHas('payments', [
            'id' => $payment->id,
            'status' => 'VERIFIED'
        ]);
    }
}
