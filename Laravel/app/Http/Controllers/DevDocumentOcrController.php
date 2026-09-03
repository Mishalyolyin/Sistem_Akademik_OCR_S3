<?php

namespace App\Http\Controllers;

use App\Models\Student;
use Illuminate\Support\Facades\Storage;

class DevDocumentOcrController extends Controller
{
    public function index()
    {
        $students = Student::whereNotNull('ktp_ocr_data')
            ->orWhereNotNull('kk_ocr_data')
            ->orWhereNotNull('ijazah_ocr_data')
            ->orWhereNotNull('profile_picture_analysis')
            ->orderBy('id', 'desc')
            ->paginate(20);

        return view('developer.documents.index', compact('students'));
    }

    public function show($id)
    {
        $student = Student::findOrFail($id);

        if (!$student->ktp_ocr_data && !$student->kk_ocr_data && !$student->ijazah_ocr_data && !$student->profile_picture_analysis) {
            abort(404, 'Belum ada data OCR dokumen untuk mahasiswa ini.');
        }

        $docs = [
            'ktp' => $this->buildDocInfo($student->ktp_file_path, $student->ktp_ocr_data),
            'kk' => $this->buildDocInfo($student->kk_file_path, $student->kk_ocr_data),
            'ijazah' => $this->buildDocInfo($student->ijazah_file_path, $student->ijazah_ocr_data),
        ];

        return view('developer.documents.show', compact('student', 'docs'));
    }

    /**
     * Build the original/grayscale/threshold image URLs for a document, mirroring
     * the debug artifacts ocr_processor.py saves next to the source file
     * (<name>_1_grayscale.jpg / <name>_2_threshold.jpg). The original may be a
     * PDF (ijazah) — that can't be shown in an <img>, so the view links to it
     * instead and only shows the two rendered debug images.
     */
    private function buildDocInfo($filePath, $ocrData)
    {
        if (!$filePath) {
            return null;
        }

        $isPdf = strtolower(pathinfo($filePath, PATHINFO_EXTENSION)) === 'pdf';
        $pathInfo = pathinfo($filePath);
        $dirname = $pathInfo['dirname'] === '.' ? '' : $pathInfo['dirname'] . '/';
        $filename = $pathInfo['filename'];

        return [
            'file_path' => $filePath,
            'is_pdf' => $isPdf,
            'original_url' => Storage::disk('public')->url($filePath),
            'grayscale_url' => Storage::disk('public')->url($dirname . $filename . '_1_grayscale.jpg'),
            'threshold_url' => Storage::disk('public')->url($dirname . $filename . '_2_threshold.jpg'),
            'ocr_data' => $ocrData,
        ];
    }
}
