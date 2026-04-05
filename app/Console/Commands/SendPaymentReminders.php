<?php

namespace App\Console\Commands;

use App\Models\Installment;
use App\Services\WhatsappService;
use Carbon\Carbon;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Log;

class SendPaymentReminders extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'payments:send-reminders';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Send WhatsApp reminders for upcoming and overdue payments';

    /**
     * Execute the console command.
     */
    public function handle(WhatsappService $whatsappService)
    {
        $this->info("Starting payment reminders...");
        
        $today = Carbon::now()->startOfDay();
        $threeDaysLater = $today->copy()->addDays(3);
        
        // 1. H-3 Reminders (Upcoming)
        // Find installments due exactly on $threeDaysLater
        $upcomingInstallments = Installment::whereDate('due_date', $threeDaysLater)
            ->where('status', '!=', 'PAID')
            ->with(['paymentPlan.student'])
            ->get();

        $this->info("Found " . $upcomingInstallments->count() . " upcoming installments (H-3).");

        foreach ($upcomingInstallments as $inst) {
            $student = $inst->paymentPlan->student;
            if ($student && $student->phone) {
                $amount = number_format($inst->amount - $inst->amount_paid, 0, ',', '.');
                $dueDate = Carbon::parse($inst->due_date)->format('d M Y');
                
                $msg = "Halo {$student->name}, Mengingatkan tagihan Angsuran Ke-{$inst->installment_no} sebesar Rp {$amount} akan jatuh tempo pada {$dueDate} (3 hari lagi). Mohon segera lakukan pembayaran.";
                
                $whatsappService->send($student->phone, $msg);
                $this->info("Sent H-3 reminder to {$student->name}");
            }
        }

        // 2. Overdue Reminders (Weekly - e.g., Every Monday)
        if ($today->isMonday()) {
            $this->info("Today is Monday. Sending Overdue Reminders...");
            
            $overdueInstallments = Installment::whereDate('due_date', '<', $today)
                ->where('status', '!=', 'PAID')
                ->with(['paymentPlan.student'])
                ->get();

            $this->info("Found " . $overdueInstallments->count() . " overdue installments.");

            foreach ($overdueInstallments as $inst) {
                $student = $inst->paymentPlan->student;
                if ($student && $student->phone) {
                    $amount = number_format($inst->amount - $inst->amount_paid, 0, ',', '.');
                    $dueDate = Carbon::parse($inst->due_date)->format('d M Y');
                    $daysLate = $today->diffInDays($inst->due_date);

                    $msg = "Halo {$student->name}, Tagihan Angsuran Ke-{$inst->installment_no} sebesar Rp {$amount} telah LEWAT JATUH TEMPO ({$dueDate}). Harap segera lunasi untuk menghindari sanksi akademik.";
                    
                    $whatsappService->send($student->phone, $msg);
                    $this->info("Sent Overdue reminder to {$student->name}");
                }
            }
        } else {
            $this->info("Today is not Monday. Skipping Overdue Reminders.");
        }

        $this->info("Done.");
    }
}
