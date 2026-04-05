<?php

namespace Tests\Feature;

use App\Jobs\ProcessPaymentOcr;
use App\Models\ImportBatch;
use App\Models\InstallmentTemplate;
use App\Models\Payment;
use App\Models\Student;
use App\Models\User;
use App\Services\OcrService;
use App\Services\PaymentAllocationService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Bus;
use Illuminate\Support\Facades\Storage;
use Laravel\Sanctum\Sanctum;
use Mockery;
use Tests\TestCase;

class OcrPipelineTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        // Additional setup if needed
    }

    private function createPaymentScenario()
    {
        $admin = User::create(['name' => 'Admin', 'email' => 'admin@e.com', 'password' => 'p', 'role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);
        
        $user = User::factory()->create(['role' => 'mahasiswa']);
        $student = Student::create([
            'user_id' => $user->id,
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
             'installments_count' => 1
        ]);

        $plan = $student->paymentPlans()->create([
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'installment_template_id' => $template->id,
        ]);
        
        $installment = $plan->installments()->create([
            'installment_no' => 1,
            'due_date' => now()->addMonth(),
            'amount' => 1000000,
            'status' => 'UNPAID',
        ]);

        return [$user, $installment, $student];
    }

    public function test_upload_dispatches_ocr_job()
    {
        Bus::fake();
        Storage::fake('public');

        [$user, $installment, $student] = $this->createPaymentScenario();

        $file = UploadedFile::fake()->image('proof.jpg');
        
        Sanctum::actingAs($user);

        $response = $this->postJson('/api/payments', [
            'installment_id' => $installment->id,
            'amount' => 1000000,
            'file' => $file,
        ]);

        $response->assertStatus(201);
        
        $paymentId = $response->json('payment.id');

        // Assert Job Dispatched
        Bus::assertDispatched(ProcessPaymentOcr::class, function ($job) use ($paymentId) {
            // Check private property via reflection if needed, or just class type
            // But ProcessPaymentOcr has public property? No, protected.
            // We can check constructor args if we could inspect closure, 
            // but standard assertion is class type.
            // However, we can use a closure to inspect the job instance
            $reflection = new \ReflectionClass($job);
            $property = $reflection->getProperty('paymentId');
            $property->setAccessible(true);
            return $property->getValue($job) === $paymentId;
        });
    }

    public function test_ocr_job_updates_status_to_auto_verified()
    {
        [$user, $installment, $student] = $this->createPaymentScenario();
        
        $payment = Payment::create([
             'student_id' => $student->id,
             'installment_id' => $installment->id,
             'amount' => 100000,
             'proof_file_path' => 'payments/dummy.jpg',
             'status' => Payment::STATUS_PENDING
        ]);

        // Mock OcrService
        $ocrService = Mockery::mock(OcrService::class);
        $ocrService->shouldReceive('processImage')
            ->once()
            ->andReturn([
                'confidence' => 0.95,
                'extracted_amount' => 100000
            ]);

        // Mock AllocationService
        $allocationService = Mockery::mock(PaymentAllocationService::class);
        $allocationService->shouldReceive('allocate')
            ->once()
            ->with(Mockery::on(function($arg) use ($payment) {
                return $arg->id === $payment->id;
            }));

        // Run Job
        $job = new ProcessPaymentOcr($payment->id);
        $job->handle($ocrService, $allocationService);

        // Assert
        $payment->refresh();
        $this->assertEquals(Payment::STATUS_AUTO_VERIFIED, $payment->status);
        $this->assertNotNull($payment->verified_at);
        $this->assertArrayHasKey('confidence', $payment->ocr_data);
    }
    
    public function test_ocr_job_updates_status_to_needs_review()
    {
        [$user, $installment, $student] = $this->createPaymentScenario();
        
        $payment = Payment::create([
             'student_id' => $student->id,
             'installment_id' => $installment->id,
             'amount' => 100000,
             'proof_file_path' => 'payments/dummy.jpg',
             'status' => Payment::STATUS_PENDING
        ]);

        // Mock OcrService
        $ocrService = Mockery::mock(OcrService::class);
        $ocrService->shouldReceive('processImage')
            ->once()
            ->andReturn([
                'confidence' => 0.40,
                'extracted_amount' => 100000
            ]);

        // Mock AllocationService
        $allocationService = Mockery::mock(PaymentAllocationService::class);
        $allocationService->shouldReceive('allocate')->never();

        // Run Job
        $job = new ProcessPaymentOcr($payment->id);
        $job->handle($ocrService, $allocationService);

        // Assert
        $payment->refresh();
        $this->assertEquals(Payment::STATUS_NEEDS_REVIEW, $payment->status);
        $this->assertNull($payment->verified_at);
    }
}
