<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class TuitionRate extends Model
{
    protected $fillable = [
        'program_type',
        'academic_year',
        'category',
        'start_term',
        'is_alumni',
        'amount',
        'description',
        'active',
    ];

    protected $casts = [
        'amount' => 'decimal:2',
        'active' => 'boolean',
        'is_alumni' => 'boolean',
    ];
}
