<?php

namespace App\Services;

use App\Models\Payment;
use Illuminate\Support\Facades\Storage;
use ZipArchive;

class DatasetExportService
{
    /**
     * Export verified payments as a dataset (images + CSV).
     *
     * @param int $limit
     * @return string Path to the ZIP file
     */
    public function export(int $limit = 100): string
    {
        // Fetch verified AND rejected payments
        // Verified = Positive Samples, Rejected = Negative Samples
        $payments = Payment::whereIn('status', ['VERIFIED', 'REJECTED'])
            ->whereNotNull('processed_file_path')
            ->limit($limit)
            ->get();

        if ($payments->isEmpty()) {
            throw new \Exception("No verified payments found for export.");
        }

        $zipFileName = 'dataset_export_' . now()->timestamp . '.zip';
        $zipFilePath = storage_path('app/public/' . $zipFileName);
        
        $zip = new ZipArchive;
        if ($zip->open($zipFilePath, ZipArchive::CREATE | ZipArchive::OVERWRITE) !== TRUE) {
            throw new \Exception("Cannot create ZIP file at $zipFilePath");
        }

        // Create CSV content
        $csvData = [];
        $csvData[] = ['filename', 'amount', 'status'];

        foreach ($payments as $payment) {
            // Check if file exists
            if (!Storage::disk('public')->exists($payment->processed_file_path)) {
                continue;
            }

            // Add file to zip
            $fileName = basename($payment->processed_file_path);
            $localPath = Storage::disk('public')->path($payment->processed_file_path);
            
            // Add to "images/" folder in zip
            $zip->addFile($localPath, 'images/' . $fileName);

            // Add to CSV data
            $csvData[] = [
                $fileName,
                $payment->amount,
                $payment->status
            ];
        }

        // Add CSV to zip
        $csvContent = '';
        foreach ($csvData as $row) {
            $csvContent .= implode(',', $row) . "\n";
        }
        $zip->addFromString('labels.csv', $csvContent);

        $zip->close();

        return $zipFilePath;
    }
}
