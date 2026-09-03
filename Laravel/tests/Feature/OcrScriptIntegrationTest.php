<?php

namespace Tests\Feature;

use App\Services\OcrService;
use Tests\TestCase;
use Illuminate\Support\Facades\File;

class OcrScriptIntegrationTest extends TestCase
{
    /**
     * Test that the Python script is callable and returns valid JSON.
     * Even if OCR libs are missing, it should return a JSON with error/status.
     */
    public function test_ocr_script_execution()
    {
        $service = new OcrService();
        
        // Use a dummy image path (doesn't need to exist for the script to start, 
        // but the script checks for existence, so we should provide a real file)
        $imagePath = base_path('tests/fixtures/sample_receipt.jpg');
        
        // Create a dummy file if not exists
        if (!File::exists(dirname($imagePath))) {
            File::makeDirectory(dirname($imagePath), 0755, true);
        }
        File::put($imagePath, 'dummy content');

        try {
            $result = $service->processImage($imagePath);
            
            // We expect an array
            $this->assertIsArray($result);
            
            // It should have either 'extracted_amount' (success) or 'error' (missing libs/file error)
            $hasKey = array_key_exists('extracted_amount', $result) || array_key_exists('error', $result);
            $this->assertTrue($hasKey, 'Result should contain extracted_amount or error. Got: ' . json_encode($result));
            
            if (array_key_exists('error', $result)) {
                // If error, it should probably be about libraries or file validity
                // Since we sent a text file as image, opencv might fail or libs might be missing
                $this->assertNotEmpty($result['error']);
            }
            
        } catch (\Exception $e) {
            $this->fail("OCR Script failed to execute: " . $e->getMessage());
        } finally {
            // Cleanup
            if (File::exists($imagePath)) {
                File::delete($imagePath);
            }
        }
    }
}
