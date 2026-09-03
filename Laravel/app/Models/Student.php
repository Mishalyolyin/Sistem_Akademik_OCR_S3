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
        'academic_year',
        'phone',
        'profile_picture',
        'profile_picture_analysis',
        'wallet_balance',
        'is_alumni',
        'import_batch_id',
        'ktp_file_path',
        'ktp_ocr_data',
        'ijazah_file_path',
        'ijazah_ocr_data',
        'documents_completed_at',
        'nik',
        'kk_number',
        'kk_file_path',
        'kk_ocr_data',
        'birth_place',
        'birth_date',
        'address',
        'pendaftaran_exempt',
    ];

    protected $casts = [
        'profile_picture_analysis' => 'array',
        'ktp_ocr_data' => 'array',
        'ijazah_ocr_data' => 'array',
        'kk_ocr_data' => 'array',
        'documents_completed_at' => 'datetime',
        'birth_date' => 'date',
        'pendaftaran_exempt' => 'boolean',
    ];

    /**
     * Wajib diisi berurutan: Foto -> No.KTP+KTP -> No.KK+KK -> Ijazah -> Alamat.
     * Dipakai wizard student.documents buat nentuin step mana yang harus ditampilkan.
     */
    public function nextIncompleteDocumentStep(): ?string
    {
        if (!$this->profile_picture) {
            return 'photo';
        }
        if (!$this->nik || !$this->ktp_file_path) {
            return 'ktp';
        }
        if (!$this->kk_number || !$this->kk_file_path) {
            return 'kk';
        }
        if (!$this->ijazah_file_path) {
            return 'ijazah';
        }
        if (!$this->address) {
            return 'address';
        }
        return null;
    }

    public function hasCompletedDocuments(): bool
    {
        return $this->nextIncompleteDocumentStep() === null;
    }

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
