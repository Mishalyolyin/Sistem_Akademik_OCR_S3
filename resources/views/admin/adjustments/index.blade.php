@extends('layouts.admin')

@section('content')
<div class="mb-8">
    <h1 class="text-2xl font-bold text-gray-800">Adjustment / Koreksi</h1>
    <p class="text-gray-500 text-sm mt-1">Lakukan penyesuaian tagihan atau pembayaran manual.</p>
</div>

{{-- Search Section --}}
<div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6 mb-8">
    <form action="{{ route('admin.adjustments') }}" method="GET" class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
            <label for="search" class="block text-sm font-medium text-gray-700 mb-1">Cari Mahasiswa</label>
            <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                </div>
                <input type="text" name="search" id="search" value="{{ $search }}" 
                    class="pl-10 w-full rounded-lg border-gray-200 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all py-2.5"
                    placeholder="Masukkan NIM atau Nama...">
            </div>
        </div>
        <div class="flex items-end">
            <button type="submit" class="w-full md:w-auto bg-blue-600 text-white px-6 py-2.5 rounded-lg hover:bg-blue-700 transition-colors font-medium shadow-sm flex items-center justify-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                Cari
            </button>
        </div>
    </form>
</div>

@if($selectedStudent)
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
        {{-- Student Info & Wallet --}}
        <div class="lg:col-span-1 space-y-6">
            <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6 overflow-hidden relative">
                <div class="absolute top-0 left-0 w-1 h-full bg-blue-500"></div>
                <h3 class="font-bold text-lg text-gray-800 mb-6 flex items-center gap-2">
                    <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                    Data Mahasiswa
                </h3>
                <div class="space-y-4 text-sm">
                    <div class="flex justify-between items-center pb-3 border-b border-gray-50">
                        <span class="text-gray-500">Nama</span>
                        <span class="font-semibold text-gray-900 text-right">{{ $selectedStudent->name }}</span>
                    </div>
                    <div class="flex justify-between items-center pb-3 border-b border-gray-50">
                        <span class="text-gray-500">NIM</span>
                        <span class="font-mono text-gray-700 bg-gray-50 px-2 py-1 rounded">{{ $selectedStudent->nim }}</span>
                    </div>
                    <div class="flex justify-between items-center pb-3 border-b border-gray-50">
                        <span class="text-gray-500">Program</span>
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-100">
                            {{ $selectedStudent->program_type }}
                        </span>
                    </div>
                    <div class="pt-4">
                        <div class="flex justify-between items-center mb-4">
                            <span class="text-gray-500">Saldo Dompet</span>
                            <span class="font-bold text-xl text-gray-900">Rp {{ number_format($selectedStudent->wallet_balance, 0, ',', '.') }}</span>
                        </div>
                        
                        {{-- Wallet Adjustment Form --}}
                        <div x-data="{ open: false }" class="bg-gray-50 rounded-lg p-4 border border-gray-100">
                            <button @click="open = !open" class="w-full flex justify-between items-center text-sm font-medium text-blue-600 hover:text-blue-700">
                                <span>+ Koreksi Saldo Dompet</span>
                                <svg class="w-4 h-4 transition-transform" :class="{'rotate-180': open}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                            </button>
                            
                            <div x-show="open" 
                                 x-transition:enter="transition ease-out duration-200"
                                 x-transition:enter-start="opacity-0 -translate-y-2"
                                 x-transition:enter-end="opacity-100 translate-y-0"
                                 class="mt-4 pt-3 border-t border-gray-200">
                                <form action="{{ route('admin.adjustments.store') }}" method="POST">
                                    @csrf
                                    <input type="hidden" name="target_type" value="student">
                                    <input type="hidden" name="target_id" value="{{ $selectedStudent->id }}">
                                    
                                    <div class="mb-3">
                                        <label class="block text-xs font-medium text-gray-600 mb-1">Nominal (+/-)</label>
                                        <input type="number" name="amount" required class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2">
                                        <p class="text-[10px] text-gray-400 mt-1">Positif untuk tambah, Negatif untuk kurangi</p>
                                    </div>
                                    <div class="mb-3">
                                        <label class="block text-xs font-medium text-gray-600 mb-1">Alasan</label>
                                        <input type="text" name="reason" required class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2">
                                    </div>
                                    <button type="submit" class="w-full bg-blue-600 text-white text-xs font-bold py-2 rounded-lg hover:bg-blue-700 shadow-sm transition-all">
                                        Simpan Koreksi
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        {{-- Payment Plan & Installments --}}
        <div class="lg:col-span-2">
            <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
                <h3 class="font-bold text-lg text-gray-800 mb-6 flex items-center gap-2">
                    <svg class="w-5 h-5 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path></svg>
                    Tagihan Aktif
                </h3>
                
                @forelse($selectedStudent->paymentPlans as $plan)
                    <div class="mb-8 last:mb-0 border border-gray-100 rounded-lg overflow-hidden">
                        <div class="flex justify-between items-center bg-gray-50 px-4 py-3 border-b border-gray-100">
                            <span class="font-semibold text-gray-800">{{ $plan->academic_year }} - {{ $plan->term }}</span>
                            <span class="text-sm text-gray-600 font-medium">Total: Rp {{ number_format($plan->total_amount, 0, ',', '.') }}</span>
                        </div>
                        
                        <div class="overflow-x-auto">
                            <table class="w-full text-sm text-left">
                                <thead class="text-xs text-gray-500 uppercase bg-white border-b border-gray-100">
                                    <tr>
                                        <th class="px-4 py-3 font-medium">Cicilan ke</th>
                                        <th class="px-4 py-3 font-medium">Jatuh Tempo</th>
                                        <th class="px-4 py-3 font-medium">Tagihan</th>
                                        <th class="px-4 py-3 font-medium">Terbayar</th>
                                        <th class="px-4 py-3 font-medium">Status</th>
                                        <th class="px-4 py-3 font-medium text-right">Aksi</th>
                                    </tr>
                                </thead>
                                <tbody class="divide-y divide-gray-50 bg-white">
                                    @foreach($plan->installments as $installment)
                                        <tr class="hover:bg-gray-50 transition-colors">
                                            <td class="px-4 py-3 font-medium text-gray-900">{{ $installment->installment_no }}</td>
                                            <td class="px-4 py-3 text-gray-600">{{ \Carbon\Carbon::parse($installment->due_date)->format('d M Y') }}</td>
                                            <td class="px-4 py-3 text-gray-600">Rp {{ number_format($installment->amount, 0, ',', '.') }}</td>
                                            <td class="px-4 py-3 text-emerald-600 font-medium">Rp {{ number_format($installment->amount_paid, 0, ',', '.') }}</td>
                                            <td class="px-4 py-3">
                                                @php
                                                    $statusClasses = [
                                                        'PAID' => 'bg-emerald-100 text-emerald-800 border-emerald-200',
                                                        'PARTIAL' => 'bg-amber-100 text-amber-800 border-amber-200',
                                                        'UNPAID' => 'bg-red-100 text-red-800 border-red-200',
                                                    ];
                                                    $statusClass = $statusClasses[$installment->status] ?? 'bg-gray-100 text-gray-800 border-gray-200';
                                                @endphp
                                                <span class="px-2.5 py-0.5 rounded-full text-xs font-semibold border {{ $statusClass }}">
                                                    {{ $installment->status }}
                                                </span>
                                            </td>
                                            <td class="px-4 py-3 text-right">
                                                <div x-data="{ open: false }" class="relative inline-block text-left">
                                                    <button @click="open = !open" class="text-xs bg-white border border-gray-300 text-gray-700 px-3 py-1.5 rounded-lg hover:bg-gray-50 hover:border-gray-400 transition-all font-medium">
                                                        Koreksi
                                                    </button>
                                                    
                                                    <!-- Dropdown / Popover for Adjustment -->
                                                    <div x-show="open" @click.outside="open = false" 
                                                         x-transition:enter="transition ease-out duration-100"
                                                         x-transition:enter-start="opacity-0 scale-95"
                                                         x-transition:enter-end="opacity-100 scale-100"
                                                         class="absolute right-0 mt-2 w-72 bg-white shadow-xl rounded-xl border border-gray-100 z-50 p-4 transform origin-top-right">
                                                        <h4 class="text-sm font-bold mb-3 text-gray-800 border-b border-gray-100 pb-2">Koreksi Cicilan #{{ $installment->installment_no }}</h4>
                                                        <form action="{{ route('admin.adjustments.store') }}" method="POST">
                                                            @csrf
                                                            <input type="hidden" name="target_type" value="installment">
                                                            <input type="hidden" name="target_id" value="{{ $installment->id }}">
                                                            
                                                            <div class="mb-3 text-left">
                                                                <label class="block text-xs font-medium text-gray-600 mb-1">Nominal (+/-)</label>
                                                                <input type="number" name="amount" placeholder="Contoh: 50000" required 
                                                                    class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2">
                                                            </div>
                                                            <div class="mb-3 text-left">
                                                                <label class="block text-xs font-medium text-gray-600 mb-1">Alasan</label>
                                                                <input type="text" name="reason" placeholder="Contoh: Koreksi manual" required 
                                                                    class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2">
                                                            </div>
                                                            <button type="submit" class="w-full bg-blue-600 text-white text-xs font-bold py-2 rounded-lg hover:bg-blue-700 transition-colors">
                                                                Simpan
                                                            </button>
                                                        </form>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                    @endforeach
                                </tbody>
                            </table>
                        </div>
                    </div>
                @empty
                    <div class="text-center py-12 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                        <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                        <p class="text-gray-500 mt-2 font-medium">Mahasiswa ini belum memiliki Payment Plan aktif.</p>
                    </div>
                @endforelse
            </div>
        </div>
    </div>
@elseif($search)
    <div class="bg-amber-50 text-amber-800 p-4 rounded-xl mb-8 border border-amber-200 flex items-center gap-3">
        <svg class="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
        <span class="font-medium">Mahasiswa dengan kata kunci "{{ $search }}" tidak ditemukan.</span>
    </div>
@endif

{{-- History Table --}}
<div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
    <div class="flex justify-between items-center mb-6">
        <h3 class="font-bold text-lg text-gray-800 flex items-center gap-2">
            <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            Riwayat Adjustment
        </h3>
    </div>
    
    <div class="overflow-x-auto">
        <table class="w-full text-sm text-left">
            <thead class="text-xs text-gray-500 uppercase bg-gray-50 border-b border-gray-100">
                <tr>
                    <th class="px-6 py-3 font-medium">Tanggal</th>
                    <th class="px-6 py-3 font-medium">Admin</th>
                    <th class="px-6 py-3 font-medium">Target</th>
                    <th class="px-6 py-3 font-medium">Nominal</th>
                    <th class="px-6 py-3 font-medium">Alasan</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
                @forelse($adjustments as $adj)
                    <tr class="hover:bg-gray-50 transition-colors">
                        <td class="px-6 py-4 text-gray-600">
                            {{ $adj->created_at->format('d M Y H:i') }}
                        </td>
                        <td class="px-6 py-4 font-medium text-gray-800">
                            <div class="flex items-center gap-2">
                                <div class="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center text-xs text-gray-600 font-bold">
                                    {{ substr($adj->admin->name ?? 'S', 0, 1) }}
                                </div>
                                {{ $adj->admin->name ?? 'System' }}
                            </div>
                        </td>
                        <td class="px-6 py-4">
                            @if($adj->adjustable_type === 'App\Models\Student')
                                <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-100">Dompet Mahasiswa</span>
                                <div class="text-xs text-gray-500 mt-1">{{ $adj->adjustable->name ?? 'Unknown' }}</div>
                            @elseif($adj->adjustable_type === 'App\Models\Installment')
                                <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-50 text-purple-700 border border-purple-100">Cicilan</span>
                                <div class="text-xs text-gray-500 mt-1">ID: {{ $adj->adjustable_id }}</div>
                            @else
                                <span class="badge badge-outline">{{ class_basename($adj->adjustable_type) }}</span>
                            @endif
                        </td>
                        <td class="px-6 py-4 font-bold {{ $adj->amount > 0 ? 'text-emerald-600' : 'text-red-600' }}">
                            {{ $adj->amount > 0 ? '+' : '' }}Rp {{ number_format($adj->amount, 0, ',', '.') }}
                        </td>
                        <td class="px-6 py-4 text-gray-600">
                            {{ $adj->reason }}
                        </td>
                    </tr>
                @empty
                    <tr>
                        <td colspan="5" class="text-center py-12 text-gray-400 italic bg-gray-50">
                            <div class="flex flex-col items-center">
                                <svg class="w-10 h-10 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                                <span>Belum ada data adjustment.</span>
                            </div>
                        </td>
                    </tr>
                @endforelse
            </tbody>
        </table>
    </div>
    
    <div class="mt-6">
        {{ $adjustments->appends(['search' => $search])->links() }}
    </div>
</div>
@endsection
