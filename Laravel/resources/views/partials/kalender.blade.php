<div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
    <div class="px-6 py-4 border-b border-gray-100 bg-gray-50/50">
        <h3 class="font-semibold text-gray-800 flex items-center gap-2">
            <span>🗓️</span> Kalender Akademik
        </h3>
    </div>
    <ul class="divide-y divide-gray-100">
        @forelse(($kalenderEvents ?? []) as $e)
            <li class="px-6 py-3 flex items-center justify-between hover:bg-gray-50 transition-colors">
                <span class="text-sm font-medium text-emerald-600 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-100">
                    {{ \Carbon\Carbon::parse($e->tanggal)->format('d M Y') }}
                </span>
                <span class="text-sm text-gray-600">{{ $e->judul_event }}</span>
            </li>
        @empty
            <li class="px-6 py-8 text-center text-gray-400 text-sm italic">
                Tidak ada event akademik dalam waktu dekat.
            </li>
        @endforelse
    </ul>
</div>

