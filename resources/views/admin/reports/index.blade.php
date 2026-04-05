@extends('layouts.admin')

@section('content')
<div class="mb-8">
    <h1 class="text-2xl font-bold text-gray-800">Laporan & Export</h1>
    <p class="text-gray-500 text-sm mt-1">Lihat dan unduh laporan keuangan mahasiswa.</p>
</div>

<div class="grid grid-cols-1 md:grid-cols-2 gap-8">
    <!-- Export Excel Card -->
    <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-8 relative overflow-hidden">
        <div class="absolute top-0 left-0 w-1 h-full bg-emerald-500"></div>
        <div class="flex items-start gap-5 mb-8">
            <div class="w-12 h-12 bg-emerald-50 rounded-xl flex items-center justify-center flex-shrink-0 text-emerald-600 border border-emerald-100">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
            </div>
            <div>
                <h3 class="text-lg font-bold text-gray-800 mb-1">Export Laporan Pembayaran</h3>
                <p class="text-sm text-gray-500 leading-relaxed">Unduh data pembayaran dalam format Excel (.xlsx) untuk keperluan arsip atau analisis lebih lanjut.</p>
            </div>
        </div>

        <form action="{{ route('admin.reports.export') }}" method="GET" class="space-y-5">
            <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">Filter Program Studi</label>
                <div class="relative">
                    <select name="program_type" class="w-full rounded-lg border-gray-300 focus:border-emerald-500 focus:ring-emerald-500 text-sm bg-gray-50 py-2.5 pl-10 appearance-none">
                        <option value="">Semua Program</option>
                        <option value="REGULER">Reguler</option>
                        <option value="RPL">RPL</option>
                    </select>
                    <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
                    </div>
                </div>
            </div>

            <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">Filter Kelas</label>
                <div class="relative">
                    <input type="text" name="class" placeholder="Contoh: TI-1A" class="w-full rounded-lg border-gray-300 focus:border-emerald-500 focus:ring-emerald-500 text-sm bg-gray-50 py-2.5 pl-10">
                    <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
                    </div>
                </div>
            </div>

            <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">Filter Status</label>
                <div class="relative">
                    <select name="status" class="w-full rounded-lg border-gray-300 focus:border-emerald-500 focus:ring-emerald-500 text-sm bg-gray-50 py-2.5 pl-10 appearance-none">
                        <option value="">Semua Status</option>
                        <option value="VERIFIED">Lunas (Verified)</option>
                        <option value="PENDING">Menunggu Verifikasi</option>
                        <option value="REJECTED">Ditolak</option>
                    </select>
                    <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
            </div>

            <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">Format Laporan</label>
                <div class="flex gap-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
                    <label class="inline-flex items-center cursor-pointer group">
                        <input type="radio" name="format" value="transaction" class="form-radio text-emerald-600 focus:ring-emerald-500" checked>
                        <span class="ml-2 text-sm text-gray-700 group-hover:text-emerald-700 transition-colors">Daftar Transaksi</span>
                    </label>
                    <label class="inline-flex items-center cursor-pointer group">
                        <input type="radio" name="format" value="ledger" class="form-radio text-emerald-600 focus:ring-emerald-500">
                        <span class="ml-2 text-sm text-gray-700 group-hover:text-emerald-700 transition-colors">Ledger (Format Gambar)</span>
                    </label>
                </div>
            </div>

            <div class="pt-2">
                <button type="submit" class="w-full flex justify-center items-center gap-2 px-4 py-3 border border-transparent rounded-xl shadow-sm text-sm font-bold text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                    Download Excel
                </button>
            </div>
        </form>
    </div>

    <!-- Info Card -->
    <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-8 flex flex-col justify-center text-center h-full relative overflow-hidden">
        <div class="absolute top-0 right-0 w-1 h-full bg-blue-500"></div>
        <div class="w-20 h-20 bg-blue-50 rounded-full flex items-center justify-center mx-auto mb-6 text-blue-600 border border-blue-100 shadow-sm">
            <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
        </div>
        <h3 class="text-xl font-bold text-gray-800 mb-3">Informasi Laporan</h3>
        <p class="text-gray-500 text-sm leading-relaxed max-w-sm mx-auto mb-8">
            Laporan yang diunduh berisi data lengkap pembayaran mahasiswa. Gunakan filter di samping untuk menyesuaikan data yang Anda butuhkan.
        </p>
        <div class="grid grid-cols-2 gap-4 max-w-sm mx-auto w-full">
            <div class="bg-gray-50 p-3 rounded-lg border border-gray-100">
                <span class="block text-xs text-gray-400 uppercase font-bold tracking-wider mb-1">Identitas</span>
                <span class="text-gray-800 font-semibold text-sm">NIM & Nama</span>
            </div>
            <div class="bg-gray-50 p-3 rounded-lg border border-gray-100">
                <span class="block text-xs text-gray-400 uppercase font-bold tracking-wider mb-1">Keuangan</span>
                <span class="text-gray-800 font-semibold text-sm">Jumlah & Tanggal</span>
            </div>
            <div class="bg-gray-50 p-3 rounded-lg border border-gray-100">
                <span class="block text-xs text-gray-400 uppercase font-bold tracking-wider mb-1">Status</span>
                <span class="text-gray-800 font-semibold text-sm">Verified/Pending</span>
            </div>
            <div class="bg-gray-50 p-3 rounded-lg border border-gray-100">
                <span class="block text-xs text-gray-400 uppercase font-bold tracking-wider mb-1">Periode</span>
                <span class="text-gray-800 font-semibold text-sm">Semester & Tahun</span>
            </div>
        </div>
    </div>
</div>
@endsection
