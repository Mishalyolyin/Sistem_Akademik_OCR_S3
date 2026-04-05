<?php

namespace Tests\Unit;

use App\Services\SemesterService;
use Carbon\Carbon;
use PHPUnit\Framework\TestCase;

class SemesterServiceTest extends TestCase
{
    protected SemesterService $service;

    protected function setUp(): void
    {
        parent::setUp();
        $this->service = new SemesterService();
    }

    public function test_it_detects_gasal_semester_correctly()
    {
        // Sept 15 -> Gasal of current year
        $date = Carbon::create(2025, 9, 15);
        $result = $this->service->getActiveSemester($date);

        $this->assertEquals('2025/2026', $result['academic_year']);
        $this->assertEquals('GASAL', $result['term']);
    }

    public function test_it_detects_genap_semester_correctly()
    {
        // Feb 20 -> Genap of previous year start
        $date = Carbon::create(2026, 2, 20);
        $result = $this->service->getActiveSemester($date);

        // Academic year starts in 2025 (Sept), so Feb 2026 is 2025/2026 Genap
        $this->assertEquals('2025/2026', $result['academic_year']);
        $this->assertEquals('GENAP', $result['term']);
    }

    public function test_it_generates_schedule_correctly()
    {
        $startDate = Carbon::create(2025, 9, 1); // Sept 1st

        // Mock items (array of objects mimicking eloquent collection)
        $items = [
            (object)['installment_no' => 1, 'month_offset' => 0, 'day_of_month' => 20],
            (object)['installment_no' => 2, 'month_offset' => 1, 'day_of_month' => 20],
            (object)['installment_no' => 3, 'month_offset' => 2, 'day_of_month' => 20],
        ];

        $schedule = $this->service->generateSchedule($startDate, $items);

        $this->assertCount(3, $schedule);
        $this->assertEquals('2025-09-20', $schedule[0]['due_date']);
        $this->assertEquals('2025-10-20', $schedule[1]['due_date']);
        $this->assertEquals('2025-11-20', $schedule[2]['due_date']);
    }

    public function test_it_handles_february_date_overflow()
    {
        // Start Jan 1st, offset 1 month -> Feb. Item day is 30.
        $startDate = Carbon::create(2025, 1, 1);
        $items = [
            (object)['installment_no' => 1, 'month_offset' => 1, 'day_of_month' => 30],
        ];

        $schedule = $this->service->generateSchedule($startDate, $items);

        // Should snap to Feb 28 (non-leap)
        $this->assertEquals('2025-02-28', $schedule[0]['due_date']);
    }
}
