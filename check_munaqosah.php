<?php

require __DIR__.'/vendor/autoload.php';

$app = require_once __DIR__.'/bootstrap/app.php';

$kernel = $app->make(Illuminate\Contracts\Console\Kernel::class);

$kernel->bootstrap();

$count = App\Models\PaymentPlan::where('category', 'MUNAOSAH')->count();

if ($count > 0) {
    echo "Found $count Munaqosah plans.\n";
    $plans = App\Models\PaymentPlan::where('category', 'MUNAOSAH')->with('student')->get();
    foreach ($plans as $plan) {
        echo "Student: {$plan->student->name} (NIM: {$plan->student->nim}) - Status: {$plan->status}\n";
    }
} else {
    echo "No Munaqosah plans found.\n";
    // Check for eligible students
    $eligible = App\Models\Student::whereHas('paymentPlans', function($q) {
        $q->where('category', 'SEMESTER')
          ->where('status', 'ACTIVE')
          ->whereDoesntHave('installments', function($sq) {
              $sq->where('status', '!=', 'PAID');
          });
    })->first();

    if ($eligible) {
        echo "Found eligible student: {$eligible->name} (NIM: {$eligible->nim}). Creating Munaqosah plan...\n";
        
        // Create plan logic
        $controller = app(App\Http\Controllers\PaymentPlanController::class);
        // Mock request? No, better use service directly or seed it.
        
        // Let's manually create it to be safe
        $template = App\Models\InstallmentTemplate::where('program_type', $eligible->program_type)
            ->where('name', 'like', '%Munaqosah%')
            ->first();
            
        if ($template) {
             $service = app(App\Services\PaymentGenerationService::class);
             try {
                 $service->generatePlan($eligible, $template, 'MUNAOSAH');
                 echo "Created Munaqosah plan for {$eligible->name}.\n";
             } catch (\Exception $e) {
                 echo "Error creating plan: " . $e->getMessage() . "\n";
             }
        } else {
            echo "No Munaqosah template found.\n";
        }
    } else {
        echo "No eligible students found (fully paid semester). Checking for any student to force-pay...\n";
        $student = App\Models\Student::first();
        if ($student) {
             echo "Forcing student {$student->name} to be eligible...\n";
             // Find active plan
             $plan = App\Models\PaymentPlan::where('student_id', $student->id)->where('category', 'SEMESTER')->first();
             if ($plan) {
                 $plan->installments()->update(['status' => 'PAID']);
                 echo "All installments for {$student->name} marked as PAID.\n";
                 
                 // Now create Munaqosah plan
                 $template = App\Models\InstallmentTemplate::where('program_type', $student->program_type)
                    ->where('name', 'like', '%Munaqosah%')
                    ->first();
                 if ($template) {
                     $service = app(App\Services\PaymentGenerationService::class);
                     $service->generatePlan($student, $template, 'MUNAOSAH');
                     echo "Created Munaqosah plan for {$student->name}.\n";
                 }
             }
        }
    }
}
