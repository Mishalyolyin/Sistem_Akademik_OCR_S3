@extends('layouts.admin')

@section('content')
<div class="mb-8 flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
    <div>
        <h1 class="text-2xl font-bold text-gray-800">Manajemen Kelas</h1>
        <p class="text-gray-500 text-sm mt-1">Kelola data kelas mahasiswa (Reguler & RPL).</p>
    </div>
    <button onclick="document.getElementById('createModal').showModal()" class="bg-blue-600 text-white px-5 py-2.5 rounded-lg hover:bg-blue-700 transition-colors font-medium shadow-sm flex items-center gap-2">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path></svg>
        Tambah Kelas
    </button>
</div>

@if(session('success'))
<div class="mb-6 bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-lg flex items-center gap-2" role="alert">
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
    <div>
        <strong class="font-bold">Sukses!</strong>
        <span class="block sm:inline">{{ session('success') }}</span>
    </div>
</div>
@endif

@if($errors->any())
<div class="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start gap-2" role="alert">
    <svg class="w-5 h-5 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
    <div>
        <strong class="font-bold">Error!</strong>
        <ul class="list-disc pl-5 mt-1 text-sm">
            @foreach ($errors->all() as $error)
                <li>{{ $error }}</li>
            @endforeach
        </ul>
    </div>
</div>
@endif

<div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
    <table class="w-full text-left border-collapse">
        <thead>
            <tr class="bg-gray-50 border-b border-gray-100 text-xs uppercase tracking-wider text-gray-500">
                <th class="p-4 font-bold">Nama Kelas</th>
                <th class="p-4 font-bold">Program</th>
                <th class="p-4 font-bold">Angkatan</th>
                <th class="p-4 font-bold text-right">Aksi</th>
            </tr>
        </thead>
        <tbody class="divide-y divide-gray-50">
            @forelse($classes as $class)
            <tr class="hover:bg-gray-50 transition-colors">
                <td class="p-4 font-medium text-gray-900">{{ $class->name }}</td>
                <td class="p-4">
                    <span class="px-2.5 py-0.5 rounded-full text-xs font-medium {{ $class->program_type == 'Reguler' ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-blue-50 text-blue-700 border border-blue-100' }}">
                        {{ $class->program_type }}
                    </span>
                </td>
                <td class="p-4 text-gray-600 font-mono text-sm">{{ $class->generation ?? '-' }}</td>
                <td class="p-4 text-right">
                    <div class="flex justify-end gap-2">
                        <button onclick="openEditModal({{ $class }})" class="text-blue-600 hover:text-blue-800 font-medium text-xs bg-blue-50 px-3 py-1.5 rounded-lg border border-blue-100 hover:bg-blue-100 transition-colors">Edit</button>
                        <form action="{{ route('admin.classes.destroy', $class->id) }}" method="POST" onsubmit="return confirm('Yakin ingin menghapus kelas ini?');">
                            @csrf
                            @method('DELETE')
                            <button type="submit" class="text-red-600 hover:text-red-800 font-medium text-xs bg-red-50 px-3 py-1.5 rounded-lg border border-red-100 hover:bg-red-100 transition-colors">Hapus</button>
                        </form>
                    </div>
                </td>
            </tr>
            @empty
            <tr>
                <td colspan="4" class="p-12 text-center text-gray-400 italic bg-gray-50">
                    <div class="flex flex-col items-center">
                        <svg class="w-10 h-10 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
                        <span>Belum ada data kelas.</span>
                    </div>
                </td>
            </tr>
            @endforelse
        </tbody>
    </table>
    <div class="p-4 border-t border-gray-100">
        {{ $classes->links() }}
    </div>
</div>

<!-- Create Modal -->
<dialog id="createModal" class="modal rounded-xl shadow-xl p-0 w-full max-w-md backdrop:bg-gray-900/50">
    <div class="bg-white p-6 rounded-xl">
        <h3 class="font-bold text-lg mb-4 text-gray-800">Tambah Kelas Baru</h3>
        <form action="{{ route('admin.classes.store') }}" method="POST">
            @csrf
            <div class="mb-4">
                <label class="block text-xs font-medium text-gray-600 mb-1">Nama Kelas</label>
                <input type="text" name="name" class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5" placeholder="Contoh: TI-2023-A" required>
            </div>
            <div class="mb-4">
                <label class="block text-xs font-medium text-gray-600 mb-1">Program</label>
                <select name="program_type" class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5" required>
                    <option value="Reguler">Reguler</option>
                    <option value="RPL">RPL</option>
                </select>
            </div>
            <div class="mb-6">
                <label class="block text-xs font-medium text-gray-600 mb-1">Angkatan (Opsional)</label>
                <input type="text" name="generation" class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5" placeholder="2023">
            </div>
            <div class="flex justify-end gap-3 pt-2 border-t border-gray-100">
                <button type="button" onclick="document.getElementById('createModal').close()" class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg text-sm font-medium transition-colors">Batal</button>
                <button type="submit" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm font-medium transition-colors shadow-sm">Simpan</button>
            </div>
        </form>
    </div>
</dialog>

<!-- Edit Modal -->
<dialog id="editModal" class="modal rounded-xl shadow-xl p-0 w-full max-w-md backdrop:bg-gray-900/50">
    <div class="bg-white p-6 rounded-xl">
        <h3 class="font-bold text-lg mb-4 text-gray-800">Edit Kelas</h3>
        <form id="editForm" method="POST">
            @csrf
            @method('PUT')
            <div class="mb-4">
                <label class="block text-xs font-medium text-gray-600 mb-1">Nama Kelas</label>
                <input type="text" id="edit_name" name="name" class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5" required>
            </div>
            <div class="mb-4">
                <label class="block text-xs font-medium text-gray-600 mb-1">Program</label>
                <select id="edit_program" name="program_type" class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5" required>
                    <option value="Reguler">Reguler</option>
                    <option value="RPL">RPL</option>
                </select>
            </div>
            <div class="mb-6">
                <label class="block text-xs font-medium text-gray-600 mb-1">Angkatan</label>
                <input type="text" id="edit_generation" name="generation" class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5">
            </div>
            <div class="flex justify-end gap-3 pt-2 border-t border-gray-100">
                <button type="button" onclick="document.getElementById('editModal').close()" class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg text-sm font-medium transition-colors">Batal</button>
                <button type="submit" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm font-medium transition-colors shadow-sm">Simpan Perubahan</button>
            </div>
        </form>
    </div>
</dialog>

<script>
    function openEditModal(data) {
        document.getElementById('edit_name').value = data.name;
        document.getElementById('edit_program').value = data.program_type;
        document.getElementById('edit_generation').value = data.generation;
        document.getElementById('editForm').action = "/admin/classes/" + data.id;
        document.getElementById('editModal').showModal();
    }
</script>
@endsection
