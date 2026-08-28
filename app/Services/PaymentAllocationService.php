<?php

namespace App\Services;

use App\Models\Payment;
use App\Models\Installment;
use App\Models\Student;
use App\Models\SystemSetting;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class PaymentAllocationService
{
    /**
     * Allocate verified payment to installments.
     * 
     * @param Payment $payment
     * @return void
     * @throws \Exception
     */
    public function allocate(Payment $payment): void
    {
        DB::transaction(function () use ($payment) {
            // 1. Lock Payment & Refresh with Relationships
            $payment = Payment::lockForUpdate()->find($payment->id);
            $payment->load('paymentPlan.student');

            // 2. Validation
            if ($payment->allocated_at) {
                Log::info("Payment {$payment->id} already allocated at {$payment->allocated_at}");
                return;
            }

            if (!in_array($payment->status, ['VERIFIED', 'AUTO_VERIFIED'])) {
                Log::warning("Payment {$payment->id} cannot be allocated. Status: {$payment->status}");
                return;
            }

            $remainingAmount = $payment->amount;
            $student = $payment->paymentPlan?->student;

            if (!$student) {
                // Fallback: try to get student from payment directly if plan is missing
                $student = $payment->student;
                if (!$student) {
                    throw new \Exception("Cannot allocate payment {$payment->id}: Student not found.");
                }
            }

            // 3. Priority 1: Targeted Installment
            if ($payment->installment_id) {
                $targetInstallment = Installment::lockForUpdate()->find($payment->installment_id);

                if ($targetInstallment) {
                    if ($targetInstallment->status !== 'PAID') {
                        $tolerance = (float) (SystemSetting::where('key', 'payment_tolerance_amount')->value('value') ?? 0);
                        $remainingAmount = $this->payInstallment($targetInstallment, $remainingAmount, $tolerance);
                    } else {
                        // The installment this payment was verified against is already PAID.
                        // Do NOT silently redirect this payment's money to a different
                        // installment or the wallet — that makes the money untraceable.
                        // Flag it for a human to look at instead.
                        Log::warning("Payment {$payment->id}: target installment {$targetInstallment->id} is already PAID. Flagging for manual review instead of auto-reallocating funds.");
                        $payment->status = Payment::STATUS_NEEDS_REVIEW;
                        $payment->save();
                        return;
                    }
                }
            }

            // 4. Priority 2: Earliest Unpaid/Partial Installments (FIFO)
            if ($remainingAmount > 0) {
                // Get all unpaid/partial installments for this plan, ordered by due_date
                $unpaidInstallments = Installment::where('payment_plan_id', $payment->payment_plan_id)
                    ->whereIn('status', ['UNPAID', 'PARTIAL', 'OVERDUE'])
                    ->orderBy('due_date', 'asc')
                    ->orderBy('installment_no', 'asc')
                    ->lockForUpdate()
                    ->get();

                foreach ($unpaidInstallments as $installment) {
                    if ($remainingAmount <= 0) break;
                    $remainingAmount = $this->payInstallment($installment, $remainingAmount);
                }
            }

            // 5. Priority 3: Deposit to Wallet (Overpayment)
            if ($remainingAmount > 0) {
                $student->wallet_balance += $remainingAmount;
                $student->save();
                Log::info("Payment {$payment->id}: Overpayment of {$remainingAmount} added to student {$student->id} wallet.");
            }

            // 6. Mark as Allocated
            $payment->allocated_at = now();
            $payment->save();
        });
    }

    /**
     * Apply amount to an installment.
     * Returns remaining amount.
     *
     * @param float $discardTolerance Leftover up to this amount (e.g. a bank/kode-unik fee
     *                                the institution never actually receives) is written off
     *                                instead of being carried to the next installment or wallet.
     */
    private function payInstallment(Installment $installment, float $amount, float $discardTolerance = 0): float
    {
        $billAmount = $installment->amount;
        $paidSoFar = $installment->amount_paid;
        $outstanding = $billAmount - $paidSoFar;

        if ($outstanding <= 0) {
            return $amount; // Already paid fully
        }

        $toPay = min($amount, $outstanding);

        $installment->amount_paid += $toPay;
        $installment->status = ($installment->amount_paid >= $billAmount) ? 'PAID' : 'PARTIAL';
        $installment->save();

        $leftover = $amount - $toPay;

        if ($leftover > 0 && $leftover <= $discardTolerance) {
            Log::info("Installment {$installment->id}: discarding Rp{$leftover} leftover within tolerance (kode unik/biaya admin), not carried to next installment or wallet.");
            return 0;
        }

        return $leftover;
    }
}
