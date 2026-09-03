@extends('layouts.student')

@section('content')

<!-- Welcome & Identity Section -->
<div class="mb-8">
    <div class="card-luxury p-6 flex flex-col md:flex-row items-center justify-between gap-6 relative overflow-hidden group">
        <!-- Background Decoration -->
        <div class="absolute top-0 right-0 w-64 h-64 bg-gold/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 pointer-events-none group-hover:bg-gold/10 transition-colors duration-500"></div>
        
        <div class="flex items-center gap-6 relative z-10">
            <div class="w-20 h-20 rounded-full bg-white p-1 border-2 border-gold/30 shadow-lg">
                <img src="https://ui-avatars.com/api/?name={{ urlencode($student->name) }}&background=0F172A&color=D4AF37" alt="Profile" class="w-full h-full rounded-full object-cover">
            </div>
            <div>
                <h1 class="font-serif text-2xl font-bold text-primary mb-1">
                    Halo, <span class="text-gold-gradient">{{ $student->name }}</span>
                </h1>
                <div class="flex flex-wrap gap-3 text-sm text-text-muted">
                    <span class="flex items-center gap-1">
                        <svg class="w-4 h-4 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0c0 .667.448 1 1 1h2c.552 0 1-.333 1-1m-6 0h6"></path></svg>
                        {{ $student->nim }}
                    </span>
                    <span class="w-1 h-1 bg-gold rounded-full self-center"></span>
                    <span class="flex items-center gap-1">
                        <svg class="w-4 h-4 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
                        Program {{ $student->program_type }}
                    </span>
                    <span class="w-1 h-1 bg-gold rounded-full self-center"></span>
                    <span class="flex items-center gap-1">
                        <svg class="w-4 h-4 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                        Kelas {{ $student->class ?? '-' }}
                    </span>
                </div>
            </div>
        </div>

        <div class="text-right hidden md:block relative z-10">
            <div class="text-xs text-gold font-bold uppercase tracking-widest mb-1 font-serif">Semester Aktif</div>
            <div class="font-serif text-2xl font-bold text-primary">{{ $semesterInfo['academic_year'] }}</div>
            <div class="text-text-muted font-medium">{{ $semesterInfo['term'] }}</div>
        </div>
    </div>
</div>

<!-- Step Indicator (Workflow) -->
<div class="mb-10 overflow-x-auto pb-4">
    <div class="flex items-center justify-between min-w-[600px] px-4">
        <!-- Step 1: Pilih Skema -->
        <div class="flex flex-col items-center gap-2 relative group">
            <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition-all duration-300 {{ $status === 'choose_plan' ? 'bg-gold-gradient text-primary ring-4 ring-gold/20 shadow-lg scale-110' : ($status === 'has_plan' ? 'bg-primary text-gold border border-gold/30' : 'bg-surface text-text-muted border border-gray-200') }}">
                @if($status === 'has_plan') ✓ @else 1 @endif
            </div>
            <span class="text-xs font-bold uppercase tracking-wider {{ $status === 'choose_plan' ? 'text-gold' : ($status === 'has_plan' ? 'text-primary' : 'text-text-muted') }}">Pilih Skema</span>
            <!-- Connector -->
            <div class="absolute top-5 left-1/2 w-[calc(100%+20px)] h-0.5 -z-10 bg-gray-200">
                <div class="h-full bg-gold-gradient transition-all duration-500" style="width: {{ $status === 'has_plan' ? '100%' : '0%' }}"></div>
            </div>
        </div>

        <!-- Step 2: Bayar Tagihan -->
        <div class="flex flex-col items-center gap-2 relative group">
            <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition-all duration-300 {{ ($status === 'has_plan' && $nextInstallment) ? 'bg-gold-gradient text-primary ring-4 ring-gold/20 shadow-lg scale-110' : 'bg-surface text-text-muted border border-gray-200' }}">
                2
            </div>
            <span class="text-xs font-bold uppercase tracking-wider {{ ($status === 'has_plan' && $nextInstallment) ? 'text-gold' : 'text-text-muted' }}">Bayar Tagihan</span>
            <div class="absolute top-5 left-1/2 w-[calc(100%+20px)] h-0.5 -z-10 bg-gray-200"></div>
        </div>

        <!-- Step 3: Upload Bukti -->
        <div class="flex flex-col items-center gap-2 relative">
            <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm bg-surface text-text-muted border border-gray-200">
                3
            </div>
            <span class="text-xs font-bold uppercase tracking-wider text-text-muted">Upload Bukti</span>
            <div class="absolute top-5 left-1/2 w-[calc(100%+20px)] h-0.5 -z-10 bg-gray-200"></div>
        </div>

        <!-- Step 4: Verifikasi -->
        <div class="flex flex-col items-center gap-2 relative">
            <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm bg-surface text-text-muted border border-gray-200">
                4
            </div>
            <span class="text-xs font-bold uppercase tracking-wider text-text-muted">Verifikasi</span>
        </div>
    </div>
</div>

@if($status === 'choose_plan')
    <!-- Plan Selection State -->
    <div class="max-w-5xl mx-auto">
        <div class="text-center mb-12">
            <h2 class="font-serif text-4xl font-bold text-primary mb-4">Pilih Skema Pembayaran</h2>
            <p class="text-text-muted max-w-lg mx-auto">Tentukan fleksibilitas pembayaran Anda. Skema ini akan berlaku selama satu semester penuh.</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
            @foreach($templates as $template)
            <div class="card-luxury p-8 flex flex-col h-full group relative overflow-hidden">
                <!-- Hover Decoration -->
                <div class="absolute top-0 right-0 w-40 h-40 bg-gold/5 rounded-full -translate-y-1/2 translate-x-1/2 group-hover:bg-gold/10 transition-colors duration-500"></div>
                
                <div class="relative z-10 flex-1">
                    <div class="flex justify-between items-start mb-6">
                        <div>
                            <h3 class="font-serif text-2xl font-bold text-primary mb-1">{{ $template->name }}</h3>
                            <span class="px-3 py-1 bg-primary/5 text-primary text-xs font-bold rounded-full uppercase tracking-wider border border-primary/10">{{ $student->program_type }} Only</span>
                        </div>
                        <div class="w-12 h-12 rounded-xl bg-primary/5 flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-gold transition-all duration-300 shadow-sm group-hover:shadow-lg">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path></svg>
                        </div>
                    </div>

                    <div class="space-y-4 mb-8">
                        <div class="flex items-center justify-between text-sm p-3 bg-surface rounded-lg border border-gray-100 group-hover:border-gold/20 transition-colors">
                            <span class="text-text-muted">Total Biaya</span>
                            <span class="font-bold text-primary">Rp {{ number_format($template->items->sum('amount'), 0, ',', '.') }}</span>
                        </div>
                        <div class="flex items-center justify-between text-sm p-3 bg-surface rounded-lg border border-gray-100 group-hover:border-gold/20 transition-colors">
                            <span class="text-text-muted">Tenor</span>
                            <span class="font-bold text-primary">{{ $template->items->count() }}x Angsuran</span>
                        </div>
                        <div class="flex items-center justify-between text-sm p-3 bg-surface rounded-lg border border-gray-100 group-hover:border-gold/20 transition-colors">
                            <span class="text-text-muted">Estimasi /bulan</span>
                            <span class="font-bold text-primary">~ Rp {{ number_format($template->items->avg('amount'), 0, ',', '.') }}</span>
                        </div>
                    </div>
                </div>

                <form action="{{ url('/mahasiswa/plan') }}" method="POST" class="mt-auto relative z-10">
                    @csrf
                    <input type="hidden" name="installment_template_id" value="{{ $template->id }}">
                    
                    <!-- Mock Start Month Selection (Visual Requirement) -->
                    <div class="mb-4">
                        <label class="block text-xs font-bold text-gold uppercase mb-2 tracking-wider">Mulai Pembayaran</label>
                        <select class="w-full text-sm input-luxury rounded-lg text-primary">
                            <option>Bulan Ini (Oktober)</option>
                            <option>Bulan Depan (November)</option>
                        </select>
                    </div>

                    <button type="submit" class="w-full py-4 btn-luxury text-white font-bold rounded-xl shadow-lg flex items-center justify-center gap-2 transform group-hover:-translate-y-1 transition-all">
                        Pilih Rencana Ini
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"></path></svg>
                    </button>
                </form>
            </div>
            @endforeach
        </div>
    </div>

@elseif($status === 'has_plan')
    <!-- Dashboard State -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        <!-- Left Column: Main Stats -->
        <div class="lg:col-span-2 space-y-8">
            
            <!-- Hero Widget: Next Bill -->
            <div class="bg-gradient-to-br from-primary to-primary-dark rounded-3xl p-8 text-white shadow-2xl relative overflow-hidden group border border-gold/10">
                <div class="absolute top-0 right-0 w-64 h-64 bg-gold/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 group-hover:bg-gold/20 transition-colors duration-500"></div>
                
                <div class="relative z-10">
                    @if($nextInstallment)
                        <div class="flex flex-col md:flex-row md:items-center justify-between gap-6">
                            <div>
                                <div class="flex items-center gap-2 mb-2">
                                    <span class="px-2 py-1 bg-gold text-primary text-xs font-bold rounded uppercase tracking-wider shadow-lg shadow-gold/20">Tagihan Aktif</span>
                                    <span class="text-white/80 text-xs">{{ $nextInstallment->name }}</span>
                                    @if(str_contains($plan->category, 'MUNAOSAH'))
                                        <span class="px-2 py-1 bg-purple-500 text-white text-xs font-bold rounded uppercase tracking-wider shadow-lg">MUNAQOSAH</span>
                                    @endif
                                </div>
                                <h2 class="font-serif text-5xl font-bold mb-2 tracking-tight text-white drop-shadow-sm">Rp {{ number_format($nextInstallment->amount, 0, ',', '.') }}</h2>
                                <p class="text-white/70 text-sm flex items-center gap-2">
                                    <svg class="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                    Jatuh tempo: <span class="text-white font-bold">{{ \Carbon\Carbon::parse($nextInstallment->due_date)->format('d M Y') }}</span>
                                </p>
                            </div>
                            
                            <div class="flex flex-col gap-3 min-w-[200px]">
                                @php
                                    $nextPending = $nextInstallment->payments->whereIn('status', ['PENDING', 'NEEDS_REVIEW', 'AUTO_VERIFIED'])->first();
                                @endphp

                                @if($nextPending)
                                    <button disabled class="w-full py-4 px-6 bg-gray-500/50 text-white font-bold rounded-xl cursor-not-allowed flex items-center justify-center gap-2">
                                        <svg class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                        Menunggu Verifikasi
                                    </button>
                                @else
                                    <button onclick="document.getElementById('upload-modal').classList.remove('hidden')" class="w-full py-4 px-6 bg-gradient-to-r from-gold to-gold-dark text-primary font-bold rounded-xl hover:to-gold transition-all shadow-lg shadow-gold/20 hover:shadow-gold/40 flex items-center justify-center gap-2 transform hover:-translate-y-1">
                                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path></svg>
                                        Bayar Sekarang
                                    </button>
                                @endif
                            </div>
                        </div>
                    @else
                        <div class="text-center py-8">
                            <div class="w-20 h-20 bg-green-500/20 text-green-400 rounded-full flex items-center justify-center mx-auto mb-6 animate-pulse">
                                <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                            </div>
                            <h2 class="font-serif text-3xl font-bold text-white mb-2">Lunas!</h2>
                            <p class="text-green-200">Terima kasih, seluruh tagihan semester ini telah Anda selesaikan.</p>
                            
                            <div class="mt-6">
                                <a href="{{ route('student.munaqosah') }}" class="inline-flex items-center gap-2 px-6 py-3 bg-white/10 hover:bg-white/20 text-white rounded-xl font-bold transition-all border border-white/20 backdrop-blur-sm">
                                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M12 14l9-5-9-5-9 5 9 5z"></path><path d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z"></path></svg>
                                    Cek Tagihan Munaqosah
                                </a>
                            </div>
                        </div>
                    @endif
                </div>
            </div>

            <!-- Financial Health & Installment List -->
            <div class="card-luxury p-6">
                <div class="flex items-center justify-between mb-6">
                    <h3 class="font-serif text-xl font-bold text-primary">Rincian Pembayaran</h3>
                    <div class="text-sm text-text-muted">Progress: <span class="font-bold text-gold">{{ number_format($progressPercentage, 0) }}%</span></div>
                </div>

                <!-- Summary Cards -->
                <div class="grid grid-cols-2 gap-4 mb-6">
                    <div class="p-4 rounded-xl bg-green-50 border border-green-100">
                        <div class="text-xs font-bold text-green-600 uppercase tracking-wider mb-1">Total Terbayar</div>
                        <div class="text-xl font-bold text-green-800">Rp {{ number_format($paidAmount, 0, ',', '.') }}</div>
                    </div>
                    <div class="p-4 rounded-xl bg-red-50 border border-red-100">
                        <div class="text-xs font-bold text-red-600 uppercase tracking-wider mb-1">Sisa Tagihan</div>
                        <div class="text-xl font-bold text-red-800">Rp {{ number_format($remainingAmount, 0, ',', '.') }}</div>
                    </div>
                </div>

                <!-- Progress Bar -->
                <div class="w-full bg-gray-100 rounded-full h-4 mb-8 overflow-hidden border border-gray-200">
                    <div class="bg-gold-gradient h-full rounded-full transition-all duration-1000 relative shadow-[0_0_10px_rgba(212,175,55,0.3)]" style="width: {{ $progressPercentage }}%">
                        <div class="absolute inset-0 bg-white/20 animate-[shimmer_2s_infinite]"></div>
                    </div>
                </div>
                
                <div class="space-y-4">
                    @foreach($plan->installments as $installment)
                    @php
                        $pending = $installment->payments->whereIn('status', ['PENDING', 'NEEDS_REVIEW', 'AUTO_VERIFIED'])->first();
                        $displayStatus = $installment->status;
                        if($pending && $installment->status != 'PAID') {
                            $displayStatus = 'VERIFIKASI';
                        }
                    @endphp
                    <div class="flex items-center justify-between p-4 rounded-xl border {{ $installment->status == 'PAID' ? 'border-green-500/30 bg-green-500/5' : ($installment->status == 'VERIFIED' ? 'border-blue-500/30 bg-blue-500/5' : ($displayStatus == 'VERIFIKASI' ? 'border-yellow-500/30 bg-yellow-500/5' : 'border-gold/10 bg-surface hover:border-gold/30 transition-colors')) }} group">
                        <div class="flex items-center gap-4">
                            <div class="w-10 h-10 rounded-full flex items-center justify-center {{ $installment->status == 'PAID' ? 'bg-green-500/10 text-green-500' : 'bg-surface border border-gold/20 text-text-muted group-hover:border-gold group-hover:text-gold transition-colors' }}">
                                <span class="font-bold text-sm">{{ $loop->iteration }}</span>
                            </div>
                            <div>
                                <div class="font-bold text-primary">{{ $installment->name }}</div>
                                <div class="text-xs text-text-muted">Jatuh tempo: {{ \Carbon\Carbon::parse($installment->due_date)->format('d M Y') }}</div>
                                @if(str_contains($plan->category, 'MUNAOSAH'))
                                    <div class="text-[10px] bg-purple-100 text-purple-700 px-1.5 py-0.5 rounded inline-block mt-1 font-bold">MUNAQOSAH</div>
                                @endif
                            </div>
                        </div>
                        <div class="text-right">
                            <div class="font-bold text-primary">Rp {{ number_format($installment->amount, 0, ',', '.') }}</div>
                            <span class="text-xs font-bold px-2 py-0.5 rounded {{ 
                                $installment->status == 'PAID' ? 'bg-green-500/10 text-green-500' : 
                                ($installment->status == 'VERIFIED' ? 'bg-blue-500/10 text-blue-500' : 
                                ($displayStatus == 'VERIFIKASI' ? 'bg-yellow-500/10 text-yellow-500' : 
                                ($installment->status == 'PENDING' ? 'bg-yellow-500/10 text-yellow-500' : 'bg-gray-100 text-gray-500'))) 
                            }}">
                                {{ $displayStatus }}
                            </span>
                        </div>
                    </div>
                    @endforeach
                </div>
            </div>
        </div>

        <!-- Right Column: Widgets -->
        <div class="space-y-8">
            
            <!-- ML Risk Meter Widget -->
            <div class="card-luxury p-6 relative overflow-hidden">
                <div class="absolute top-0 right-0 w-24 h-24 bg-gold/5 rounded-full blur-xl"></div>
                
                <div class="flex items-center gap-3 mb-4">
                    <div class="p-2 bg-primary/5 text-primary rounded-lg border border-primary/10">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                    </div>
                    <h3 class="font-serif text-lg font-bold text-primary">Prediksi Kelancaran</h3>
                </div>

                <div class="flex flex-col items-center justify-center py-4">
                    <div class="relative w-32 h-16 overflow-hidden mb-2">
                        <!-- Semi circle gauge background -->
                        <div class="w-32 h-32 rounded-full border-[12px] border-gray-100 box-border"></div>
                        <!-- Needle (Static for now, pointing to Low Risk/Green) -->
                        <div class="absolute top-0 left-0 w-32 h-32 rounded-full border-[12px] border-transparent border-t-green-500 border-r-transparent border-b-transparent border-l-transparent transform -rotate-45 box-border"></div>
                    </div>
                    <div class="text-center">
                        <div class="text-2xl font-bold text-green-500">Resiko Rendah</div>
                        <p class="text-xs text-text-muted mt-1 text-center leading-relaxed">
                            Berdasarkan pola pembayaran sebelumnya, Anda diprediksi lancar membayar semester ini.
                        </p>
                    </div>
                </div>
            </div>

            <!-- Broadcast Board -->
            <div class="sidebar-gradient rounded-2xl p-6 text-white relative overflow-hidden border border-gold/20 shadow-xl">
                <div class="absolute top-0 right-0 w-32 h-32 bg-gold/10 rounded-full blur-2xl"></div>
                
                <h3 class="font-serif text-lg font-bold mb-4 flex items-center gap-2 text-white">
                    <svg class="w-5 h-5 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"></path></svg>
                    Papan Pengumuman
                </h3>

                <div class="space-y-4">
                    @foreach($broadcasts as $news)
                    <div class="pb-4 border-b border-white/10 last:border-0 last:pb-0">
                        <div class="text-xs text-gold font-bold mb-1">{{ \Carbon\Carbon::parse($news['date'])->format('d M Y') }}</div>
                        <h4 class="font-bold text-sm mb-1 text-white">{{ $news['title'] }}</h4>
                        <p class="text-white/60 text-xs leading-relaxed">{{ $news['content'] }}</p>
                    </div>
                    @endforeach
                </div>
            </div>

        </div>
    </div>
@endif

<!-- Payment Upload Modal -->
<div id="upload-modal" class="hidden fixed inset-0 z-50 overflow-y-auto" aria-labelledby="modal-title" role="dialog" aria-modal="true">
    <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
        <div class="fixed inset-0 bg-primary/80 transition-opacity backdrop-blur-sm" aria-hidden="true" onclick="document.getElementById('upload-modal').classList.add('hidden')"></div>
        <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
        <div class="inline-block align-bottom card-luxury rounded-2xl text-left overflow-hidden shadow-2xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full border border-gold/20 relative">
             <!-- Background Decoration -->
             <div class="absolute top-0 right-0 w-64 h-64 bg-gold/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 pointer-events-none"></div>

            <div class="px-4 pt-5 pb-4 sm:p-6 sm:pb-4 relative z-10">
                <div class="sm:flex sm:items-start">
                    <div class="mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-gold/10 sm:mx-0 sm:h-10 sm:w-10 border border-gold/20">
                        <svg class="h-6 w-6 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path></svg>
                    </div>
                    <div class="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left w-full">
                        <h3 class="text-lg leading-6 font-bold text-primary font-serif" id="modal-title">Upload Bukti Pembayaran</h3>
                        <div class="mt-2">
                            <p class="text-sm text-text-muted mb-4">Pastikan foto bukti transfer terlihat jelas dan nominal sesuai tagihan.</p>
                            
                            <form action="{{ url('/mahasiswa/payments') }}" method="POST" enctype="multipart/form-data" class="space-y-4">
                                @csrf
                                @if(isset($nextInstallment))
                                    <input type="hidden" name="installment_id" value="{{ $nextInstallment->id }}">
                                    <input type="hidden" name="amount" value="{{ $nextInstallment->amount }}">
                                @endif

                                @if ($errors->any())
                                    <div class="mb-4 bg-red-50 border-l-4 border-red-500 p-4 rounded">
                                        <div class="flex">
                                            <div class="flex-shrink-0">
                                                <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                                                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                                                </svg>
                                            </div>
                                            <div class="ml-3">
                                                <p class="text-sm text-red-700 font-bold">
                                                    Gagal mengupload bukti pembayaran:
                                                </p>
                                                <ul class="mt-1 text-sm text-red-700 list-disc list-inside">
                                                    @foreach ($errors->all() as $error)
                                                        <li>{{ $error }}</li>
                                                    @endforeach
                                                </ul>
                                            </div>
                                        </div>
                                    </div>
                                @endif

                                <div class="border-2 border-dashed border-gold/30 rounded-xl p-6 text-center hover:border-gold hover:bg-gold/5 transition-all cursor-pointer bg-surface relative group @error('file') border-red-500 bg-red-50 @enderror" onclick="document.getElementById('file-upload').click()">
                                    <svg class="mx-auto h-12 w-12 text-gold/50 group-hover:text-gold transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
                                    <p class="mt-1 text-sm text-primary font-medium">Klik untuk upload atau drag & drop</p>
                                    <p class="text-xs text-text-muted">PNG, JPG, PDF up to 2MB</p>
                                    <p id="filename-display" class="mt-2 text-sm font-bold text-gold hidden"></p>
                                    <input id="file-upload" type="file" name="file" class="hidden" onchange="document.getElementById('filename-display').innerText = this.files[0].name; document.getElementById('filename-display').classList.remove('hidden');">
                                </div>
                                @error('file')
                                    <p class="mt-1 text-sm text-red-600 font-bold">{{ $message }}</p>
                                @enderror
                                <button type="submit" class="w-full btn-luxury py-3 rounded-xl text-white font-bold shadow-lg transform hover:-translate-y-1 transition-all">
                                    Kirim Bukti Pembayaran
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
            <div class="bg-surface/50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse border-t border-gold/10 relative z-10">
                <button type="button" class="mt-3 w-full inline-flex justify-center rounded-xl border border-gold/30 shadow-sm px-4 py-2 bg-surface text-base font-medium text-text-muted hover:text-primary hover:border-gold hover:bg-gold/5 focus:outline-none transition-all sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" onclick="document.getElementById('upload-modal').classList.add('hidden')">
                    Batal
                </button>
            </div>
        </div>
    </div>
</div>

@if($errors->any())
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            document.getElementById('upload-modal').classList.remove('hidden');
        });
    </script>
@endif

@endsection