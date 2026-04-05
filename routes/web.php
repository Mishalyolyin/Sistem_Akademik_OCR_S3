<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\StudentImportController;
use App\Http\Controllers\PaymentPlanController;
use App\Http\Controllers\PaymentController;
use App\Http\Controllers\AdminDashboardController;
use App\Http\Controllers\AdminStudentController;
use App\Http\Controllers\AdminAdjustmentController;
use App\Http\Controllers\AdminTuitionController;
use App\Http\Controllers\AuthController;
use Illuminate\Support\Facades\Auth;

Route::get('/', function () {
    return view('welcome');
});

Route::get('/login', [AuthController::class, 'showLoginForm'])->name('login');
Route::post('/login', [AuthController::class, 'login']);

Route::post('/logout', function () {
    Auth::logout();
    return redirect('/');
})->name('logout');

Route::middleware(['auth'])->group(function () {
    Route::get('/payments/{payment}/receipt', [App\Http\Controllers\ReceiptController::class, 'download'])->name('payments.receipt.download');
});

Route::middleware(['auth', 'role:admin'])->group(function () {
    Route::get('/admin/dashboard', [AdminDashboardController::class, 'index'])->name('admin.dashboard');
    
    // Navigation Routes
    Route::get('/admin/payments', [\App\Http\Controllers\AdminPaymentController::class, 'indexView'])->name('admin.payments');
    Route::get('/admin/payments/reguler', [\App\Http\Controllers\AdminPaymentController::class, 'reguler'])->name('admin.payments.reguler');
    Route::get('/admin/payments/rpl', [\App\Http\Controllers\AdminPaymentController::class, 'rpl'])->name('admin.payments.rpl');
    Route::get('/admin/payments/munaqosah/reguler', [\App\Http\Controllers\AdminPaymentController::class, 'munaqosahReguler'])->name('admin.payments.munaqosah.reguler');
    Route::get('/admin/payments/munaqosah/rpl', [\App\Http\Controllers\AdminPaymentController::class, 'munaqosahRpl'])->name('admin.payments.munaqosah.rpl');

    Route::post('/admin/payments/{payment}/verify', [\App\Http\Controllers\AdminPaymentController::class, 'verify'])->name('admin.payments.verify');
    Route::post('/admin/payments/reset/{student}', [\App\Http\Controllers\AdminPaymentController::class, 'resetStudentInstallments'])->name('admin.payments.reset');
    Route::get('/admin/payments/export/{student}', [\App\Http\Controllers\AdminPaymentController::class, 'exportStudent'])->name('admin.payments.export');
    Route::get('/admin/payments/export-all/{programType}', [\App\Http\Controllers\AdminPaymentController::class, 'exportAll'])->name('admin.payments.export_all');
    
    // Tuition Rates (Total Tagihan)
    Route::get('/admin/tuition', [AdminTuitionController::class, 'index'])->name('admin.tuition');
    Route::post('/admin/tuition', [AdminTuitionController::class, 'store'])->name('admin.tuition.store');
    Route::delete('/admin/tuition/{tuitionRate}', [AdminTuitionController::class, 'destroy'])->name('admin.tuition.destroy');

    Route::get('/admin/students', [AdminStudentController::class, 'index'])->name('admin.students');
    Route::get('/admin/students/reguler', [AdminStudentController::class, 'reguler'])->name('admin.students.reguler');
    Route::get('/admin/students/rpl', [AdminStudentController::class, 'rpl'])->name('admin.students.rpl');
    Route::get('/admin/students/import', function() { return view('admin.students.import'); })->name('admin.students.import.form');
    Route::post('/admin/students/import', [StudentImportController::class, 'store'])->name('admin.students.import');
    Route::get('/admin/students/import/template', [StudentImportController::class, 'downloadTemplate'])->name('admin.students.import.template');
    Route::post('/admin/students/{student}/reset-password', [AdminStudentController::class, 'resetPassword'])->name('admin.students.reset_password');
    Route::get('/admin/students/{student}/edit', [AdminStudentController::class, 'edit'])->name('admin.students.edit');
    Route::put('/admin/students/{student}', [AdminStudentController::class, 'update'])->name('admin.students.update');
    Route::get('/admin/adjustments', [AdminAdjustmentController::class, 'index'])->name('admin.adjustments');
    Route::post('/admin/adjustments', [AdminAdjustmentController::class, 'store'])->name('admin.adjustments.store');
    
    // Class Management
    Route::resource('admin/classes', \App\Http\Controllers\AdminClassController::class)->names([
        'index' => 'admin.classes.index',
        'store' => 'admin.classes.store',
        'update' => 'admin.classes.update',
        'destroy' => 'admin.classes.destroy',
    ]);

    Route::get('/admin/reports', function() { return view('admin.reports.index'); })->name('admin.reports');
    Route::get('/admin/reports/export', [AdminDashboardController::class, 'exportExcel'])->name('admin.reports.export');
    Route::get('/admin/ml-settings', [\App\Http\Controllers\AdminMlSettingController::class, 'index'])->name('admin.ml_settings');
    Route::post('/admin/ml-settings', [\App\Http\Controllers\AdminMlSettingController::class, 'store'])->name('admin.ml_settings.store');
    Route::get('/admin/ml-settings/download-dataset', [\App\Http\Controllers\AdminMlSettingController::class, 'downloadDataset'])->name('admin.ml_settings.download_dataset');
});

Route::middleware(['auth', 'role:mahasiswa'])->group(function () {
    Route::get('/mahasiswa/dashboard', [PaymentPlanController::class, 'index'])->name('student.dashboard');
    Route::post('/mahasiswa/plan', [PaymentPlanController::class, 'store']);
    Route::post('/mahasiswa/payments', [PaymentController::class, 'store']);
    
    // Munaqosah Routes
    Route::get('/mahasiswa/munaqosah', [\App\Http\Controllers\MunaqosahController::class, 'index'])->name('student.munaqosah');
    Route::post('/mahasiswa/munaqosah', [\App\Http\Controllers\MunaqosahController::class, 'store'])->name('student.munaqosah.store');
    
    // Navigation Routes
    Route::get('/mahasiswa/history', function(\App\Services\SemesterService $semesterService) { 
        $user = auth()->user();
        $student = $user->student;
        
        $paidAmount = 0;
        $remainingAmount = 0;

        if ($student) {
            // Get Active Plan for Summary
            $today = \Carbon\Carbon::now();
            $semesterInfo = $semesterService->getActiveSemester($today);
            
            $activePlan = \App\Models\PaymentPlan::where('student_id', $student->id)
                ->where('academic_year', $semesterInfo['academic_year'])
                ->where('term', $semesterInfo['term'])
                ->with('installments')
                ->first();
                
            if ($activePlan) {
                $paidAmount = $activePlan->installments->where('status', 'PAID')->sum('amount');
                $remainingAmount = $activePlan->total_amount - $paidAmount;
            }
        }

        $payments = \App\Models\Payment::whereHas('student', function($q) {
            $q->where('user_id', auth()->id());
        })->with(['installment', 'paymentPlan'])->latest()->get();
        
        return view('student.history.index', compact('payments', 'paidAmount', 'remainingAmount')); 
    })->name('student.history');
    Route::get('/mahasiswa/profile', [\App\Http\Controllers\StudentProfileController::class, 'index'])->name('student.profile');
    Route::put('/mahasiswa/password', [\App\Http\Controllers\StudentProfileController::class, 'updatePassword'])->name('student.password.update');
});
