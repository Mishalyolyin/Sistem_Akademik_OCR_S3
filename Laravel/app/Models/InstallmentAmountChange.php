<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class InstallmentAmountChange extends Model
{
    protected $fillable = [
        'installment_id',
        'old_amount',
        'new_amount',
        'reason',
        'admin_id',
    ];

    public function installment()
    {
        return $this->belongsTo(Installment::class);
    }

    public function admin()
    {
        return $this->belongsTo(User::class, 'admin_id');
    }
}
