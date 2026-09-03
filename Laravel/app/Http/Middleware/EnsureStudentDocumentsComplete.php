<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class EnsureStudentDocumentsComplete
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $student = $request->user()?->student;

        $exemptRoutes = [
            'student.documents',
            'student.documents.photo',
            'student.documents.ktp',
            'student.documents.kk',
            'student.documents.ijazah',
            'student.documents.address',
            'logout',
        ];

        if ($student && !$student->hasCompletedDocuments() && !in_array($request->route()?->getName(), $exemptRoutes)) {
            return redirect()->route('student.documents');
        }

        return $next($request);
    }
}
