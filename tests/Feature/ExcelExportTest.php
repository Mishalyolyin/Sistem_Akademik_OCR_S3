<?php

namespace Tests\Feature;

use Tests\TestCase;
use App\Models\User;
use App\Models\Student;
use App\Models\PaymentPlan;
use App\Models\Installment;
use App\Models\Payment;
use App\Exports\StudentLedgerExport;
use App\Exports\StudentLedgerSheetExport;
use Maatwebsite\Excel\Facades\Excel;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Carbon\Carbon;

class ExcelExportTest extends TestCase
{
    use RefreshDatabase;

    public function test_student_ledger_export_sheets()
    {
        // 1. Setup Data
        $user = User::factory()->create();
        $batch = \App\Models\ImportBatch::create([
            'file_name' => 'test.xlsx',
            'imported_by_admin_id' => $user->id,
            'summary_json' => []
        ]);

        // Student Class A
        Student::create([
            'user_id' => $user->id,
            'nim' => '21502500006',
            'name' => 'Abdul Ghafur',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'class' => 'A-1',
            'import_batch_id' => $batch->id,
        ]);

        // Student Class B
        Student::create([
            'user_id' => User::factory()->create()->id,
            'nim' => '21502500007',
            'name' => 'Ahsin',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'class' => 'B-2',
            'import_batch_id' => $batch->id,
        ]);

        // 2. Instantiate Main Export
        $export = new StudentLedgerExport('REGULER');
        $sheets = $export->sheets();

        // 3. Verify Sheets Count
        // Should have 2 sheets (A-1, B-2)
        $this->assertCount(2, $sheets);
        $this->assertInstanceOf(StudentLedgerSheetExport::class, $sheets[0]);
    }

    public function test_sheet_export_structure()
    {
        // 1. Setup Data
        $user = User::factory()->create();
        $batch = \App\Models\ImportBatch::create([
            'file_name' => 'test.xlsx',
            'imported_by_admin_id' => $user->id,
            'summary_json' => []
        ]);

        $student = Student::create([
            'user_id' => $user->id,
            'nim' => '21502500006',
            'name' => 'Abdul Ghafur Temrawut',
            'program_type' => 'REGULER',
            'start_term' => 'GASAL',
            'class' => 'A-1',
            'import_batch_id' => $batch->id,
        ]);

        $template = \App\Models\InstallmentTemplate::create([
            'name' => 'Test Template',
            'program_type' => 'REGULER',
            'academic_year' => '2025/2026',
            'total_amount' => 5000000,
            'installments_count' => 6,
        ]);

        $plan = PaymentPlan::create([
            'student_id' => $student->id,
            'academic_year' => '2025/2026',
            'term' => 'GASAL',
            'total_amount' => 5000000,
            'installment_template_id' => $template->id,
        ]);

        $inst1 = Installment::create([
            'payment_plan_id' => $plan->id,
            'installment_no' => 1,
            'amount' => 1000000,
            'due_date' => Carbon::now()->addMonth(),
        ]);

        Payment::create([
            'student_id' => $student->id,
            'installment_id' => $inst1->id,
            'amount' => 1000000,
            'status' => 'VERIFIED',
            'paid_at' => Carbon::now(),
            'payment_method' => 'MANUAL',
            'proof_file_path' => 'dummy/path.jpg',
        ]);

        // 2. Instantiate Sheet Export directly
        $sheetExport = new StudentLedgerSheetExport('REGULER', 'A-1');

        // 3. Check Headings
        $headings = $sheetExport->headings();
        // Row 1
        $this->assertEquals('No.', $headings[0][0]);
        $this->assertEquals('Nama', $headings[0][1]);
        $this->assertEquals('NIM & Username SIM', $headings[0][2]);
        $this->assertEquals('Password', $headings[0][3]);
        $this->assertEquals('Pembayaran', $headings[0][4]);
        
        // Row 2
        $this->assertEquals('Pendaftaran', $headings[1][4]);

        // 4. Check Mapping
        $collection = $sheetExport->collection();
        $this->assertNotEmpty($collection);
        
        $mapped = $sheetExport->map($collection->first());
        
        // Check Row Content
        $this->assertEquals(1, $mapped[0]); // No.
        $this->assertEquals('Abdul Ghafur Temrawut', $mapped[1]); // Name
        $this->assertEquals('21502500006', $mapped[2]); // NIM
        $this->assertEquals('', $mapped[3]); // Password
        // Pendaftaran
        $this->assertNotEmpty($mapped[4]);
    }
}
