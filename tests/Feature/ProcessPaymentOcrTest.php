<?php

namespace Tests\Feature;

use App\Jobs\ProcessPaymentOcr;
use App\Models\ImportBatch;
use App\Models\Installment;
use App\Models\InstallmentTemplate;
use App\Models\Payment;
use App\Models\PaymentPlan;
use App\Models\Student;
use App\Models\SystemSetting;
use App\Models\User;
use App\Services\OcrService;
use App\Services\PaymentAllocationService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use Mockery;
use Tests\TestCase;

class ProcessPaymentOcrTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        // Setup System Settings
        SystemSetting::create(['key' => 'ocr_confidence_threshold', 'value' => '80']);
        SystemSetting::create(['key' => 'payment_tolerance_amount', 'value' => '5000']);
        
        // Mock Storage
        Storage::fake('public');
    }

    public function test_ocr_job_auto_verifies_valid_payment()
    {
        // Arrange
        $user = User::factory()->create();
        $batch = ImportBatch::create([
            'file_name' => 'test.xlsx',
            'imported_by_admin_id' => $user->id,
            'summary_json' => []
        ]);

        $student = Student::create([
            'user_id' => $user->id,
            'import_batch_id' => $batch->id,
            'nim' => '12345678',
            'name' => 'Budi Santoso',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL'
        ]);
        
        $template = InstallmentTemplate::create([
            'name' => 'Template Reguler',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'installments_count' => 6
        ]);
        
        $plan = PaymentPlan::create([
            'student_id' => $student->id,
            'installment_template_id' => $template->id,
            'academic_year' => '2025/2026',
            'term' => '1',
            'total_amount' => 5000000
        ]);

        $installment = Installment::create([
            'payment_plan_id' => $plan->id,
            'installment_no' => 1,
            'due_date' => now()->addMonth(),
            'amount' => 100000,
            'status' => 'UNPAID'
        ]);
        
        $payment = Payment::create([
            'student_id' => $student->id,
            'payment_plan_id' => $plan->id,
            'installment_id' => $installment->id,
            'amount' => 100000,
            'proof_file_path' => 'proofs/valid.jpg',
            'status' => 'PENDING'
        ]);

        // Mock OcrService
        $ocrServiceMock = Mockery::mock(OcrService::class);
        $ocrServiceMock->shouldReceive('processImage')
            ->once()
            ->with(
                Mockery::on(function ($path) {
                    return str_contains($path, 'proofs/valid.jpg');
                }),
                'REGULER',
                'Budi Santoso'
            )
            ->andReturn([
                'confidence' => 0.9,
                'extracted_amount' => 100000,
                'verification_status' => 'verified',
                'flags' => []
            ]);

        // Mock PaymentAllocationService
        $allocationServiceMock = Mockery::mock(PaymentAllocationService::class);
        $allocationServiceMock->shouldReceive('allocate')
            ->once()
            ->with(Mockery::on(function($arg) use ($payment) {
                return $arg->id === $payment->id;
            }));

        // Act
        $job = new ProcessPaymentOcr($payment->id);
        $job->handle($ocrServiceMock, $allocationServiceMock);

        // Assert
        $payment->refresh();
        $this->assertEquals('AUTO_VERIFIED', $payment->status);
        $this->assertEquals(0.9, $payment->ocr_data['confidence']);
    }

    public function test_ocr_job_rejects_on_blacklist()
    {
        // Arrange
        $user = User::factory()->create();
        $batch = ImportBatch::create([
            'file_name' => 'test.xlsx',
            'imported_by_admin_id' => $user->id,
            'summary_json' => []
        ]);

        $student = Student::create([
            'user_id' => $user->id,
            'import_batch_id' => $batch->id,
            'nim' => '87654321',
            'name' => 'Andi',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL'
        ]);
        
        $payment = Payment::create([
            'student_id' => $student->id,
            'amount' => 100000,
            'proof_file_path' => 'proofs/blacklisted.jpg',
            'status' => 'PENDING'
        ]);

        $ocrServiceMock = Mockery::mock(OcrService::class);
        $ocrServiceMock->shouldReceive('processImage')
            ->once()
            ->andReturn([
                'confidence' => 0.9,
                'extracted_amount' => 100000,
                'verification_status' => 'rejected',
                'flags' => ['Blacklist keyword found: gagal']
            ]);

        $allocationServiceMock = Mockery::mock(PaymentAllocationService::class);
        $allocationServiceMock->shouldReceive('allocatePayment')->never();

        // Act
        $job = new ProcessPaymentOcr($payment->id);
        $job->handle($ocrServiceMock, $allocationServiceMock);

        // Assert
        $payment->refresh();
        $this->assertEquals('REJECTED', $payment->status);
        $this->assertStringContainsString('Blacklist keyword found', json_encode($payment->ocr_data));
    }
    
    public function test_ocr_job_needs_review_on_amount_mismatch()
    {
        // Arrange
        $user = User::factory()->create();
        $batch = ImportBatch::create([
            'file_name' => 'test.xlsx',
            'imported_by_admin_id' => $user->id,
            'summary_json' => []
        ]);

        $student = Student::create([
            'user_id' => $user->id,
            'import_batch_id' => $batch->id,
            'nim' => '99999999',
            'name' => 'Mismatch',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL'
        ]);
        
        $payment = Payment::create([
            'student_id' => $student->id,
            'amount' => 100000,
            'proof_file_path' => 'proofs/wrong_amount.jpg',
            'status' => 'PENDING'
        ]);

        $ocrServiceMock = Mockery::mock(OcrService::class);
        $ocrServiceMock->shouldReceive('processImage')
            ->once()
            ->andReturn([
                'confidence' => 0.95,
                'extracted_amount' => 50000, // Mismatch > 5000 tolerance
                'verification_status' => 'verified',
                'flags' => []
            ]);

        $allocationServiceMock = Mockery::mock(PaymentAllocationService::class);
        $allocationServiceMock->shouldReceive('allocatePayment')->never();

        // Act
        $job = new ProcessPaymentOcr($payment->id);
        $job->handle($ocrServiceMock, $allocationServiceMock);

        // Assert
        $payment->refresh();
        $this->assertEquals('NEEDS_REVIEW', $payment->status);
        $this->assertStringContainsString('Amount mismatch', json_encode($payment->ocr_data));
    }
}
