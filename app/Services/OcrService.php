<?php

namespace App\Services;

use Symfony\Component\Process\Process;
use Symfony\Component\Process\Exception\ProcessFailedException;
use Illuminate\Support\Facades\Log;
use App\Models\SystemSetting;

class OcrService
{
    /**
     * Process the image using Python script.
     *
     * @param string $imagePath
     * @param string|null $programType 'REGULER' or 'RPL' to validate bank account
     * @param string|null $studentName Student Name to validate
     * @return array
     */
    public function processImage(string $imagePath, ?string $programType = null, ?string $studentName = null)
    {
        $scriptPath = base_path('app/Scripts/ocr_processor.py');
        
        // Check if script exists
        if (!file_exists($scriptPath)) {
            Log::channel('ocr')->error('OCR Script not found', ['path' => $scriptPath]);
            throw new \Exception("OCR Script not found at {$scriptPath}");
        }

        // Fetch Settings from DB
        $blacklist = SystemSetting::where('key', 'ocr_blacklist_keywords')->value('value') ?? '';
        $maxDays = SystemSetting::where('key', 'ocr_date_validation_days')->value('value');
        
        $accounts = '';
        if ($programType) {
            $key = $programType === 'REGULER' ? 'bank_account_reguler' : 'bank_account_rpl';
            $accounts = SystemSetting::where('key', $key)->value('value') ?? '';
        }

        // Build Command
        $command = ['python', $scriptPath, $imagePath];
        
        if (!empty($accounts)) {
            $command[] = '--accounts';
            $command[] = $accounts;
        }
        
        if (!empty($blacklist)) {
            $command[] = '--blacklist';
            $command[] = $blacklist;
        }

        if ($maxDays) {
            $command[] = '--max-days';
            $command[] = $maxDays;
        }
        
        if ($studentName) {
            $command[] = '--student-name';
            $command[] = $studentName;
        }

        // Run Python script
        $process = new Process($command);
        $process->setTimeout(60); // 60 seconds timeout
        $process->run();

        if (!$process->isSuccessful()) {
            Log::channel('ocr')->error('OCR Process Failed', [
                'error' => $process->getErrorOutput(),
                'output' => $process->getOutput(),
                'path' => $imagePath,
                'command' => implode(' ', $command)
            ]);
            throw new ProcessFailedException($process);
        }

        $output = $process->getOutput();
        Log::channel('ocr')->info('OCR Raw Output', ['output' => $output]);
        
        $decoded = json_decode($output, true);
        
        if (json_last_error() !== JSON_ERROR_NONE) {
            Log::channel('ocr')->error('OCR JSON Decode Error', ['json_error' => json_last_error_msg()]);
            return ['error' => 'Invalid JSON output'];
        }

        return $decoded ?? [];
    }
}
