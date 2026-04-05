<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class AuthController extends Controller
{
    public function showLoginForm()
    {
        return view('auth.login');
    }

    public function login(Request $request)
    {
        $request->validate([
            'email' => ['required', 'string'], // Changed from 'email' to 'string' to accept NIM
            'password' => ['required'],
        ]);

        $loginType = filter_var($request->email, FILTER_VALIDATE_EMAIL) ? 'email' : 'nim';
        
        $credentials = [
            'password' => $request->password
        ];

        if ($loginType === 'email') {
            $credentials['email'] = $request->email;
        } else {
            // Find student by NIM
            $student = \App\Models\Student::where('nim', $request->email)->first();
            if ($student && $student->user) {
                $credentials['email'] = $student->user->email;
            } else {
                return back()->withErrors([
                    'email' => 'NIM tidak ditemukan atau belum terdaftar.',
                ])->onlyInput('email');
            }
        }

        if (Auth::attempt($credentials, $request->boolean('remember'))) {
            $request->session()->regenerate();

            if (Auth::user()->role === 'admin') {
                return redirect()->intended(route('admin.dashboard'));
            }

            return redirect()->intended(route('student.dashboard'));
        }

        return back()->withErrors([
            'email' => 'Kredensial tidak cocok dengan data kami.',
        ])->onlyInput('email');
    }
}
