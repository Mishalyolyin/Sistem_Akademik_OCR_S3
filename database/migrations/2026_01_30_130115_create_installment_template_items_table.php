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
        Schema::create('installment_template_items', function (Blueprint $table) {
            $table->id();
            $table->foreignId('installment_template_id')->constrained('installment_templates')->cascadeOnDelete();
            $table->integer('installment_no');
            $table->integer('month_offset');
            $table->integer('day_of_month')->default(20);
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('installment_template_items');
    }
};
