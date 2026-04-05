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
        // 1. Table `tuition_rates`
        if (!Schema::hasColumn('tuition_rates', 'category')) {
             Schema::table('tuition_rates', function (Blueprint $table) {
                $table->enum('category', ['SEMESTER', 'MUNAOSAH'])->default('SEMESTER')->after('program_type');
                $table->dropUnique(['program_type', 'academic_year']);
                $table->unique(['program_type', 'academic_year', 'category']);
            });
        }

        // 2. Table `payment_plans`
        if (!Schema::hasColumn('payment_plans', 'category')) {
             Schema::table('payment_plans', function (Blueprint $table) {
                $table->enum('category', ['SEMESTER', 'MUNAOSAH'])->default('SEMESTER')->after('term');
            });
        }
        
        // Handle index changes for payment_plans
        // We do this in a separate schema call to ensure category exists
        Schema::table('payment_plans', function (Blueprint $table) {
             // We need to drop the foreign key first because it might depend on the unique index
             $table->dropForeign(['student_id']);
             
             // Now we can drop the unique index
             // We use try-catch or check existence? Laravel doesn't have hasIndex easily in Blueprint.
             // But we know it exists if it failed previously.
             // To be safe for re-runs where it might have succeeded, we can wrap in try-catch? 
             // No, migration files shouldn't be that complex.
             // We'll just assume standard state or broken state where it exists.
             
             // If the index was already renamed/dropped, this might fail.
             // But let's assume standard path.
             $table->dropUnique(['student_id', 'academic_year', 'term']);
             
             // Create new unique index
             $table->unique(['student_id', 'academic_year', 'term', 'category']);
             
             // Restore foreign key
             $table->foreign('student_id')->references('id')->on('students')->onDelete('cascade');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        // 1. Table `tuition_rates`
        if (Schema::hasColumn('tuition_rates', 'category')) {
            Schema::table('tuition_rates', function (Blueprint $table) {
                $table->dropUnique(['program_type', 'academic_year', 'category']);
                $table->unique(['program_type', 'academic_year']);
                $table->dropColumn('category');
            });
        }

        // 2. Table `payment_plans`
        if (Schema::hasColumn('payment_plans', 'category')) {
            Schema::table('payment_plans', function (Blueprint $table) {
                $table->dropForeign(['student_id']);
                $table->dropUnique(['student_id', 'academic_year', 'term', 'category']);
                $table->unique(['student_id', 'academic_year', 'term']);
                $table->foreign('student_id')->references('id')->on('students')->onDelete('cascade');
                $table->dropColumn('category');
            });
        }
    }
};
