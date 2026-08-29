<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Admin Dashboard - Magister Pendidikan Agama Islam</title>
    @vite(['resources/css/app.css', 'resources/js/app.js'])
    <script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"></script>
</head>
<body class="bg-surface text-text-main font-sans antialiased" 
      x-data="{ 
          sidebarOpen: window.innerWidth >= 1024,
          isMobile: window.innerWidth < 1024
      }"
      @resize.window="isMobile = window.innerWidth < 1024; if(!isMobile) sidebarOpen = true; else sidebarOpen = false">

    <!-- Mobile Overlay -->
    <div x-show="isMobile && sidebarOpen" 
         @click="sidebarOpen = false"
         x-transition:enter="transition-opacity ease-linear duration-300"
         x-transition:enter-start="opacity-0"
         x-transition:enter-end="opacity-100"
         x-transition:leave="transition-opacity ease-linear duration-300"
         x-transition:leave-start="opacity-100"
         x-transition:leave-end="opacity-0"
         class="fixed inset-0 bg-black/50 z-[90] lg:hidden">
    </div>

    <!-- Sidebar -->
    <aside class="fixed inset-y-0 left-0 z-[100] w-64 sidebar-gradient text-white transition-transform duration-300 transform border-r border-white/10"
           :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'">
        
        <!-- Logo -->
        <div class="flex items-center gap-3 px-6 h-auto py-5 border-b border-white/5 relative z-10">
            <div class="w-12 h-12 bg-white rounded-full flex items-center justify-center p-1 overflow-hidden shadow-lg shadow-black/20 border border-white/20">
                <img src="{{ asset('images/unissula-logo.png') }}" alt="Logo Unissula" class="w-full h-full object-contain">
            </div>
            <span class="font-serif font-bold text-sm tracking-tight leading-tight text-white">
                Magister Pendidikan <br> <span class="text-gray-200 text-base">Agama Islam</span>
            </span>
        </div>

        <!-- Menu -->
        <nav class="p-4 space-y-1 overflow-y-auto h-[calc(100vh-5rem)] relative z-10">
            <a href="{{ route('admin.dashboard') }}" class="relative z-20 cursor-pointer flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium transition-all duration-200 {{ request()->routeIs('admin.dashboard') ? 'bg-white/20 text-white font-bold shadow-md ring-1 ring-white/20' : 'text-gray-400 hover:text-white hover:bg-white/10' }}">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path></svg>
                Dashboard
            </a>

            <p class="px-3 text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 mt-6 font-sans">Operasional</p>
            
            <div x-data="{ open: {{ request()->routeIs('admin.payments.*') ? 'true' : 'false' }} }">
                <button @click="open = !open" class="w-full relative z-20 cursor-pointer flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium transition-all duration-200 group {{ request()->routeIs('admin.payments.*') ? 'text-white' : 'text-gray-400 hover:text-white hover:bg-white/10' }}">
                    <svg class="w-5 h-5 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    Verifikasi Pembayaran
                    <svg :class="open ? 'rotate-180' : ''" class="w-4 h-4 ml-auto transition-transform text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                </button>
                <div x-show="open" class="pl-11 space-y-1 mt-1">
                    <a href="{{ route('admin.payments.reguler') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.payments.reguler') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        Reguler
                    </a>
                    <a href="{{ route('admin.payments.rpl') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.payments.rpl') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        RPL
                    </a>
                    <a href="{{ route('admin.payments.munaqosah.reguler') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.payments.munaqosah.reguler') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        Munaqosah Reguler
                    </a>
                    <a href="{{ route('admin.payments.munaqosah.rpl') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.payments.munaqosah.rpl') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        Munaqosah RPL
                    </a>
                    <a href="{{ route('admin.payments.pendaftaran.reguler') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.payments.pendaftaran.reguler') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        Pendaftaran Reguler
                    </a>
                    <a href="{{ route('admin.payments.pendaftaran.rpl') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.payments.pendaftaran.rpl') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        Pendaftaran RPL
                    </a>
                    <a href="{{ route('admin.payments.kerjasama.rpl') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.payments.kerjasama.rpl') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        Kelas Kerjasama RPL
                    </a>
                </div>
            </div>

            <div x-data="{ open: {{ request()->routeIs('admin.students.*') || request()->routeIs('admin.classes.*') ? 'true' : 'false' }} }">
                <button @click="open = !open" class="w-full relative z-20 cursor-pointer flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium transition-all duration-200 group {{ request()->routeIs('admin.students.*') || request()->routeIs('admin.classes.*') ? 'text-white' : 'text-gray-400 hover:text-white hover:bg-white/10' }}">
                    <svg class="w-5 h-5 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                    Data Mahasiswa
                    <svg :class="open ? 'rotate-180' : ''" class="w-4 h-4 ml-auto transition-transform text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                </button>
                <div x-show="open" class="pl-11 space-y-1 mt-1">
                    <a href="{{ route('admin.students.reguler') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.students.reguler') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        Reguler
                    </a>
                    <a href="{{ route('admin.students.rpl') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.students.rpl') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        RPL
                    </a>
                    <a href="{{ route('admin.classes.index') }}" class="block px-3 py-2 rounded-lg text-sm transition-all duration-200 hover:text-white hover:bg-white/10 {{ request()->routeIs('admin.classes.*') ? 'text-white bg-white/20 font-bold shadow-sm' : 'text-gray-400' }}">
                        Manajemen Kelas
                    </a>
                </div>
            </div>

            <a href="{{ route('admin.students.import.form') }}" class="relative z-20 cursor-pointer flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium transition-all duration-200 group {{ request()->routeIs('admin.students.import.form') ? 'bg-white/20 text-white font-bold shadow-md ring-1 ring-white/20' : 'text-gray-400 hover:text-white hover:bg-white/10' }}">
                <svg class="w-5 h-5 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
                Import Mahasiswa
            </a>

            <p class="px-3 text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 mt-6 font-sans">Keuangan</p>
            
            <a href="{{ route('admin.tuition') }}" class="relative z-20 cursor-pointer flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium transition-all duration-200 {{ request()->routeIs('admin.tuition') ? 'bg-white/20 text-white font-bold shadow-md ring-1 ring-white/20' : 'text-gray-400 hover:text-white hover:bg-white/10' }}">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                Atur Biaya Kuliah
            </a>

            <a href="{{ route('admin.adjustments') }}" class="relative z-20 cursor-pointer flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium transition-all duration-200 {{ request()->routeIs('admin.adjustments') ? 'bg-white/20 text-white font-bold shadow-md ring-1 ring-white/20' : 'text-gray-400 hover:text-white hover:bg-white/10' }}">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"></path></svg>
                Adjustment / Koreksi
            </a>

            <a href="{{ route('admin.reports') }}" class="relative z-20 cursor-pointer flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium transition-all duration-200 {{ request()->routeIs('admin.reports') ? 'bg-white/20 text-white font-bold shadow-md ring-1 ring-white/20' : 'text-gray-400 hover:text-white hover:bg-white/10' }}">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                Laporan & Export
            </a>

            <p class="px-3 text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 mt-6 font-sans">Sistem</p>
            
            <a href="{{ route('admin.ml_settings') }}" class="relative z-20 cursor-pointer flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium transition-all duration-200 group {{ request()->routeIs('admin.ml_settings') ? 'bg-white/20 text-white font-bold shadow-md ring-1 ring-white/20' : 'text-gray-400 hover:text-white hover:bg-white/10' }}">
                <svg class="w-5 h-5 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.384-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"></path></svg>
                Pengaturan ML
            </a>

            <form method="POST" action="{{ route('logout') }}" class="mt-8 pt-8 border-t border-white/10 relative z-20">
                @csrf
                <button type="submit" class="w-full flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium text-red-400 hover:text-white hover:bg-red-500/10 transition-colors cursor-pointer">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
                    Logout
                </button>
            </form>
        </nav>
    </aside>

    <!-- Main Content -->
    <div class="transition-all duration-300 min-h-screen flex flex-col" :class="(!isMobile && sidebarOpen) ? 'lg:ml-64' : ''">
        
        <!-- Topbar -->
        <header class="h-16 bg-white sticky top-0 z-40 px-4 lg:px-8 flex items-center justify-between border-b border-gray-200 shadow-sm">
            <button @click="sidebarOpen = !sidebarOpen" class="text-gray-500 hover:text-gray-700 transition-colors">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7"></path></svg>
            </button>


            <div class="flex items-center gap-6">
                <!-- Notifications -->
                <div class="relative" x-data="{ open: false }">
                    <button @click="open = !open" class="relative text-gray-400 hover:text-gray-600 transition-colors">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path></svg>
                        <span class="absolute -top-1 -right-1 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
                    </button>
                    <!-- Dropdown -->
                    <div x-show="open" @click.away="open = false" class="absolute right-0 mt-4 w-80 bg-white rounded-xl shadow-xl border border-gray-100 overflow-hidden z-50">
                        <div class="p-4 border-b border-gray-100 font-bold text-sm text-gray-800">Notifikasi</div>
                        <div class="max-h-64 overflow-y-auto">
                            <a href="#" class="block p-4 hover:bg-gray-50 transition-colors border-b border-gray-50">
                                <p class="text-sm font-medium text-gray-800">Pembayaran Baru</p>
                                <p class="text-xs text-gray-500 mt-1">Budi Santoso mengupload bukti pembayaran.</p>
                            </a>
                        </div>
                    </div>
                </div>

                <!-- Profile -->
                <div class="flex items-center gap-3 pl-6 border-l border-gray-200">
                    <div class="text-right hidden md:block">
                        <p class="text-sm font-bold text-gray-800">{{ Auth::user()->name ?? 'Administrator' }}</p>
                        <p class="text-xs text-gray-500">Admin Keuangan</p>
                    </div>
                    <div class="w-9 h-9 rounded-full bg-gray-100 flex items-center justify-center text-gray-600 font-bold border border-gray-200">
                        {{ substr(Auth::user()->name ?? 'A', 0, 1) }}
                    </div>
                </div>
            </div>
        </header>

        <!-- Content -->
        <main class="p-4 lg:p-8">
            @yield('content')
        </main>
    </div>

</body>
</html>
