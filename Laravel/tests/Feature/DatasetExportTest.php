<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\Payment;
use App\Models\Student;
use App\Models\User;
use App\Services\DatasetExportService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;
use ZipArchive;

class DatasetExportTest extends TestCase
{
    use RefreshDatabase;

    public function test_export_service_generates_zip_with_csv_and_images()
    {
        Storage::fake('public');

        // Setup data
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);
        
        $user = User::factory()->create(['role' => 'mahasiswa']);
        $student = Student::create([
            'user_id' => $user->id, 
            'nim' => '123456', 
            'name' => 'Test', 
            'program_type' => 'REGULER', 
            'start_term' => 'GASAL', 
            'wallet_balance' => 0,
            'import_batch_id' => $batch->id
        ]);
        
        // Create dummy file
        $file = UploadedFile::fake()->image('payment.jpg');
        $path = $file->store('payments', 'public');

        Payment::create([
            'student_id' => $student->id,
            'amount' => 100000,
            'status' => 'VERIFIED', // Must be verified
            'proof_file_path' => $path,
            'processed_file_path' => $path,
            'payment_method' => 'TRANSFER',
        ]);

        $service = new DatasetExportService();
        $zipPath = $service->export();

        $this->assertFileExists($zipPath);

        // Verify ZIP content
        $zip = new ZipArchive;
        $res = $zip->open($zipPath);
        $this->assertTrue($res === TRUE);
        
        // Check for CSV
        $this->assertNotFalse($zip->locateName('labels.csv'));
        $csvContent = $zip->getFromName('labels.csv');
        $this->assertStringContainsString(basename($path), $csvContent);
        $this->assertStringContainsString('100000', $csvContent);

        // Check for Image
        $this->assertNotFalse($zip->locateName('images/' . basename($path)));

        $zip->close();
    }

    public function test_command_runs_successfully()
    {
        Storage::fake('public');

        // Setup data to avoid "No verified payments" error
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        $user = User::factory()->create(['role' => 'mahasiswa']);
        $student = Student::create([
            'user_id' => $user->id, 
            'nim' => '123456', 
            'name' => 'Test', 
            'program_type' => 'REGULER', 
            'start_term' => 'GASAL', 
            'wallet_balance' => 0,
            'import_batch_id' => $batch->id
        ]);
        
        $file = UploadedFile::fake()->image('payment.jpg');
        $path = $file->store('payments', 'public');

        Payment::create([
            'student_id' => $student->id,
            'amount' => 100000,
            'status' => 'VERIFIED',
            'proof_file_path' => $path,
            'processed_file_path' => $path,
            'payment_method' => 'TRANSFER',
        ]);

        $this->artisan('app:export-dataset')
             ->assertExitCode(0)
             ->expectsOutputToContain('Dataset exported successfully!');
    }
}
