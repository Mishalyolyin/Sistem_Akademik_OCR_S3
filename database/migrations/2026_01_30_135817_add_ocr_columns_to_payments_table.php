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
        Schema::table('payments', function (Blueprint $table) {
            $table->string('processed_file_path')->nullable()->after('proof_file_path');
            $table->string('thumbnail_file_path')->nullable()->after('processed_file_path');
            $table->json('ocr_data')->nullable()->after('thumbnail_file_path'); // Prepare for OCR
        });
    }

    public function down(): void
    {
        Schema::table('payments', function (Blueprint $table) {
            $table->dropColumn(['processed_file_path', 'thumbnail_file_path', 'ocr_data']);
        });
    }
};
