@extends('layouts.admin')

@section('content')
<div class="mb-8">
    <h1 class="text-2xl font-bold text-gray-800">Edit Data Mahasiswa</h1>
    <p class="text-gray-500 text-sm mt-1">Perbarui informasi dasar mahasiswa.</p>
</div>

<div class="max-w-3xl bg-white rounded-xl border border-gray-100 shadow-sm p-8" x-data="{ programType: '{{ old('program_type', $student->program_type) }}' }">
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
                    <select name="program_type" x-model="programType" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
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

            <div x-show="programType === 'REGULER'" class="mt-6 p-4 bg-blue-50 border border-blue-100 rounded-lg flex items-center gap-3">
                <input type="checkbox" name="is_alumni" id="is_alumni" value="1" {{ old('is_alumni', $student->is_alumni) ? 'checked' : '' }} class="w-4 h-4 text-blue-600 bg-white border-gray-300 rounded focus:ring-blue-500 focus:ring-2">
                <label for="is_alumni" class="text-sm font-medium text-blue-800 cursor-pointer">
                    Mahasiswa ini adalah Alumni S1 UNISSULA (Mendapat penyesuaian biaya khusus)
                </label>
            </div>

            <div class="border-t border-gray-100 pt-6">
                <p class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-4">Data dari Alur Wajib Pendaftaran</p>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">No. KTP (NIK)</label>
                        <input type="text" name="nik" maxlength="16" value="{{ old('nik', $student->nik) }}" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                        @error('nik') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">No. KK</label>
                        <input type="text" name="kk_number" maxlength="16" value="{{ old('kk_number', $student->kk_number) }}" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                        @error('kk_number') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Tempat Lahir</label>
                        <input type="text" name="birth_place" value="{{ old('birth_place', $student->birth_place) }}" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                        <p class="text-xs text-gray-400 mt-1">Otomatis terisi dari OCR Ijazah, bisa dikoreksi manual.</p>
                        @error('birth_place') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Tanggal Lahir</label>
                        <input type="date" name="birth_date" value="{{ old('birth_date', optional($student->birth_date)->format('Y-m-d')) }}" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">
                        <p class="text-xs text-gray-400 mt-1">Otomatis terisi dari OCR Ijazah, bisa dikoreksi manual.</p>
                        @error('birth_date') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
                    </div>
                </div>
                <div class="mt-6">
                    <label class="block text-sm font-medium text-gray-700 mb-1">Alamat Lengkap</label>
                    <textarea name="address" rows="3" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all">{{ old('address', $student->address) }}</textarea>
                    @error('address') <span class="text-red-500 text-xs mt-1">{{ $message }}</span> @enderror
                </div>
                <div class="mt-4 p-4 bg-amber-50 border border-amber-100 rounded-lg flex items-center gap-3">
                    <input type="checkbox" name="pendaftaran_exempt" id="pendaftaran_exempt" value="1" {{ old('pendaftaran_exempt', $student->pendaftaran_exempt) ? 'checked' : '' }} class="w-4 h-4 text-amber-600 bg-white border-gray-300 rounded focus:ring-amber-500 focus:ring-2">
                    <label for="pendaftaran_exempt" class="text-sm font-medium text-amber-800 cursor-pointer">
                        Exempt dari bayar Pendaftaran Rp 500.000 (mahasiswa lama / kasus khusus)
                    </label>
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

<div class="max-w-3xl bg-white rounded-xl border border-gray-100 shadow-sm p-8 mt-6">
    <h2 class="text-lg font-bold text-gray-800 mb-1">Dokumen Mahasiswa</h2>
    <p class="text-gray-500 text-sm mb-6">Dokumen diupload mandiri oleh mahasiswa. Hasil OCR bersifat informasi saja, bukan verifikasi keaslian.</p>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="border border-gray-200 rounded-lg p-4">
            <div class="font-bold text-gray-700 mb-2">Foto Profil</div>
            @if($student->profile_picture)
                <img src="{{ Storage::url($student->profile_picture) }}" alt="Foto Profil" class="w-28 h-28 object-cover rounded-lg border border-gray-200 mb-2">
                <div>
                    <a href="{{ Storage::url($student->profile_picture) }}" target="_blank" class="text-blue-600 hover:underline text-sm">Lihat Ukuran Penuh</a>
                </div>
                @if($student->profile_picture_analysis)
                    <div class="mt-2 inline-block px-2.5 py-0.5 rounded-full text-xs font-bold {{ ($student->profile_picture_analysis['red_background'] ?? false) ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-amber-50 text-amber-700 border border-amber-100' }}">
                        {{ ($student->profile_picture_analysis['red_background'] ?? false) ? 'Background Merah' : 'Background Bukan Merah — Cek Manual' }}
                    </div>
                @else
                    <div class="mt-2 text-xs text-gray-400">Analisis background belum diproses</div>
                @endif
                <p class="mt-2 text-xs text-gray-400">Pemakaian peci/penutup kepala tidak bisa dideteksi otomatis — mohon cek manual dari foto di atas.</p>
            @else
                <span class="text-gray-400 text-sm">Belum diupload mahasiswa</span>
            @endif
        </div>

        <div class="border border-gray-200 rounded-lg p-4">
            <div class="font-bold text-gray-700 mb-2">Ijazah S1</div>
            @if($student->ijazah_file_path)
                <a href="{{ Storage::url($student->ijazah_file_path) }}" target="_blank" class="text-blue-600 hover:underline text-sm">Lihat / Download Dokumen</a>
                @if($student->ijazah_ocr_data)
                    <div class="mt-2 inline-block px-2.5 py-0.5 rounded-full text-xs font-bold {{ ($student->ijazah_ocr_data['name_match'] ?? false) ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-amber-50 text-amber-700 border border-amber-100' }}">
                        {{ ($student->ijazah_ocr_data['name_match'] ?? false) ? 'Nama Cocok' : 'Perlu Dicek Manual' }}
                    </div>
                @else
                    <div class="mt-2 text-xs text-gray-400">OCR belum diproses</div>
                @endif
            @else
                <span class="text-gray-400 text-sm">Belum diupload mahasiswa</span>
            @endif
        </div>

        <div class="border border-gray-200 rounded-lg p-4">
            <div class="font-bold text-gray-700 mb-2">KTP</div>
            @if($student->ktp_file_path)
                <a href="{{ Storage::url($student->ktp_file_path) }}" target="_blank" class="text-blue-600 hover:underline text-sm">Lihat / Download Dokumen</a>
                @if($student->ktp_ocr_data)
                    @php $nikOcrMatch = $student->nik && ($student->ktp_ocr_data['nik'] ?? null) === $student->nik; @endphp
                    <div class="mt-1 text-xs text-gray-500">NIK terbaca OCR: {{ $student->ktp_ocr_data['nik'] ?? '-' }}</div>
                    <div class="mt-1 inline-block px-2.5 py-0.5 rounded-full text-xs font-bold {{ $nikOcrMatch ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-amber-50 text-amber-700 border border-amber-100' }}">
                        {{ $nikOcrMatch ? 'NIK Cocok' : 'Perlu Dicek Manual' }}
                    </div>
                @else
                    <div class="mt-2 text-xs text-gray-400">OCR belum diproses</div>
                @endif
            @else
                <span class="text-gray-400 text-sm">Belum diupload mahasiswa</span>
            @endif
        </div>

        <div class="border border-gray-200 rounded-lg p-4">
            <div class="font-bold text-gray-700 mb-2">Kartu Keluarga (KK)</div>
            @if($student->kk_file_path)
                <a href="{{ Storage::url($student->kk_file_path) }}" target="_blank" class="text-blue-600 hover:underline text-sm">Lihat / Download Dokumen</a>
                @if($student->kk_ocr_data)
                    @php $kkOcrMatch = $student->kk_number && ($student->kk_ocr_data['kk_number'] ?? null) === $student->kk_number; @endphp
                    <div class="mt-1 text-xs text-gray-500">No. KK terbaca OCR: {{ $student->kk_ocr_data['kk_number'] ?? '-' }}</div>
                    <div class="mt-1 flex flex-wrap gap-1.5">
                        <span class="inline-block px-2.5 py-0.5 rounded-full text-xs font-bold {{ $kkOcrMatch ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-amber-50 text-amber-700 border border-amber-100' }}">
                            {{ $kkOcrMatch ? 'Nomor Cocok' : 'Perlu Dicek Manual' }}
                        </span>
                        <span class="inline-block px-2.5 py-0.5 rounded-full text-xs font-bold {{ ($student->kk_ocr_data['name_found_in_family'] ?? false) ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-amber-50 text-amber-700 border border-amber-100' }}">
                            {{ ($student->kk_ocr_data['name_found_in_family'] ?? false) ? 'Nama Ditemukan' : 'Nama Tidak Ditemukan' }}
                        </span>
                    </div>
                @else
                    <div class="mt-2 text-xs text-gray-400">OCR belum diproses</div>
                @endif
            @else
                <span class="text-gray-400 text-sm">Belum diupload mahasiswa</span>
            @endif
        </div>
    </div>
</div>

<div class="max-w-3xl bg-white rounded-xl border border-gray-100 shadow-sm p-8 mt-6">
    <h2 class="text-lg font-bold text-gray-800 mb-1">Status Bukti Bayar Pendaftaran</h2>
    <p class="text-gray-500 text-sm mb-4">Syarat terakhir sebelum mahasiswa bisa akses Tagihan UKT.</p>
    @php
        $pendaftaranPlanRow = $student->paymentPlans->firstWhere('category', 'PENDAFTARAN');
        $pendaftaranPaid = $pendaftaranPlanRow && $pendaftaranPlanRow->installments->isNotEmpty() && $pendaftaranPlanRow->installments->every(fn($i) => $i->status === 'PAID');
    @endphp
    @if($student->pendaftaran_exempt)
        <span class="px-2.5 py-0.5 rounded-full text-xs font-bold border bg-blue-50 text-blue-700 border-blue-100">Exempt (Mahasiswa Lama)</span>
    @else
        <span class="px-2.5 py-0.5 rounded-full text-xs font-bold border {{ $pendaftaranPaid ? 'bg-emerald-50 text-emerald-700 border-emerald-100' : 'bg-amber-50 text-amber-700 border-amber-100' }}">
            {{ $pendaftaranPaid ? 'Lunas' : ($pendaftaranPlanRow ? 'Sedang Berjalan' : 'Belum Bayar') }}
        </span>
    @endif
</div>

@if(strtoupper($student->program_type) === 'RPL')
    @php
        $kerjasamaPlan = $student->paymentPlans->firstWhere('category', 'KERJASAMA');
    @endphp
    <div class="max-w-3xl bg-white rounded-xl border border-gray-100 shadow-sm p-8 mt-6">
        <div class="flex items-center justify-between mb-1">
            <h2 class="text-lg font-bold text-gray-800">Kelas Kerjasama</h2>
            @if($kerjasamaPlan)
                <a href="{{ route('admin.payments.kerjasama.rpl', ['search' => $student->nim]) }}" class="text-xs font-semibold text-indigo-600 hover:underline">Lihat di Verifikasi Pembayaran &rarr;</a>
            @endif
        </div>
        @if(!$kerjasamaPlan)
            <p class="text-gray-400 text-sm">Mahasiswa ini tidak mengikuti Kelas Kerjasama.</p>
        @else
            @php
                $kerjasamaLunas = $kerjasamaPlan->installments->isNotEmpty() && $kerjasamaPlan->installments->every(fn($i) => $i->status === 'PAID');
                $kerjasamaDibayar = $kerjasamaPlan->installments->sum('amount_paid');
            @endphp
            <p class="text-gray-500 text-sm mb-4">{{ $kerjasamaPlan->academic_year }} / {{ $kerjasamaPlan->term }} &mdash; Total Tagihan: Rp{{ number_format($kerjasamaPlan->total_amount, 0, ',', '.') }}</p>
            <span class="px-2.5 py-0.5 rounded-full text-xs font-bold border {{ $kerjasamaLunas ? 'bg-emerald-50 text-emerald-700 border-emerald-100' : 'bg-amber-50 text-amber-700 border-amber-100' }}">
                {{ $kerjasamaLunas ? 'Lunas' : 'Sedang Berjalan' }}
            </span>
            <span class="ml-2 text-xs text-gray-500">Terbayar: Rp{{ number_format($kerjasamaDibayar, 0, ',', '.') }} / Rp{{ number_format($kerjasamaPlan->total_amount, 0, ',', '.') }}</span>

            <table class="w-full text-sm mt-4 border-t border-gray-100">
                <thead>
                    <tr class="text-xs text-gray-400 uppercase">
                        <th class="text-left py-2 font-semibold">#</th>
                        <th class="text-left py-2 font-semibold">Jatuh Tempo</th>
                        <th class="text-left py-2 font-semibold">Tagihan</th>
                        <th class="text-left py-2 font-semibold">Status</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-gray-50">
                    @foreach($kerjasamaPlan->installments->sortBy('installment_no') as $inst)
                        <tr>
                            <td class="py-2 text-gray-500">{{ $inst->installment_no }}</td>
                            <td class="py-2 text-gray-700">{{ \Carbon\Carbon::parse($inst->due_date)->translatedFormat('F Y') }}</td>
                            <td class="py-2 text-gray-700">Rp{{ number_format($inst->amount, 0, ',', '.') }}</td>
                            <td class="py-2">
                                @php
                                    $instColors = ['PAID' => 'bg-emerald-50 text-emerald-700 border-emerald-100', 'PARTIAL' => 'bg-indigo-50 text-indigo-700 border-indigo-100', 'UNPAID' => 'bg-gray-100 text-gray-500 border-gray-200', 'OVERDUE' => 'bg-red-50 text-red-700 border-red-100'];
                                @endphp
                                <span class="px-2 py-0.5 rounded-full text-[11px] font-bold border {{ $instColors[$inst->status] ?? 'bg-gray-100 text-gray-500 border-gray-200' }}">{{ $inst->status }}</span>
                            </td>
                        </tr>
                    @endforeach
                </tbody>
            </table>
        @endif
    </div>
@endif
@endsection
