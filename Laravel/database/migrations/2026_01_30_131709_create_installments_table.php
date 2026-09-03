<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('installments', function (Blueprint $table) {
            $table->id();
            $table->foreignId('payment_plan_id')->constrained()->onDelete('cascade');
            $table->integer('installment_no');
            $table->date('due_date');
            $table->decimal('amount', 15, 2); // Tagihan bulan ini
            $table->decimal('amount_paid', 15, 2)->default(0); // Yang sudah dibayar
            $table->enum('status', ['UNPAID', 'PARTIAL', 'PAID', 'OVERDUE'])->default('UNPAID');
            $table->timestamps();

            $table->unique(['payment_plan_id', 'installment_no']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('installments');
    }
};
