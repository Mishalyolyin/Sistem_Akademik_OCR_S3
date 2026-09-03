<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\Installment;
use App\Models\InstallmentTemplate;
use App\Models\Payment;
use App\Models\PaymentPlan;
use App\Models\Student;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Facades\Bus;
use Tests\TestCase;

class PaymentSecurityTest extends TestCase
{
    use RefreshDatabase;

    private $user;
    private $student;
    private $plan;

    protected function setUp(): void
    {
        parent::setUp();
        Bus::fake(); // Prevent OCR jobs
        Storage::fake('public');

        $admin = User::create(['name' => 'Admin', 'email' => 'admin@e.com', 'password' => 'p', 'role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        $this->user = User::create([
            'name' => 'Mhs',
            'email' => 'mhs@example.com',
            'password' => bcrypt('password'),
            'role' => 'mahasiswa'
        ]);

        $this->student = Student::create([
            'user_id' => $this->user->id,
            'nim' => '123456',
            'name' => 'Mhs',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id,
        ]);

        $template = InstallmentTemplate::create(['name' => 'T1', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'installments_count' => 1]);
        
        $this->plan = PaymentPlan::create([
            'student_id' => $this->student->id,
            'installment_template_id' => $template->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'status' => 'ACTIVE'
        ]);
    }

    public function test_it_enforces_earliest_unpaid_installment_rule()
    {
        // Create 2 installments
        $inst1 = Installment::create(['payment_plan_id' => $this->plan->id, 'installment_no' => 1, 'due_date' => now(), 'amount' => 1000, 'status' => 'UNPAID']);
        $inst2 = Installment::create(['payment_plan_id' => $this->plan->id, 'installment_no' => 2, 'due_date' => now()->addMonth(), 'amount' => 1000, 'status' => 'UNPAID']);

        // Try to pay installment 2 first
        $response = $this->actingAs($this->user)->postJson('/mahasiswa/payments', [
            'installment_id' => $inst2->id,
            'amount' => 1000,
            'file' => UploadedFile::fake()->image('proof.jpg'),
        ]);

        $response->assertStatus(422)
            ->assertJsonFragment(['error' => 'Please pay earlier installments first.']);
            
        // Pay installment 1
        $response1 = $this->actingAs($this->user)->postJson('/mahasiswa/payments', [
            'installment_id' => $inst1->id,
            'amount' => 1000,
            'file' => UploadedFile::fake()->createWithContent('proof1.jpg', 'content-1'),
        ]);
        $response1->assertCreated();
        
        // Mark inst1 as PAID manually (since OCR job is faked)
        $inst1->update(['status' => 'PAID']);
        $this->assertEquals('PAID', $inst1->fresh()->status);

        // Now paying installment 2 should work
        $response2 = $this->actingAs($this->user)->postJson('/mahasiswa/payments', [
            'installment_id' => $inst2->id,
            'amount' => 1000,
            'file' => UploadedFile::fake()->createWithContent('proof2.jpg', 'content-2-different'),
        ]);
        
        if ($response2->status() !== 201) {
            dump($response2->json());
        }
        
        $response2->assertCreated();
    }

    public function test_it_prevents_duplicate_upload_via_idempotency_key()
    {
        $inst1 = Installment::create(['payment_plan_id' => $this->plan->id, 'installment_no' => 1, 'due_date' => now(), 'amount' => 1000, 'status' => 'UNPAID']);
        $key = 'unique-key-123';
        $file = UploadedFile::fake()->image('proof.jpg');

        // First request
        $response1 = $this->actingAs($this->user)->postJson('/mahasiswa/payments', [
            'installment_id' => $inst1->id,
            'amount' => 1000,
            'file' => $file,
        ], ['Idempotency-Key' => $key]);

        $response1->assertCreated();
        $paymentId = $response1->json('payment.id');

        // Second request with same key
        $response2 = $this->actingAs($this->user)->postJson('/mahasiswa/payments', [
            'installment_id' => $inst1->id,
            'amount' => 1000,
            'file' => $file,
        ], ['Idempotency-Key' => $key]);

        $response2->assertOk(); // 200 OK, not 201 Created
        $this->assertEquals($paymentId, $response2->json('payment.id'));
        $this->assertDatabaseCount('payments', 1);
    }

    public function test_it_detects_duplicate_file_content_hash()
    {
        $inst1 = Installment::create(['payment_plan_id' => $this->plan->id, 'installment_no' => 1, 'due_date' => now(), 'amount' => 1000, 'status' => 'UNPAID']);
        $inst2 = Installment::create(['payment_plan_id' => $this->plan->id, 'installment_no' => 2, 'due_date' => now(), 'amount' => 1000, 'status' => 'UNPAID']);
        
        // Same file content
        $file1 = UploadedFile::fake()->createWithContent('proof.jpg', 'fake-image-content');
        $file2 = UploadedFile::fake()->createWithContent('proof_duplicate.jpg', 'fake-image-content');

        // Upload first
        $this->actingAs($this->user)->postJson('/mahasiswa/payments', [
            'installment_id' => $inst1->id,
            'amount' => 1000,
            'file' => $file1,
        ])->assertCreated();
        
        $inst1->update(['status' => 'PAID']); // Allow next payment

        // Upload second (duplicate content) for next installment
        $response = $this->actingAs($this->user)->postJson('/mahasiswa/payments', [
            'installment_id' => $inst2->id,
            'amount' => 1000,
            'file' => $file2,
        ]);

        $response->assertStatus(422)
            ->assertJsonFragment(['error' => 'Duplicate payment proof detected.']);
    }
}
