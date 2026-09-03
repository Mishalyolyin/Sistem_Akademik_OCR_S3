<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class PaymentPlan extends Model
{
    protected $fillable = [
        'student_id',
        'installment_template_id',
        'academic_year',
        'term',
        'category',
        'total_amount',
        'status',
    ];

    protected $casts = [
        'total_amount' => 'decimal:2',
    ];

    public function student()
    {
        return $this->belongsTo(Student::class);
    }

    public function installmentTemplate()
    {
        return $this->belongsTo(InstallmentTemplate::class);
    }

    public function installments()
    {
        return $this->hasMany(Installment::class);
    }

    public function munaqosahDetail()
    {
        return $this->hasOne(MunaqosahDetail::class);
    }
}
