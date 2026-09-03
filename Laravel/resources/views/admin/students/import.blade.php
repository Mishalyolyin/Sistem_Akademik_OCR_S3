@extends('layouts.admin')

@section('content')
<div x-data="studentImport()" class="max-w-4xl mx-auto">
    <div class="mb-8 flex items-end justify-between">
        <div>
            <h1 class="text-2xl font-bold text-gray-800">Import Mahasiswa</h1>
            <p class="text-gray-500 text-sm mt-1">Upload data mahasiswa secara massal menggunakan file Excel (.xlsx).</p>
        </div>
        <a href="{{ route('admin.students.import.template') }}" class="text-sm font-medium text-blue-600 hover:text-blue-700 underline underline-offset-4 decoration-blue-200 hover:decoration-blue-400 transition-all flex items-center gap-1">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
            Download Template
        </a>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-8">
        <!-- Form Area -->
        <form @submit.prevent="submitForm" class="space-y-6">
            
            <!-- File Drop Zone -->
            <div 
                class="relative group cursor-pointer"
                @dragover.prevent="dragOver = true"
                @dragleave.prevent="dragOver = false"
                @drop.prevent="handleDrop($event)"
            >
                <input type="file" x-ref="fileInput" @change="handleFileSelect" class="hidden" accept=".xlsx,.xls,.csv">
                
                <div 
                    class="border-2 border-dashed rounded-xl p-10 text-center transition-all duration-300"
                    :class="dragOver ? 'border-blue-500 bg-blue-50 scale-[0.99]' : 'border-gray-200 hover:border-blue-400 hover:bg-gray-50'"
                    @click="$refs.fileInput.click()"
                >
                    <div class="w-16 h-16 rounded-full bg-blue-50 flex items-center justify-center mx-auto mb-4 text-blue-600 group-hover:scale-110 transition-transform duration-300 border border-blue-100">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                        </svg>
                    </div>
                    
                    <template x-if="!file">
                        <div>
                            <p class="text-lg font-bold text-gray-800 mb-1">Klik atau Drag file Excel ke sini</p>
                            <p class="text-sm text-gray-400">Format yang didukung: .xlsx, .csv</p>
                        </div>
                    </template>

                    <template x-if="file">
                        <div>
                            <p class="text-lg font-bold text-gray-800 mb-1" x-text="file.name"></p>
                            <p class="text-sm text-emerald-600 font-medium flex items-center justify-center gap-1">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                                File siap diupload
                            </p>
                            <button type="button" @click.stop="file = null" class="mt-4 text-xs text-red-500 hover:text-red-700 font-bold uppercase tracking-wider px-3 py-1 rounded hover:bg-red-50 transition-colors">
                                Hapus File
                            </button>
                        </div>
                    </template>
                </div>
            </div>

            <!-- Options -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                    <label class="block text-xs font-bold text-gray-500 uppercase tracking-widest mb-2">Mode Duplikasi</label>
                    <div class="relative">
                        <select x-model="mode" class="w-full pl-4 pr-10 py-3 rounded-lg border border-gray-200 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none appearance-none bg-white font-medium text-gray-800 transition-colors cursor-pointer">
                            <option value="skip">Lewati Data Duplikat</option>
                            <option value="update">Update Data Ada</option>
                            <option value="cancel">Batalkan Semua Jika Ada Duplikat</option>
                        </select>
                        <div class="absolute inset-y-0 right-0 flex items-center px-3 pointer-events-none text-gray-400">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                        </div>
                    </div>
                    <p class="mt-2 text-xs text-gray-400 leading-relaxed">
                        <span x-show="mode === 'skip'">Data mahasiswa dengan NIM yang sama akan dilewati.</span>
                        <span x-show="mode === 'update'">Data mahasiswa dengan NIM yang sama akan diperbarui dengan data baru.</span>
                        <span x-show="mode === 'cancel'">Jika ditemukan NIM yang sama, seluruh proses import akan dibatalkan.</span>
                    </p>
                </div>

                <div class="flex items-end">
                    <button 
                        type="submit" 
                        class="w-full bg-blue-600 text-white py-3 rounded-lg font-bold shadow-sm hover:bg-blue-700 hover:shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none"
                        :disabled="!file || loading"
                    >
                        <svg x-show="loading" class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        <span x-text="loading ? 'Memproses...' : 'Mulai Import'"></span>
                    </button>
                </div>
            </div>

        </form>

        <!-- Result Stats -->
        <div x-show="result" x-transition class="mt-8 pt-8 border-t border-gray-100">
            <div class="flex items-center gap-3 mb-6">
                <div class="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                </div>
                <div>
                    <h3 class="font-bold text-gray-800 text-lg">Import Berhasil</h3>
                    <p class="text-sm text-gray-500">Proses import telah selesai dijalankan.</p>
                </div>
            </div>

            <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div class="bg-gray-50 p-4 rounded-xl border border-gray-100 text-center">
                    <p class="text-xs font-bold text-gray-400 uppercase tracking-wider">Total Baris</p>
                    <p class="text-2xl font-bold text-gray-800 mt-1" x-text="result?.summary?.total_rows ?? 0"></p>
                </div>
                <div class="bg-emerald-50 p-4 rounded-xl border border-emerald-100 text-center">
                    <p class="text-xs font-bold text-emerald-600 uppercase tracking-wider">Sukses</p>
                    <p class="text-2xl font-bold text-emerald-700 mt-1" x-text="result?.summary?.successful_inserts ?? 0"></p>
                </div>
                <div class="bg-blue-50 p-4 rounded-xl border border-blue-100 text-center">
                    <p class="text-xs font-bold text-blue-600 uppercase tracking-wider">Diperbarui</p>
                    <p class="text-2xl font-bold text-blue-700 mt-1" x-text="result?.summary?.updated_records ?? 0"></p>
                </div>
                <div class="bg-amber-50 p-4 rounded-xl border border-amber-100 text-center">
                    <p class="text-xs font-bold text-amber-600 uppercase tracking-wider">Dilewati</p>
                    <p class="text-2xl font-bold text-amber-700 mt-1" x-text="result?.summary?.skipped_duplicates ?? 0"></p>
                </div>
            </div>
        </div>
        
        <!-- Error Message -->
        <div x-show="errorMessage" x-transition class="mt-6 bg-red-50 border border-red-200 rounded-lg p-4 flex items-start gap-3">
             <svg class="w-5 h-5 text-red-500 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
             <div>
                 <h4 class="font-bold text-red-800 text-sm">Terjadi Kesalahan</h4>
                 <p class="text-red-600 text-sm mt-1 leading-relaxed" x-text="errorMessage"></p>
             </div>
        </div>

    </div>
</div>

<script>
    function studentImport() {
        return {
            dragOver: false,
            file: null,
            mode: 'skip',
            loading: false,
            result: null,
            errorMessage: null,

            handleFileSelect(e) {
                if (e.target.files.length) {
                    this.file = e.target.files[0];
                    this.result = null;
                    this.errorMessage = null;
                }
            },

            handleDrop(e) {
                this.dragOver = false;
                if (e.dataTransfer.files.length) {
                    this.file = e.dataTransfer.files[0];
                    this.result = null;
                    this.errorMessage = null;
                }
            },

            async submitForm() {
                if (!this.file) return;

                this.loading = true;
                this.result = null;
                this.errorMessage = null;

                const formData = new FormData();
                formData.append('file', this.file);
                formData.append('mode', this.mode);

                try {
                    const response = await axios.post('{{ route("admin.students.import") }}', formData, {
                        headers: {
                            'Content-Type': 'multipart/form-data'
                        }
                    });

                    this.result = response.data;
                    this.file = null; // Reset file after success
                } catch (error) {
                    console.error(error);
                    if (error.response && error.response.data) {
                        this.errorMessage = error.response.data.message || 'Gagal mengupload file.';
                        
                        if (error.response.data.errors) {
                            const errors = error.response.data.errors;
                            
                            // Check for Excel Validation Failures (Array of objects)
                            if (Array.isArray(errors) && errors.length > 0 && typeof errors[0] === 'object' && errors[0].row) {
                                const failure = errors[0];
                                const row = failure.row;
                                const msg = failure.errors && failure.errors[0] ? failure.errors[0] : 'Data tidak valid';
                                this.errorMessage = `Validasi Gagal (Baris ${row}): ${msg}`;
                            } 
                            // Standard Laravel Validation
                            else {
                                const firstError = Object.values(errors)[0];
                                this.errorMessage += ' ' + (Array.isArray(firstError) ? firstError[0] : firstError);
                            }
                        }
                    } else {
                        this.errorMessage = 'Terjadi kesalahan sistem. Silakan coba lagi.';
                    }
                } finally {
                    this.loading = false;
                }
            }
        }
    }
</script>
@endsection
