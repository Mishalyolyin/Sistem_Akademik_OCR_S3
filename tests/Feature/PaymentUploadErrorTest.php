<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\Installment;
use App\Models\InstallmentTemplate;
use App\Models\PaymentPlan;
use App\Models\Student;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class PaymentUploadErrorTest extends TestCase
{
    use RefreshDatabase;

    public function test_payment_upload_without_file_shows_error()
    {
        // 1. Setup Student & Plan
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
            'academic_year' => '2025/2026', // Make sure this matches the SemesterService logic or mock it
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

        // Mock SemesterService to ensure dashboard renders correctly
        $this->mock(\App\Services\SemesterService::class, function ($mock) {
            $mock->shouldReceive('getActiveSemester')->andReturn([
                'academic_year' => '2025/2026',
                'term' => 'GASAL'
            ]);
        });

        // 2. Act: Submit without file
        $response = $this->actingAs($user)->post('/mahasiswa/payments', [
            'installment_id' => $installment->id,
            'amount' => 1000000,
            // file is missing
        ]);

        // 3. Assert Redirect & Session Errors
        $response->assertSessionHasErrors(['file']);
        
        // 4. Follow Redirect and check View content
        $response = $this->actingAs($user)->get(route('student.dashboard'));
        
        // Check for the error message container I added
        $response->assertSee('Gagal mengupload bukti pembayaran');
        // Check for the specific validation message (Laravel default or localized)
        // Usually "The file field is required." -> Now "Bukti pembayaran wajib diunggah."
        $response->assertSee('Bukti pembayaran wajib diunggah');
        // Check for the script that opens the modal
        $response->assertSee("document.getElementById('upload-modal').classList.remove('hidden')", false);
    }
}
