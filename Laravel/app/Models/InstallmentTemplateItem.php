<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class InstallmentTemplateItem extends Model
{
    protected $fillable = [
        'installment_template_id',
        'installment_no',
        'month_offset',
        'day_of_month',
    ];

    public function template()
    {
        return $this->belongsTo(InstallmentTemplate::class);
    }
}
