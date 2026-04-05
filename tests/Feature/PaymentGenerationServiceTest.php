<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\InstallmentTemplate;
use App\Models\Student;
use App\Models\TuitionRate;
use App\Services\PaymentGenerationService;
use App\Services\SemesterService;
use Carbon\Carbon;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;
use App\Models\User;

class PaymentGenerationServiceTest extends TestCase
{
    use RefreshDatabase;

    protected PaymentGenerationService $service;

    protected function setUp(): void
    {
        parent::setUp();
        // Resolve service with real dependencies
        $this->service = app(PaymentGenerationService::class);
    }

    public function test_it_generates_plan_and_installments_successfully()
    {
        // 1. Setup Data
        $admin = User::create([
            'name' => 'Admin',
            'email' => 'admin@example.com',
            'password' => bcrypt('password'),
            'role' => 'admin'
        ]);

        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);
        $student = Student::create([
            'nim' => '123456',
            'name' => 'John Doe',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id,
        ]);

        // Mock current date to be in 2025/2026 Gasal (Sept 2025)
        Carbon::setTestNow(Carbon::create(2025, 9, 15));

        // Create Rate for 2025/2026 REGULER
        TuitionRate::create([
            'program_type' => 'REGULER',
            'academic_year' => '2025/2026',
            'amount' => 5000000,
            'active' => true,
        ]);

        // Create Template
        $template = InstallmentTemplate::create([
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'installments_count' => 4,
            'name' => '4x Test',
            'active' => true,
        ]);
        
        // Add 4 items
        for ($i = 1; $i <= 4; $i++) {
            $template->items()->create([
                'installment_no' => $i,
                'month_offset' => $i - 1,
                'day_of_month' => 20,
            ]);
        }

        // 2. Execute
        $plan = $this->service->generatePlan($student, $template);

        // 3. Assertions
        $this->assertNotNull($plan);
        $this->assertEquals('ACTIVE', $plan->status);
        $this->assertEquals(5000000, $plan->total_amount);
        $this->assertEquals('2025/2026', $plan->academic_year);
        $this->assertEquals('GASAL', $plan->term);

        // Check Installments
        $this->assertCount(4, $plan->installments);
        $this->assertEquals(1250000, $plan->installments->first()->amount);
        $this->assertEquals(5000000, $plan->installments->sum('amount'));
    }

    public function test_it_prevents_duplicate_active_plans()
    {
        // 1. Setup Data
        $admin = User::create([
            'name' => 'Admin',
            'email' => 'admin2@example.com',
            'password' => bcrypt('password'),
            'role' => 'admin'
        ]);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);
        $student = Student::create([
            'nim' => '123456',
            'name' => 'John Doe',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id,
        ]);

        Carbon::setTestNow(Carbon::create(2025, 9, 15));

        TuitionRate::create([
            'program_type' => 'REGULER',
            'academic_year' => '2025/2026',
            'amount' => 5000000,
            'active' => true,
        ]);

        $template = InstallmentTemplate::create([
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'installments_count' => 4,
            'name' => '4x Test',
            'active' => true,
        ]);
        $template->items()->create(['installment_no' => 1, 'month_offset' => 0, 'day_of_month' => 20]);

        // 2. First Call - Success
        $this->service->generatePlan($student, $template);

        // 3. Second Call - Expect Exception
        $this->expectException(\Exception::class);
        $this->expectExceptionMessage("Student already has an active SEMESTER payment plan");

        $this->service->generatePlan($student, $template);
    }
}
