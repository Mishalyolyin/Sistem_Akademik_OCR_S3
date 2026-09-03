<?php

namespace App\Http\Middleware;

use App\Models\Installment;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class EnsureRegistrationFeePaid
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $student = $request->user()?->student;

        $exemptRoutes = ['student.pendaftaran', 'student.pendaftaran.store', 'logout'];

        if ($student && !$student->pendaftaran_exempt && !in_array($request->route()?->getName(), $exemptRoutes)) {
            $paid = Installment::whereHas('paymentPlan', function ($q) use ($student) {
                $q->where('student_id', $student->id)->where('category', 'PENDAFTARAN');
            })->where('status', 'PAID')->exists();

            if (!$paid) {
                return redirect()->route('student.pendaftaran');
            }
        }

        return $next($request);
    }
}
