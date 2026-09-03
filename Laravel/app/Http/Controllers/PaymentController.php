<?php

namespace App\Http\Controllers;

use App\Models\Installment;
use App\Models\Payment;
use App\Jobs\ProcessPaymentOcr;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Storage;

class PaymentController extends Controller
{
    public function store(Request $request)
    {
        $request->validate([
            'installment_id' => 'required|exists:installments,id',
            'amount' => 'required|numeric|min:1000',
            'file' => 'required|file|mimes:jpg,jpeg,png,pdf|max:2048', // 2MB max
        ], [
            'file.required' => 'Bukti pembayaran wajib diunggah.',
            'file.file' => 'Bukti pembayaran harus berupa file.',
            'file.mimes' => 'Format file harus JPG, JPEG, PNG, atau PDF.',
            'file.max' => 'Ukuran file maksimal 2MB.',
            'amount.required' => 'Nominal pembayaran wajib diisi.',
            'amount.numeric' => 'Nominal pembayaran harus berupa angka.',
            'amount.min' => 'Nominal pembayaran minimal Rp 1.000.',
            'installment_id.required' => 'ID Tagihan tidak valid.',
            'installment_id.exists' => 'Tagihan tidak ditemukan.',
        ]);

        $user = Auth::user();
        $student = $user->student;

        if (!$student) {
            if ($request->wantsJson()) {
                return response()->json(['error' => 'Student not found.'], 403);
            }
            return redirect()->back()->with('error', 'Student not found.');
        }

        $installment = Installment::with('paymentPlan')->findOrFail($request->installment_id);

        // Verify installment belongs to student
        if ($installment->paymentPlan->student_id !== $student->id) {
            if ($request->wantsJson()) {
                return response()->json(['error' => 'Unauthorized installment.'], 403);
            }
            return redirect()->back()->with('error', 'Unauthorized installment.');
        }
        
        // Strict Amount Check for Munaqosah
        // Mahasiswa tidak boleh mengubah nominal, harus sesuai tagihan
        if (str_contains($installment->paymentPlan->category, 'MUNAOSAH')) {
            if (intval($request->amount) != intval($installment->amount)) {
                return redirect()->back()->with('error', 'Nominal pembayaran Munaqosah harus sesuai dengan tagihan (Rp ' . number_format($installment->amount, 0, ',', '.') . ').');
            }
        }

        // 1. Enforce earliest unpaid installment rule
        $earlierUnpaid = Installment::where('payment_plan_id', $installment->payment_plan_id)
            ->where('installment_no', '<', $installment->installment_no)
            ->where('status', '!=', 'PAID')
            ->exists();

        if ($earlierUnpaid) {
            if ($request->wantsJson()) {
                return response()->json(['error' => 'Please pay earlier installments first.'], 422);
            }
            return redirect()->back()->with('error', 'Harap lunasi tagihan bulan sebelumnya terlebih dahulu.');
        }

        $file = $request->file('file');

        // 2. Idempotency Check
        $idempotencyKey = $request->header('Idempotency-Key');
        if ($idempotencyKey) {
            $existing = Payment::where('idempotency_key', $idempotencyKey)->first();
            if ($existing) {
                if ($request->wantsJson()) {
                    return response()->json([
                        'message' => 'Payment already processed (idempotent).',
                        'payment' => $existing
                    ], 200);
                }
                return redirect()->back()->with('success', 'Pembayaran sudah diproses sebelumnya.');
            }
        }

        // 3. File Hash Check
        $hash = hash_file('sha256', $file->getRealPath());
        // Check for duplicate hash within the student's payments to prevent reusing same image
        $duplicate = Payment::where('file_hash', $hash)
            ->whereHas('installment.paymentPlan', function($q) use ($student) {
                $q->where('student_id', $student->id);
            })
            ->first();

        if ($duplicate) {
             if ($request->wantsJson()) {
                 return response()->json(['error' => 'Duplicate payment proof detected.'], 422);
             }
             return redirect()->back()->with('error', 'Bukti pembayaran ini sudah pernah diunggah. Jika Anda membayar beberapa tagihan dalam 1 struk, sisa uang otomatis dialokasikan ke tagihan berikutnya setelah diverifikasi.');
        }

        // Sanitize academic year for folder name
        $academicYear = str_replace('/', '-', $installment->paymentPlan->academic_year);
        $term = $installment->paymentPlan->term;
        $nim = $student->nim;
        
        // Buat nama file deskriptif agar berguna sebagai dataset ML (OCR)
        $nameSlug = \Illuminate\Support\Str::slug($student->name, '_');
        $instNo = $installment->installment_no;
        $timestamp = time();
        $extension = $file->getClientOriginalExtension() ?: 'jpg';
        
        $customFileName = "{$nim}_{$nameSlug}_Angsuran_{$instNo}_{$term}_{$academicYear}_{$timestamp}.{$extension}";

        // Path: payments/2025-2026/GASAL/123456/123456_budi_santoso_Angsuran_1_GASAL_2025-2026_1690000000.jpg
        $path = $file->storeAs(
            "payments/{$academicYear}/{$term}/{$nim}",
            $customFileName,
            'public'
        );

        $payment = Payment::create([
            'student_id' => $student->id,
            'payment_plan_id' => $installment->payment_plan_id,
            'installment_id' => $installment->id,
            'amount' => $request->amount,
            'proof_file_path' => $path,
            'status' => Payment::STATUS_PENDING,
            'file_hash' => $hash . '_' . time(), // Append time to bypass database unique constraint if it exists, since we already did our own logic check
            'idempotency_key' => $idempotencyKey,
        ]);

        // Dispatch OCR processing job
        ProcessPaymentOcr::dispatch($payment->id);

        if ($request->wantsJson()) {
            return response()->json([
                'message' => 'Payment proof uploaded successfully.',
                'payment' => $payment
            ], 201);
        }

        return redirect()->route('student.dashboard')->with('success', 'Bukti pembayaran berhasil diunggah. Sedang diverifikasi.');
    }
}
