<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\Payment;
use App\Models\Student;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class ReceiptTest extends TestCase
{
    use RefreshDatabase;

    public function test_student_can_download_receipt_for_verified_payment()
    {
        Storage::fake('public');

        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        $user = User::factory()->create(['role' => 'mahasiswa']);
        $student = Student::create(['user_id' => $user->id, 'nim' => '123456', 'name' => 'Test', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'wallet_balance' => 0, 'import_batch_id' => $batch->id]);
        
        $payment = Payment::create([
            'student_id' => $student->id,
            'amount' => 100000,
            'status' => 'VERIFIED',
            'proof_file_path' => 'test.jpg',
            'processed_file_path' => 'test.jpg',
            'payment_method' => 'TRANSFER',
        ]);

        $response = $this->actingAs($user)
            ->getJson("/api/payments/{$payment->id}/receipt");

        $response->assertStatus(200);
        $response->assertHeader('content-type', 'application/pdf');
    }

    public function test_student_cannot_download_receipt_for_unverified_payment()
    {
        Storage::fake('public');

        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        $user = User::factory()->create(['role' => 'mahasiswa']);
        $student = Student::create(['user_id' => $user->id, 'nim' => '123456', 'name' => 'Test', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'wallet_balance' => 0, 'import_batch_id' => $batch->id]);
        
        $payment = Payment::create([
            'student_id' => $student->id,
            'amount' => 100000,
            'status' => 'PENDING',
            'proof_file_path' => 'test.jpg',
            'processed_file_path' => 'test.jpg',
            'payment_method' => 'TRANSFER',
        ]);

        $response = $this->actingAs($user)
            ->getJson("/api/payments/{$payment->id}/receipt");

        $response->assertStatus(400);
    }

    public function test_student_cannot_download_other_students_receipt()
    {
        Storage::fake('public');

        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        $user1 = User::factory()->create(['role' => 'mahasiswa']);
        $student1 = Student::create(['user_id' => $user1->id, 'nim' => '123456', 'name' => 'Test1', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'wallet_balance' => 0, 'import_batch_id' => $batch->id]);
        
        $user2 = User::factory()->create(['role' => 'mahasiswa']);
        $student2 = Student::create(['user_id' => $user2->id, 'nim' => '654321', 'name' => 'Test2', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'wallet_balance' => 0, 'import_batch_id' => $batch->id]);

        $payment = Payment::create([
            'student_id' => $student1->id,
            'amount' => 100000,
            'status' => 'VERIFIED',
            'proof_file_path' => 'test.jpg',
            'processed_file_path' => 'test.jpg',
            'payment_method' => 'TRANSFER',
        ]);

        $response = $this->actingAs($user2)
            ->getJson("/api/payments/{$payment->id}/receipt");

        $response->assertStatus(403);
    }

    public function test_admin_can_download_any_receipt()
    {
        Storage::fake('public');

        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);
        
        $user = User::factory()->create(['role' => 'mahasiswa']);
        $student = Student::create(['user_id' => $user->id, 'nim' => '123456', 'name' => 'Test', 'program_type' => 'REGULER', 'start_term' => 'GASAL', 'wallet_balance' => 0, 'import_batch_id' => $batch->id]);
        
        $payment = Payment::create([
            'student_id' => $student->id,
            'amount' => 100000,
            'status' => 'VERIFIED',
            'proof_file_path' => 'test.jpg',
            'processed_file_path' => 'test.jpg',
            'payment_method' => 'TRANSFER',
        ]);

        $response = $this->actingAs($admin)
            ->getJson("/api/payments/{$payment->id}/receipt");

        $response->assertStatus(200);
    }
}
