<?php

namespace App\Http\Controllers;

use App\Models\Adjustment;
use App\Models\Installment;
use App\Models\Student;
use App\Services\AdjustmentService;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class AdminAdjustmentController extends Controller
{
    protected $adjustmentService;

    public function __construct(AdjustmentService $adjustmentService)
    {
        $this->adjustmentService = $adjustmentService;
    }

    public function index(Request $request)
    {
        $search = $request->query('search');
        $selectedStudent = null;

        if ($search) {
            $selectedStudent = Student::where('nim', $search)
                ->orWhere('name', 'like', "%{$search}%")
                ->with(['paymentPlans' => function($q) {
                    $q->latest()->where('status', 'ACTIVE');
                }, 'paymentPlans.installments', 'user'])
                ->first();
        }

        $adjustments = Adjustment::with(['admin', 'adjustable'])
            ->latest()
            ->paginate(10);

        return view('admin.adjustments.index', compact('adjustments', 'selectedStudent', 'search'));
    }

    public function store(Request $request)
    {
        $request->validate([
            'target_type' => ['required', Rule::in(['installment', 'student'])],
            'target_id' => 'required|integer',
            'amount' => 'required|numeric|not_in:0',
            'reason' => 'required|string|min:5',
        ]);

        try {
            if ($request->target_type === 'installment') {
                $target = Installment::findOrFail($request->target_id);
                // Find related student for redirect context if needed, though we rely on search param
            } else {
                $target = Student::findOrFail($request->target_id);
            }

            $adjustment = $this->adjustmentService->createAdjustment(
                $target,
                $request->amount,
                $request->reason,
                $request->user()
            );

            if ($request->wantsJson()) {
                return response()->json([
                    'message' => 'Adjustment created successfully',
                    'adjustment' => $adjustment,
                    'new_balance' => $request->target_type === 'installment' 
                        ? $target->fresh()->amount_paid 
                        : $target->fresh()->wallet_balance
                ], 201);
            }

            return redirect()->back()->with('success', 'Adjustment created successfully');

        } catch (\Exception $e) {
            if ($request->wantsJson()) {
                return response()->json(['message' => $e->getMessage()], 400);
            }
            return redirect()->back()->with('error', $e->getMessage());
        }
    }
}
