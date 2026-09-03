<?php

namespace App\Services;

use App\Models\Payment;
use App\Models\User;
use App\Models\VerificationLog;
use Illuminate\Support\Facades\DB;

class PaymentVerificationService
{
    protected $allocationService;
    protected $whatsappService;

    public function __construct(PaymentAllocationService $allocationService, WhatsappService $whatsappService)
    {
        $this->allocationService = $allocationService;
        $this->whatsappService = $whatsappService;
    }

    public function verify(Payment $payment, User $admin, string $action, ?string $notes = null)
    {
        return DB::transaction(function () use ($payment, $admin, $action, $notes) {
            // Ensure we are working with fresh data and locking isn't strictly needed if we check status,
            // but for high concurrency, lockForUpdate is better.
            // However, since we passed the model, we can't easily lock it without reloading.
            // Let's reload and lock.
            $payment = Payment::lockForUpdate()->find($payment->id);

            if (!in_array($payment->status, [Payment::STATUS_PENDING, Payment::STATUS_NEEDS_REVIEW])) {
                throw new \Exception("Payment status is {$payment->status}, cannot be verified.");
            }

            VerificationLog::create([
                'payment_id' => $payment->id,
                'admin_id' => $admin->id,
                'action' => $action,
                'notes' => $notes,
            ]);

            $payment->load('student');
            $studentPhone = $payment->student->phone;
            $amountFmt = number_format($payment->amount, 0, ',', '.');

            if ($action === 'approve') {
                $payment->update([
                    'status' => Payment::STATUS_VERIFIED,
                    'verified_at' => now(),
                    'verified_by_user_id' => $admin->id,
                ]);
                $this->allocationService->allocate($payment);
                
                if ($studentPhone) {
                    $this->whatsappService->send($studentPhone, "Halo {$payment->student->name}, Pembayaran manual Anda sebesar Rp {$amountFmt} telah DIVERIFIKASI oleh Admin. Terima kasih!");
                }
            } elseif ($action === 'reject') {
                $payment->update([
                    'status' => Payment::STATUS_REJECTED,
                    'verified_at' => now(),
                    'verified_by_user_id' => $admin->id,
                ]);
                
                if ($studentPhone) {
                    $reason = $notes ? "Alasan: {$notes}" : "Mohon cek kembali bukti Anda.";
                    $this->whatsappService->send($studentPhone, "Halo {$payment->student->name}, Pembayaran sebesar Rp {$amountFmt} DITOLAK oleh Admin. {$reason}");
                }
            } elseif ($action === 'request_reupload') {
                // Treated as REJECTED for the payment record itself
                $payment->update([
                    'status' => Payment::STATUS_REJECTED,
                    'verified_at' => now(),
                    'verified_by_user_id' => $admin->id,
                ]);
                
                if ($studentPhone) {
                     $reason = $notes ? "Alasan: {$notes}" : "";
                     $this->whatsappService->send($studentPhone, "Halo {$payment->student->name}, Mohon UPLOAD ULANG bukti pembayaran Rp {$amountFmt}. {$reason}");
                }
            } else {
                throw new \Exception("Invalid action: $action");
            }

            return $payment;
        });
    }
}
