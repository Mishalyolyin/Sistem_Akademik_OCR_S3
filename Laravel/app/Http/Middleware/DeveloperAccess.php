<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class DeveloperAccess
{
    public function handle(Request $request, Closure $next): Response
    {
        // Hindari penggunaan env() langsung di production jika config dicache.
        // Hardcode email developer atau ambil dari config yang benar.
        $developerEmails = ['stevenbahctiar@gmail.com'];

        if (!auth()->check()) {
            return redirect()->route('login')->with('error', 'Silakan login terlebih dahulu.');
        }

        if (!in_array(auth()->user()->email, $developerEmails)) {
            abort(403, 'AKSES DITOLAK. HALAMAN INI KHUSUS UNTUK DEVELOPER.');
        }

        return $next($request);
    }
}
