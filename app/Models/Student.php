<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Student extends Model
{
    protected $fillable = [
        'user_id',
        'nim',
        'name',
        'class',
        'program_type',
        'start_term',
        'phone',
        'wallet_balance',
        'import_batch_id',
    ];

    public function user()
    {
        return $this->belongsTo(User::class);
    }

    public function importBatch()
    {
        return $this->belongsTo(ImportBatch::class);
    }

    public function paymentPlans()
    {
        return $this->hasMany(PaymentPlan::class);
    }

    public function payments()
    {
        return $this->hasMany(Payment::class);
    }
}
