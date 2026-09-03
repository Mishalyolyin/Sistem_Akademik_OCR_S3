<?php

require __DIR__.'/vendor/autoload.php';

$app = require_once __DIR__.'/bootstrap/app.php';

$kernel = $app->make(Illuminate\Contracts\Console\Kernel::class);

$kernel->bootstrap();

$templates = App\Models\InstallmentTemplate::with('items')->get();

foreach ($templates as $t) {
    echo "ID: {$t->id} - {$t->name} - Items: {$t->items->count()}\n";
    if ($t->items->count() == 0) {
        // Fix empty templates (Munaqosah ones likely failed during seeding due to missing column error previously)
        if (strpos($t->name, 'Munaqosah') !== false) {
             echo "Fixing Munaqosah template ID {$t->id}...\n";
             App\Models\InstallmentTemplateItem::create([
                'installment_template_id' => $t->id,
                'installment_no' => 1,
                'month_offset' => 0,
                'day_of_month' => 10,
            ]);
            echo "Fixed.\n";
        }
    }
}
