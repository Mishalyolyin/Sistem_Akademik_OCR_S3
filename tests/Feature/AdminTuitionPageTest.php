<?php

namespace Tests\Feature;

use App\Models\User;
use App\Models\TuitionRate;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AdminTuitionPageTest extends TestCase
{
    use RefreshDatabase;

    protected $admin;

    protected function setUp(): void
    {
        parent::setUp();
        $this->admin = User::factory()->create(['role' => 'admin']);
    }

    public function test_admin_can_view_tuition_page()
    {
        $response = $this->actingAs($this->admin)->get(route('admin.tuition'));
        
        $response->assertStatus(200);
        $response->assertSee('Atur Biaya Kuliah');
        $response->assertSee('REGULER');
        $response->assertSee('RPL');
    }

    public function test_admin_can_create_tuition_rate()
    {
        $response = $this->actingAs($this->admin)->post(route('admin.tuition.store'), [
            'program_type' => 'REGULER',
            'academic_year' => '2026/2027',
            'amount' => 5000000,
            'description' => 'Test Rate',
            'active' => '1'
        ]);

        $response->assertRedirect();
        $this->assertDatabaseHas('tuition_rates', [
            'program_type' => 'REGULER',
            'academic_year' => '2026/2027',
            'amount' => 5000000,
        ]);
    }

    public function test_admin_can_update_existing_tuition_rate()
    {
        TuitionRate::create([
            'program_type' => 'REGULER',
            'academic_year' => '2025/2026',
            'amount' => 4000000,
        ]);

        $response = $this->actingAs($this->admin)->post(route('admin.tuition.store'), [
            'program_type' => 'REGULER',
            'academic_year' => '2025/2026',
            'amount' => 6000000, // Updated amount
        ]);

        $response->assertRedirect();
        $this->assertDatabaseHas('tuition_rates', [
            'program_type' => 'REGULER',
            'academic_year' => '2025/2026',
            'amount' => 6000000,
        ]);
    }

    public function test_admin_can_delete_tuition_rate()
    {
        $rate = TuitionRate::create([
            'program_type' => 'RPL',
            'academic_year' => '2025/2026',
            'amount' => 4500000,
        ]);

        $response = $this->actingAs($this->admin)->delete(route('admin.tuition.destroy', $rate));

        $response->assertRedirect();
        $this->assertDatabaseMissing('tuition_rates', ['id' => $rate->id]);
    }
}
