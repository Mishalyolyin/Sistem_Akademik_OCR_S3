<?php

namespace App\Http\Controllers;

use App\Jobs\ProcessStudentDocumentOcr;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\Rules\Password;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

class StudentProfileController extends Controller
{
    public function index()
    {
        return view('student.profile.index');
    }

    public function updateProfile(Request $request)
    {
        $user = $request->user();
        $student = $user->student;

        $hasExistingPhoto = $student && $student->profile_picture;

        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'nim' => 'required|string|max:20|unique:students,nim,' . ($student->id ?? ''),
            'phone' => 'nullable|string|max:20',
            'profile_picture' => [$hasExistingPhoto ? 'nullable' : 'required', 'image', 'mimes:jpg,jpeg,png', 'max:2048'],
        ], [
            'profile_picture.required' => 'Foto profil wajib diunggah.',
        ]);

        if ($request->hasFile('profile_picture')) {
            $file = $request->file('profile_picture');
            $extension = $file->getClientOriginalExtension();
            $className = $student ? Str::slug($student->class) : 'unassigned';
            $fileName = $validated['nim'] . '_' . Str::slug($validated['name']) . '.' . $extension;
            $path = 'profile_pictures/' . $className;

            // Hapus foto lama jika ada
            if ($student && $student->profile_picture && Storage::disk('public')->exists($student->profile_picture)) {
                Storage::disk('public')->delete($student->profile_picture);
            }

            // Simpan foto baru
            $savedPath = $file->storeAs($path, $fileName, 'public');

            if ($student) {
                $student->profile_picture = $savedPath;
                $student->profile_picture_analysis = null;
            }
        }

        // Update User
        $user->update([
            'name' => $validated['name'],
        ]);

        // Update Student
        if ($student) {
            $student->name = $validated['name'];
            $student->nim = $validated['nim'];
            $student->phone = $validated['phone'];
            $student->save();
        }

        if ($student && $request->hasFile('profile_picture')) {
            ProcessStudentDocumentOcr::dispatch($student->id, 'photo');
        }

        return back()->with('success', 'Profil berhasil diperbarui.');
    }

    public function updatePassword(Request $request)
    {
        $validated = $request->validate([
            'current_password' => ['required', 'current_password'],
            'password' => ['required', 'confirmed', Password::defaults()],
        ]);

        $request->user()->update([
            'password' => Hash::make($validated['password']),
        ]);

        return back()->with('success', 'Password berhasil diperbarui.');
    }
}
