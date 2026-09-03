<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class VerificationLog extends Model
{
    protected $fillable = [
        'payment_id',
        'admin_id',
        'action',
        'notes',
    ];

    public function payment()
    {
        return $this->belongsTo(Payment::class);
    }

    public function admin()
    {
        return $this->belongsTo(User::class, 'admin_id');
    }
}
