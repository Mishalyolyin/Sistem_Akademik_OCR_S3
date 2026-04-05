<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class InstallmentTemplate extends Model
{
    protected $fillable = [
        'program_type',
        'start_term',
        'installments_count',
        'name',
        'active',
    ];

    public function items()
    {
        return $this->hasMany(InstallmentTemplateItem::class);
    }
}
