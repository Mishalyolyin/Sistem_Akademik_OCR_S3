<?php

namespace App\Http\Controllers;

use App\Models\Payment;
use Barryvdh\DomPDF\Facade\Pdf;
use Illuminate\Http\Request;

class ReceiptController extends Controller
{
    public function download(Payment $payment)
    {
        // Ensure the user is authorized to view this receipt
        // For simplicity, we allow the owner (student) or admin
        $user = auth()->user();
        
        if ($user->role !== 'admin' && $user->id !== $payment->student->user_id) {
            abort(403, 'Unauthorized action.');
        }

        // Receipt is only available for verified payments
        if ($payment->status !== 'VERIFIED') {
            abort(400, 'Receipt is only available for verified payments.');
        }

        $pdf = Pdf::loadView('pdf.receipt', ['payment' => $payment]);
        
        return $pdf->download('receipt-' . $payment->id . '.pdf');
    }
}
