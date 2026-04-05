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
        Schema::create('installment_templates', function (Blueprint $table) {
            $table->id();
            $table->enum('program_type', ['REGULER', 'RPL']);
            $table->enum('start_term', ['GASAL', 'GENAP']);
            $table->integer('installments_count'); // 4, 6, 8, 10
            $table->string('name');
            $table->boolean('active')->default(true);
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('installment_templates');
    }
};
