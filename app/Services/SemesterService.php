<?php

namespace App\Services;

use Carbon\Carbon;

class SemesterService
{
    /**
     * Get the current active academic year and term based on date.
     *
     * Rules (Common in ID):
     * - Gasal (Odd): Sept - Feb (Starts ~Aug/Sept)
     * - Genap (Even): March - Aug (Starts ~Feb/March)
     *
     * @param Carbon|null $date
     * @return array ['academic_year' => '2025/2026', 'term' => 'GASAL']
     */
    public function getActiveSemester(?Carbon $date = null): array
    {
        $date = $date ?? now();
        $month = $date->month;
        $year = $date->year;

        // Logic:
        // If month is >= 8 (August) -> Start of Odd Semester of (Year)/(Year+1)
        // If month is < 8 (Jan-July) -> Even Semester of (Year-1)/(Year) if month >= 2 (Feb)
        // Actually, let's simplify standard Indo univ:
        // Gasal: August - January
        // Genap: February - July

        if ($month >= 8) { // Aug - Dec
            $academicYear = $year . '/' . ($year + 1);
            $term = 'GASAL';
        } elseif ($month == 1) { // Jan (End of Gasal)
            $academicYear = ($year - 1) . '/' . $year;
            $term = 'GASAL';
        } else { // Feb - July (Genap)
            $academicYear = ($year - 1) . '/' . $year;
            $term = 'GENAP';
        }

        return [
            'academic_year' => $academicYear,
            'term' => $term,
        ];
    }

    /**
     * Generate installment schedule dates based on template items.
     *
     * @param Carbon $startDate
     * @param \Illuminate\Database\Eloquent\Collection $templateItems
     * @return array
     */
    public function generateSchedule(Carbon $startDate, $templateItems): array
    {
        $schedule = [];

        foreach ($templateItems as $item) {
            // Calculate due date based on month_offset
            // month_offset 0 = same month as start date
            $dueDate = $startDate->copy()
                ->addMonths($item->month_offset)
                ->setDay($item->day_of_month);

            // Handle edge case where day doesn't exist (e.g. Feb 30) -> automatically becomes March 1/2
            // Carbon handles this by overflow. If strict required, use endOfMonth.
            // For installments, usually we want exact day or end of month.
            if ($dueDate->day !== $item->day_of_month) {
                // If overflowed (e.g. set Feb 30 -> March 2), snap back to end of previous month (Feb 28)
                $dueDate = $startDate->copy()
                    ->addMonths($item->month_offset)
                    ->endOfMonth();
            }

            $schedule[] = [
                'installment_no' => $item->installment_no,
                'due_date' => $dueDate->format('Y-m-d'),
            ];
        }

        return $schedule;
    }
}
