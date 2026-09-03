<?php

namespace App\Http\Controllers;

use App\Models\StudyClass;
use Illuminate\Http\Request;

class AdminClassController extends Controller
{
    public function index()
    {
        $classes = StudyClass::orderBy('name')->paginate(10);
        return view('admin.classes.index', compact('classes'));
    }

    public function store(Request $request)
    {
        $request->validate([
            'name' => 'required|unique:study_classes,name|max:255',
            'program_type' => 'required|in:Reguler,RPL',
            'generation' => 'nullable|string|max:4'
        ]);

        StudyClass::create($request->all());

        return redirect()->back()->with('success', 'Kelas berhasil ditambahkan.');
    }

    public function update(Request $request, StudyClass $class)
    {
        $request->validate([
            'name' => 'required|max:255|unique:study_classes,name,' . $class->id,
            'program_type' => 'required|in:Reguler,RPL',
            'generation' => 'nullable|string|max:4'
        ]);

        $class->update($request->all());

        return redirect()->back()->with('success', 'Kelas berhasil diperbarui.');
    }

    public function destroy(StudyClass $class)
    {
        // Optional: Check if used by students before deleting
        // $inUse = \App\Models\Student::where('class', $class->name)->exists();
        // if ($inUse) { return back()->with('error', 'Kelas sedang digunakan oleh mahasiswa.'); }

        $class->delete();
        return redirect()->back()->with('success', 'Kelas berhasil dihapus.');
    }
}
