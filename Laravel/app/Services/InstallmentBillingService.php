<?php

namespace App\Services;

use App\Models\Installment;
use App\Models\InstallmentAmountChange;
use App\Models\User;
use Illuminate\Support\Facades\DB;

class InstallmentBillingService
{
    /**
     * Change the bill amount (tagihan) of a single installment. Unlike
     * AdjustmentService (which adjusts amount_paid — money received),
     * this changes what's owed, and keeps its own audit trail so the two
     * kinds of financial history never get mixed up.
     */
    public function updateAmount(Installment $installment, float $newAmount, string $reason, User $admin): InstallmentAmountChange
    {
        return DB::transaction(function () use ($installment, $newAmount, $reason, $admin) {
            $locked = Installment::lockForUpdate()->find($installment->id);
            $oldAmount = $locked->amount;

            $locked->amount = $newAmount;
            // Recompute status against the NEW amount using the EXISTING amount_paid.
            // amount_paid itself is never touched here — refunding an overage (if the
            // new amount is now lower than what's already been paid) is a separate
            // concern handled through the existing Adjustment/wallet tooling.
            if ($locked->amount_paid >= $locked->amount) {
                $locked->status = 'PAID';
            } elseif ($locked->amount_paid > 0) {
                $locked->status = 'PARTIAL';
            } else {
                $locked->status = 'UNPAID';
            }
            $locked->save();

            // Keep the plan's total_amount consistent with what's actually billed.
            $plan = $locked->paymentPlan;
            $plan->total_amount = $plan->installments()->sum('amount');
            $plan->save();

            return InstallmentAmountChange::create([
                'installment_id' => $locked->id,
                'old_amount' => $oldAmount,
                'new_amount' => $newAmount,
                'reason' => $reason,
                'admin_id' => $admin->id,
            ]);
        });
    }
}
