<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class MunaqosahDetail extends Model
{
    protected $fillable = [
        'payment_plan_id',
        'supervisor_name',
        'supervisor_name_2',
        'thesis_title',
        'thesis_file_path',
        'article_file_path',
    ];

    public function paymentPlan()
    {
        return $this->belongsTo(PaymentPlan::class);
    }
}
