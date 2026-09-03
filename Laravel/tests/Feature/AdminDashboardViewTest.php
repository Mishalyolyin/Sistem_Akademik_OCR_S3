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

class AdminDashboardViewTest extends TestCase
{
    use RefreshDatabase;

    public function test_admin_dashboard_displays_split_stats()
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        // Create RPL Student & Plan
        $userRpl = User::factory()->create(['role' => 'mahasiswa']);
        $studentRpl = Student::create([
            'user_id' => $userRpl->id,
            'nim' => 'RPL001',
            'name' => 'RPL Student',
            'program_type' => 'RPL',
            'start_term' => 'GASAL',
            'wallet_balance' => 0,
            'import_batch_id' => $batch->id
        ]);
        $templateRpl = InstallmentTemplate::create(['name' => 'RPL Template', 'program_type' => 'RPL', 'installments_count' => 1]);
        PaymentPlan::create([
            'student_id' => $studentRpl->id,
            'installment_template_id' => $templateRpl->id,
            'total_amount' => 10000000,
            'status' => 'ACTIVE',
            'academic_year' => '2025/2026',
            'term' => 'GASAL'
        ]);

        // Create Reguler Student & Plan
        $userReg = User::factory()->create(['role' => 'mahasiswa']);
        $studentReg = Student::create([
            'user_id' => $userReg->id,
            'nim' => 'REG001',
            'name' => 'Reguler Student',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'wallet_balance' => 0,
            'import_batch_id' => $batch->id
        ]);
        $templateReg = InstallmentTemplate::create(['name' => 'Reguler Template', 'program_type' => 'REGULER', 'installments_count' => 1]);
        PaymentPlan::create([
            'student_id' => $studentReg->id,
            'installment_template_id' => $templateReg->id,
            'total_amount' => 5000000,
            'status' => 'ACTIVE',
            'academic_year' => '2025/2026',
            'term' => 'GASAL'
        ]);

        // Create Payments (Verified for RPL, Needs Review for Reguler)
        Payment::create([
            'student_id' => $studentRpl->id,
            'amount' => 2000000,
            'status' => 'VERIFIED',
            'proof_file_path' => 'rpl.jpg',
            'payment_method' => 'TRANSFER',
        ]);
        
        Payment::create([
            'student_id' => $studentReg->id,
            'amount' => 1000000,
            'status' => 'NEEDS_REVIEW',
            'proof_file_path' => 'reg.jpg',
            'payment_method' => 'TRANSFER',
        ]);

        $response = $this->actingAs($admin)->get(route('admin.dashboard'));

        $response->assertStatus(200);
        $response->assertViewIs('admin.dashboard');
        
        // Assert View Data Structure
        $response->assertViewHas(['rplStats', 'regulerStats', 'months', 'rplTrend', 'regulerTrend']);

        // Assert RPL Stats
        $rplStats = $response->viewData('rplStats');
        $this->assertEquals(1, $rplStats['active_students']);
        $this->assertEquals(10000000, $rplStats['total_tagihan']);
        $this->assertEquals(2000000, $rplStats['ratio']['total_uang']);
        $this->assertEquals(0, $rplStats['waiting_verification']); 

        // Assert Reguler Stats
        $regulerStats = $response->viewData('regulerStats');
        $this->assertEquals(1, $regulerStats['active_students']);
        $this->assertEquals(5000000, $regulerStats['total_tagihan']);
        $this->assertEquals(0, $regulerStats['ratio']['total_uang']); // Only VERIFIED payments count
        $this->assertEquals(1, $regulerStats['waiting_verification']);
    }
}
