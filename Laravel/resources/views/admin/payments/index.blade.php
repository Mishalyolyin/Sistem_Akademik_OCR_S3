@extends('layouts.admin')

@section('content')
<div class="mb-8 flex flex-col lg:flex-row lg:justify-between lg:items-end gap-4">
    <div>
        <h1 class="text-3xl font-bold text-gray-800 mb-2">{{ $pageTitle ?? 'Verifikasi Tagihan' }}</h1>
        <p class="text-gray-500 text-sm">Kelola dan verifikasi bukti pembayaran mahasiswa dengan teliti.</p>
    </div>
    <div class="flex flex-col md:flex-row gap-2 w-full lg:w-auto">
        <form action="{{ request()->url() }}" method="GET" class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 w-full">
            @if(request('view'))
                <input type="hidden" name="view" value="{{ request('view') }}">
            @endif
            @if(request('status'))
                <input type="hidden" name="status" value="{{ request('status') }}">
            @endif

            <!-- Academic Year Filter Dropdown -->
            <div class="relative w-full sm:w-auto">
                <select name="academic_year" onchange="this.form.submit()" class="w-full sm:w-auto pl-4 pr-8 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent appearance-none bg-white shadow-sm">
                    <option value="">Semua Periode</option>
                    @if(isset($availableAcademicYears))
                        @foreach($availableAcademicYears as $year)
                            <option value="{{ $year['value'] }}" {{ request('academic_year') == $year['value'] ? 'selected' : '' }}>
                                {{ $year['label'] }}
                            </option>
                        @endforeach
                    @endif
                </select>
                <div class="absolute right-3 top-2.5 text-gray-400 pointer-events-none">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                </div>
            </div>

            <div class="relative w-full sm:w-auto">
                <input type="text" name="search" value="{{ request('search') }}" placeholder="Cari Nama/NIM/Kelas..." class="w-full sm:w-auto pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent shadow-sm">
                <div class="absolute left-3 top-2.5 text-gray-400">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                </div>
            </div>
        </form>
        
        <div class="flex flex-wrap gap-2">
            <a href="{{ $category === 'KERJASAMA' ? route('admin.payments.export_all_kerjasama') : route('admin.payments.export_all', ['programType' => $programType]) }}" class="whitespace-nowrap px-4 py-2 rounded-lg text-sm font-bold bg-emerald-600 text-white hover:bg-emerald-700 transition-colors flex items-center gap-2 shadow-sm">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                Export Semua
            </a>
            <a href="{{ request()->fullUrlWithQuery(['view' => 'group']) }}" class="whitespace-nowrap px-4 py-2 rounded-lg text-sm font-bold transition-colors shadow-sm {{ ($viewMode ?? 'group') == 'group' ? 'bg-blue-100 text-blue-700 border border-blue-200' : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50' }}">
                Tampilan Group
            </a>
            <a href="{{ request()->fullUrlWithQuery(['view' => 'flat']) }}" class="whitespace-nowrap px-4 py-2 rounded-lg text-sm font-bold transition-colors shadow-sm {{ ($viewMode ?? 'group') == 'flat' ? 'bg-blue-100 text-blue-700 border border-blue-200' : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50' }}">
                Tampilan Flat
            </a>
        </div>
    </div>
</div>

<div id="payments-content" style="transition: opacity 150ms ease-in-out;">
<div class="mb-6 flex flex-wrap gap-2" id="status-tabs">
    @php
        $statusTabs = [
            null => ['label' => 'Semua', 'active' => 'bg-gray-800 text-white border border-gray-800', 'inactive' => 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'],
            'pending' => ['label' => 'Menunggu Verifikasi', 'active' => 'bg-amber-500 text-white border border-amber-500', 'inactive' => 'bg-amber-50 border border-amber-100 text-amber-700 hover:bg-amber-100'],
            'rejected' => ['label' => 'Ditolak', 'active' => 'bg-red-500 text-white border border-red-500', 'inactive' => 'bg-red-50 border border-red-100 text-red-700 hover:bg-red-100'],
            'verified' => ['label' => 'Terverifikasi', 'active' => 'bg-emerald-500 text-white border border-emerald-500', 'inactive' => 'bg-emerald-50 border border-emerald-100 text-emerald-700 hover:bg-emerald-100'],
        ];
        $currentStatus = request('status') ?: null;
    @endphp
    @foreach($statusTabs as $value => $tab)
        @php
            $isActive = $currentStatus === $value;
            $count = $value ? ($statusCounts[$value] ?? 0) : null;
        @endphp
        <a href="{{ request()->fullUrlWithQuery(['status' => $value, 'page' => null]) }}" class="status-tab-link whitespace-nowrap px-4 py-2 rounded-lg text-sm font-bold transition-colors shadow-sm {{ $isActive ? $tab['active'] : $tab['inactive'] }}">
            {{ $tab['label'] }}{{ $count !== null ? " ({$count})" : '' }}
        </a>
    @endforeach
</div>

@if(session('success'))
<div class="mb-4 bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-lg flex items-center gap-2" role="alert">
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
    <div>
        <strong class="font-bold">Sukses!</strong>
        <span class="block sm:inline">{{ session('success') }}</span>
    </div>
</div>
@endif

@if(session('error'))
<div class="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2" role="alert">
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
    <div>
        <strong class="font-bold">Error!</strong>
        <span class="block sm:inline">{{ session('error') }}</span>
    </div>
</div>
@endif

<div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden min-h-[500px]">
    
    @if(($viewMode ?? 'group') == 'group')
        {{-- GROUPED VIEW --}}
        <div class="overflow-x-auto">
            <div class="min-w-[800px] flex flex-col divide-y divide-gray-100">
                <div class="bg-gray-50 px-6 py-3 grid grid-cols-12 gap-4 text-xs font-bold text-gray-500 uppercase tracking-wider">
                <div class="col-span-1 text-center">#</div>
                <div class="col-span-6">Mahasiswa</div>
                <div class="col-span-3 text-right">Ringkasan</div>
                <div class="col-span-2 text-right">Aksi</div>
            </div>

            @forelse($students as $index => $student)
            @php
                $hasPending = false;
                $totalInstallments = 0;
                $totalAmount = 0;
                foreach($student->paymentPlans as $plan) {
                    foreach($plan->installments as $inst) {
                        $totalInstallments++;
                        $totalAmount += $inst->amount;
                        foreach($inst->payments as $p) {
                            if(in_array($p->status, ['PENDING', 'NEEDS_REVIEW'])) {
                                $hasPending = true;
                            }
                        }
                    }
                }
            @endphp
            <div x-data="{ expanded: {{ $hasPending ? 'true' : 'false' }} }" class="group transition-colors border-b border-gray-100 {{ $hasPending ? 'bg-amber-50/50 hover:bg-amber-50' : 'bg-white hover:bg-gray-50' }}">
                <!-- Header Row -->
                <div class="px-6 py-4 flex items-center gap-4 cursor-pointer" @click="expanded = !expanded">
                    <div class="w-8 text-center text-gray-400 font-mono text-sm">
                        {{ $students->firstItem() + $index }}
                    </div>
                    
                    <div class="flex-1 flex items-start gap-3">
                        <div class="mt-1.5 transition-transform duration-200 text-gray-400" :class="expanded ? 'rotate-90' : ''">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
                        </div>
                        <div>
                            <div class="font-bold text-gray-900 text-base">{{ $student->name }}</div>
                            <div class="text-sm text-gray-500">{{ $student->nim }}</div>
                            @if($hasPending)
                                <div class="mt-1">
                                    <span class="inline-block px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-100">
                                        Menunggu Verifikasi
                                    </span>
                                </div>
                            @endif
                        </div>
                    </div>

                    <div class="flex items-center gap-6">
                        <div class="flex items-center gap-2">
                            <span class="px-3 py-1 rounded-full bg-gray-50 text-gray-600 border border-gray-200 text-xs font-bold">Total: {{ $totalInstallments }}</span>
                            <span class="px-3 py-1 rounded-full bg-blue-50 text-blue-700 border border-blue-100 text-xs font-bold">Rp {{ number_format($totalAmount, 0, ',', '.') }}</span>
                        </div>
                        
                        <div class="flex items-center gap-2 text-xs font-bold" @click.stop>
                            <form action="{{ route('admin.payments.reset', $student->id) }}" method="POST" class="inline-block ajax-action-form" onsubmit="return confirm('Apakah Anda yakin ingin mereset semua tagihan yang belum diverifikasi untuk mahasiswa ini? Tindakan ini akan menghapus data upload bukti bayar yang belum diverifikasi.');">
                                @csrf
                                <button type="submit" class="text-orange-500 hover:text-orange-700 hover:bg-orange-50 px-3 py-1.5 rounded-lg transition-all">Reset Angsuran</button>
                            </form>
                            <a href="{{ route('admin.payments.export', $student->id) }}" class="text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 px-3 py-1.5 rounded-lg transition-all">Export Excel</a>
                        </div>
                    </div>
                </div>

                <!-- Expanded Content -->
                <div x-show="expanded" x-collapse class="border-t border-gray-100 bg-white">
                    @if(($category ?? null) === 'MUNAOSAH')
                        @php
                            $munaqosahDetail = optional($student->paymentPlans->first())->munaqosahDetail;
                            $ktpOcr = $student->ktp_ocr_data;
                            $ijazahOcr = $student->ijazah_ocr_data;
                        @endphp
                        <div class="p-4 border-b border-gray-100 bg-gray-50/50">
                            <div class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Dokumen &amp; Data Skripsi</div>
                            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
                                <div class="bg-white border border-gray-200 rounded-lg p-3">
                                    <div class="font-bold text-gray-700 mb-1">Ijazah S1</div>
                                    @if($student->ijazah_file_path)
                                        <a href="{{ Storage::url($student->ijazah_file_path) }}" target="_blank" class="text-blue-600 hover:underline">Lihat Dokumen</a>
                                        @if($ijazahOcr)
                                            <div class="mt-1 inline-block px-2 py-0.5 rounded text-[10px] font-bold {{ ($ijazahOcr['name_match'] ?? false) ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-amber-50 text-amber-700 border border-amber-100' }}">
                                                {{ ($ijazahOcr['name_match'] ?? false) ? 'Nama Cocok' : 'Perlu Cek Manual' }}
                                            </div>
                                        @else
                                            <div class="mt-1 text-[10px] text-gray-400">OCR belum diproses</div>
                                        @endif
                                    @else
                                        <span class="text-gray-400">Belum diupload</span>
                                    @endif
                                </div>
                                <div class="bg-white border border-gray-200 rounded-lg p-3">
                                    <div class="font-bold text-gray-700 mb-1">KTP</div>
                                    @if($student->ktp_file_path)
                                        <a href="{{ Storage::url($student->ktp_file_path) }}" target="_blank" class="text-blue-600 hover:underline">Lihat Dokumen</a>
                                        @if($ktpOcr)
                                            <div class="mt-1 text-[10px] text-gray-500">NIK: {{ $ktpOcr['nik'] ?? '-' }}</div>
                                            <div class="mt-1 inline-block px-2 py-0.5 rounded text-[10px] font-bold {{ ($ktpOcr['name_match'] ?? false) ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-amber-50 text-amber-700 border border-amber-100' }}">
                                                {{ ($ktpOcr['name_match'] ?? false) ? 'Nama Cocok' : 'Perlu Cek Manual' }}
                                            </div>
                                        @else
                                            <div class="mt-1 text-[10px] text-gray-400">OCR belum diproses</div>
                                        @endif
                                    @else
                                        <span class="text-gray-400">Belum diupload</span>
                                    @endif
                                </div>
                                <div class="bg-white border border-gray-200 rounded-lg p-3">
                                    <div class="font-bold text-gray-700 mb-1">Skripsi</div>
                                    @if($munaqosahDetail)
                                        <div class="text-gray-600"><span class="font-semibold">Pembimbing:</span> {{ $munaqosahDetail->supervisor_name }}</div>
                                        <div class="text-gray-600 mt-0.5"><span class="font-semibold">Judul:</span> {{ $munaqosahDetail->thesis_title }}</div>
                                        @if($munaqosahDetail->thesis_file_path)
                                            <a href="{{ Storage::url($munaqosahDetail->thesis_file_path) }}" target="_blank" class="text-blue-600 hover:underline mt-1 inline-block">Lihat File Skripsi</a>
                                        @endif
                                    @else
                                        <span class="text-gray-400">Belum diisi mahasiswa</span>
                                    @endif
                                </div>
                            </div>
                        </div>
                    @endif
                    @if($student->paymentPlans->isEmpty())
                        <div class="p-6 text-center text-gray-400 italic text-sm">
                            Belum ada data tagihan atau rencana pembayaran.
                        </div>
                    @else
                    <div class="p-4">
                        <table class="w-full text-sm text-left">
                            <thead>
                                <tr class="text-xs text-gray-400 uppercase tracking-wider border-b border-gray-100">
                                    <th class="py-3 px-8 font-semibold w-12 text-center">#</th>
                                     <th class="py-3 px-4 font-semibold">Nama Mahasiswa</th>
                                     <th class="py-3 px-4 font-semibold">Semester / TA</th>
                                    <th class="py-3 px-4 font-semibold">Bulan</th>
                                    <th class="py-3 px-4 font-semibold">Tanggal Upload</th>
                                    <th class="py-3 px-4 font-semibold">Nominal</th>
                                    <th class="py-3 px-4 font-semibold">Status</th>
                                    <th class="py-3 px-4 font-semibold text-right">Aksi</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-gray-50">
                                @foreach($student->paymentPlans as $plan)
                                    @foreach($plan->installments as $idx => $inst)
                                        @php
                                            $latestPayment = $inst->payments->first();
                                            $manualAdjustment = $inst->adjustments->first();

                                            $statusLabel = 'Belum';
                                            $statusClass = 'bg-gray-100 text-gray-500';
                                            $rowBg = 'hover:bg-gray-50';

                                            if ($latestPayment) {
                                                switch($latestPayment->status) {
                                                    case 'VERIFIED':
                                                    case 'AUTO_VERIFIED':
                                                        if ($inst->status == 'PARTIAL') {
                                                            $statusLabel = 'Terbayar Sebagian';
                                                            $statusClass = 'bg-indigo-50 text-indigo-600 border border-indigo-100';
                                                            $rowBg = 'bg-indigo-50/30';
                                                        } else {
                                                            $statusLabel = 'Lunas';
                                                            $statusClass = 'bg-emerald-50 text-emerald-600 border border-emerald-100';
                                                            $rowBg = 'bg-emerald-50/30';
                                                        }
                                                        break;
                                                    case 'PENDING':
                                                    case 'NEEDS_REVIEW':
                                                        $statusLabel = 'Menunggu Verifikasi';
                                                        $statusClass = 'bg-amber-50 text-amber-600 border border-amber-100';
                                                        $rowBg = 'bg-amber-50/30';
                                                        break;
                                                    case 'REJECTED':
                                                    case 'FAILED':
                                                        $statusLabel = 'Ditolak / Gagal';
                                                        $statusClass = 'bg-red-50 text-red-600 border border-red-100';
                                                        $rowBg = 'bg-red-50/30';
                                                        break;
                                                }
                                            } elseif ($manualAdjustment && $inst->status == 'PAID') {
                                                $statusLabel = 'Lunas (Manual)';
                                                $statusClass = 'bg-emerald-50 text-emerald-600 border border-emerald-100';
                                            } elseif ($manualAdjustment && $inst->status == 'PARTIAL') {
                                                $statusLabel = 'Terbayar Sebagian (Manual)';
                                                $statusClass = 'bg-indigo-50 text-indigo-600 border border-indigo-100';
                                            } elseif ($inst->status == 'PAID') {
                                                $statusLabel = 'Lunas (Otomatis)';
                                                $statusClass = 'bg-teal-50 text-teal-600 border border-teal-100';
                                            } elseif ($inst->status == 'PARTIAL') {
                                                $statusLabel = 'Terbayar Sebagian';
                                                $statusClass = 'bg-indigo-50 text-indigo-600 border border-indigo-100';
                                            }
                                        @endphp
                                        <tr class="{{ $rowBg }} transition-colors">
                                             <td class="py-4 px-8 text-center text-gray-400 font-medium text-xs">{{ $loop->parent->iteration }}.{{ $idx + 1 }}</td>
                                             <td class="py-4 px-4 font-medium text-gray-800">{{ $student->name }}</td>
                                             <td class="py-4 px-4 font-semibold text-gray-800">{{ $plan->term }} / {{ $plan->academic_year }}</td>
                                            <td class="py-4 px-4 text-gray-600">{{ \Carbon\Carbon::parse($inst->due_date)->format('F Y') }}</td>
                                            <td class="py-4 px-4 text-gray-500">{{ $latestPayment ? $latestPayment->created_at->format('d M Y H:i') : '-' }}</td>
                                            <td class="py-4 px-4">
                                                <div class="text-[11px] text-gray-500">Tagihan Utuh: Rp{{ number_format($inst->amount, 0, ',', '.') }}</div>
                                                @if($category === 'KERJASAMA')
                                                    <form action="{{ route('admin.installments.update_amount', $inst->id) }}" method="POST" class="inline ajax-action-form">
                                                        @csrf
                                                        <input type="hidden" name="amount" value="">
                                                        <input type="hidden" name="reason" value="">
                                                        <button type="submit" class="text-[10px] text-indigo-600 hover:text-indigo-800 hover:underline font-semibold inline-flex items-center gap-0.5 mb-1" title="Ubah nominal tagihan cicilan ini" onclick="
                                                            var newAmount = prompt('Nominal tagihan baru untuk cicilan ini (Rp):', {{ (int) $inst->amount }});
                                                            if (newAmount === null || newAmount.trim() === '') return false;
                                                            var cleaned = newAmount.replace(/[^0-9]/g, '');
                                                            if (!cleaned) { alert('Nominal tidak valid.'); return false; }
                                                            var reason = prompt('Alasan perubahan tagihan:');
                                                            if (reason === null || reason.trim() === '') return false;
                                                            this.form.amount.value = cleaned;
                                                            this.form.reason.value = reason;
                                                            return true;
                                                        ">
                                                            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                                                            Ubah Tagihan
                                                        </button>
                                                    </form>
                                                @endif
                                                @if($inst->amount_paid > 0 && $inst->status != 'PAID')
                                                    <div class="text-[10px] font-semibold text-emerald-600 mb-1">Sudah Dicicil: Rp{{ number_format($inst->amount_paid, 0, ',', '.') }}</div>
                                                @endif
                                                @php
                                                    $remainingAmount = $inst->amount - $inst->amount_paid;
                                                @endphp
                                                <div class="text-xs font-bold text-gray-700 mb-1">Sisa Tagihan: Rp{{ number_format($remainingAmount, 0, ',', '.') }}</div>
                                                @if($latestPayment)
                                                    @php
                                                        $actualAmount = isset($latestPayment->ocr_data['extracted_amount']) && $latestPayment->ocr_data['extracted_amount'] > 0 ? $latestPayment->ocr_data['extracted_amount'] : $latestPayment->amount;
                                                        
                                                        $isVerified = in_array($latestPayment->status, ['VERIFIED', 'AUTO_VERIFIED']);
                                                        $isRejected = in_array($latestPayment->status, ['REJECTED', 'FAILED', 'AUTO_REJECTED']);
                                                        
                                                        if ($isVerified) {
                                                            $otherPaid = 0;
                                                            foreach($inst->payments as $p) {
                                                                if (in_array($p->status, ['VERIFIED', 'AUTO_VERIFIED']) && $p->created_at < $latestPayment->created_at) {
                                                                    $otherPaid += $p->amount;
                                                                }
                                                            }
                                                            $historicalTarget = max(0, $inst->amount - $otherPaid);
                                                        } else {
                                                            $historicalTarget = $inst->amount - $inst->amount_paid;
                                                        }
                                                        
                                                        $diff = $actualAmount - $historicalTarget;
                                                        $hasOcr = isset($latestPayment->ocr_data['extracted_amount']) && $latestPayment->ocr_data['extracted_amount'] > 0;
                                                        $previouslyRejected = $inst->payments->skip(1)->contains(fn($p) => in_array($p->status, ['REJECTED', 'FAILED']));
                                                    @endphp
                                                    
                                                    @if($isRejected)
                                                        <div class="text-xs text-gray-500 font-bold mt-1">
                                                            <s class="text-red-400">Nominal Upload: Rp{{ number_format($actualAmount, 0, ',', '.') }}</s>
                                                            <span class="text-[10px] text-red-500 ml-1">❌ Ditolak</span>
                                                        </div>
                                                    @else
                                                        <div class="text-xs {{ $hasOcr ? 'text-purple-700' : 'text-blue-600' }} font-bold mt-1">
                                                            Dibayar: Rp{{ number_format($actualAmount, 0, ',', '.') }}
                                                            @if($hasOcr)
                                                                <span class="text-[9px] bg-purple-100 text-purple-700 px-1 rounded ml-1" title="Hasil pembacaan otomatis dari gambar">via Sistem OCR</span>
                                                            @endif
                                                        </div>

                                                        @if($previouslyRejected)
                                                            <div class="mt-1 text-[9px] text-gray-400" title="Upload sebelumnya untuk cicilan ini sempat ditolak, lalu diupload ulang dan sudah diverifikasi.">
                                                                ↺ Sempat ditolak sebelumnya, sudah diperbaiki
                                                            </div>
                                                        @endif

                                                        @if($diff > 0 && $diff <= $paymentTolerance)
                                                            <div class="mt-1.5 inline-block px-2 py-0.5 bg-gray-50 text-gray-600 border border-gray-200 rounded text-[10px] font-bold shadow-sm" title="Selisih Rp{{ number_format($diff, 0, ',', '.') }} masih dalam batas toleransi (mis. kode unik/biaya admin) — tidak dikreditkan kemana-mana.">
                                                                ✓ Nominal Pas (dalam toleransi)
                                                            </div>
                                                        @elseif($diff > 0)
                                                            <div class="mt-1.5 block px-2 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded text-[11px] shadow-sm">
                                                                <div class="font-bold">✨ Lebih: Rp{{ number_format($diff, 0, ',', '.') }}</div>
                                                                <div class="text-[9px] leading-tight mt-0.5 text-emerald-600">Sisa masuk ke Saldo Wallet mahasiswa otomatis.</div>
                                                            </div>
                                                        @elseif($diff < 0)
                                                            <div class="mt-1.5 block px-2 py-1 bg-rose-50 text-rose-700 border border-rose-200 rounded text-[11px] shadow-sm">
                                                                <div class="font-bold">⚠️ Kurang: Rp{{ number_format(abs($diff), 0, ',', '.') }}</div>
                                                                <div class="text-[9px] leading-tight mt-0.5 text-rose-600">Status akan menjadi Terbayar Sebagian (Partial).</div>
                                                            </div>
                                                        @else
                                                            <div class="mt-1.5 inline-block px-2 py-0.5 bg-gray-50 text-gray-600 border border-gray-200 rounded text-[10px] font-bold shadow-sm">
                                                                ✓ Nominal Pas
                                                            </div>
                                                        @endif
                                                    @endif

                                                    @if($latestPayment->payment_proof_date)
                                                        @php $proofAgeDays = (int) $latestPayment->payment_proof_date->diffInDays(now()); @endphp
                                                        <div class="mt-1.5 inline-block px-2 py-0.5 rounded text-[10px] font-semibold {{ $proofAgeDays > 7 ? 'bg-amber-50 text-amber-600 border border-amber-100' : 'bg-gray-50 text-gray-500 border border-gray-100' }}" title="Tanggal pada bukti pembayaran, bukan tanggal upload">
                                                            Tanggal Bukti: {{ $latestPayment->payment_proof_date->format('d/m/Y') }} ({{ $proofAgeDays }} hari lalu)
                                                        </div>
                                                    @endif
                                                @elseif($manualAdjustment)
                                                    <div class="mt-1.5 block px-2 py-1 bg-sky-50 text-sky-700 border border-sky-200 rounded text-[11px] shadow-sm">
                                                        <div class="font-bold flex items-center gap-1">
                                                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                                                            Disesuaikan Manual oleh Admin
                                                        </div>
                                                        <div class="text-[9px] leading-tight mt-0.5 text-sky-600">
                                                            {{ $manualAdjustment->reason ?: 'Tanpa keterangan' }} — {{ $manualAdjustment->admin->name ?? 'Admin' }}, {{ $manualAdjustment->created_at->format('d/m/Y') }}
                                                        </div>
                                                    </div>
                                                @elseif($inst->status == 'PAID' && $inst->amount_paid >= $inst->amount)
                                                    @php
                                                        $sourceInstallment = null;
                                                        $sourcePayment = null;
                                                        foreach ($plan->installments->sortByDesc('due_date') as $sibling) {
                                                            if ($sibling->id === $inst->id || $sibling->due_date >= $inst->due_date) continue;
                                                            $sp = $sibling->payments->first();
                                                            if ($sp && in_array($sp->status, ['VERIFIED', 'AUTO_VERIFIED'])) {
                                                                $spActual = isset($sp->ocr_data['extracted_amount']) && $sp->ocr_data['extracted_amount'] > 0 ? $sp->ocr_data['extracted_amount'] : $sp->amount;
                                                                if ($spActual > $sibling->amount) {
                                                                    $sourceInstallment = $sibling;
                                                                    $sourcePayment = $sp;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    @endphp
                                                    <div class="mt-1.5 block px-2 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded text-[11px] shadow-sm">
                                                        <div class="font-bold flex items-center gap-1">
                                                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                                                            Lunas Otomatis
                                                        </div>
                                                        <div class="text-[9px] leading-tight mt-0.5 text-emerald-600">
                                                            @if($sourceInstallment)
                                                                Dari kelebihan bayar {{ \Carbon\Carbon::parse($sourceInstallment->due_date)->format('F Y') }}.
                                                            @else
                                                                Dari kelebihan pembayaran bulan sebelumnya.
                                                            @endif
                                                        </div>
                                                        @if($sourcePayment && $sourcePayment->proof_file_path)
                                                            <a href="{{ Storage::url($sourcePayment->proof_file_path) }}" target="_blank" class="mt-1 inline-flex items-center gap-1 text-[9px] font-semibold text-emerald-700 underline hover:text-emerald-800">
                                                                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                                                                Lihat Bukti Sumber
                                                            </a>
                                                        @endif
                                                    </div>
                                                @endif
                                            </td>
                                            <td class="py-4 px-4">
                                                <span class="px-2.5 py-0.5 rounded-full text-xs font-bold {{ $statusClass }}">
                                                    {{ $statusLabel }}
                                                </span>
                                            </td>
                                            <td class="py-4 px-4 text-right">
                                                <div class="flex justify-end items-center gap-2">
                                                    @if($latestPayment && $latestPayment->proof_file_path)
                                                        @php
                                                            $fileExtension = pathinfo($latestPayment->proof_file_path, PATHINFO_EXTENSION) ?: 'jpg';
                                                            $downloadName = Str::slug($student->nim . '_' . $student->name . '_Angsuran_' . $inst->installment_no . '_' . $plan->term . '_' . str_replace('/', '-', $plan->academic_year)) . '.' . $fileExtension;
                                                        @endphp
                                                        <a href="{{ Storage::url($latestPayment->proof_file_path) }}" download="{{ $downloadName }}" class="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors" title="Download Bukti">
                                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                                                        </a>
                                                        <a href="{{ Storage::url($latestPayment->proof_file_path) }}" target="_blank" class="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors" title="Lihat/Preview">
                                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                                                        </a>
                                                    @endif

                                                    @if($latestPayment && in_array($latestPayment->status, ['PENDING', 'NEEDS_REVIEW']))
                                                        <form action="{{ route('admin.payments.verify', $latestPayment->id) }}" method="POST" class="inline ajax-action-form">
                                                            @csrf
                                                            <input type="hidden" name="action" value="approve">
                                                            <button type="submit" class="w-8 h-8 flex items-center justify-center rounded-lg bg-emerald-500 text-white hover:bg-emerald-600 transition-colors shadow-sm" title="Approve" onclick="return confirm('Verifikasi pembayaran ini?')">
                                                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                                                            </button>
                                                        </form>
                                                        <form action="{{ route('admin.payments.verify', $latestPayment->id) }}" method="POST" class="inline ajax-action-form">
                                                            @csrf
                                                            <input type="hidden" name="action" value="reject">
                                                            <input type="hidden" name="notes" value="">
                                                            <button type="submit" class="w-8 h-8 flex items-center justify-center rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors shadow-sm" title="Reject" onclick="var reason = prompt('Alasan penolakan:'); if(reason === null) return false; this.form.notes.value = reason; return true;">
                                                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                                                            </button>
                                                        </form>
                                                    @elseif($latestPayment && in_array($latestPayment->status, ['VERIFIED', 'AUTO_VERIFIED', 'REJECTED', 'AUTO_REJECTED']))
                                                        <form action="{{ route('admin.payments.reset_status', $latestPayment->id) }}" method="POST" class="inline ajax-action-form">
                                                            @csrf
                                                            <button type="submit" class="w-8 h-8 flex items-center justify-center rounded-lg border border-amber-200 text-amber-600 hover:bg-amber-50 transition-colors" title="Reset/Revert ke Menunggu Verifikasi" onclick="return confirm('Reset status pembayaran ini menjadi Menunggu Verifikasi?')">
                                                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                                                            </button>
                                                        </form>
                                                    @else
                                                        {{-- Placeholder for actions on Unpaid items --}}
                                                        <button class="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-100 text-gray-300 cursor-not-allowed">
                                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                                                        </button>
                                                    @endif
                                                </div>
                                            </td>
                                        </tr>
                                    @endforeach
                                @endforeach
                            </tbody>
                        </table>
                    </div>
                    @endif
                </div>
            </div>
            @empty
            <div class="p-12 text-center text-gray-500">
                <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                <p class="text-lg">Belum ada data mahasiswa dengan tagihan.</p>
            </div>
            @endforelse
            </div>
        </div>
        
        @if($students->hasPages())
        <div class="p-4 border-t border-gray-100">
            {{ $students->appends(['view' => 'group'])->links() }}
        </div>
        @endif

    @else
        {{-- FLAT VIEW (Original) --}}
        <div class="overflow-x-auto">
            <table class="w-full min-w-[1000px] text-left border-collapse">
                <thead>
                    <tr class="border-b border-gray-200 text-xs uppercase tracking-wider text-gray-500">
                        <th class="p-4 font-semibold">Tanggal</th>
                        <th class="p-4 font-semibold">Mahasiswa</th>
                        <th class="p-4 font-semibold">Pembayaran</th>
                        <th class="p-4 font-semibold">Jumlah</th>
                        <th class="p-4 font-semibold">Bukti</th>
                        <th class="p-4 font-semibold">Status</th>
                        <th class="p-4 font-semibold text-right">Aksi</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                    @forelse($payments as $payment)
                    <tr class="hover:bg-gray-50 transition-colors">
                        <td class="p-4 text-sm text-gray-500">
                            {{ $payment->created_at->format('d M Y') }}<br>
                            <span class="text-xs">{{ $payment->created_at->format('H:i') }}</span>
                        </td>
                        <td class="p-4">
                            <div class="font-bold text-gray-900">{{ $payment->student->name }}</div>
                            <div class="text-sm text-gray-500">{{ $payment->student->nim }}</div>
                            <div class="text-xs text-blue-600 font-semibold mt-1">Kelas: {{ $payment->student->class ?? '-' }}</div>
                        </td>
                        <td class="p-4 text-sm">
                            @if($payment->paymentPlan)
                            <div class="text-gray-900 font-semibold">{{ $payment->paymentPlan->academic_year }} ({{ $payment->paymentPlan->term }})</div>
                            <div class="text-gray-500">Angsuran Ke-{{ $payment->installment->installment_no }}</div>
                            @else
                            <span class="text-red-500">Plan Invalid</span>
                            @endif
                        </td>
                        <td class="p-4">
                            @if($payment->installment)
                                <div class="text-[11px] text-gray-500 mb-0.5">Tagihan Utuh: Rp {{ number_format($payment->installment->amount, 0, ',', '.') }}</div>
                                @if($category === 'KERJASAMA')
                                    <form action="{{ route('admin.installments.update_amount', $payment->installment->id) }}" method="POST" class="inline ajax-action-form">
                                        @csrf
                                        <input type="hidden" name="amount" value="">
                                        <input type="hidden" name="reason" value="">
                                        <button type="submit" class="text-[10px] text-indigo-600 hover:text-indigo-800 hover:underline font-semibold inline-flex items-center gap-0.5 mb-0.5" title="Ubah nominal tagihan cicilan ini" onclick="
                                            var newAmount = prompt('Nominal tagihan baru untuk cicilan ini (Rp):', {{ (int) $payment->installment->amount }});
                                            if (newAmount === null || newAmount.trim() === '') return false;
                                            var cleaned = newAmount.replace(/[^0-9]/g, '');
                                            if (!cleaned) { alert('Nominal tidak valid.'); return false; }
                                            var reason = prompt('Alasan perubahan tagihan:');
                                            if (reason === null || reason.trim() === '') return false;
                                            this.form.amount.value = cleaned;
                                            this.form.reason.value = reason;
                                            return true;
                                        ">
                                            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                                            Ubah Tagihan
                                        </button>
                                    </form>
                                @endif
                                @if($payment->installment->amount_paid > 0 && $payment->installment->status != 'PAID')
                                    <div class="text-[10px] font-semibold text-emerald-600 mb-0.5">Sudah Dicicil: Rp {{ number_format($payment->installment->amount_paid, 0, ',', '.') }}</div>
                                @endif
                                @php
                                    $remainingFlat = $payment->installment->amount - $payment->installment->amount_paid;
                                @endphp
                                <div class="text-xs font-bold text-gray-700 mb-1">Sisa Tagihan: Rp {{ number_format($remainingFlat, 0, ',', '.') }}</div>
                                
                                @php
                                    $actualAmountFlat = isset($payment->ocr_data['extracted_amount']) && $payment->ocr_data['extracted_amount'] > 0 ? $payment->ocr_data['extracted_amount'] : $payment->amount;
                                    
                                    $isVerifiedFlat = in_array($payment->status, ['VERIFIED', 'AUTO_VERIFIED']);
                                    $isRejectedFlat = in_array($payment->status, ['REJECTED', 'FAILED', 'AUTO_REJECTED']);
                                    
                                    if ($isVerifiedFlat) {
                                        $otherPaidFlat = 0;
                                        foreach($payment->installment->payments as $p) {
                                            if (in_array($p->status, ['VERIFIED', 'AUTO_VERIFIED']) && $p->created_at < $payment->created_at) {
                                                $otherPaidFlat += $p->amount;
                                            }
                                        }
                                        $historicalTargetFlat = max(0, $payment->installment->amount - $otherPaidFlat);
                                    } else {
                                        $historicalTargetFlat = $payment->installment->amount - $payment->installment->amount_paid;
                                    }
                                    
                                    $diffFlat = $actualAmountFlat - $historicalTargetFlat;
                                    $hasOcrFlat = isset($payment->ocr_data['extracted_amount']) && $payment->ocr_data['extracted_amount'] > 0;
                                @endphp
                                
                                @if($isRejectedFlat)
                                    <div class="mt-2 text-xs text-gray-500 font-bold">
                                        <s class="text-red-400">Nominal Upload: Rp {{ number_format($actualAmountFlat, 0, ',', '.') }}</s>
                                        <span class="text-[10px] text-red-500 ml-1">❌ Ditolak</span>
                                    </div>
                                @else
                                    <div class="font-bold {{ $hasOcrFlat ? 'text-purple-700' : 'text-blue-600' }}">
                                        Dibayar: Rp {{ number_format($actualAmountFlat, 0, ',', '.') }}
                                        @if($hasOcrFlat)
                                            <span class="text-[9px] bg-purple-100 text-purple-700 px-1 py-0.5 rounded align-middle ml-1" title="Hasil pembacaan otomatis dari gambar">via OCR</span>
                                        @endif
                                    </div>

                                    @if($diffFlat > 0 && $diffFlat <= $paymentTolerance)
                                        <div class="mt-2 inline-block px-2 py-0.5 bg-gray-50 text-gray-600 border border-gray-200 rounded text-[10px] font-bold shadow-sm" title="Selisih Rp {{ number_format($diffFlat, 0, ',', '.') }} masih dalam batas toleransi (mis. kode unik/biaya admin) — tidak dikreditkan kemana-mana.">
                                            ✓ Nominal Pas (dalam toleransi)
                                        </div>
                                    @elseif($diffFlat > 0)
                                        <div class="mt-2 block px-2 py-1.5 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded text-[11px] shadow-sm">
                                            <div class="font-bold">✨ Lebih: Rp {{ number_format($diffFlat, 0, ',', '.') }}</div>
                                            <div class="text-[10px] leading-tight mt-0.5 text-emerald-600">Otomatis masuk ke Saldo Wallet mahasiswa</div>
                                        </div>
                                    @elseif($diffFlat < 0)
                                        <div class="mt-2 block px-2 py-1.5 bg-rose-50 text-rose-700 border border-rose-200 rounded text-[11px] shadow-sm">
                                            <div class="font-bold">⚠️ Kurang: Rp {{ number_format(abs($diffFlat), 0, ',', '.') }}</div>
                                            <div class="text-[10px] leading-tight mt-0.5 text-rose-600">Status akan menjadi Terbayar Sebagian (Partial)</div>
                                        </div>
                                    @else
                                        <div class="mt-2 inline-block px-2 py-0.5 bg-gray-50 text-gray-600 border border-gray-200 rounded text-[10px] font-bold shadow-sm">
                                            ✓ Nominal Pas
                                        </div>
                                    @endif
                                @endif

                                @if($payment->payment_proof_date)
                                    @php $proofAgeDaysFlat = (int) $payment->payment_proof_date->diffInDays(now()); @endphp
                                    <div class="mt-1.5 inline-block px-2 py-0.5 rounded text-[10px] font-semibold {{ $proofAgeDaysFlat > 7 ? 'bg-amber-50 text-amber-600 border border-amber-100' : 'bg-gray-50 text-gray-500 border border-gray-100' }}" title="Tanggal pada bukti pembayaran, bukan tanggal upload">
                                        Tanggal Bukti: {{ $payment->payment_proof_date->format('d/m/Y') }} ({{ $proofAgeDaysFlat }} hari lalu)
                                    </div>
                                @endif
                            @endif
                        </td>
                        <td class="p-4">
                            <a href="{{ Storage::url($payment->proof_file_path) }}" target="_blank" class="group relative block w-16 h-16 rounded-lg overflow-hidden border border-gray-200">
                                <img src="{{ Storage::url($payment->proof_file_path) }}" alt="Bukti" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300">
                                <div class="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                                    <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                                </div>
                            </a>
                        </td>
                        <td class="p-4">
                            @php
                                $statusColors = [
                                    'PENDING' => 'bg-amber-100 text-amber-800 border-amber-200',
                                    'NEEDS_REVIEW' => 'bg-orange-100 text-orange-800 border-orange-200',
                                    'FAILED' => 'bg-red-100 text-red-800 border-red-200',
                                    'VERIFIED' => 'bg-emerald-100 text-emerald-800 border-emerald-200',
                                    'REJECTED' => 'bg-gray-100 text-gray-800 border-gray-200',
                                ];
                                $colorClass = $statusColors[$payment->status] ?? 'bg-gray-100 text-gray-800';
                            @endphp
                            <span class="px-3 py-1 rounded-full text-xs font-bold border {{ $colorClass }}">
                                {{ str_replace('_', ' ', $payment->status) }}
                            </span>
                            @if($payment->ocr_data && isset($payment->ocr_data['confidence']))
                                <div class="mt-1 text-xs text-gray-500">
                                    OCR: {{ round($payment->ocr_data['confidence']) }}%
                                </div>
                            @endif
                        </td>
                        <td class="p-4 text-right space-x-2">
                            <div class="flex items-center justify-end gap-2">
                                @if(in_array($payment->status, ['PENDING', 'NEEDS_REVIEW']))
                                    <form action="{{ route('admin.payments.verify', $payment->id) }}" method="POST" class="ajax-action-form" onsubmit="return confirm('Verifikasi pembayaran ini?');">
                                        @csrf
                                        <input type="hidden" name="action" value="approve">
                                        <button type="submit" class="p-2 rounded-lg bg-emerald-500 text-white hover:bg-emerald-600 transition-colors shadow-sm" title="Terima / Approve">
                                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                                        </button>
                                    </form>

                                    <form action="{{ route('admin.payments.verify', $payment->id) }}" method="POST" class="ajax-action-form" onsubmit="var reason = prompt('Alasan penolakan (opsional):'); if(reason === null) return false; this.notes.value = reason; return true;">
                                        @csrf
                                        <input type="hidden" name="action" value="reject">
                                        <input type="hidden" name="notes" value="">
                                        <button type="submit" class="p-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors shadow-sm" title="Tolak / Reject">
                                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                                        </button>
                                    </form>
                                @elseif(in_array($payment->status, ['VERIFIED', 'AUTO_VERIFIED', 'REJECTED', 'AUTO_REJECTED']))
                                    <form action="{{ route('admin.payments.reset_status', $payment->id) }}" method="POST" class="inline ajax-action-form">
                                        @csrf
                                        <button type="submit" class="p-2 rounded-lg border border-amber-200 text-amber-600 hover:bg-amber-50 transition-colors shadow-sm" title="Reset/Revert ke Menunggu Verifikasi" onclick="return confirm('Reset status pembayaran ini menjadi Menunggu Verifikasi?')">
                                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                                        </button>
                                    </form>
                                @endif
                            </div>
                        </td>
                    </tr>
                    @empty
                    <tr>
                        <td colspan="7" class="p-12 text-center text-gray-500">
                            <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                            <p class="text-lg">Tidak ada pembayaran yang perlu diverifikasi saat ini.</p>
                        </td>
                    </tr>
                    @endforelse
                </tbody>
            </table>
        </div>
        
        @if($payments->hasPages())
        <div class="p-4 border-t border-gray-100">
            {{ $payments->appends(['view' => 'flat'])->links() }}
        </div>
        @endif
    @endif
</div>
</div>

<script>
(function() {
    const container = document.getElementById('payments-content');
    if (!container) return;

    function showToast(message, isError) {
        const toast = document.createElement('div');
        toast.className = 'fixed top-4 right-4 z-50 px-4 py-3 rounded-lg shadow-lg text-sm font-semibold ' +
            (isError ? 'bg-red-600 text-white' : 'bg-emerald-600 text-white');
        toast.textContent = message;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 4000);
    }

    function setLoading(isLoading) {
        container.style.opacity = isLoading ? '0.5' : '1';
        container.style.pointerEvents = isLoading ? 'none' : 'auto';
    }

    function swapContent(fresh) {
        container.innerHTML = fresh.innerHTML;
        bindForms();
        bindTabs();
    }

    async function navigateTo(url, { push = true } = {}) {
        setLoading(true);
        try {
            const res = await fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } });
            if (!res.ok) throw new Error('Request failed');
            const html = await res.text();
            const doc = new DOMParser().parseFromString(html, 'text/html');
            const fresh = doc.getElementById('payments-content');
            if (fresh) {
                if (document.startViewTransition) {
                    await document.startViewTransition(() => swapContent(fresh)).finished;
                } else {
                    swapContent(fresh);
                }
            }
            if (push) history.pushState({}, '', url);
        } catch (err) {
            showToast('Gagal memuat data. Periksa koneksi lalu coba lagi.', true);
            window.location.href = url;
        } finally {
            setLoading(false);
        }
    }

    async function refreshContent() {
        await navigateTo(window.location.href, { push: false });
    }

    async function handleSubmit(e) {
        const form = e.target;
        e.preventDefault();

        const buttons = form.querySelectorAll('button');
        buttons.forEach(b => b.disabled = true);
        setLoading(true);

        try {
            const res = await fetch(form.getAttribute('action'), {
                method: 'POST',
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                body: new FormData(form),
            });
            const data = await res.json().catch(() => ({}));

            if (!res.ok) {
                showToast(data.message || 'Terjadi kesalahan, silakan coba lagi.', true);
                buttons.forEach(b => b.disabled = false);
                setLoading(false);
                return;
            }

            showToast(data.message || 'Berhasil diproses.', false);
            await refreshContent();
        } catch (err) {
            showToast('Gagal terhubung ke server. Periksa koneksi lalu coba lagi.', true);
            buttons.forEach(b => b.disabled = false);
        } finally {
            setLoading(false);
        }
    }

    function bindForms() {
        container.querySelectorAll('form.ajax-action-form').forEach(form => {
            if (form.dataset.bound === 'true') return;
            form.dataset.bound = 'true';
            // Inline onsubmit (confirm()/prompt()) and onclick-on-submit-button confirm()
            // handlers run before this listener and may already cancel the event
            // (form-level: e.defaultPrevented becomes true; button-level: submit never fires).
            form.addEventListener('submit', function(e) {
                if (e.defaultPrevented) return;
                handleSubmit(e);
            });
        });
    }

    function bindTabs() {
        container.querySelectorAll('a.status-tab-link').forEach(link => {
            if (link.dataset.bound === 'true') return;
            link.dataset.bound = 'true';
            link.addEventListener('click', function(e) {
                e.preventDefault();
                navigateTo(this.href);
            });
        });
    }

    window.addEventListener('popstate', function() {
        navigateTo(window.location.href, { push: false });
    });

    bindForms();
    bindTabs();
})();
</script>
@endsection
