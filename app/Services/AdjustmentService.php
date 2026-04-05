<?php

namespace App\Services;

use App\Models\Adjustment;
use App\Models\Installment;
use App\Models\Student;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Illuminate\Database\Eloquent\Model;

class AdjustmentService
{
    /**
     * Create an adjustment for a model (Installment or Student/Wallet).
     * 
     * @param Model $adjustable
     * @param float $amount
     * @param string $reason
     * @param User $admin
     * @return Adjustment
     * @throws \Exception
     */
    public function createAdjustment(Model $adjustable, float $amount, string $reason, User $admin): Adjustment
    {
        return DB::transaction(function () use ($adjustable, $amount, $reason, $admin) {
            // Lock the adjustable record
            if ($adjustable instanceof Installment) {
                $record = Installment::lockForUpdate()->find($adjustable->id);
                // Update amount_paid
                // amount > 0: Paid more (credit)
                // amount < 0: Refund/Correction (debit)
                // Wait, if I "adjust" +1000, does it mean "Student paid 1000 more"? Yes.
                // If I "adjust" -1000, does it mean "Student paid 1000 less" (Refund)? Yes.
                
                $record->amount_paid += $amount;
                
                // Update status if needed
                if ($record->amount_paid >= $record->amount) {
                    $record->status = 'PAID';
                } elseif ($record->amount_paid > 0) {
                    $record->status = 'PARTIAL';
                } else {
                    $record->status = 'UNPAID';
                }
                $record->save();

            } elseif ($adjustable instanceof Student) {
                $record = Student::lockForUpdate()->find($adjustable->id);
                // Update wallet_balance
                $record->wallet_balance += $amount;
                $record->save();
            } else {
                throw new \Exception("Unsupported adjustable type: " . get_class($adjustable));
            }

            return Adjustment::create([
                'adjustable_type' => get_class($adjustable),
                'adjustable_id' => $adjustable->id,
                'amount' => $amount,
                'reason' => $reason,
                'admin_id' => $admin->id,
            ]);
        });
    }
}
