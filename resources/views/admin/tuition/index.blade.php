@extends('layouts.admin')

@section('content')
<div class="mb-8">
    <h1 class="text-2xl font-bold text-gray-800">Atur Biaya Kuliah</h1>
    <p class="text-gray-500 text-sm mt-1">Manajemen total tagihan per tahun akademik untuk Reguler & RPL.</p>
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

<div x-data="{ activeTab: 'REGULER' }">
    {{-- Tabs --}}
    <div class="flex border-b border-gray-200 mb-6 gap-6">
        <button @click="activeTab = 'REGULER'" 
            :class="activeTab === 'REGULER' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'"
            class="pb-4 px-1 text-center border-b-2 font-medium text-sm transition-all duration-200 flex items-center gap-2">
            <svg class="w-5 h-5" :class="activeTab === 'REGULER' ? 'text-blue-600' : 'text-gray-400'" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg>
            REGULER
        </button>
        <button @click="activeTab = 'RPL'" 
            :class="activeTab === 'RPL' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'"
            class="pb-4 px-1 text-center border-b-2 font-medium text-sm transition-all duration-200 flex items-center gap-2">
            <svg class="w-5 h-5" :class="activeTab === 'RPL' ? 'text-blue-600' : 'text-gray-400'" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
            RPL (Rekognisi Pembelajaran Lampau)
        </button>
    </div>

    {{-- Content --}}
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {{-- Form Section --}}
        <div class="lg:col-span-1">
            <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6 sticky top-6">
                <h3 class="font-bold text-lg text-gray-800 mb-6 flex items-center gap-2">
                    <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v3m0 0v3m0-3h3m-3 0H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    <span x-text="'Tambah / Update Tarif ' + activeTab"></span>
                </h3>
                
                <form action="{{ route('admin.tuition.store') }}" method="POST">
                    @csrf
                    <input type="hidden" name="program_type" x-model="activeTab">
                    
                    <div class="mb-4">
                        <label class="block text-xs font-medium text-gray-600 mb-1">Tahun Akademik</label>
                        <select name="academic_year" class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5" required>
                            <option value="">-- Pilih Tahun Akademik --</option>
                            @foreach($availableAcademicYears as $year)
                                <option value="{{ $year }}">{{ $year }}</option>
                            @endforeach
                        </select>
                    </div>

                    <div class="mb-4">
                        <label class="block text-xs font-medium text-gray-600 mb-1">Kategori Biaya</label>
                        <select name="category" class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5" required>
                            <option value="SEMESTER">Biaya SPP Semester</option>
                            <option value="MUNAOSAH">Biaya Munaqosah</option>
                        </select>
                    </div>

                    <div class="mb-4">
                        <label class="block text-xs font-medium text-gray-600 mb-1">Total Tagihan (Rp)</label>
                        <input type="number" name="amount" placeholder="5000000" min="0"
                            class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5"
                            required>
                    </div>

                    <div class="mb-4">
                        <label class="block text-xs font-medium text-gray-600 mb-1">Keterangan (Opsional)</label>
                        <textarea name="description" rows="3"
                            class="w-full text-sm rounded-lg border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 py-2.5"></textarea>
                    </div>

                    <div class="mb-6 flex items-center">
                        <input type="checkbox" name="active" value="1" id="active" checked
                            class="rounded border-gray-300 text-blue-600 focus:ring-blue-500 h-4 w-4">
                        <label for="active" class="ml-2 text-sm text-gray-700 font-medium">Aktifkan Tarif Ini</label>
                    </div>

                    <button type="submit" class="w-full bg-blue-600 text-white py-2.5 rounded-lg hover:bg-blue-700 transition-colors font-medium shadow-sm">
                        Simpan Tarif
                    </button>
                </form>
            </div>
        </div>

        {{-- List Section --}}
        <div class="lg:col-span-2">
            
            {{-- REGULER List --}}
            <div x-show="activeTab === 'REGULER'" class="space-y-6">
                <!-- Semester Rates -->
                <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
                    <h3 class="font-bold text-lg text-gray-800 mb-6 flex items-center gap-2">
                        <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        Daftar Tarif SPP Semester (Reguler)
                    </h3>
                    @include('admin.tuition.partials.table', ['rates' => $regulerSemesterRates])
                </div>

                <!-- Munaqosah Rates -->
                <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
                    <h3 class="font-bold text-lg text-gray-800 mb-6 flex items-center gap-2">
                        <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M12 14l9-5-9-5-9 5 9 5z"></path><path d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z"></path></svg>
                        Daftar Tarif Munaqosah (Reguler)
                    </h3>
                    @include('admin.tuition.partials.table', ['rates' => $regulerMunaqosahRates])
                </div>
            </div>

            {{-- RPL List --}}
            <div x-show="activeTab === 'RPL'" class="space-y-6" style="display: none;">
                <!-- Semester Rates -->
                <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
                    <h3 class="font-bold text-lg text-gray-800 mb-6 flex items-center gap-2">
                        <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        Daftar Tarif SPP Semester (RPL)
                    </h3>
                    @include('admin.tuition.partials.table', ['rates' => $rplSemesterRates])
                </div>

                <!-- Munaqosah Rates -->
                <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
                    <h3 class="font-bold text-lg text-gray-800 mb-6 flex items-center gap-2">
                        <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M12 14l9-5-9-5-9 5 9 5z"></path><path d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z"></path></svg>
                        Daftar Tarif Munaqosah (RPL)
                    </h3>
                    @include('admin.tuition.partials.table', ['rates' => $rplMunaqosahRates])
                </div>
            </div>

        </div>
    </div>
</div>
@endsection
