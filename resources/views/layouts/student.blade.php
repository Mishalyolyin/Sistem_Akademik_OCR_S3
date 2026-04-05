<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>@yield('title', 'Dashboard Mahasiswa')</title>
    @vite(['resources/css/app.css', 'resources/js/app.js'])
    <script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"></script>
</head>
<body class="bg-gray-50 font-sans antialiased text-gray-800" x-data="{ sidebarOpen: false }">
    <div x-show="sidebarOpen" @click="sidebarOpen = false" x-transition:enter="transition-opacity ease-linear duration-300" x-transition:enter-start="opacity-0" x-transition:enter-end="opacity-100" x-transition:leave="transition-opacity ease-linear duration-300" x-transition:leave-start="opacity-100" x-transition:leave-end="opacity-0" class="fixed inset-0 bg-gray-900/80 z-40 lg:hidden" x-cloak></div>
    <header class="lg:hidden bg-emerald-950 text-white p-4 flex justify-between items-center sticky top-0 z-50">
        <div class="flex items-center gap-2">
            <img src="{{ asset('images/unissula-logo.png') }}" alt="Logo" class="w-8 h-8 bg-white rounded-full p-0.5">
            <span class="font-bold text-sm">MPAI UNISSULA</span>
        </div>
        <button @click="sidebarOpen = !sidebarOpen" class="text-emerald-300 focus:outline-none">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path></svg>
        </button>
    </header>
    <aside class="fixed inset-y-0 left-0 z-50 w-72 bg-emerald-950 text-white transition-transform duration-300 ease-in-out transform shadow-2xl lg:translate-x-0" :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'">
        <div class="flex items-center gap-3 p-6 border-b border-white/10 bg-emerald-900/50">
            <img src="{{ asset('images/unissula-logo.png') }}" alt="Logo" class="w-10 h-10 drop-shadow-md bg-white rounded-full p-1">
            <div>
                <h1 class="font-serif font-bold text-lg leading-tight tracking-wide">Magister PAI</h1>
                <p class="text-xs text-emerald-300 font-light tracking-wider uppercase">Mahasiswa Area</p>
            </div>
        </div>
        @php
            $authUser = Auth::user();
            $student = $authUser?->student;
            $logoutAvailable = \Illuminate\Support\Facades\Route::has('logout');
            $isDash = request()->routeIs('student.dashboard');
            $isHist = request()->routeIs('student.history');
            $isProf = request()->routeIs('student.profile');
        @endphp
        <div class="p-6 text-center border-b border-white/10">
            <div class="relative inline-block">
                <div class="w-20 h-20 rounded-full border-4 border-emerald-800 bg-white flex items-center justify-center text-emerald-900 font-bold text-2xl mx-auto mb-3 shadow-lg">
                    {{ strtoupper(substr($authUser?->name ?? 'M', 0, 1)) }}
                </div>
            </div>
            <h3 class="font-bold text-white truncate px-2">{{ $authUser?->name ?? 'Mahasiswa' }}</h3>
            <p class="text-xs text-emerald-400 mt-1 font-mono">{{ $student?->nim ?? '' }}</p>
        </div>
        <nav class="p-4 space-y-1 overflow-y-auto h-[calc(100vh-320px)]">
            <p class="px-4 text-xs font-semibold text-emerald-400 uppercase tracking-wider mb-2 mt-2">Menu Utama</p>
            <a href="{{ route('student.dashboard') }}" class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-300 group {{ $isDash ? 'bg-gradient-to-r from-emerald-600 to-emerald-700 text-white shadow-lg' : 'text-gray-300 hover:bg-white/5 hover:text-white' }}">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z"></path></svg>
                <span class="font-medium">Dashboard</span>
            </a>
            <a href="{{ route('student.history') }}" class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-300 group {{ request()->routeIs('student.history') ? 'bg-gradient-to-r from-emerald-600 to-emerald-700 text-white shadow-lg' : 'text-gray-300 hover:bg-white/5 hover:text-white' }}">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path></svg>
                <span class="font-medium">Riwayat</span>
            </a>
            <a href="{{ route('student.munaqosah') }}" class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-300 group {{ request()->routeIs('student.munaqosah') ? 'bg-gradient-to-r from-emerald-600 to-emerald-700 text-white shadow-lg' : 'text-gray-300 hover:bg-white/5 hover:text-white' }}">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M12 14l9-5-9-5-9 5 9 5z"></path><path d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z"></path></svg>
                <span class="font-medium">Munaqosah</span>
            </a>
            <a href="{{ route('student.profile') }}" class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-300 group {{ $isProf ? 'bg-gradient-to-r from-emerald-600 to-emerald-700 text-white shadow-lg' : 'text-gray-300 hover:bg-white/5 hover:text-white' }}">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                <span class="font-medium">Profil</span>
            </a>
            <p class="px-4 text-xs font-semibold text-emerald-400 uppercase tracking-wider mb-2 mt-6">Akun</p>
        </nav>
        @if($logoutAvailable)
        <div class="absolute bottom-0 left-0 w-full p-4 bg-emerald-950 border-t border-white/10">
            <form action="{{ route('logout') }}" method="POST">
                @csrf
                <button type="submit" class="w-full flex items-center justify-center gap-2 px-4 py-3 bg-red-500/10 text-red-400 hover:bg-red-500 hover:text-white rounded-xl transition-all duration-300 font-medium border border-red-500/20">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
                    Keluar
                </button>
            </form>
        </div>
        @endif
    </aside>
    <div class="lg:ml-72 min-h-screen flex flex-col transition-all duration-300">
        <header class="hidden lg:flex bg-white/80 backdrop-blur-md sticky top-0 z-40 border-b border-gray-200 px-8 py-4 items-center justify-between shadow-sm">
            <h2 class="text-xl font-serif font-bold text-emerald-950 tracking-tight">
                @yield('title', 'Dashboard Mahasiswa')
            </h2>
            <div class="flex items-center gap-4">
                <div class="text-right">
                    <p class="text-sm font-medium text-gray-900">{{ $authUser?->name ?? 'Mahasiswa' }}</p>
                    <p class="text-xs text-gray-500">{{ $student?->nim ?? '' }}</p>
                </div>
            </div>
        </header>
        <main class="flex-1 p-4 lg:p-8 space-y-6">
            @include('partials.semester')
            @yield('content')
        </main>
        <footer class="bg-white border-t border-gray-200 py-6 text-center text-sm text-gray-500">
            &copy; {{ date('Y') }} Magister Pendidikan Agama Islam - UNISSULA.
        </footer>
    </div>
</body>
</html>
