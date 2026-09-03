<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Installment extends Model
{
    protected $fillable = [
        'payment_plan_id',
        'installment_no',
        'due_date',
        'amount',
        'amount_paid',
        'status',
    ];

    protected $casts = [
        'due_date' => 'date',
        'amount' => 'decimal:2',
        'amount_paid' => 'decimal:2',
    ];

    public function paymentPlan()
    {
        return $this->belongsTo(PaymentPlan::class);
    }

    public function payments()
    {
        return $this->hasMany(Payment::class);
    }

    public function adjustments()
    {
        return $this->morphMany(Adjustment::class, 'adjustable');
    }
}
