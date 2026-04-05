<?php

namespace App\Console\Commands;

use App\Services\DatasetExportService;
use Illuminate\Console\Command;

class ExportDatasetCommand extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'app:export-dataset {--limit=100 : Number of records to export}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Export verified payments as a dataset for ML training';

    /**
     * Execute the console command.
     */
    public function handle(DatasetExportService $service)
    {
        $limit = (int) $this->option('limit');
        $this->info("Exporting dataset with limit: $limit...");

        try {
            $path = $service->export($limit);
            $this->info("Dataset exported successfully!");
            $this->info("Path: $path");
        } catch (\Exception $e) {
            $this->error("Error: " . $e->getMessage());
            return 1;
        }

        return 0;
    }
}
