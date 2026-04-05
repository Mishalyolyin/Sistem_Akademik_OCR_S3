<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class WhatsappService
{
    protected $apiKey;
    protected $endpoint;

    public function __construct()
    {
        // Default to Fonnte or similar structure, can be configured in .env
        $this->apiKey = env('WA_API_KEY', 'demo-key'); 
        $this->endpoint = env('WA_ENDPOINT', 'https://api.fonnte.com/send');
    }

    /**
     * Send a WhatsApp message.
     *
     * @param string $target Phone number (e.g., '08123456789')
     * @param string $message The message content
     * @return bool
     */
    public function send(string $target, string $message): bool
    {
        // 1. Sanitize Phone Number (Indonesia specific)
        // Ensure it starts with 62 or 08, but gateways usually prefer 08 or 62.
        // Let's assume the gateway handles '08'.
        
        if (empty($target)) {
            Log::warning("WhatsappService: Target number is empty.");
            return false;
        }

        // Development Mode: Don't actually send, just log
        if (env('APP_ENV') === 'local') {
            Log::channel('single')->info("WA_MOCK_SEND to {$target}: {$message}");
            return true;
        }

        try {
            $response = Http::withHeaders([
                'Authorization' => $this->apiKey,
            ])->post($this->endpoint, [
                'target' => $target,
                'message' => $message,
                'countryCode' => '62', // Optional, depends on gateway
            ]);

            if ($response->successful()) {
                Log::info("WhatsappService: Message sent to {$target}");
                return true;
            } else {
                Log::error("WhatsappService: Failed to send to {$target}. Status: " . $response->status() . " Body: " . $response->body());
                return false;
            }
        } catch (\Exception $e) {
            Log::error("WhatsappService: Exception sending to {$target}: " . $e->getMessage());
            return false;
        }
    }
}
