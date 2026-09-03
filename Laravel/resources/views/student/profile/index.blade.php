@extends('layouts.student')

@section('content')
<div class="mb-8">
    <h1 class="text-3xl font-serif font-bold text-primary mb-2">Profil <span class="text-gold-gradient">Mahasiswa</span></h1>
    <p class="text-slate-500">Informasi akun dan data akademik Anda.</p>
</div>

<div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
    <!-- Profile Card -->
    <div class="card-luxury p-8 text-center h-fit">
        <div class="w-32 h-32 rounded-full bg-gold/10 mx-auto mb-6 p-1 border-2 border-gold/30">
            <div class="w-full h-full rounded-full bg-gradient-to-br from-primary to-primary-dark flex items-center justify-center text-gold text-4xl font-serif font-bold shadow-inner">
                {{ substr(auth()->user()->name, 0, 1) }}
            </div>
        </div>
        <h2 class="text-2xl font-serif font-bold text-primary mb-1">{{ auth()->user()->name }}</h2>
        <p class="text-gold font-medium mb-6">{{ auth()->user()->email }}</p>
        
        <div class="space-y-3">
            <div class="flex items-center justify-between p-3 rounded-lg bg-surface border border-gold/10">
                <span class="text-sm text-slate-500">Status</span>
                <span class="text-sm font-bold text-green-600 bg-green-50 px-3 py-1 rounded-full border border-green-100">Aktif</span>
            </div>
            <div class="flex items-center justify-between p-3 rounded-lg bg-surface border border-gold/10">
                <span class="text-sm text-slate-500">Bergabung</span>
                <span class="text-sm font-bold text-primary">{{ auth()->user()->created_at->format('d M Y') }}</span>
            </div>
        </div>
    </div>

    <!-- Details Form -->
    <div class="lg:col-span-2 space-y-8">
        <div class="card-luxury p-8">
            <h3 class="text-xl font-serif font-bold text-primary mb-6 flex items-center gap-2">
                <svg class="w-5 h-5 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                Informasi Pribadi
            </h3>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                    <label class="block text-xs font-bold text-primary uppercase tracking-wider mb-2">Nama Lengkap</label>
                    <input type="text" value="{{ auth()->user()->name }}" class="input-luxury w-full bg-slate-50" readonly>
                </div>
                <div>
                    <label class="block text-xs font-bold text-primary uppercase tracking-wider mb-2">Email</label>
                    <input type="email" value="{{ auth()->user()->email }}" class="input-luxury w-full bg-slate-50" readonly>
                </div>
                <div>
                    <label class="block text-xs font-bold text-primary uppercase tracking-wider mb-2">Role</label>
                    <input type="text" value="{{ auth()->user()->role }}" class="input-luxury w-full bg-slate-50 uppercase" readonly>
                </div>
                <div>
                    <label class="block text-xs font-bold text-primary uppercase tracking-wider mb-2">Program</label>
                    <input type="text" value="{{ auth()->user()->student->program_type ?? '-' }}" class="input-luxury w-full bg-slate-50" readonly>
                </div>
                <div>
                    <label class="block text-xs font-bold text-primary uppercase tracking-wider mb-2">Kelas</label>
                    <input type="text" value="{{ auth()->user()->student->class ?? '-' }}" class="input-luxury w-full bg-slate-50" readonly>
                </div>
            </div>
        </div>

        <!-- Password Change Form -->
        <div class="card-luxury p-8">
            <h3 class="text-xl font-serif font-bold text-primary mb-6 flex items-center gap-2">
                <svg class="w-5 h-5 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                Ganti Password
            </h3>

            @if(session('success'))
                <div class="mb-4 bg-green-50 text-green-700 px-4 py-3 rounded-lg border border-green-200">
                    {{ session('success') }}
                </div>
            @endif

            @if($errors->any())
                <div class="mb-4 bg-red-50 text-red-700 px-4 py-3 rounded-lg border border-red-200">
                    <ul class="list-disc pl-5 text-sm">
                        @foreach($errors->all() as $error)
                            <li>{{ $error }}</li>
                        @endforeach
                    </ul>
                </div>
            @endif

            <form action="{{ route('student.password.update') }}" method="POST" class="space-y-6">
                @csrf
                @method('PUT')
                
                <div>
                    <label class="block text-xs font-bold text-primary uppercase tracking-wider mb-2">Password Saat Ini</label>
                    <input type="password" name="current_password" class="input-luxury w-full" required>
                </div>
                
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <label class="block text-xs font-bold text-primary uppercase tracking-wider mb-2">Password Baru</label>
                        <input type="password" name="password" class="input-luxury w-full" required>
                    </div>
                    <div>
                        <label class="block text-xs font-bold text-primary uppercase tracking-wider mb-2">Konfirmasi Password</label>
                        <input type="password" name="password_confirmation" class="input-luxury w-full" required>
                    </div>
                </div>

                <div class="pt-6 border-t border-gold/10 flex justify-end">
                    <button type="submit" class="btn-luxury px-6 py-2 rounded-lg font-bold text-sm text-white bg-primary hover:bg-primary-dark transition-colors">
                        Simpan Password
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection
