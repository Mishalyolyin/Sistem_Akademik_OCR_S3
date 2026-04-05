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
                \App\Models\StudyClass::firstOrCreate(
                    ['name' => $row['class']],
                    [
                        'program_type' => $row['program_type'],
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
                        $user = \App\Models\User::firstOrCreate(
                            ['email' => $row['nim'] . '@student.ac.id'],
                            [
                                'name' => $row['name'],
                                'password' => bcrypt($row['nim']), // Default password is NIM
                                'role' => 'mahasiswa',
                            ]
                        );
                        $student->user_id = $user->id;
                    }

                    $student->update([
                        'user_id' => $student->user_id,
                        'name' => $row['name'],
                        'class' => $row['class'],
                        'program_type' => $row['program_type'],
                        'start_term' => $row['start_term'],
                        'phone' => $row['phone'] ?? null,
                        'import_batch_id' => $this->batchId, // Update batch ID to latest
                    ]);
                    $this->stats['updated_records']++;
                }
            } else {
                // Create User for login
                $user = \App\Models\User::firstOrCreate(
                    ['email' => $row['nim'] . '@student.ac.id'],
                    [
                        'name' => $row['name'],
                        'password' => bcrypt($row['nim']), // Default password is NIM
                        'role' => 'mahasiswa',
                    ]
                );

                Student::create([
                    'user_id' => $user->id,
                    'nim' => $row['nim'],
                    'name' => $row['name'],
                    'class' => $row['class'],
                    'program_type' => $row['program_type'],
                    'start_term' => $row['start_term'],
                    'phone' => $row['phone'] ?? null,
                    'import_batch_id' => $this->batchId,
                ]);
                $this->stats['successful_inserts']++;
            }
        }
    }

    public function rules(): array
    {
        return [
            'nim' => ['required', 'string'],
            'name' => ['required', 'string'],
            'class' => ['required', 'string'],
            'program_type' => ['required', Rule::in(['REGULER', 'RPL'])],
            'start_term' => ['required', Rule::in(['GASAL', 'GENAP'])],
            'phone' => ['nullable', 'string'],
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
