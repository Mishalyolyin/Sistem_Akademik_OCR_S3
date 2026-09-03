<?php

namespace App\Http\Controllers;

use App\Imports\StudentsImport;
use App\Models\ImportBatch;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Maatwebsite\Excel\Facades\Excel;
use App\Exports\StudentTemplateExport;

class StudentImportController extends Controller
{
    public function downloadTemplate()
    {
        return Excel::download(new StudentTemplateExport, 'template_import_mahasiswa.xlsx');
    }

    public function store(Request $request)
    {
        // Perpanjang waktu eksekusi server (5 menit) karena hashing bcrypt memakan waktu lama
        set_time_limit(300);

        $request->validate([
            'file' => 'required|file|mimes:csv,txt,xlsx',
            'mode' => 'required|in:skip,update,cancel',
        ]);

        try {
            return DB::transaction(function () use ($request) {
                // 1. Create Batch Record
                $batch = ImportBatch::create([
                    'file_name' => $request->file('file')->getClientOriginalName(),
                    'imported_by_admin_id' => $request->user()->id ?? 1, // Fallback to 1 for testing if no auth
                    'summary_json' => [],
                ]);

                // 2. Run Import
                $import = new StudentsImport($batch->id, $request->mode);
                Excel::import($import, $request->file('file'));

                // 3. Update Summary
                $batch->update([
                    'summary_json' => $import->stats,
                ]);

                $message = 'Import successful';
                if ($import->stats['total_rows'] === 0) {
                    $message = 'Import finished but no rows were processed. Please check if your file is empty or headers are missing.';
                }

                return response()->json([
                    'message' => $message,
                    'batch_id' => $batch->id,
                    'summary' => $import->stats,
                ], 201);
            });
        } catch (\Maatwebsite\Excel\Validators\ValidationException $e) {
            $failures = $e->failures();
            return response()->json([
                'message' => 'Validation failed in file',
                'errors' => $failures,
            ], 422);
        } catch (\Exception $e) {
            return response()->json([
                'message' => 'Import failed',
                'error' => $e->getMessage(),
            ], 500);
        }
    }
}
