<?php

namespace App\Http\Controllers;

use App\Models\Installment;
use App\Services\InstallmentBillingService;
use Illuminate\Http\Request;

class AdminInstallmentController extends Controller
{
    protected $billingService;

    public function __construct(InstallmentBillingService $billingService)
    {
        $this->billingService = $billingService;
    }

    public function updateAmount(Request $request, Installment $installment)
    {
        $request->validate([
            'amount' => 'required|numeric|min:0',
            'reason' => 'required|string|min:5',
        ]);

        try {
            $this->billingService->updateAmount($installment, (float) $request->amount, $request->reason, $request->user());

            $message = 'Tagihan berhasil diperbarui.';
            if ($request->wantsJson()) {
                return response()->json(['message' => $message]);
            }
            return redirect()->back()->with('success', $message);
        } catch (\Exception $e) {
            if ($request->wantsJson()) {
                return response()->json(['message' => $e->getMessage()], 400);
            }
            return redirect()->back()->with('error', $e->getMessage());
        }
    }
}
