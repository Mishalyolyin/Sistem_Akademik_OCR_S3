<?php

namespace Tests\Feature;

use App\Models\Adjustment;
use App\Models\ImportBatch;
use App\Models\Student;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AdminAdjustmentPageTest extends TestCase
{
    use RefreshDatabase;

    protected $admin;
    protected $student;

    protected function setUp(): void
    {
        parent::setUp();
        
        $this->admin = User::factory()->create(['role' => 'admin']);
        $studentUser = User::factory()->create(['role' => 'mahasiswa']);
        
        $batch = ImportBatch::create(['file_name' => 'test.csv', 'imported_by_admin_id' => $this->admin->id]);

        $this->student = Student::create([
            'user_id' => $studentUser->id,
            'nim' => '12345678',
            'name' => 'John Doe',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'wallet_balance' => 0,
            'import_batch_id' => $batch->id,
        ]);
    }

    public function test_admin_can_view_adjustment_page()
    {
        $response = $this->actingAs($this->admin)->get(route('admin.adjustments'));
        $response->assertStatus(200);
        $response->assertSee('Adjustment / Koreksi');
    }

    public function test_admin_can_search_student()
    {
        $response = $this->actingAs($this->admin)->get(route('admin.adjustments', ['search' => '12345678']));
        
        $response->assertStatus(200);
        $response->assertSee('John Doe');
        $response->assertSee('12345678');
        $response->assertSee('Data Mahasiswa');
    }

    public function test_admin_search_not_found()
    {
        $response = $this->actingAs($this->admin)->get(route('admin.adjustments', ['search' => '999999']));
        
        $response->assertStatus(200);
        $response->assertSee('Mahasiswa dengan kata kunci');
        $response->assertSee('999999');
        $response->assertSee('tidak ditemukan');
    }
}
