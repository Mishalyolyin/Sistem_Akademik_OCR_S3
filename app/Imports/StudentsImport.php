<?php

namespace App\Imports;

use App\Models\Student;
use Illuminate\Support\Collection;
use Maatwebsite\Excel\Concerns\ToCollection;
use Maatwebsite\Excel\Concerns\WithHeadingRow;
use Maatwebsite\Excel\Concerns\WithValidation;
use Illuminate\Validation\Rule;

class StudentsImport implements ToCollection, WithHeadingRow, WithValidation
{
    protected $batchId;
    protected $mode; // skip, update, cancel

    public $stats = [
        'total_rows' => 0,
        'successful_inserts' => 0,
        'updated_records' => 0,
        'skipped_duplicates' => 0,
        'failed_records' => 0,
        'kerjasama_created' => 0,
        'kerjasama_failed' => [],
    ];

    public function __construct($batchId, $mode)
    {
        $this->batchId = $batchId;
        $this->mode = $mode;
    }

    public function collection(Collection $rows)
    {
        $this->stats['total_rows'] = $rows->count();

        foreach ($rows as $row) {
            // Populate Master Class if not exists
            if (!empty($row['class'])) {
                $programTypeForClass = $row['program_type'];
                if (strtoupper($programTypeForClass) === 'REGULER') {
                    $programTypeForClass = 'Reguler';
                }

                \App\Models\StudyClass::firstOrCreate(
                    ['name' => $row['class']],
                    [
                        'program_type' => $programTypeForClass,
                        'generation' => date('Y') // Default to current year if creating new
                    ]
                );
            }

            $student = Student::where('nim', $row['nim'])->first();

            if ($student) {
                if ($this->mode === 'cancel') {
                    throw new \Exception("Duplicate NIM found: {$row['nim']}. Import cancelled.");
                }

                if ($this->mode === 'skip') {
                    $this->stats['skipped_duplicates']++;
                    continue;
                }

                if ($this->mode === 'update') {
                    // Ensure User exists (for legacy data or manual inserts)
                    if (!$student->user_id) {
                        $user = \App\Models\User::where('email', $row['nim'] . '@student.ac.id')->first();
                        if (!$user) {
                            $user = \App\Models\User::create([
                                'email' => $row['nim'] . '@student.ac.id',
                                'name' => $row['name'],
                                'password' => bcrypt($row['nim']), // Default password is NIM
                                'role' => 'mahasiswa',
                            ]);
                        }
                        $student->user_id = $user->id;
                    }

                    $student->update([
                        'user_id' => $student->user_id,
                        'name' => $row['name'],
                        'class' => $row['class'],
                        'program_type' => $row['program_type'],
                        'is_alumni' => isset($row['is_alumni']) ? filter_var($row['is_alumni'], FILTER_VALIDATE_BOOLEAN) : false,
                        'start_term' => $row['start_term'],
                        'academic_year' => $row['academic_year'] ?? null,
                        'phone' => $row['phone'] ?? null,
                        'import_batch_id' => $this->batchId, // Update batch ID to latest
                    ]);
                    $this->stats['updated_records']++;
                }
            } else {
                // Create User for login
                $user = \App\Models\User::where('email', $row['nim'] . '@student.ac.id')->first();
                if (!$user) {
                    $user = \App\Models\User::create([
                        'email' => $row['nim'] . '@student.ac.id',
                        'name' => $row['name'],
                        'password' => bcrypt($row['nim']), // Default password is NIM
                        'role' => 'mahasiswa',
                    ]);
                }

                $student = Student::create([
                    'user_id' => $user->id,
                    'nim' => $row['nim'],
                    'name' => $row['name'],
                    'class' => $row['class'],
                    'program_type' => $row['program_type'],
                    'is_alumni' => isset($row['is_alumni']) ? filter_var($row['is_alumni'], FILTER_VALIDATE_BOOLEAN) : false,
                    'start_term' => $row['start_term'],
                    'academic_year' => $row['academic_year'] ?? null,
                    'phone' => $row['phone'] ?? null,
                    'import_batch_id' => $this->batchId,
                ]);
                $this->stats['successful_inserts']++;
            }

            $this->assignKerjasamaPlanIfRequested($row, $student);
        }
    }

    /**
     * Optional "kelas_kerjasama" column: generates a KERJASAMA payment plan
     * (RPL only, reuses the existing "RPL 4x Angsuran" installment schedule)
     * for the student on this row, without needing a separate import/template.
     */
    private function assignKerjasamaPlanIfRequested($row, ?Student $student): void
    {
        if (!$student || empty($row['kelas_kerjasama']) || !filter_var($row['kelas_kerjasama'], FILTER_VALIDATE_BOOLEAN)) {
            return;
        }

        if (strtoupper($student->program_type) !== 'RPL') {
            $this->stats['kerjasama_failed'][] = "NIM {$row['nim']}: Kelas Kerjasama hanya untuk mahasiswa RPL.";
            return;
        }

        try {
            $template = \App\Models\InstallmentTemplate::where('program_type', 'RPL')
                ->where('start_term', strtoupper($student->start_term))
                ->where('installments_count', 4)
                ->first();

            if (!$template) {
                throw new \Exception("Template RPL 4x Angsuran ({$student->start_term}) tidak ditemukan.");
            }

            app(\App\Services\PaymentGenerationService::class)
                ->generatePlan($student, $template, 'KERJASAMA');

            $this->stats['kerjasama_created']++;
        } catch (\Exception $e) {
            $this->stats['kerjasama_failed'][] = "NIM {$row['nim']}: {$e->getMessage()}";
        }
    }

    public function rules(): array
    {
        return [
            'nim' => ['required', 'string'],
            'name' => ['required', 'string'],
            'class' => ['required', 'string'],
            'program_type' => ['required', Rule::in(['REGULER', 'RPL'])],
            'is_alumni' => ['nullable'],
            'start_term' => ['required', Rule::in(['GASAL', 'GENAP'])],
            'phone' => ['nullable', 'string'],
            'kelas_kerjasama' => ['nullable'],
        ];
    }

    public function prepareForValidation($data, $index)
    {
        // Ensure NIM is treated as string (Excel often reads it as int)
        if (isset($data['nim'])) {
            $data['nim'] = (string) $data['nim'];
        }
        
        // Ensure Phone is treated as string
        if (isset($data['phone'])) {
            $data['phone'] = (string) $data['phone'];
        }

        if (isset($data['program_type'])) {
            $data['program_type'] = strtoupper($data['program_type']);
        }
        if (isset($data['start_term'])) {
            $data['start_term'] = strtoupper($data['start_term']);
        }
        
        return $data;
    }
}
