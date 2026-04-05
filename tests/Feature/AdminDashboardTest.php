<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\InstallmentTemplate;
use App\Models\Payment;
use App\Models\PaymentPlan;
use App\Models\Student;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AdminDashboardTest extends TestCase
{
    use RefreshDatabase;

    public function test_admin_can_view_dashboard_stats()
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);
        
        $user = User::factory()->create(['role' => 'mahasiswa']);
        $student = Student::create(['user_id' => $user->id, 'nim' => '123456', 'name' => 'Test', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'wallet_balance' => 0, 'import_batch_id' => $batch->id]);
        
        $template = InstallmentTemplate::create(['name' => 'Test Template', 'program_type' => 'REGULER', 'installments_count' => 1]);

        // Create active payment plan
        PaymentPlan::create([
            'student_id' => $student->id,
            'installment_template_id' => $template->id,
            'total_amount' => 5000000,
            'status' => 'ACTIVE',
            'academic_year' => '2025/2026',
            'term' => 'GASAL'
        ]);

        // Create payments
        Payment::create([
            'student_id' => $student->id,
            'amount' => 100000,
            'status' => 'VERIFIED',
            'proof_file_path' => 'test.jpg',
            'payment_method' => 'TRANSFER',
        ]);
        
        Payment::create([
            'student_id' => $student->id,
            'amount' => 200000,
            'status' => 'PENDING',
            'proof_file_path' => 'test.jpg',
            'payment_method' => 'TRANSFER',
        ]);

        $response = $this->actingAs($admin)
            ->getJson('/api/admin/dashboard/stats');

        $response->assertStatus(200)
            ->assertJsonStructure([
                'rplStats' => [
                    'active_students',
                    'total_tagihan',
                    'waiting_verification',
                    'sudah_lunas',
                    'ratio',
                    'students'
                ],
                'regulerStats',
                'months',
                'rplTrend',
                'regulerTrend'
            ]);
        
        // Basic check - Reguler stats (since we created a Reguler student)
        $this->assertEquals(1, $response->json('regulerStats.active_students'));
        $this->assertEquals(100000, $response->json('regulerStats.ratio.total_uang'));
        $this->assertEquals(0, $response->json('rplStats.active_students'));
    }

    public function test_student_cannot_view_dashboard_stats()
    {
        $user = User::factory()->create(['role' => 'mahasiswa']);
        
        $response = $this->actingAs($user)
            ->getJson('/api/admin/dashboard/stats');

        $response->assertStatus(403);
    }
}
