@extends('layouts.student')

@section('content')
<div class="mb-8">
    <h1 class="text-3xl font-serif font-bold text-primary mb-2">Riwayat <span class="text-gold-gradient">Pembayaran</span></h1>
    <p class="text-slate-500">Lihat semua transaksi pembayaran Anda.</p>
</div>

<!-- Summary Cards -->
<div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
    <div class="p-6 rounded-xl bg-green-50 border border-green-100 flex justify-between items-center shadow-sm">
        <div>
            <div class="text-xs font-bold text-green-600 uppercase tracking-wider mb-1">Total Terbayar</div>
            <div class="text-2xl font-bold text-green-800">Rp {{ number_format($paidAmount ?? 0, 0, ',', '.') }}</div>
        </div>
        <div class="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center text-green-600">
             <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
        </div>
    </div>
    <div class="p-6 rounded-xl bg-red-50 border border-red-100 flex justify-between items-center shadow-sm">
        <div>
            <div class="text-xs font-bold text-red-600 uppercase tracking-wider mb-1">Sisa Tagihan</div>
            <div class="text-2xl font-bold text-red-800">Rp {{ number_format($remainingAmount ?? 0, 0, ',', '.') }}</div>
        </div>
        <div class="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center text-red-600">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
        </div>
    </div>
</div>

<div class="card-luxury p-0 overflow-hidden">
    <div class="overflow-x-auto">
        <table class="w-full text-left">
            <thead>
                <tr class="bg-primary/5 border-b border-primary/10 text-primary text-xs uppercase tracking-wider font-bold">
                    <th class="px-6 py-4 font-serif">Tanggal</th>
                    <th class="px-6 py-4 font-serif">Pembayaran</th>
                    <th class="px-6 py-4 font-serif">Jumlah</th>
                    <th class="px-6 py-4 font-serif">Status</th>
                    <th class="px-6 py-4 font-serif text-right">Aksi</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @forelse($payments as $payment)
                <tr class="hover:bg-slate-50 transition-colors group">
                    <td class="px-6 py-4">
                        <div class="font-bold text-primary">{{ $payment->created_at->format('d M Y') }}</div>
                        <div class="text-xs text-slate-500">{{ $payment->created_at->format('H:i') }} WIB</div>
                    </td>
                    <td class="px-6 py-4">
                        <div class="font-medium text-slate-700">
                            Cicilan ke-{{ $payment->installment ? $payment->installment->installment_no : '-' }}
                            @if($payment->paymentPlan && str_contains($payment->paymentPlan->category, 'MUNAOSAH'))
                                <span class="text-[10px] bg-purple-100 text-purple-700 px-1.5 py-0.5 rounded inline-block ml-1 font-bold">MUNAQOSAH</span>
                            @endif
                        </div>
                    </td>
                    <td class="px-6 py-4">
                        <span class="font-bold text-primary font-mono">Rp {{ number_format($payment->amount, 0, ',', '.') }}</span>
                    </td>
                    <td class="px-6 py-4">
                        @if($payment->status === 'VERIFIED')
                            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 border border-green-200">
                                <span class="w-1.5 h-1.5 rounded-full bg-green-500 mr-1.5"></span>
                                VERIFIED
                            </span>
                        @elseif($payment->status === 'REJECTED')
                            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800 border border-red-200">
                                <span class="w-1.5 h-1.5 rounded-full bg-red-500 mr-1.5"></span>
                                REJECTED
                            </span>
                        @else
                            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800 border border-yellow-200">
                                <span class="w-1.5 h-1.5 rounded-full bg-yellow-500 mr-1.5"></span>
                                PENDING
                            </span>
                        @endif
                    </td>
                    <td class="px-6 py-4 text-right">
                        @if($payment->status === 'VERIFIED')
                        <a href="{{ route('payments.receipt.download', $payment) }}" 
                           class="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-bold bg-white border border-gray-200 text-slate-600 hover:text-primary hover:border-gold hover:bg-gold/5 transition-all shadow-sm">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                            Kuitansi
                        </a>
                        @else
                        <span class="text-xs text-slate-400 italic">Menunggu Verifikasi</span>
                        @endif
                    </td>
                </tr>
                @empty
                <tr>
                    <td colspan="5" class="px-6 py-12 text-center text-slate-500">
                        <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <svg class="w-8 h-8 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                        </div>
                        <p>Belum ada riwayat pembayaran.</p>
                    </td>
                </tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>
@endsection
