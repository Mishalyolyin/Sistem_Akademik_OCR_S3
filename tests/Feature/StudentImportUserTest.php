<?php

namespace Tests\Feature;

use App\Models\ImportBatch;
use App\Models\Student;
use App\Models\User;
use App\Imports\StudentsImport;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class StudentImportUserTest extends TestCase
{
    use RefreshDatabase;

    public function test_import_creates_user_account_with_default_password()
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        $import = new StudentsImport($batch->id, 'update');
        
        $nim = '999999';
        $rows = collect([
            [
                'nim' => $nim,
                'name' => 'New User Student',
                'class' => 'TI-1A',
                'program_type' => 'REGULER',
                'start_term' => 'GASAL',
                'phone' => '08123456789'
            ]
        ]);

        $import->collection($rows);

        // Assert Student created
        $this->assertDatabaseHas('students', [
            'nim' => $nim,
            'name' => 'New User Student'
        ]);

        // Assert User created
        $student = Student::where('nim', $nim)->first();
        $this->assertNotNull($student->user_id);
        
        $user = User::find($student->user_id);
        $this->assertNotNull($user);
        $this->assertEquals($nim . '@student.ac.id', $user->email);
        $this->assertEquals('mahasiswa', $user->role);
        
        // Assert Password is NIM
        $this->assertTrue(Hash::check($nim, $user->password));
    }

    public function test_import_update_links_existing_user_if_missing()
    {
        $admin = User::factory()->create(['role' => 'admin']);
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $admin->id]);

        // Create student WITHOUT user
        $nim = '888888';
        $student = Student::create([
            'nim' => $nim,
            'name' => 'Legacy Student',
            'class' => 'TI-OLD',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'import_batch_id' => $batch->id
        ]);

        $this->assertNull($student->user_id);

        $import = new StudentsImport($batch->id, 'update');
        
        $rows = collect([
            [
                'nim' => $nim,
                'name' => 'Legacy Student Updated',
                'class' => 'TI-NEW',
                'program_type' => 'REGULER',
                'start_term' => 'GASAL',
                'phone' => '08123456789'
            ]
        ]);

        $import->collection($rows);

        $student->refresh();
        $this->assertNotNull($student->user_id);
        $this->assertEquals('Legacy Student Updated', $student->name);
        
        $user = User::find($student->user_id);
        $this->assertEquals($nim . '@student.ac.id', $user->email);
        $this->assertTrue(Hash::check($nim, $user->password));
    }
}
