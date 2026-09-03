<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        DB::statement("ALTER TABLE payment_plans MODIFY category ENUM('SEMESTER', 'MUNAOSAH', 'PENDAFTARAN', 'KERJASAMA') DEFAULT 'SEMESTER'");
        DB::statement("ALTER TABLE tuition_rates MODIFY category ENUM('SEMESTER', 'MUNAOSAH', 'PENDAFTARAN', 'KERJASAMA') DEFAULT 'SEMESTER'");
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        DB::statement("ALTER TABLE payment_plans MODIFY category ENUM('SEMESTER', 'MUNAOSAH', 'PENDAFTARAN') DEFAULT 'SEMESTER'");
        DB::statement("ALTER TABLE tuition_rates MODIFY category ENUM('SEMESTER', 'MUNAOSAH', 'PENDAFTARAN') DEFAULT 'SEMESTER'");
    }
};
