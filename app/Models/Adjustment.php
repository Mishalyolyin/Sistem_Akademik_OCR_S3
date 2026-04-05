<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Adjustment extends Model
{
    protected $fillable = [
        'adjustable_type',
        'adjustable_id',
        'amount',
        'reason',
        'admin_id',
    ];

    public function adjustable()
    {
        return $this->morphTo();
    }

    public function admin()
    {
        return $this->belongsTo(User::class, 'admin_id');
    }
}
