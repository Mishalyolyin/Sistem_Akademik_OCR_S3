@php
$s = $semesterInfo ?? ($semesterAktif ?? null);
$term = is_array($s) ? ($s['term'] ?? $s['kode'] ?? $s['name'] ?? null) : (is_string($s) ? $s : null);
$year = is_array($s) ? ($s['academic_year'] ?? $s['tahun'] ?? null) : null;
$periode = is_array($s) ? ($s['periode'] ?? null) : null;
@endphp
@if($term || $year || $periode)
<div class="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100 text-blue-900 px-6 py-4 rounded-xl shadow-sm text-center">
    <div class="font-semibold text-lg tracking-tight">
        Semester Aktif:
        <span class="text-blue-700">{{ strtoupper(trim(($term ? $term : ''))) }}</span>
        @if($year)
            <span class="text-blue-700">{{ $year }}</span>
        @endif
    </div>
    @if($periode)
        <div class="text-sm text-blue-600 mt-1 font-medium bg-white/50 inline-block px-3 py-1 rounded-full border border-blue-100">
            Periode {{ $periode }}
        </div>
    @endif
</div>
@endif

