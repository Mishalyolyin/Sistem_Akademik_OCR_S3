<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\InstallmentTemplate;
use App\Models\PaymentPlan;
use App\Models\Student;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class StudentDashboardViewTest extends TestCase
{
    use RefreshDatabase;

    public function test_student_can_view_dashboard_choose_plan()
    {
        $this->withoutVite();
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);
        
        $user = User::factory()->create(['role' => 'mahasiswa']);
        $student = Student::create([
            'user_id' => $user->id,
            'nim' => '123456',
            'name' => 'Test Student',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'wallet_balance' => 0,
            'import_batch_id' => $batch->id
        ]);

        InstallmentTemplate::create([
            'name' => 'Plan A',
            'program_type' => 'REGULER',
            'start_term' => 'GENAP',
            'installments_count' => 6,
            'active' => true
        ]);

        $response = $this->actingAs($user)->get(route('student.dashboard'));

        $response->assertStatus(200);
        $response->assertSee('Pilih Skema Pembayaran');
        $response->assertSee('Plan A');
        $response->assertSee('Test Student');
    }

    public function test_student_can_view_dashboard_has_plan()
    {
        $this->withoutVite();
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        $user = User::factory()->create(['role' => 'mahasiswa']);
        $student = Student::create([
            'user_id' => $user->id,
            'nim' => '123456',
            'name' => 'Test Student',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'wallet_balance' => 0,
            'import_batch_id' => $batch->id
        ]);

        $template = InstallmentTemplate::create([
            'name' => 'Plan A',
            'program_type' => 'REGULER',
            'installments_count' => 1,
            'active' => true
        ]);

        $plan = PaymentPlan::create([
            'student_id' => $student->id,
            'installment_template_id' => $template->id,
            'total_amount' => 1000000,
            'status' => 'ACTIVE',
            'academic_year' => '2025/2026',
            'term' => 'GASAL'
        ]);
        
        $response = $this->actingAs($user)->get(route('student.dashboard'));
        
        $response->assertStatus(200);
    }
}
