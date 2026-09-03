<?php

namespace App\Http\Controllers;

use App\Models\Payment;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

class DevOcrController extends Controller
{
    public function index()
    {
        $payments = Payment::with('student')
            ->whereNotNull('ocr_data')
            ->orderBy('id', 'desc')
            ->paginate(20);

        return view('developer.ocr.index', compact('payments'));
    }

    public function show($id)
    {
        $payment = Payment::with(['student', 'paymentPlan'])->findOrFail($id);

        if (!$payment->ocr_data) {
            abort(404, 'Tidak ada data OCR untuk transaksi ini.');
        }

        $pathInfo = pathinfo($payment->proof_file_path);
        $dirname = $pathInfo['dirname'] === '.' ? '' : $pathInfo['dirname'] . '/';
        $filename = $pathInfo['filename'];
        $extension = isset($pathInfo['extension']) ? '.' . $pathInfo['extension'] : '';

        // Pastikan path untuk file storage lokal benar (gunakan format dari Laravel disk public)
        $images = [
            'original' => Storage::disk('public')->url($payment->proof_file_path),
            'grayscale' => Storage::disk('public')->url($dirname . $filename . '_1_grayscale' . $extension),
            'threshold' => Storage::disk('public')->url($dirname . $filename . '_2_threshold' . $extension),
        ];

        return view('developer.ocr.show', compact('payment', 'images'));
    }
}
