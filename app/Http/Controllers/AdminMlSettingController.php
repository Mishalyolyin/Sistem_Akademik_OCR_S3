<?php

namespace App\Http\Controllers;

use App\Models\Payment;
use App\Models\SystemSetting;
use Illuminate\Http\Request;

class AdminMlSettingController extends Controller
{
    public function index()
    {
        $settings = SystemSetting::all()->keyBy('key');
        
        // ML Stats for "Learning" Status
        $verifiedCount = Payment::where('status', 'VERIFIED')->count();
        $rejectedCount = Payment::where('status', 'REJECTED')->count();
        $totalProcessed = Payment::whereNotNull('ocr_data')->count();
        $totalSamples = $verifiedCount + $rejectedCount;
        
        return view('admin.ml_settings.index', compact('settings', 'verifiedCount', 'rejectedCount', 'totalProcessed', 'totalSamples'));
    }

    public function downloadDataset()
    {
        try {
            $service = new \App\Services\DatasetExportService();
            $path = $service->export(1000); // Export up to 1000 samples
            return response()->download($path)->deleteFileAfterSend(true);
        } catch (\Exception $e) {
            return back()->with('error', 'Gagal mengunduh dataset: ' . $e->getMessage());
        }
    }

    public function store(Request $request)
    {
        $allowedKeys = [
            'ocr_confidence_threshold',
            'bank_account_reguler',
            'bank_account_rpl',
            'payment_tolerance_amount',
            'ocr_blacklist_keywords',
            'ocr_date_validation_days'
        ];

        foreach ($allowedKeys as $key) {
            if ($request->has($key)) {
                SystemSetting::updateOrCreate(
                    ['key' => $key],
                    ['value' => $request->input($key)]
                );
            }
        }

        return back()->with('success', 'Pengaturan berhasil diperbarui.');
    }
}
