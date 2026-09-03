<?php

namespace App\Jobs;

use App\Models\Student;
use App\Services\OcrService;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;
use Exception;

class ProcessStudentDocumentOcr implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public $tries = 3;
    public $backoff = [10, 30, 60];

    protected $studentId;
    protected $docType; // 'ktp', 'ijazah', 'kk' or 'photo'

    /**
     * @param int $studentId
     * @param string $docType 'ktp', 'ijazah', 'kk' or 'photo'
     */
    public function __construct($studentId, $docType)
    {
        $this->studentId = $studentId;
        $this->docType = $docType;
    }

    public function handle(OcrService $ocrService)
    {
        $student = Student::find($this->studentId);

        if (!$student) {
            Log::channel('ocr')->warning("Student #{$this->studentId} not found during document OCR job.");
            return;
        }

        $filePath = match ($this->docType) {
            'ktp' => $student->ktp_file_path,
            'ijazah' => $student->ijazah_file_path,
            'kk' => $student->kk_file_path,
            'photo' => $student->profile_picture,
        };

        if (!$filePath) {
            Log::channel('ocr')->warning("Student #{$this->studentId} has no {$this->docType} file to process.");
            return;
        }

        try {
            $fullPath = storage_path('app/public/' . $filePath);
            $result = $ocrService->processDocument($fullPath, $this->docType, $student->name);

            $field = match ($this->docType) {
                'ktp' => 'ktp_ocr_data',
                'ijazah' => 'ijazah_ocr_data',
                'kk' => 'kk_ocr_data',
                'photo' => 'profile_picture_analysis',
            };
            $updateData = [$field => $result];

            // Ijazah is the source of truth for birth place/date — no manual
            // input exists for these, so the OCR result becomes the reference.
            if ($this->docType === 'ijazah') {
                if (!empty($result['extracted_birth_place'])) {
                    $updateData['birth_place'] = $result['extracted_birth_place'];
                }
                if (!empty($result['extracted_birth_date'])) {
                    $updateData['birth_date'] = $result['extracted_birth_date'];
                }
            }

            $student->update($updateData);

            Log::channel('ocr')->info("Document OCR completed for Student #{$student->id} ({$this->docType})", $result);
        } catch (Exception $e) {
            Log::channel('ocr')->error("Document OCR Job Failed for Student #{$this->studentId} ({$this->docType}): " . $e->getMessage());
            throw $e; // Re-throw to trigger retry mechanism
        }
    }
}
