<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ImportBatch extends Model
{
    protected $fillable = [
        'file_name',
        'imported_by_admin_id',
        'summary_json',
    ];

    protected $casts = [
        'summary_json' => 'array',
    ];

    public function admin()
    {
        return $this->belongsTo(User::class, 'imported_by_admin_id');
    }

    public function students()
    {
        return $this->hasMany(Student::class);
    }
}
