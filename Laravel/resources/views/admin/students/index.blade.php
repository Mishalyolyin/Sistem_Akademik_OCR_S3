@extends('layouts.admin')

@section('content')
<div class="mb-8 flex flex-col md:flex-row md:justify-between md:items-end gap-4">
    <div>
        <h1 class="text-3xl font-bold text-gray-800 mb-2">{{ $pageTitle ?? 'Data Mahasiswa' }}</h1>
        <p class="text-gray-500 text-sm">Kelola data induk mahasiswa secara terpusat.</p>
    </div>
    <div class="flex gap-3">
        <a href="{{ route('admin.students.import.form') }}" class="bg-blue-600 hover:bg-blue-700 px-4 py-2 rounded-lg text-white font-bold text-sm shadow-sm transition-colors flex items-center gap-2">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
            Import Mahasiswa
        </a>
    </div>
</div>

<div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
    <!-- Search & Filter -->
    <form action="{{ url()->current() }}" method="GET" class="mb-6 flex flex-col md:flex-row gap-4">
        <div class="flex-1">
            <input type="text" name="search" value="{{ request('search') }}" placeholder="Cari Nama atau NIM..." class="w-full px-4 py-2 rounded-lg border border-gray-200 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all">
        </div>
        @unless(isset($programType))
        <div class="w-full md:w-48">
            <select name="program_type" class="w-full px-4 py-2 rounded-lg border border-gray-200 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all" onchange="this.form.submit()">
                <option value="">Semua Program</option>
                <option value="REGULER" {{ request('program_type') == 'REGULER' ? 'selected' : '' }}>Reguler</option>
                <option value="RPL" {{ request('program_type') == 'RPL' ? 'selected' : '' }}>RPL</option>
            </select>
        </div>
        @endunless
        <button type="submit" class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium">
            Cari
        </button>
    </form>

    <!-- Table -->
    <div class="overflow-x-auto">
        <table class="w-full min-w-[800px] text-left border-collapse">
            <thead>
                <tr class="border-b border-gray-200 text-xs uppercase text-gray-500 font-semibold tracking-wider">
                    <th class="py-3 px-4">NIM</th>
                    <th class="py-3 px-4">Nama Mahasiswa</th>
                    <th class="py-3 px-4">Program</th>
                    <th class="py-3 px-4">Kelas</th>
                    <th class="py-3 px-4">Mulai Masuk</th>
                    <th class="py-3 px-4">No. HP</th>
                    <th class="py-3 px-4">Status</th>
                    <th class="py-3 px-4 text-right">Aksi</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @forelse($students as $student)
                <tr class="hover:bg-gray-50 transition-colors group">
                    <td class="py-3 px-4 font-mono text-gray-700">{{ $student->nim }}</td>
                    <td class="py-3 px-4">
                        <div class="font-medium text-gray-900">{{ $student->name }}</div>
                        <div class="text-xs text-gray-500">{{ $student->email ?? '-' }}</div>
                    </td>
                    <td class="py-3 px-4">
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-bold border {{ $student->program_type == 'RPL' ? 'bg-blue-50 text-blue-700 border-blue-100' : 'bg-emerald-50 text-emerald-700 border-emerald-100' }}">
                            {{ $student->program_type }}
                        </span>
                    </td>
                    <td class="py-3 px-4 font-semibold text-gray-700">{{ $student->class ?? '-' }}</td>
                    <td class="py-3 px-4 text-sm text-gray-500">{{ $student->start_term }}</td>
                    <td class="py-3 px-4 text-sm text-gray-500">{{ $student->phone ?? '-' }}</td>
                    <td class="py-3 px-4">
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-100 flex items-center w-fit gap-1.5">
                            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                            Aktif
                        </span>
                    </td>
                    <td class="py-3 px-4 text-right flex justify-end gap-2">
                        <form action="{{ route('admin.students.reset_password', $student->id) }}" method="POST" onsubmit="return confirm('Apakah Anda yakin ingin mereset password mahasiswa ini menjadi NIM?');">
                            @csrf
                            <button type="submit" class="text-orange-400 hover:text-orange-600 hover:bg-orange-50 p-2 rounded-lg transition-all" title="Reset Password ke NIM">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"></path></svg>
                            </button>
                        </form>
                        <a href="{{ route('admin.students.edit', $student->id) }}" class="text-gray-400 hover:text-blue-600 hover:bg-blue-50 p-2 rounded-lg transition-all" title="Edit Mahasiswa">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>
                        </a>
                    </td>
                </tr>
                @empty
                <tr>
                    <td colspan="7" class="py-12 text-center text-text-muted">
                        <div class="flex flex-col items-center justify-center">
                            <svg class="w-12 h-12 text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path></svg>
                            <p>Belum ada data mahasiswa.</p>
                        </div>
                    </td>
                </tr>
                @endforelse
            </tbody>
        </table>
    </div>

    <!-- Pagination -->
    <div class="mt-6">
        {{ $students->links() }}
    </div>
</div>
@endsection
