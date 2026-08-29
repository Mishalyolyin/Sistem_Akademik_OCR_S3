<?php

namespace App\Http\Controllers;

use App\Models\Student;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use ZipArchive;

class AdminStudentController extends Controller
{
    public function index(Request $request)
    {
        // Redirect to Reguler by default if accessing base index, or show all
        // For now, let's redirect to Reguler as it's the likely default
        return redirect()->route('admin.students.reguler');
    }

    public function reguler(Request $request)
    {
        return $this->getStudents($request, 'Reguler');
    }

    public function rpl(Request $request)
    {
        return $this->getStudents($request, 'RPL');
    }

    public function resetPassword(\App\Models\Student $student)
    {
        $user = $student->user;
        if ($user) {
            $user->update([
                'password' => \Illuminate\Support\Facades\Hash::make($student->nim), // Reset to NIM
            ]);
            return back()->with('success', 'Password mahasiswa berhasil direset menjadi NIM.');
        }
        return back()->with('error', 'Akun user untuk mahasiswa ini tidak ditemukan.');
    }

    public function edit(\App\Models\Student $student)
    {
        $student->load(['paymentPlans' => function ($q) {
            $q->whereIn('category', ['PENDAFTARAN', 'KERJASAMA'])->with('installments');
        }]);

        return view('admin.students.edit', compact('student'));
    }

    public function update(Request $request, \App\Models\Student $student)
    {
        $request->validate([
            'name' => 'required|string|max:255',
            'nim' => 'required|string|max:20|unique:students,nim,' . $student->id,
            'phone' => 'nullable|string|max:20',
            'class' => 'nullable|string|max:50',
            'program_type' => 'required|in:REGULER,RPL',
            'start_term' => 'required|in:GASAL,GENAP',
            'is_alumni' => 'boolean',
            'nik' => 'nullable|digits:16|unique:students,nik,' . $student->id,
            'kk_number' => 'nullable|digits:16',
            'birth_place' => 'nullable|string|max:255',
            'birth_date' => 'nullable|date',
            'address' => 'nullable|string|max:1000',
            'pendaftaran_exempt' => 'boolean',
        ]);

        $isAlumni = $request->program_type === 'REGULER' && $request->has('is_alumni') ? true : false;

        $student->update([
            'name' => $request->name,
            'nim' => $request->nim,
            'phone' => $request->phone,
            'class' => $request->class,
            'program_type' => $request->program_type,
            'start_term' => $request->start_term,
            'is_alumni' => $isAlumni,
            'nik' => $request->nik,
            'kk_number' => $request->kk_number,
            'birth_place' => $request->birth_place,
            'birth_date' => $request->birth_date,
            'address' => $request->address,
            'pendaftaran_exempt' => $request->has('pendaftaran_exempt'),
        ]);

        // Update User Name/Email if linked
        if ($student->user) {
            $student->user->update([
                'name' => $request->name,
                // Optional: update email if NIM changes? Usually email is nim@student.ac.id
                'email' => $request->nim . '@student.ac.id',
            ]);
        }

        return redirect()->route('admin.students.reguler')->with('success', 'Data mahasiswa berhasil diperbarui.');
    }

    public function destroy(\App\Models\Student $student)
    {
        try {
            // Hapus user terkait jika ada
            if ($student->user) {
                $student->user->delete();
            }
            
            // Hapus data mahasiswa
            $student->delete();
            
            return back()->with('success', 'Data mahasiswa beserta akun loginnya berhasil dihapus.');
        } catch (\Exception $e) {
            return back()->with('error', 'Gagal menghapus mahasiswa. Pastikan data tidak memiliki relasi penting yang tertahan.');
        }
    }

    public function bulkDelete(Request $request)
    {
        $request->validate([
            'student_ids' => 'required|array',
            'student_ids.*' => 'exists:students,id'
        ]);

        try {
            $students = Student::whereIn('id', $request->student_ids)->get();
            
            foreach ($students as $student) {
                if ($student->user) {
                    $student->user->delete();
                }
                $student->delete();
            }

            return back()->with('success', count($request->student_ids) . ' data mahasiswa beserta akun loginnya berhasil dihapus.');
        } catch (\Exception $e) {
            return back()->with('error', 'Gagal menghapus mahasiswa. Pastikan data tidak memiliki relasi penting yang tertahan.');
        }
    }

    public function downloadPhotos(Request $request)
    {
        $className = $request->query('class');
        if (!$className) {
            return back()->with('error', 'Kelas tidak ditentukan.');
        }

        $slugClass = Str::slug($className);
        $folderPath = 'profile_pictures/' . $slugClass;

        if (!Storage::disk('public')->exists($folderPath)) {
            return back()->with('error', 'Tidak ada foto untuk kelas ini.');
        }

        $files = Storage::disk('public')->files($folderPath);
        if (empty($files)) {
            return back()->with('error', 'Tidak ada foto untuk kelas ini.');
        }

        $zip = new ZipArchive;
        $zipFileName = 'Foto_Kelas_' . $slugClass . '_' . date('YmdHis') . '.zip';
        $zipFilePath = storage_path('app/public/' . $zipFileName);

        if ($zip->open($zipFilePath, ZipArchive::CREATE | ZipArchive::OVERWRITE) === TRUE) {
            foreach ($files as $file) {
                $absolutePath = storage_path('app/public/' . $file);
                $relativeName = basename($file);
                $zip->addFile($absolutePath, $relativeName);
            }
            $zip->close();
        } else {
            return back()->with('error', 'Gagal membuat file ZIP.');
        }

        return response()->download($zipFilePath)->deleteFileAfterSend(true);
    }

    private function getStudents(Request $request, $programType)
    {
        $query = Student::query()->where('program_type', $programType)
            ->with(['paymentPlans' => function ($q) {
                $q->where('category', 'MUNAOSAH')->with('installments');
            }]);

        if ($request->filled('search')) {
            $search = $request->search;
            $query->where(function($q) use ($search) {
                $q->where('name', 'like', "%{$search}%")
                  ->orWhere('nim', 'like', "%{$search}%");
            });
        }

        $kelengkapan = $request->input('kelengkapan');
        if ($kelengkapan === 'lengkap') {
            $query->whereNotNull('documents_completed_at');
        } elseif ($kelengkapan === 'belum') {
            $query->whereNull('documents_completed_at');
        }

        // Sort by Class first (A-Z), then by Name (A-Z)
        $students = $query->orderBy('class', 'asc')
                          ->orderBy('name', 'asc')
                          ->paginate(10)
                          ->withQueryString();

        $pageTitle = "Manajemen Mahasiswa " . $programType;
        $classes = Student::where('program_type', $programType)->whereNotNull('class')->distinct()->pluck('class');

        return view('admin.students.index', compact('students', 'pageTitle', 'programType', 'classes', 'kelengkapan'));
    }

    public function export($programType)
    {
        $filename = 'Data_Mahasiswa_' . $programType . '_' . date('YmdHis') . '.xlsx';
        return \Maatwebsite\Excel\Facades\Excel::download(new \App\Exports\StudentDataExport($programType), $filename);
    }
}
