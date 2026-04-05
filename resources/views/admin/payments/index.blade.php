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
            <a href="{{ route('admin.payments.export_all', ['programType' => $programType]) }}" class="whitespace-nowrap px-4 py-2 rounded-lg text-sm font-bold bg-emerald-600 text-white hover:bg-emerald-700 transition-colors flex items-center gap-2 shadow-sm">
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
                            <form action="{{ route('admin.payments.reset', $student->id) }}" method="POST" class="inline-block" onsubmit="return confirm('Apakah Anda yakin ingin mereset semua tagihan yang belum diverifikasi untuk mahasiswa ini? Tindakan ini akan menghapus data upload bukti bayar yang belum diverifikasi.');">
                                @csrf
                                <button type="submit" class="text-orange-500 hover:text-orange-700 hover:bg-orange-50 px-3 py-1.5 rounded-lg transition-all">Reset Angsuran</button>
                            </form>
                            <a href="{{ route('admin.payments.export', $student->id) }}" class="text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 px-3 py-1.5 rounded-lg transition-all">Export Excel</a>
                        </div>
                    </div>
                </div>

                <!-- Expanded Content -->
                <div x-show="expanded" x-collapse class="border-t border-gray-100 bg-white">
                    @if($student->paymentPlans->isEmpty())
                        <div class="p-6 text-center text-gray-400 italic text-sm">
                            Belum ada data tagihan atau rencana pembayaran.
                        </div>
                    @else
                    <div class="p-4">
                        <table class="w-full text-sm text-left">
                            <thead>
                                <tr class="text-xs text-gray-400 uppercase tracking-wider border-b border-gray-100">
                                    <th class="py-3 px-4 font-semibold w-12 text-center">#</th>
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
                                            
                                            $statusLabel = 'Belum';
                                            $statusClass = 'bg-gray-100 text-gray-500';
                                            $rowBg = 'hover:bg-gray-50';

                                            if ($latestPayment) {
                                                if ($latestPayment->status == 'VERIFIED') {
                                                    $statusLabel = 'Lunas';
                                                    $statusClass = 'bg-emerald-50 text-emerald-700 border border-emerald-100';
                                                } elseif (in_array($latestPayment->status, ['PENDING', 'NEEDS_REVIEW'])) {
                                                    $statusLabel = 'Menunggu Verifikasi';
                                                    $statusClass = 'bg-amber-50 text-amber-700 border border-amber-100';
                                                    $rowBg = 'bg-amber-50/30 hover:bg-amber-50/50';
                                                } elseif ($latestPayment->status == 'REJECTED') {
                                                    $statusLabel = 'Ditolak';
                                                    $statusClass = 'bg-red-50 text-red-700 border border-red-100';
                                                }
                                            }
                                        @endphp
                                        <tr class="transition-colors {{ $rowBg }}">
                                            <td class="py-4 px-4 text-center text-gray-400">{{ $loop->parent->iteration }}.{{ $loop->iteration }}</td>
                                            <td class="py-4 px-4 font-medium text-gray-900">
                                                {{ $plan->term }} / {{ $plan->academic_year }}
                                            </td>
                                            <td class="py-4 px-4 text-gray-600">
                                                {{ $inst->due_date->translatedFormat('F Y') }}
                                            </td>
                                            <td class="py-4 px-4 text-gray-600">
                                                {{ $latestPayment ? $latestPayment->created_at->format('d M Y H:i') : '-' }}
                                            </td>
                                            <td class="py-4 px-4 font-medium text-gray-900">
                                                Rp{{ number_format($inst->amount, 0, ',', '.') }}
                                            </td>
                                            <td class="py-4 px-4">
                                                <span class="px-2.5 py-0.5 rounded-full text-xs font-bold {{ $statusClass }}">
                                                    {{ $statusLabel }}
                                                </span>
                                            </td>
                                            <td class="py-4 px-4 text-right">
                                                <div class="flex justify-end items-center gap-2">
                                                    @if($latestPayment && $latestPayment->proof_file_path)
                                                        <a href="{{ Storage::url($latestPayment->proof_file_path) }}" target="_blank" class="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors" title="Download/Lihat Bukti">
                                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                                                        </a>
                                                        <button class="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors" title="Preview">
                                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                                                        </button>
                                                    @endif

                                                    @if($latestPayment && in_array($latestPayment->status, ['PENDING', 'NEEDS_REVIEW']))
                                                        <form action="{{ route('admin.payments.verify', $latestPayment->id) }}" method="POST" class="inline">
                                                            @csrf
                                                            <input type="hidden" name="action" value="approve">
                                                            <button type="submit" class="w-8 h-8 flex items-center justify-center rounded-lg bg-emerald-500 text-white hover:bg-emerald-600 transition-colors shadow-sm" title="Approve" onclick="return confirm('Verifikasi pembayaran ini?')">
                                                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                                                            </button>
                                                        </form>
                                                        <form action="{{ route('admin.payments.verify', $latestPayment->id) }}" method="POST" class="inline">
                                                            @csrf
                                                            <input type="hidden" name="action" value="reject">
                                                            <input type="hidden" name="notes" value="">
                                                            <button type="submit" class="w-8 h-8 flex items-center justify-center rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors shadow-sm" title="Reject" onclick="let reason = prompt('Alasan penolakan:'); if(reason === null) return false; this.form.notes.value = reason; return true;">
                                                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                                                            </button>
                                                        </form>
                                                    @elseif($latestPayment && $latestPayment->status == 'VERIFIED')
                                                         <button class="w-8 h-8 flex items-center justify-center rounded-lg border border-amber-200 text-amber-600 hover:bg-amber-50 transition-colors" title="Reset/Revert">
                                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                                                        </button>
                                                    @else
                                                        {{-- Placeholder for actions on Unpaid/Rejected items --}}
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
                        <td class="p-4 font-mono text-gray-900 font-bold">
                            Rp {{ number_format($payment->amount, 0, ',', '.') }}
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
                                <form action="{{ route('admin.payments.verify', $payment->id) }}" method="POST" onsubmit="return confirm('Verifikasi pembayaran ini?');">
                                    @csrf
                                    <input type="hidden" name="action" value="approve">
                                    <button type="submit" class="p-2 rounded-lg bg-emerald-500 text-white hover:bg-emerald-600 transition-colors shadow-sm" title="Terima / Approve">
                                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                                    </button>
                                </form>
                                
                                <form action="{{ route('admin.payments.verify', $payment->id) }}" method="POST" onsubmit="let reason = prompt('Alasan penolakan (opsional):'); if(reason === null) return false; this.notes.value = reason; return true;">
                                    @csrf
                                    <input type="hidden" name="action" value="reject">
                                    <input type="hidden" name="notes" value="">
                                    <button type="submit" class="p-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors shadow-sm" title="Tolak / Reject">
                                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                                    </button>
                                </form>
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
@endsection
