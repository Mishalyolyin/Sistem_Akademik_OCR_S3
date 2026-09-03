<?php

namespace App\Http\Controllers;

use App\Jobs\ProcessStudentDocumentOcr;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

class StudentDocumentController extends Controller
{
    public function index(Request $request)
    {
        $student = $request->user()->student;
        $step = $student->nextIncompleteDocumentStep();

        if ($step === null) {
            return redirect()->route('student.dashboard');
        }

        return view('student.documents.index', compact('student', 'step'));
    }

    private function markCompleteIfDone($student)
    {
        if ($student->nextIncompleteDocumentStep() === null && !$student->documents_completed_at) {
            $student->update(['documents_completed_at' => now()]);
        }
    }

    /**
     * Every step endpoint must reject out-of-order submissions server-side —
     * the wizard's step order is a hard requirement (update.txt), not just a
     * UI convention, so a direct POST to a later step must not be able to
     * skip ahead of an incomplete earlier one.
     */
    private function ensureStep($student, string $expectedStep)
    {
        $actualStep = $student->nextIncompleteDocumentStep();

        if ($actualStep === $expectedStep) {
            return null;
        }

        if ($actualStep === null) {
            return redirect()->route('student.dashboard');
        }

        return redirect()->route('student.documents')->with('error', 'Harap lengkapi data secara berurutan.');
    }

    public function storePhoto(Request $request)
    {
        $student = $request->user()->student;
        if ($redirect = $this->ensureStep($student, 'photo')) {
            return $redirect;
        }

        $validated = $request->validate([
            'profile_picture' => 'required|image|mimes:jpg,jpeg,png|max:2048',
        ], [
            'profile_picture.required' => 'Foto profil wajib diunggah.',
        ]);

        $file = $request->file('profile_picture');
        $extension = $file->getClientOriginalExtension();
        $className = Str::slug($student->class ?: 'unassigned');
        $fileName = $student->nim . '_' . Str::slug($student->name) . '.' . $extension;

        if ($student->profile_picture && Storage::disk('public')->exists($student->profile_picture)) {
            Storage::disk('public')->delete($student->profile_picture);
        }

        $path = $file->storeAs('profile_pictures/' . $className, $fileName, 'public');
        $student->update(['profile_picture' => $path, 'profile_picture_analysis' => null]);

        ProcessStudentDocumentOcr::dispatch($student->id, 'photo');
        $this->markCompleteIfDone($student);

        return redirect()->route('student.documents')->with('success', 'Foto profil berhasil diunggah.');
    }

    public function storeKtp(Request $request)
    {
        $student = $request->user()->student;
        if ($redirect = $this->ensureStep($student, 'ktp')) {
            return $redirect;
        }

        $validated = $request->validate([
            'nik' => 'required|digits:16|unique:students,nik,' . $student->id,
            'ktp_file' => 'required|image|mimes:jpg,jpeg,png|max:5120',
        ]);

        if ($student->ktp_file_path && Storage::disk('public')->exists($student->ktp_file_path)) {
            Storage::disk('public')->delete($student->ktp_file_path);
        }

        $file = $request->file('ktp_file');
        $fileName = 'ktp_' . Str::slug($student->name) . '.' . $file->getClientOriginalExtension();
        $path = $file->storeAs('student_documents/' . $student->nim, $fileName, 'public');

        $student->update([
            'nik' => $validated['nik'],
            'ktp_file_path' => $path,
            'ktp_ocr_data' => null,
        ]);

        ProcessStudentDocumentOcr::dispatch($student->id, 'ktp');
        $this->markCompleteIfDone($student);

        return redirect()->route('student.documents')->with('success', 'Data KTP berhasil diunggah.');
    }

    public function storeKk(Request $request)
    {
        $student = $request->user()->student;
        if ($redirect = $this->ensureStep($student, 'kk')) {
            return $redirect;
        }

        $validated = $request->validate([
            'kk_number' => 'required|digits:16',
            'kk_file' => 'required|image|mimes:jpg,jpeg,png|max:5120',
        ]);

        if ($student->kk_file_path && Storage::disk('public')->exists($student->kk_file_path)) {
            Storage::disk('public')->delete($student->kk_file_path);
        }

        $file = $request->file('kk_file');
        $fileName = 'kk_' . Str::slug($student->name) . '.' . $file->getClientOriginalExtension();
        $path = $file->storeAs('student_documents/' . $student->nim, $fileName, 'public');

        $student->update([
            'kk_number' => $validated['kk_number'],
            'kk_file_path' => $path,
            'kk_ocr_data' => null,
        ]);

        ProcessStudentDocumentOcr::dispatch($student->id, 'kk');
        $this->markCompleteIfDone($student);

        return redirect()->route('student.documents')->with('success', 'Data Kartu Keluarga berhasil diunggah.');
    }

    public function storeIjazah(Request $request)
    {
        $student = $request->user()->student;
        if ($redirect = $this->ensureStep($student, 'ijazah')) {
            return $redirect;
        }

        $request->validate([
            'ijazah_file' => 'required|mimes:jpg,jpeg,png,pdf|max:10240',
        ]);

        if ($student->ijazah_file_path && Storage::disk('public')->exists($student->ijazah_file_path)) {
            Storage::disk('public')->delete($student->ijazah_file_path);
        }

        $file = $request->file('ijazah_file');
        $fileName = 'ijazah_' . Str::slug($student->name) . '.' . $file->getClientOriginalExtension();
        $path = $file->storeAs('student_documents/' . $student->nim, $fileName, 'public');

        $student->update([
            'ijazah_file_path' => $path,
            'ijazah_ocr_data' => null,
        ]);

        ProcessStudentDocumentOcr::dispatch($student->id, 'ijazah');
        $this->markCompleteIfDone($student);

        return redirect()->route('student.documents')->with('success', 'Ijazah S1 berhasil diunggah. Tempat/tanggal lahir akan otomatis terisi dari hasil pembacaan dokumen.');
    }

    public function storeAddress(Request $request)
    {
        $student = $request->user()->student;
        if ($redirect = $this->ensureStep($student, 'address')) {
            return $redirect;
        }

        $validated = $request->validate([
            'address' => 'required|string|max:1000',
        ]);

        $student->update(['address' => $validated['address']]);

        $this->markCompleteIfDone($student);

        if ($student->documents_completed_at) {
            return redirect()->route('student.dashboard')->with('success', 'Semua data lengkap. Terima kasih!');
        }

        return redirect()->route('student.documents')->with('success', 'Alamat berhasil disimpan.');
    }
}
