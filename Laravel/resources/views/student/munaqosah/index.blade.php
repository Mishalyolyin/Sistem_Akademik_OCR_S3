@extends('layouts.student')

@section('title', 'Pembayaran Munaqosah')

@section('content')

<!-- Header -->
<div class="mb-8">
    <div class="flex items-center gap-4">
        <div class="w-16 h-16 bg-white rounded-xl flex items-center justify-center shadow-lg shadow-emerald-200 overflow-hidden border border-emerald-100">
            <img src="{{ asset('images/unissula-logo.png') }}" alt="Logo Unissula" class="w-full h-full object-contain p-1">
        </div>
        <div>
            <h1 class="text-2xl font-serif font-bold text-gray-800">Pembayaran Munaqosah</h1>
            <p class="text-gray-500 text-sm">Kelola tagihan sidang akhir Munaqosah Anda.</p>
        </div>
    </div>
</div>

@if(session('success'))
<div class="mb-6 bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-lg flex items-center gap-2 animate-fade-in-down">
    <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
    <span>{{ session('success') }}</span>
</div>
@endif

@if(session('error'))
<div class="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2 animate-fade-in-down">
    <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
    <span>{{ session('error') }}</span>
</div>
@endif

@if(!$eligible && $status !== 'has_plan')
    <!-- LOCKED STATE (Not Eligible) -->
    <div class="flex flex-col items-center justify-center py-16 px-4 text-center">
        <div class="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mb-6 relative">
            <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
            <div class="absolute -bottom-2 -right-2 bg-red-500 text-white rounded-full p-2 border-4 border-white">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
            </div>
        </div>
        <h2 class="text-2xl font-bold text-gray-800 mb-2">Halaman Terkunci</h2>
        <p class="text-gray-500 max-w-md mb-8">
            Menu pembayaran Munaqosah belum tersedia. Anda diwajibkan untuk melunasi seluruh tagihan <strong>SPP Semester</strong> terlebih dahulu.
        </p>
        <a href="{{ route('student.dashboard') }}" class="inline-flex items-center gap-2 px-6 py-3 bg-white border border-gray-300 text-gray-700 font-bold rounded-xl hover:bg-gray-50 hover:text-gray-900 transition-all shadow-sm">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
            Kembali ke Dashboard
        </a>
    </div>

@elseif($status === 'no_plan' && $eligible)
    <!-- ELIGIBLE STATE (Ready to Choose) -->
    <div class="max-w-4xl mx-auto">
        <!-- Notification Banner -->
        <div class="bg-gradient-to-r from-emerald-500 to-teal-600 rounded-2xl p-6 text-white shadow-xl mb-10 relative overflow-hidden">
            <div class="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2"></div>
            <div class="relative z-10 flex flex-col md:flex-row items-center gap-6 text-center md:text-left">
                <div class="w-16 h-16 bg-white/20 rounded-full flex items-center justify-center flex-shrink-0 backdrop-blur-sm border border-white/30">
                    <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                </div>
                <div>
                    <h2 class="text-xl font-bold mb-1">Selamat! SPP Semester Lunas</h2>
                    <p class="text-emerald-50 text-sm leading-relaxed">
                        Terima kasih telah menyelesaikan kewajiban administrasi semester. 
                        Kini Anda dapat melanjutkan ke tahap pembayaran <strong>Munaqosah</strong> sebagai syarat sidang akhir.
                    </p>
                </div>
            </div>
        </div>

        <h3 class="font-bold text-gray-800 text-lg mb-6 flex items-center gap-2">
            <span>Pilih Skema Pembayaran</span>
            <span class="h-px bg-gray-200 flex-1 ml-2"></span>
        </h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            @foreach($templates as $template)
            <div class="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm hover:shadow-lg transition-all duration-300 group relative overflow-hidden">
                <div class="absolute top-0 right-0 w-24 h-24 bg-emerald-50 rounded-bl-full -mr-4 -mt-4 transition-transform group-hover:scale-110"></div>
                
                <div class="relative z-10">
                    <div class="flex justify-between items-start mb-4">
                        <div class="bg-emerald-100 text-emerald-700 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider">
                            Paket
                        </div>
                    </div>
                    
                    <h3 class="font-serif font-bold text-xl text-gray-800 mb-2 group-hover:text-emerald-700 transition-colors">{{ $template->name }}</h3>
                    <p class="text-gray-500 text-sm mb-6 line-clamp-2">Paket pembayaran khusus untuk biaya pendaftaran dan pelaksanaan sidang Munaqosah.</p>
                    
                    <div class="flex items-end justify-between mt-auto">
                        <div>
                            <p class="text-xs text-gray-400 uppercase font-bold">Total Biaya</p>
                            <p class="font-bold text-2xl text-gray-800">Rp {{ number_format($template->items->sum('amount') ?? 2500000, 0, ',', '.') }}</p>
                        </div>
                        
                        <form action="{{ route('student.munaqosah.store') }}" method="POST">
                            @csrf
                            <input type="hidden" name="installment_template_id" value="{{ $template->id }}">
                            <button type="submit" class="bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg shadow-emerald-200 transition-all transform hover:-translate-y-0.5 active:translate-y-0">
                                Pilih
                            </button>
                        </form>
                    </div>
                </div>
            </div>
            @endforeach
        </div>
    </div>

@elseif($status === 'has_plan')
    <!-- HAS PLAN STATE -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Main Stats -->
        <div class="lg:col-span-2 space-y-8">
            <!-- Hero Widget -->
            <div class="bg-gradient-to-br from-emerald-900 to-green-800 rounded-3xl p-8 text-white shadow-2xl shadow-emerald-900/20 relative overflow-hidden border border-white/10">
                <!-- Decorative Elements -->
                <div class="absolute top-0 right-0 w-80 h-80 bg-white/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/3"></div>
                <div class="absolute bottom-0 left-0 w-64 h-64 bg-emerald-500/20 rounded-full blur-3xl translate-y-1/3 -translate-x-1/4"></div>
                
                <div class="relative z-10">
                    @if($nextInstallment)
                        <div class="flex flex-col md:flex-row md:items-center justify-between gap-8">
                            <div>
                                <div class="flex items-center gap-2 mb-3">
                                    <span class="px-2.5 py-1 bg-white/20 backdrop-blur-md border border-white/20 text-white text-[10px] font-bold rounded uppercase tracking-wider">Tagihan Aktif</span>
                                    <span class="text-emerald-200 text-sm font-medium">{{ $nextInstallment->name }}</span>
                                </div>
                                <h2 class="font-serif text-4xl md:text-5xl font-bold mb-3 tracking-tight text-white drop-shadow-lg">Rp {{ number_format($nextInstallment->amount, 0, ',', '.') }}</h2>
                                
                                <div class="mt-6 p-4 bg-white/10 rounded-xl border border-white/10 backdrop-blur-sm">
                                    <div class="flex items-center gap-3">
                                        <div class="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                                            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path></svg>
                                        </div>
                                        <div>
                                            <p class="font-bold text-white text-sm">Tagihan Munaqosah Telah Terbit</p>
                                            <p class="text-emerald-200 text-xs">Silakan lakukan pembayaran untuk melanjutkan proses sidang.</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="flex flex-col gap-3 min-w-[200px]">
                                @php
                                    $nextPending = $nextInstallment->payments->whereIn('status', ['PENDING', 'NEEDS_REVIEW', 'AUTO_VERIFIED'])->first();
                                @endphp

                                @if($nextPending)
                                    <button disabled class="w-full py-4 px-6 bg-white/10 text-white/50 font-bold rounded-xl cursor-not-allowed flex items-center justify-center gap-2 border border-white/10">
                                        <svg class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                        Sedang Diverifikasi
                                    </button>
                                @else
                                    <button onclick="document.getElementById('upload-modal').classList.remove('hidden')" class="w-full py-4 px-6 bg-white text-emerald-900 font-bold rounded-xl hover:bg-gray-50 transition-all shadow-lg flex items-center justify-center gap-2 transform hover:-translate-y-1">
                                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path></svg>
                                        Bayar Sekarang
                                    </button>
                                @endif
                            </div>
                        </div>
                    @else
                        <!-- All Paid State -->
                        <div class="text-center py-8">
                            <div class="w-24 h-24 bg-gradient-to-br from-emerald-400 to-green-500 rounded-full flex items-center justify-center mx-auto mb-6 shadow-lg shadow-green-500/30 animate-pulse">
                                <svg class="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path></svg>
                            </div>
                            <h2 class="font-serif text-3xl font-bold text-white mb-2">Lunas!</h2>
                            <p class="text-emerald-100 max-w-lg mx-auto">Selamat! Seluruh biaya Munaqosah telah lunas. Semoga sidang akhir Anda berjalan lancar dan sukses.</p>
                        </div>
                    @endif
                </div>
            </div>

            <!-- Installment List -->
            <div class="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
                <h3 class="font-bold text-lg text-gray-800 mb-6 flex items-center gap-2">
                    <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path></svg>
                    Rincian Tagihan
                </h3>
                <div class="space-y-4">
                    @foreach($plan->installments as $installment)
                    @php
                        $pending = $installment->payments->whereIn('status', ['PENDING', 'NEEDS_REVIEW', 'AUTO_VERIFIED'])->first();
                        $displayStatus = $installment->status;
                        if($pending && $installment->status != 'PAID') {
                            $displayStatus = 'VERIFIKASI';
                        }
                    @endphp
                    <div class="flex items-center justify-between p-5 rounded-xl border transition-all hover:shadow-md {{ $installment->status == 'PAID' ? 'border-emerald-100 bg-emerald-50/50' : ($installment->status == 'VERIFIED' ? 'border-blue-100 bg-blue-50/50' : ($displayStatus == 'VERIFIKASI' ? 'border-yellow-100 bg-yellow-50/50' : 'border-gray-100 bg-white')) }}">
                        <div class="flex items-center gap-4">
                            <div class="w-10 h-10 rounded-full flex items-center justify-center border-2 {{ $installment->status == 'PAID' ? 'border-emerald-200 bg-emerald-100 text-emerald-600' : 'border-gray-100 bg-gray-50 text-gray-400' }}">
                                <span class="font-bold text-sm">{{ $loop->iteration }}</span>
                            </div>
                            <div>
                                <div class="font-bold text-gray-800">{{ $installment->name }}</div>
                                <div class="text-xs text-gray-500">Tagihan Munaqosah</div>
                            </div>
                        </div>
                        <div class="text-right">
                            <div class="font-bold text-gray-800 text-lg">Rp {{ number_format($installment->amount, 0, ',', '.') }}</div>
                            <span class="text-[10px] font-bold px-2.5 py-1 rounded-full uppercase tracking-wide {{ 
                                $installment->status == 'PAID' ? 'bg-emerald-100 text-emerald-700' : 
                                ($displayStatus == 'VERIFIKASI' ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-600') 
                            }}">
                                {{ $displayStatus == 'UNPAID' ? 'BELUM BAYAR' : $displayStatus }}
                            </span>
                        </div>
                    </div>
                    @endforeach
                </div>
            </div>
        </div>

        <!-- Sidebar Info -->
        <div class="space-y-6">
            <div class="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm sticky top-24">
                <h3 class="font-bold text-lg text-gray-800 mb-4">Informasi Penting</h3>
                <ul class="space-y-4 text-sm text-gray-600">
                    <li class="flex items-start gap-3">
                        <div class="w-6 h-6 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0 mt-0.5">
                            <span class="font-bold text-xs">1</span>
                        </div>
                        <span>Pembayaran Munaqosah wajib dilunasi sebelum mendaftar sidang akhir.</span>
                    </li>
                    <li class="flex items-start gap-3">
                        <div class="w-6 h-6 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0 mt-0.5">
                            <span class="font-bold text-xs">2</span>
                        </div>
                        <span>Simpan bukti pembayaran (kuitansi digital) sebagai syarat pemberkasan akademik.</span>
                    </li>
                    <li class="flex items-start gap-3">
                        <div class="w-6 h-6 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0 mt-0.5">
                            <span class="font-bold text-xs">3</span>
                        </div>
                        <span>Jika mengalami kendala pembayaran, silakan hubungi bagian keuangan.</span>
                    </li>
                </ul>
            </div>
        </div>
    </div>

    <!-- Upload Modal -->
    <div id="upload-modal" class="fixed inset-0 z-50 hidden" aria-labelledby="modal-title" role="dialog" aria-modal="true">
        <div class="fixed inset-0 bg-gray-900/60 backdrop-blur-sm transition-opacity"></div>
        <div class="fixed inset-0 z-10 overflow-y-auto">
            <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
                <div class="relative transform overflow-hidden rounded-2xl bg-white text-left shadow-2xl transition-all sm:my-8 sm:w-full sm:max-w-lg border border-emerald-100">
                    <div class="bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
                        <div class="sm:flex sm:items-start">
                            <div class="mx-auto flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-emerald-100 sm:mx-0 sm:h-10 sm:w-10">
                                <svg class="h-6 w-6 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
                                </svg>
                            </div>
                            <div class="mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left w-full">
                                <h3 class="text-lg font-serif font-bold leading-6 text-gray-900" id="modal-title">Upload Bukti Pembayaran</h3>
                                <div class="mt-2">
                                    <p class="text-sm text-gray-500 mb-4">Pastikan foto bukti transfer terlihat jelas, tidak buram, dan memuat tanggal serta nominal transaksi.</p>
                                    
                                    <form action="{{ url('/mahasiswa/payments') }}" method="POST" enctype="multipart/form-data" id="payment-form">
                                        @csrf
                                        <input type="hidden" name="installment_id" value="{{ $nextInstallment ? $nextInstallment->id : '' }}">
                                        
                                        <div class="space-y-4">
                                            <div>
                                                <label class="block text-xs font-bold text-gray-700 uppercase mb-1">Nominal Transfer</label>
                                                <div class="relative rounded-md shadow-sm">
                                                    <div class="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                                                        <span class="text-gray-500 sm:text-sm font-bold">Rp</span>
                                                    </div>
                                                    <input type="text" 
                                                        class="block w-full rounded-lg border-gray-300 pl-10 bg-gray-100 text-gray-500 cursor-not-allowed focus:border-gray-300 focus:ring-0 sm:text-sm py-2.5 font-bold" 
                                                        value="{{ $nextInstallment ? number_format($nextInstallment->amount, 0, ',', '.') : '' }}"
                                                        readonly>
                                                    <input type="hidden" name="amount" value="{{ $nextInstallment ? intval($nextInstallment->amount) : '' }}">
                                                </div>
                                                <div class="mt-2 p-3 bg-blue-50 border border-blue-100 rounded-lg flex items-start gap-2">
                                                    <svg class="w-5 h-5 text-blue-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                                    <p class="text-xs text-blue-700 leading-relaxed">
                                                        Silakan transfer ke rekening <strong>BSI MPAI UNISSULA</strong>.
                                                        <br>Nomor Rekening: <strong>7123456789</strong> (a.n. Magister PAI Unissula)
                                                    </p>
                                                </div>
                                            </div>

                                            <div>
                                                <label class="block text-xs font-bold text-gray-700 uppercase mb-1">Bukti Foto</label>
                                                <input type="file" name="file" class="block w-full text-sm text-gray-500
                                                    file:mr-4 file:py-2.5 file:px-4
                                                    file:rounded-lg file:border-0
                                                    file:text-sm file:font-semibold
                                                    file:bg-emerald-50 file:text-emerald-700
                                                    hover:file:bg-emerald-100
                                                    transition-colors cursor-pointer" required>
                                                <p class="mt-1 text-xs text-gray-400">Format: JPG, PNG, PDF (Max 2MB)</p>
                                            </div>
                                        </div>

                                        <div class="mt-6 flex flex-row-reverse gap-2">
                                            <button type="submit" class="inline-flex w-full justify-center rounded-lg bg-emerald-600 px-3 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-emerald-500 sm:ml-3 sm:w-auto transition-colors">Kirim Bukti</button>
                                            <button type="button" onclick="document.getElementById('upload-modal').classList.add('hidden')" class="mt-3 inline-flex w-full justify-center rounded-lg bg-white px-3 py-2.5 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:mt-0 sm:w-auto transition-colors">Batal</button>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
@endif

@endsection
