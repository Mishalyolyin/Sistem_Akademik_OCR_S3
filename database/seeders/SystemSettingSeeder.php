<?php

namespace Database\Seeders;

use App\Models\SystemSetting;
use Illuminate\Database\Seeder;

class SystemSettingSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $settings = [
            [
                'key' => 'ocr_confidence_threshold',
                'value' => '80',
                'group' => 'ocr',
                'type' => 'number',
                'description' => 'Ambang batas keyakinan (%) untuk verifikasi otomatis.'
            ],
            [
                'key' => 'bank_account_reguler',
                'value' => '',
                'group' => 'payment',
                'type' => 'text',
                'description' => 'Nomor Rekening untuk mahasiswa Reguler (Validasi tujuan transfer).'
            ],
            [
                'key' => 'bank_account_rpl',
                'value' => '',
                'group' => 'payment',
                'type' => 'text',
                'description' => 'Nomor Rekening untuk mahasiswa RPL (Validasi tujuan transfer).'
            ],
            [
                'key' => 'payment_tolerance_amount',
                'value' => '0',
                'group' => 'payment',
                'type' => 'number',
                'description' => 'Toleransi selisih nominal pembayaran (misal biaya admin).'
            ],
            [
                'key' => 'ocr_blacklist_keywords',
                'value' => 'GAGAL,PENDING,DIBATALKAN,UNSUCCESSFUL,ERROR',
                'group' => 'ocr',
                'type' => 'textarea',
                'description' => 'Kata kunci terlarang yang akan menyebabkan penolakan otomatis (pisahkan dengan koma).'
            ],
            [
                'key' => 'ocr_date_validation_days',
                'value' => '30',
                'group' => 'ocr',
                'type' => 'number',
                'description' => 'Batas maksimum umur bukti pembayaran (hari).'
            ],
        ];

        foreach ($settings as $setting) {
            SystemSetting::updateOrCreate(
                ['key' => $setting['key']],
                $setting
            );
        }
    }
}
