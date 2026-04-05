<?php

namespace App\Http\Controllers;

use App\Models\Student;
use Illuminate\Http\Request;

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
        ]);

        $student->update([
            'name' => $request->name,
            'nim' => $request->nim,
            'phone' => $request->phone,
            'class' => $request->class,
            'program_type' => $request->program_type,
            'start_term' => $request->start_term,
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

    private function getStudents(Request $request, $programType)
    {
        $query = Student::query()->where('program_type', $programType);

        if ($request->filled('search')) {
            $search = $request->search;
            $query->where(function($q) use ($search) {
                $q->where('name', 'like', "%{$search}%")
                  ->orWhere('nim', 'like', "%{$search}%");
            });
        }
        
        // Sort by Class first (A-Z), then by Name (A-Z)
        $students = $query->orderBy('class', 'asc')
                          ->orderBy('name', 'asc')
                          ->paginate(10)
                          ->withQueryString();
                          
        $pageTitle = "Manajemen Mahasiswa " . $programType;

        return view('admin.students.index', compact('students', 'pageTitle', 'programType'));
    }
}
