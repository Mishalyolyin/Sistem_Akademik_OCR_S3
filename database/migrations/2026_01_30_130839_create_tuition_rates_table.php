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
        Schema::create('tuition_rates', function (Blueprint $table) {
            $table->id();
            $table->enum('program_type', ['REGULER', 'RPL']);
            $table->string('academic_year'); // e.g. "2025/2026"
            $table->decimal('amount', 15, 2);
            $table->string('description')->nullable();
            $table->boolean('active')->default(true);
            $table->timestamps();

            $table->unique(['program_type', 'academic_year']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('tuition_rates');
    }
};
