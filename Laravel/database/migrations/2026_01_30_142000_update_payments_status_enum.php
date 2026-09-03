<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Facades\DB;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        // Use raw SQL to alter enum because Doctrine DBAL has issues with enum modification sometimes
        // But Laravel Schema might handle it.
        // Safer to just modify the column definition.
        
        Schema::table('payments', function (Blueprint $table) {
             $table->enum('status', [
                'PENDING', 
                'VERIFIED', 
                'REJECTED', 
                'AUTO_VERIFIED', 
                'NEEDS_REVIEW', 
                'FAILED'
            ])->default('PENDING')->change();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
         Schema::table('payments', function (Blueprint $table) {
             $table->enum('status', ['PENDING', 'VERIFIED', 'REJECTED'])->default('PENDING')->change();
        });
    }
};
