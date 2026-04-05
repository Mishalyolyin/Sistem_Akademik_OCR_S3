<div class="overflow-x-auto">
    <table class="w-full text-sm text-left">
        <thead class="text-xs text-gray-500 uppercase bg-gray-50 border-b border-gray-100">
            <tr>
                <th class="px-6 py-3 font-medium">Tahun Akademik</th>
                <th class="px-6 py-3 font-medium">Total Tagihan</th>
                <th class="px-6 py-3 font-medium">Status</th>
                <th class="px-6 py-3 font-medium">Keterangan</th>
                <th class="px-6 py-3 font-medium text-right">Aksi</th>
            </tr>
        </thead>
        <tbody class="divide-y divide-gray-50">
            @forelse($rates as $rate)
                <tr class="hover:bg-gray-50 transition-colors">
                    <td class="px-6 py-4 font-medium text-gray-900">{{ $rate->academic_year }}</td>
                    <td class="px-6 py-4 font-bold text-emerald-600">Rp {{ number_format($rate->amount, 0, ',', '.') }}</td>
                    <td class="px-6 py-4">
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-semibold {{ $rate->active ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-gray-100 text-gray-600 border border-gray-200' }}">
                            {{ $rate->active ? 'Aktif' : 'Non-Aktif' }}
                        </span>
                    </td>
                    <td class="px-6 py-4 text-gray-500 truncate max-w-xs">{{ $rate->description ?? '-' }}</td>
                    <td class="px-6 py-4 text-right">
                        <form action="{{ route('admin.tuition.destroy', $rate) }}" method="POST" onsubmit="return confirm('Apakah Anda yakin ingin menghapus tarif ini?');">
                            @csrf
                            @method('DELETE')
                            <button type="submit" class="text-red-600 hover:text-red-800 text-xs font-medium bg-red-50 px-3 py-1.5 rounded-lg border border-red-100 hover:bg-red-100 transition-colors">Hapus</button>
                        </form>
                    </td>
                </tr>
            @empty
                <tr>
                    <td colspan="5" class="text-center py-12 text-gray-400 italic bg-gray-50">
                        <div class="flex flex-col items-center">
                            <svg class="w-10 h-10 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                            <span>Belum ada data tarif untuk program ini.</span>
                        </div>
                    </td>
                </tr>
            @endforelse
        </tbody>
    </table>
</div>
