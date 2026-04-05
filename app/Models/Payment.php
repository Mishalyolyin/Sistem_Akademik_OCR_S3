<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Payment extends Model
{
    // Status Constants
    const STATUS_PENDING = 'PENDING';
    const STATUS_AUTO_VERIFIED = 'AUTO_VERIFIED';
    const STATUS_NEEDS_REVIEW = 'NEEDS_REVIEW';
    const STATUS_VERIFIED = 'VERIFIED';
    const STATUS_REJECTED = 'REJECTED';
    const STATUS_FAILED = 'FAILED';

    protected $fillable = [
        'student_id',
        'payment_plan_id',
        'installment_id',
        'amount',
        'bank_name',
        'proof_file_path',
        'processed_file_path',
        'thumbnail_file_path',
        'ocr_data',
        'file_hash',
        'idempotency_key',
        'status',
        'paid_at',
        'verified_at',
        'verified_by_user_id',
    ];

    protected $casts = [
        'ocr_data' => 'array',
        'paid_at' => 'datetime',
        'verified_at' => 'datetime',
    ];

    public function student()
    {
        return $this->belongsTo(Student::class);
    }

    public function installment()
    {
        return $this->belongsTo(Installment::class);
    }

    public function paymentPlan()
    {
        return $this->belongsTo(PaymentPlan::class);
    }

    public function verifiedBy()
    {
        return $this->belongsTo(User::class, 'verified_by_user_id');
    }
}
