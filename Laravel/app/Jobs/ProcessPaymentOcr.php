<?php

namespace App\Jobs;

use App\Models\Payment;
use App\Models\SystemSetting;
use App\Services\OcrService;
use App\Services\PaymentAllocationService;
use App\Services\WhatsappService;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;
use Exception;

class ProcessPaymentOcr implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    /**
     * The number of times the job may be attempted.
     *
     * @var int
     */
    public $tries = 3;

    /**
     * The number of seconds to wait before retrying the job.
     *
     * @var array
     */
    public $backoff = [10, 30, 60];

    protected $paymentId;

    /**
     * Create a new job instance.
     *
     * @param int $paymentId
     */
    public function __construct($paymentId)
    {
        $this->paymentId = $paymentId;
    }

    /**
     * Execute the job.
     *
     * @param OcrService $ocrService
     * @param PaymentAllocationService $allocationService
     * @param WhatsappService $whatsappService
     * @return void
     */
    public function handle(OcrService $ocrService, PaymentAllocationService $allocationService, WhatsappService $whatsappService)
    {
        $payment = Payment::with(['student', 'installment.paymentPlan.installmentTemplate'])->find($this->paymentId);
        
        if (!$payment) {
            Log::channel('ocr')->warning("Payment #{$this->paymentId} not found during OCR job.");
            return;
        }

        try {
            Log::channel('ocr')->info("Starting OCR for Payment #{$payment->id}");

            // Assuming proof_file_path is stored relative to 'public' disk
            // We need absolute path for Python script
            $fullPath = storage_path('app/public/' . $payment->proof_file_path);
            
            // Determine Program Type for Bank Account Validation
            $programType = $payment->installment?->paymentPlan?->installmentTemplate?->program_type;
            $studentName = $payment->student->name ?? null;

            $result = $ocrService->processImage($fullPath, $programType, $studentName);
            
            $confidence = $result['confidence'] ?? 0;
            $ocrStatus = $result['verification_status'] ?? 'pending';
            $extractedAmount = $result['extracted_amount'] ?? 0;
            $bankName = $result['bank_name'] ?? null;
            $extractedDate = $result['extracted_date'] ?? null;

            $updateData = [
                'ocr_data' => $result,
                'bank_name' => $bankName,
                // In a real app, we might create a separate 'processed' image with bounding boxes
                'processed_file_path' => $payment->proof_file_path,
            ];

            // Tanggal pada struk/bukti transfer — disimpan sebagai data pelaporan saja,
            // tidak lagi memengaruhi status verifikasi (lihat ocr_processor.py).
            if ($extractedDate) {
                try {
                    $updateData['payment_proof_date'] = \Carbon\Carbon::parse($extractedDate);
                } catch (Exception $e) {
                    // Abaikan tanggal yang tidak bisa di-parse, tidak menggagalkan job.
                }
            }

            // Fetch Settings
            $thresholdPercent = SystemSetting::where('key', 'ocr_confidence_threshold')->value('value') ?? 80;
            $threshold = (float)$thresholdPercent / 100.0;
            
            $tolerance = SystemSetting::where('key', 'payment_tolerance_amount')->value('value') ?? 0;

            // Decision Logic
            $amountMismatch = false;
            
            // Verify Amount if extracted
            if ($extractedAmount > 0) {
                $diff = abs($payment->amount - $extractedAmount);
                if ($diff > $tolerance) {
                    $amountMismatch = true;
                    // Append flag to local result array (to be saved)
                    $result['flags'][] = "Amount mismatch: Expected " . number_format($payment->amount) . ", Found " . number_format($extractedAmount);
                    $updateData['ocr_data'] = $result; // Update the data to be saved
                    
                    // Update payment amount to the extracted amount so it reflects the actual receipt
                    $updateData['amount'] = $extractedAmount;
                    
                    Log::channel('ocr')->info("Payment #{$payment->id} Amount Mismatch. Diff: {$diff}. Updated amount to {$extractedAmount}.");
                }
            }

            if ($ocrStatus === 'rejected') {
                // Explicit rejection from OCR (Blacklist or Fraud or Expired Date)
                $updateData['status'] = Payment::STATUS_REJECTED;
                Log::channel('ocr')->info("Payment #{$payment->id} Rejected by OCR.");
            } elseif ($confidence >= $threshold && !$amountMismatch && $extractedAmount > 0) {
                $updateData['status'] = Payment::STATUS_AUTO_VERIFIED;
                $updateData['verified_at'] = now();
                Log::channel('ocr')->info("Payment #{$payment->id} Auto Verified. Confidence: {$confidence}");
            } elseif ($confidence < 0.1) {
                // Confidence < 10% implies no amount, no account, no name, and insufficient keywords found.
                // Likely not a payment proof or completely unreadable.
                $updateData['status'] = Payment::STATUS_REJECTED;
                $updateData['ocr_data'] = $result; // Ensure OCR data is saved
                // Add a flag explaining the rejection
                $ocrFlags = $result['flags'] ?? [];
                $ocrFlags[] = "Auto-Rejected: Low confidence ({$confidence}). Likely not a payment proof.";
                $updateData['ocr_data']['flags'] = $ocrFlags;
                
                Log::channel('ocr')->info("Payment #{$payment->id} Auto Rejected. Low Confidence: {$confidence}");
            } else {
                $updateData['status'] = Payment::STATUS_NEEDS_REVIEW;
                Log::channel('ocr')->info("Payment #{$payment->id} Needs Review. Confidence: {$confidence}, Mismatch: " . ($amountMismatch ? 'Yes' : 'No'));
            }

            // Guard: an admin may have manually approved/rejected this payment while the OCR
            // job was running (OCR runs async and can be delayed). A manual decision must
            // never be silently overwritten by a late-arriving automatic result.
            $freshStatus = Payment::where('id', $payment->id)->value('status');
            if (!in_array($freshStatus, [Payment::STATUS_PENDING, Payment::STATUS_NEEDS_REVIEW])) {
                Log::channel('ocr')->info("Payment #{$payment->id} already handled manually (status: {$freshStatus}) before OCR finished. Saving OCR data only, not overriding status.");
                unset($updateData['status'], $updateData['verified_at'], $updateData['amount']);
                $payment->update($updateData);
                return;
            }

            $payment->update($updateData);

            // Notification Logic
            $studentPhone = $payment->student->phone;
            $amountFmt = number_format($payment->amount, 0, ',', '.');
            
            if ($payment->status === Payment::STATUS_AUTO_VERIFIED) {
                // Notify Student (Success)
                if ($studentPhone) {
                    $msg = "Halo {$payment->student->name}, Pembayaran sebesar Rp {$amountFmt} telah BERHASIL diverifikasi otomatis oleh sistem. Terima kasih!";
                    $whatsappService->send($studentPhone, $msg);
                }
                
                Log::channel('ocr')->info("Payment #{$payment->id} Auto-Verified. Starting Allocation...");
                $allocationService->allocate($payment);
            } elseif ($payment->status === Payment::STATUS_REJECTED) {
                 // Notify Student (Rejected)
                if ($studentPhone) {
                    $msg = "Halo {$payment->student->name}, Pembayaran sebesar Rp {$amountFmt} DITOLAK sistem otomatis. Kemungkinan bukti tidak terbaca atau tidak valid. Silakan upload ulang bukti yang jelas.";
                    $whatsappService->send($studentPhone, $msg);
                }
            } elseif ($payment->status === Payment::STATUS_NEEDS_REVIEW) {
                // Notify Admin (Needs Review)
                $adminPhone = SystemSetting::where('key', 'admin_phone_notification')->value('value');
                if ($adminPhone) {
                    $msg = "Admin Alert: Ada pembayaran baru Rp {$amountFmt} dari {$payment->student->name} ({$payment->student->nim}) yang butuh tinjauan manual (OCR kurang yakin).";
                    $whatsappService->send($adminPhone, $msg);
                }
            }
            
            Log::channel('ocr')->info("OCR Completed for Payment #{$payment->id}. Status: {$updateData['status']}", $result);

        } catch (Exception $e) {
            Log::channel('ocr')->error("OCR Job Failed for Payment #{$payment->id}: " . $e->getMessage());
            
            // On final failure, mark as FAILED
            if ($this->attempts() >= $this->tries) {
                $payment->update(['status' => Payment::STATUS_FAILED]);
            }
            
            throw $e; // Re-throw to trigger retry mechanism
        }
    }
}
