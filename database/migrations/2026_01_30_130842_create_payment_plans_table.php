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
        Schema::create('payment_plans', function (Blueprint $table) {
            $table->id();
            $table->foreignId('student_id')->constrained()->onDelete('cascade');
            $table->foreignId('installment_template_id')->constrained('installment_templates');
            $table->string('academic_year');
            $table->enum('term', ['GASAL', 'GENAP']);
            $table->decimal('total_amount', 15, 2); // Snapshot of tuition rate
            $table->enum('status', ['ACTIVE', 'COMPLETED', 'CANCELLED'])->default('ACTIVE');
            $table->timestamps();

            $table->unique(['student_id', 'academic_year', 'term']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('payment_plans');
    }
};
