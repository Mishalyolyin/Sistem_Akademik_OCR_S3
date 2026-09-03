<?php

namespace Tests\Feature;

use App\Models\User;
use App\Models\SystemSetting;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AdminMlSettingPageTest extends TestCase
{
    use RefreshDatabase;

    protected $admin;

    protected function setUp(): void
    {
        parent::setUp();
        
        $this->admin = User::factory()->create(['role' => 'admin']);
    }

    public function test_admin_can_view_ml_settings_page()
    {
        $response = $this->actingAs($this->admin)->get(route('admin.ml_settings'));
        $response->assertStatus(200);
        $response->assertSee('Pengaturan Machine Learning');
        $response->assertSee('Konfigurasi OCR');
    }

    public function test_admin_can_save_ml_settings()
    {
        $data = [
            'ocr_confidence_threshold' => 85,
            'ocr_date_validation_days' => 5,
            'ocr_blacklist_keywords' => 'failed,rejected',
            'payment_tolerance_amount' => 500,
            'bank_account_reguler' => '1234567890',
            'bank_account_rpl' => '0987654321',
        ];

        $response = $this->actingAs($this->admin)->post(route('admin.ml_settings.store'), $data);

        $response->assertRedirect();
        $response->assertSessionHas('success');

        foreach ($data as $key => $value) {
            $this->assertDatabaseHas('system_settings', [
                'key' => $key,
                'value' => (string) $value,
            ]);
        }
    }
}
