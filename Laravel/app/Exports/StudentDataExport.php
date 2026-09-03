<?php

namespace App\Exports;

use App\Models\Student;
use Maatwebsite\Excel\Concerns\FromCollection;
use Maatwebsite\Excel\Concerns\WithHeadings;
use Maatwebsite\Excel\Concerns\WithMapping;
use Maatwebsite\Excel\Concerns\WithStyles;
use Maatwebsite\Excel\Concerns\WithEvents;
use Maatwebsite\Excel\Concerns\ShouldAutoSize;
use Maatwebsite\Excel\Events\AfterSheet;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;
use PhpOffice\PhpSpreadsheet\Style\Fill;

class StudentDataExport implements FromCollection, WithHeadings, WithMapping, WithStyles, WithEvents, ShouldAutoSize
{
    protected $programType;

    // 1-indexed spreadsheet columns (after the header row) for conditional highlighting.
    const COL_IJAZAH_OCR = 9;
    const COL_KTP_NIK_COCOK = 12;
    const COL_KK_NOMOR_COCOK = 15;
    const COL_KK_NAMA_DITEMUKAN = 16;

    // Columns whose value 'Tidak'/'Tidak Cocok'/'Tidak Ditemukan' should be
    // highlighted pink to flag a mismatch for admin review.
    const MISMATCH_COLUMNS = [
        self::COL_IJAZAH_OCR => 'Tidak Cocok',
        self::COL_KTP_NIK_COCOK => 'Tidak',
        self::COL_KK_NOMOR_COCOK => 'Tidak',
        self::COL_KK_NAMA_DITEMUKAN => 'Tidak Ditemukan',
    ];

    public function __construct(string $programType)
    {
        $this->programType = $programType;
    }

    public function collection()
    {
        return Student::where('program_type', $this->programType)
            ->with(['paymentPlans' => function ($q) {
                $q->whereIn('category', ['MUNAOSAH', 'PENDAFTARAN'])->with(['installments', 'munaqosahDetail']);
            }])
            ->orderBy('class', 'asc')
            ->orderBy('name', 'asc')
            ->get();
    }

    public function map($student): array
    {
        $ijazahOcr = $student->ijazah_ocr_data;
        $ktpOcr = $student->ktp_ocr_data;
        $kkOcr = $student->kk_ocr_data;

        $ijazahOcrLabel = '-';
        if ($student->ijazah_file_path) {
            $ijazahOcrLabel = $ijazahOcr
                ? (($ijazahOcr['name_match'] ?? false) ? 'Nama Cocok' : 'Tidak Cocok')
                : 'Belum Terbaca';
        }

        $ktpNikCocokLabel = '-';
        if ($student->ktp_file_path) {
            $ktpNikCocokLabel = $ktpOcr
                ? (($student->nik && ($ktpOcr['nik'] ?? null) === $student->nik) ? 'Ya' : 'Tidak')
                : 'Belum Terbaca';
        }

        $kkNomorCocokLabel = '-';
        $kkNamaDitemukanLabel = '-';
        if ($student->kk_file_path) {
            $kkNomorCocokLabel = $kkOcr
                ? (($student->kk_number && ($kkOcr['kk_number'] ?? null) === $student->kk_number) ? 'Ya' : 'Tidak')
                : 'Belum Terbaca';
            $kkNamaDitemukanLabel = $kkOcr
                ? (($kkOcr['name_found_in_family'] ?? false) ? 'Ya' : 'Tidak Ditemukan')
                : 'Belum Terbaca';
        }

        $munaqosahPlan = $student->paymentPlans->firstWhere('category', 'MUNAOSAH');
        $munaqosahDetail = $munaqosahPlan?->munaqosahDetail;

        $munaqosahStatus = 'Belum Eligible';
        if ($munaqosahPlan) {
            $installments = $munaqosahPlan->installments;
            $allPaid = $installments->isNotEmpty() && $installments->every(fn($i) => $i->status === 'PAID');
            $munaqosahStatus = $allPaid ? 'Lunas' : 'Sedang Berjalan';
        }

        $pendaftaranPlan = $student->paymentPlans->firstWhere('category', 'PENDAFTARAN');
        $pendaftaranStatus = 'Belum Bayar';
        if ($student->pendaftaran_exempt) {
            $pendaftaranStatus = 'Exempt (Mahasiswa Lama)';
        } elseif ($pendaftaranPlan) {
            $installments = $pendaftaranPlan->installments;
            $allPaid = $installments->isNotEmpty() && $installments->every(fn($i) => $i->status === 'PAID');
            $pendaftaranStatus = $allPaid ? 'Lunas' : 'Sedang Berjalan';
        }

        return [
            $student->nim,
            $student->name,
            $student->class ?? '-',
            $student->program_type,
            $student->start_term . ' ' . ($student->academic_year ?? '-'),
            $student->phone ?? '-',
            $student->is_alumni ? 'Ya' : 'Tidak',
            $student->ijazah_file_path ? 'Ya' : 'Belum',
            $ijazahOcrLabel,
            $student->nik ?? '-',
            $student->ktp_file_path ? 'Ya' : 'Belum',
            $ktpNikCocokLabel,
            $student->kk_number ?? '-',
            $student->kk_file_path ? 'Ya' : 'Belum',
            $kkNomorCocokLabel,
            $kkNamaDitemukanLabel,
            $student->birth_place ?? '-',
            optional($student->birth_date)->format('d/m/Y') ?? '-',
            $student->address ?? '-',
            $pendaftaranStatus,
            $munaqosahStatus,
            $munaqosahDetail->supervisor_name ?? '-',
            $munaqosahDetail->supervisor_name_2 ?? '-',
            $munaqosahDetail->thesis_title ?? '-',
            $munaqosahDetail ? ($munaqosahDetail->thesis_file_path ? 'Ya' : 'Belum') : '-',
            $munaqosahDetail ? ($munaqosahDetail->article_file_path ? 'Ya' : 'Belum') : '-',
        ];
    }

    public function headings(): array
    {
        return [
            'NIM',
            'Nama',
            'Kelas',
            'Program',
            'Angkatan',
            'No HP',
            'Status Alumni',
            'Ijazah Upload',
            'Ijazah OCR',
            'No. KTP',
            'KTP Upload',
            'KTP NIK Cocok',
            'No. KK',
            'KK Upload',
            'KK Nomor Cocok',
            'KK Nama Ditemukan',
            'Tempat Lahir',
            'Tanggal Lahir',
            'Alamat',
            'Status Bukti Pendaftaran',
            'Status Munaqosah',
            'Dosen Pembimbing 1',
            'Dosen Pembimbing 2',
            'Judul Skripsi',
            'File Skripsi',
            'File Artikel',
        ];
    }

    public function styles(Worksheet $sheet)
    {
        return [
            1 => ['font' => ['bold' => true], 'fill' => [
                'fillType' => Fill::FILL_SOLID,
                'startColor' => ['rgb' => 'F3F4F6'],
            ]],
        ];
    }

    public function registerEvents(): array
    {
        return [
            AfterSheet::class => function (AfterSheet $event) {
                $sheet = $event->sheet->getDelegate();
                $highestRow = $sheet->getHighestRow();
                $highestColumn = $sheet->getHighestColumn();

                $sheet->setAutoFilter("A1:{$highestColumn}1");
                $sheet->freezePane('A2');

                for ($row = 2; $row <= $highestRow; $row++) {
                    foreach (self::MISMATCH_COLUMNS as $column => $mismatchValue) {
                        $cell = $sheet->getCellByColumnAndRow($column, $row);
                        if ($cell->getValue() === $mismatchValue) {
                            $sheet->getStyleByColumnAndRow($column, $row)->getFill()
                                ->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB('FCE7F3');
                        }
                    }
                }
            },
        ];
    }
}
