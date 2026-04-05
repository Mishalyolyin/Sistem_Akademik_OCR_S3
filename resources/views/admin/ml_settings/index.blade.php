@extends('layouts.admin')

@section('content')
<div class="mb-8">
    <h1 class="text-2xl font-bold text-gray-800">Pengaturan Machine Learning</h1>
    <p class="text-gray-500 text-sm mt-1">Konfigurasi parameter OCR dan logika verifikasi otomatis.</p>
</div>

@if(session('success'))
<div class="mb-6 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-center gap-3">
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
    {{ session('success') }}
</div>
@endif

<form action="{{ route('admin.ml_settings.store') }}" method="POST">
    @csrf
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <!-- Dataset & Model Status -->
        <div class="lg:col-span-2 bg-gradient-to-r from-purple-900 to-indigo-900 rounded-xl border border-purple-800 shadow-xl p-6 text-white relative overflow-hidden">
            <div class="absolute top-0 right-0 p-8 opacity-10">
                <svg class="w-64 h-64" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z"/></svg>
            </div>
            
            <div class="relative z-10">
                <div class="flex items-center gap-4 mb-8">
                    <div class="w-12 h-12 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center">
                        <svg class="w-7 h-7 text-purple-200" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.384-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"></path></svg>
                    </div>
                    <div>
                        <h2 class="text-xl font-bold">Status Pembelajaran Mesin (AI)</h2>
                        <p class="text-purple-200 text-sm">Monitor data yang dipelajari oleh sistem dari verifikasi manual Anda.</p>
                    </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                    <div class="bg-white/10 backdrop-blur-md rounded-lg p-4 border border-white/10">
                        <span class="block text-purple-200 text-xs font-medium uppercase tracking-wider mb-1">Total Sampel</span>
                        <span class="text-3xl font-bold">{{ $totalSamples ?? 0 }}</span>
                        <span class="text-xs text-purple-300 block mt-1">Data terkumpul</span>
                    </div>
                    <div class="bg-white/10 backdrop-blur-md rounded-lg p-4 border border-white/10">
                        <span class="block text-emerald-300 text-xs font-medium uppercase tracking-wider mb-1">Positif (Valid)</span>
                        <span class="text-3xl font-bold text-emerald-400">{{ $verifiedCount ?? 0 }}</span>
                        <span class="text-xs text-purple-300 block mt-1">Pembayaran Sah</span>
                    </div>
                    <div class="bg-white/10 backdrop-blur-md rounded-lg p-4 border border-white/10">
                        <span class="block text-red-300 text-xs font-medium uppercase tracking-wider mb-1">Negatif (Invalid)</span>
                        <span class="text-3xl font-bold text-red-400">{{ $rejectedCount ?? 0 }}</span>
                        <span class="text-xs text-purple-300 block mt-1">Ditolak / Bukan Bukti</span>
                    </div>
                    <div class="bg-white/10 backdrop-blur-md rounded-lg p-4 border border-white/10">
                        <span class="block text-blue-300 text-xs font-medium uppercase tracking-wider mb-1">Diproses AI</span>
                        <span class="text-3xl font-bold text-blue-400">{{ $totalProcessed ?? 0 }}</span>
                        <span class="text-xs text-purple-300 block mt-1">Total pembacaan OCR</span>
                    </div>
                </div>

                <div class="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white/5 rounded-lg p-4 border border-white/10">
                    <div>
                        <h4 class="font-semibold text-purple-100">Dataset Training</h4>
                        <p class="text-sm text-purple-300">Unduh dataset ini untuk melatih ulang model AI agar lebih akurat.</p>
                    </div>
                    <a href="{{ route('admin.ml_settings.download_dataset') }}" class="px-5 py-2.5 bg-white text-purple-900 font-bold rounded-lg hover:bg-purple-50 transition-colors flex items-center gap-2 shadow-lg">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                        Download Dataset (.zip)
                    </a>
                </div>
            </div>
        </div>

        <!-- OCR Configuration -->
        <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
            <div class="flex items-center gap-3 mb-6">
                <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center text-blue-600">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                </div>
                <div>
                    <h2 class="text-lg font-bold text-gray-800">Konfigurasi OCR</h2>
                    <p class="text-gray-500 text-xs">Parameter pembacaan gambar</p>
                </div>
            </div>

            <div class="space-y-5">
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Confidence Threshold (%)</label>
                    <input type="number" name="ocr_confidence_threshold" value="{{ $settings['ocr_confidence_threshold']->value ?? 80 }}" min="0" max="100" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                    <p class="text-xs text-gray-400 mt-1">Nilai keyakinan minimum agar pembayaran terverifikasi otomatis.</p>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Validasi Tanggal (Hari)</label>
                    <input type="number" name="ocr_date_validation_days" value="{{ $settings['ocr_date_validation_days']->value ?? 3 }}" min="0" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                    <p class="text-xs text-gray-400 mt-1">Batas toleransi selisih tanggal upload dengan tanggal di bukti transfer.</p>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Blacklist Keywords</label>
                    <textarea name="ocr_blacklist_keywords" rows="3" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">{{ $settings['ocr_blacklist_keywords']->value ?? 'gagal,pending,error,declined' }}</textarea>
                    <p class="text-xs text-gray-400 mt-1">Kata kunci yang menandakan bukti transfer tidak valid (pisahkan dengan koma).</p>
                </div>
            </div>
        </div>

        <!-- Payment Configuration -->
        <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
            <div class="flex items-center gap-3 mb-6">
                <div class="w-10 h-10 rounded-lg bg-green-50 flex items-center justify-center text-green-600">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                </div>
                <div>
                    <h2 class="text-lg font-bold text-gray-800">Data Pembayaran</h2>
                    <p class="text-gray-500 text-xs">Informasi rekening dan toleransi</p>
                </div>
            </div>

            <div class="space-y-5">
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Toleransi Nominal (Rp)</label>
                    <input type="number" name="payment_tolerance_amount" value="{{ $settings['payment_tolerance_amount']->value ?? 0 }}" min="0" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none transition-all">
                    <p class="text-xs text-gray-400 mt-1">Batas toleransi selisih nominal pembayaran (misal untuk kode unik).</p>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">No. Rekening Reguler</label>
                    <input type="text" name="bank_account_reguler" value="{{ $settings['bank_account_reguler']->value ?? '' }}" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none transition-all">
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">No. Rekening RPL</label>
                    <input type="text" name="bank_account_rpl" value="{{ $settings['bank_account_rpl']->value ?? '' }}" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none transition-all">
                </div>
            </div>
        </div>
    </div>

    <div class="mt-8 flex justify-end">
        <button type="submit" class="bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-8 rounded-xl shadow-lg shadow-blue-600/20 transition-all transform hover:-translate-y-1">
            Simpan Pengaturan
        </button>
    </div>
</form>
@endsection
