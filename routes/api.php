<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\PaymentController;

Route::get('/user', function (Request $request) {
    return $request->user();
})->middleware('auth:sanctum');

Route::middleware('auth:sanctum')->group(function () {
    Route::post('/payments', [PaymentController::class, 'store'])->middleware('throttle:5,1');
    Route::get('/payments/{payment}/receipt', [\App\Http\Controllers\ReceiptController::class, 'download']);
});

Route::middleware(['auth:sanctum', 'role:admin'])->group(function () {
    Route::get('/admin/dashboard/stats', [\App\Http\Controllers\AdminDashboardController::class, 'stats']);
    Route::get('/admin/export-report', [\App\Http\Controllers\AdminDashboardController::class, 'exportReport']);
    Route::get('/admin/payments/review', [\App\Http\Controllers\AdminPaymentController::class, 'index']);
    Route::post('/admin/payments/{payment}/verify', [\App\Http\Controllers\AdminPaymentController::class, 'verify']);
    Route::post('/admin/adjustments', [\App\Http\Controllers\AdminAdjustmentController::class, 'store']);
});
