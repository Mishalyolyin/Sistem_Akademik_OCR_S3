<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\Installment;
use App\Models\InstallmentTemplate;
use App\Models\PaymentPlan;
use App\Models\Student;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Facades\Bus;
use Tests\TestCase;

class PaymentUploadTest extends TestCase
{
    use RefreshDatabase;

    public function test_student_can_upload_payment_proof()
    {
        Bus::fake();
        Storage::fake('public');

        // 1. Setup
        $admin = User::create(['name' => 'Admin', 'email' => 'admin@e.com', 'password' => 'p', 'role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        $user = User::create([
            'name' => 'Mhs',
            'email' => 'mhs@example.com',
            'password' => bcrypt('password'),
            'role' => 'mahasiswa'
        ]);

        $student = Student::create([
            'user_id' => $user->id,
            'nim' => '123456',
            'name' => 'Mhs',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id,
        ]);

        $template = InstallmentTemplate::create(['name' => 'T1', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'installments_count' => 1]);
        
        $plan = PaymentPlan::create([
            'student_id' => $student->id,
            'installment_template_id' => $template->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'status' => 'ACTIVE'
        ]);

        $installment = Installment::create([
            'payment_plan_id' => $plan->id,
            'installment_no' => 1,
            'due_date' => now()->addMonth(),
            'amount' => 1000000,
            'status' => 'UNPAID'
        ]);

        $file = UploadedFile::fake()->image('proof.jpg');

        // 2. Act
        $response = $this->actingAs($user)->postJson('/mahasiswa/payments', [
            'installment_id' => $installment->id,
            'amount' => 1000000,
            'file' => $file,
        ]);

        // 3. Assert
        $response->assertCreated();
        $this->assertDatabaseHas('payments', [
            'installment_id' => $installment->id,
            'amount' => 1000000,
            'status' => 'PENDING',
        ]);

        // Check file exists
        // Path: payments/2025-2026/GASAL/123456/hashName.jpg
        // Note: 2025/2026 is sanitized to 2025-2026
        $path = "payments/2025-2026/GASAL/123456/" . $file->hashName();
        Storage::disk('public')->assertExists($path);
    }

    public function test_student_cannot_upload_for_other_student_installment()
    {
        // 1. Setup Owner
        $admin = User::create(['name' => 'Admin', 'email' => 'admin@e.com', 'password' => 'p', 'role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        $ownerUser = User::create(['name' => 'Owner', 'email' => 'o@e.com', 'password' => 'p', 'role' => 'mahasiswa']);
        $ownerStudent = Student::create(['user_id' => $ownerUser->id, 'nim' => '111', 'name' => 'O', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'import_batch_id' => $batch->id]);
        $template = InstallmentTemplate::create(['name' => 'T1', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'installments_count' => 1]);
        $plan = PaymentPlan::create(['student_id' => $ownerStudent->id, 'installment_template_id' => $template->id, 'academic_year' => '2025/2026', 'term' => 'GASAL', 'total_amount' => 10000, 'status' => 'ACTIVE']);
        $installment = Installment::create(['payment_plan_id' => $plan->id, 'installment_no' => 1, 'due_date' => now(), 'amount' => 10000]);

        // 2. Setup Attacker
        $attackerUser = User::create(['name' => 'Attacker', 'email' => 'a@e.com', 'password' => 'p', 'role' => 'mahasiswa']);
        $attackerStudent = Student::create(['user_id' => $attackerUser->id, 'nim' => '222', 'name' => 'A', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'import_batch_id' => $batch->id]);

        $file = UploadedFile::fake()->image('proof.jpg');

        // 3. Act
        $response = $this->actingAs($attackerUser)->postJson('/mahasiswa/payments', [
            'installment_id' => $installment->id, // Owner's installment
            'amount' => 10000,
            'file' => $file,
        ]);

        // 4. Assert
        $response->assertStatus(403);
    }
}
