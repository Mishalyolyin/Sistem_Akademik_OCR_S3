@extends('layouts.admin')

@section('content')
<div class="mb-8">
    <h1 class="text-2xl font-bold text-gray-800">Edit Data Mahasiswa</h1>
    <p class="text-gray-500 text-sm mt-1">Perbarui informasi dasar mahasiswa.</p>
</div>

<div class="max-w-3xl bg-white rounded-xl border border-gray-100 shadow-sm p-8">
    <form action="{{ route('admin.students.update', $student->id) }}" method="POST">
        @csrf
        @method('PUT')

        <div class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">NIM</label>
                    <input type="text" name="nim" value="{{ old('nim', $student->nim) }}" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                    @error('nim') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Nama Lengkap</label>
                    <input type="text" name="name" value="{{ old('name', $student->name) }}" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                    @error('name') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
                </div>
            </div>

            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Nomor HP (WhatsApp)</label>
                <input type="text" name="phone" value="{{ old('phone', $student->phone) }}" placeholder="08..." class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                @error('phone') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
            </div>

            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Kelas</label>
                <input type="text" name="class" value="{{ old('class', $student->class) }}" placeholder="Contoh: TI-2023-A" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                @error('class') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Program</label>
                    <select name="program_type" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                        <option value="REGULER" {{ $student->program_type == 'REGULER' ? 'selected' : '' }}>Reguler</option>
                        <option value="RPL" {{ $student->program_type == 'RPL' ? 'selected' : '' }}>RPL</option>
                    </select>
                    @error('program_type') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Semester Mulai</label>
                    <select name="start_term" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                        <option value="GASAL" {{ $student->start_term == 'GASAL' ? 'selected' : '' }}>Gasal (Ganjil)</option>
                        <option value="GENAP" {{ $student->start_term == 'GENAP' ? 'selected' : '' }}>Genap</option>
                    </select>
                    @error('start_term') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
                </div>
            </div>
        </div>

        <div class="mt-8 flex items-center justify-end gap-4">
            <a href="{{ route('admin.students.reguler') }}" class="px-6 py-2 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 font-medium transition-all">
                Batal
            </a>
            <button type="submit" class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 shadow-lg shadow-blue-600/20 font-bold transition-all">
                Simpan Perubahan
            </button>
        </div>
    </form>
</div>
@endsection
