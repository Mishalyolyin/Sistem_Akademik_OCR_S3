<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}" class="scroll-smooth">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Magister Pendidikan Agama Islam - UNISSULA</title>
    
    <!-- Scripts & Styles -->
    @vite(['resources/css/app.css', 'resources/js/app.js'])
    
    <!-- Alpine.js for interactions -->
    <script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"></script>
</head>
<body class="font-sans text-primary bg-surface antialiased selection:bg-gold selection:text-white">

    <!-- Navbar (Sticky + Glassmorphism) -->
    <nav x-data="{ scrolled: false, mobileMenuOpen: false }" 
         @scroll.window="scrolled = (window.pageYOffset > 20)"
         :class="{ 'glass shadow-sm py-4': scrolled, 'bg-transparent py-6': !scrolled }"
         class="fixed w-full z-50 transition-all duration-300 top-0 left-0">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex justify-between items-center">
                <!-- Logo -->
                <div class="flex items-center gap-3">
                    <img src="{{ asset('images/unissula-logo.png') }}" alt="Logo Unissula" class="w-12 h-12 object-contain drop-shadow-md">
                    <div class="flex flex-col">
                        <span class="font-serif font-bold text-lg text-primary leading-tight tracking-tight">
                            Magister Pendidikan <br> <span class="text-gold">Agama Islam</span>
                        </span>
                    </div>
                </div>

                <!-- Desktop Menu -->
                <div class="hidden md:flex items-center gap-8">
                    <a href="#promo" class="text-sm font-medium hover:text-gold transition-colors relative group">
                        Promo
                        <span class="absolute -top-2 -right-3 px-1.5 py-0.5 bg-red-500 text-white text-[10px] rounded-full font-bold animate-pulse">Baru</span>
                    </a>
                    <a href="#pengantar" class="text-sm font-medium hover:text-gold transition-colors">Pengantar</a>
                    <a href="#visi-misi" class="text-sm font-medium hover:text-gold transition-colors">Visi Misi</a>
                    <a href="#pengurus" class="text-sm font-medium hover:text-gold transition-colors">Pengurus</a>
                    <a href="#kurikulum" class="text-sm font-medium hover:text-gold transition-colors">Kurikulum</a>
                    <a href="#kontak" class="text-sm font-medium hover:text-gold transition-colors">Kontak</a>
                </div>

                <!-- CTA Buttons -->
                <div class="hidden md:flex items-center gap-4">
                    <a href="{{ route('login') }}" class="px-5 py-2 text-sm font-medium text-primary hover:text-gold transition-colors">
                        Login Admin
                    </a>
                    <a href="{{ route('login') }}" class="btn-luxury px-6 py-2.5 text-sm font-bold text-white rounded-full shadow-lg shadow-gold/20">
                        Login Mahasiswa
                    </a>
                </div>

                <!-- Mobile Menu Button -->
                <div class="md:hidden">
                    <button @click="mobileMenuOpen = !mobileMenuOpen" class="text-primary hover:text-gold">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
                        </svg>
                    </button>
                </div>
            </div>
        </div>

        <!-- Mobile Menu Dropdown -->
        <div x-show="mobileMenuOpen" 
             x-transition
             @click.away="mobileMenuOpen = false"
             class="md:hidden absolute top-full left-0 w-full bg-white/95 backdrop-blur-md shadow-xl border-t border-gold/20">
            <div class="flex flex-col p-4 gap-4">
                <a href="#promo" class="text-base font-medium text-primary hover:text-gold">Promo</a>
                <a href="#pengantar" class="text-base font-medium text-primary hover:text-gold">Pengantar</a>
                <a href="#visi-misi" class="text-base font-medium text-primary hover:text-gold">Visi Misi</a>
                <a href="#pengurus" class="text-base font-medium text-primary hover:text-gold">Pengurus</a>
                <a href="#kurikulum" class="text-base font-medium text-primary hover:text-gold">Kurikulum</a>
                <a href="#kontak" class="text-base font-medium text-primary hover:text-gold">Kontak</a>
                <hr class="border-gold/20">
                <a href="{{ route('login') }}" class="w-full text-center py-2.5 font-bold text-white btn-luxury rounded-lg">
                    Login Mahasiswa
                </a>
            </div>
        </div>
    </nav>

    <!-- Main Content -->
    <main>
        @yield('content')
    </main>

    <!-- Footer -->
    <footer class="bg-primary text-white pt-20 pb-10 border-t border-gold/20 relative overflow-hidden">
        <!-- Abstract Decoration -->
        <div class="absolute top-0 right-0 w-96 h-96 bg-gold/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 pointer-events-none"></div>

        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
            <div class="grid grid-cols-1 md:grid-cols-4 gap-12 mb-16">
                <!-- Brand -->
                <div class="col-span-1 md:col-span-1">
                    <div class="flex items-center gap-3 mb-6">
                        <div class="bg-white p-1 rounded-full shadow-lg">
                            <img src="https://upload.wikimedia.org/wikipedia/commons/8/85/Logo_Unissula.png" alt="Logo Unissula" class="w-10 h-10 object-contain">
                        </div>
                        <span class="font-serif font-bold text-lg text-white leading-tight tracking-tight">
                            Magister Pendidikan <br> <span class="text-gold">Agama Islam</span>
                        </span>
                    </div>
                    <p class="text-white/60 text-sm leading-relaxed mb-6">
                        Sistem pembayaran SKS modern, transparan, dan fleksibel untuk mendukung masa depan pendidikan Anda.
                    </p>
                    <div class="flex gap-4">
                        <a href="#" class="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-gold hover:text-white hover:bg-gold transition-all border border-white/10 hover:border-gold">
                            <!-- Icon Socmed -->
                            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M24 4.557c-.883.392-1.832.656-2.828.775 1.017-.609 1.798-1.574 2.165-2.724-.951.564-2.005.974-3.127 1.195-.897-.957-2.178-1.555-3.594-1.555-3.179 0-5.515 2.966-4.797 6.045-4.091-.205-7.719-2.165-10.148-5.144-1.29 2.213-.669 5.108 1.523 6.574-.806-.026-1.566-.247-2.229-.616-.054 2.281 1.581 4.415 3.949 4.89-.693.188-1.452.232-2.224.084.626 1.956 2.444 3.379 4.6 3.419-2.07 1.623-4.678 2.348-7.29 2.04 2.179 1.397 4.768 2.212 7.548 2.212 9.142 0 14.307-7.721 13.995-14.646.962-.695 1.797-1.562 2.457-2.549z"/></svg>
                        </a>
                        <!-- More icons... -->
                    </div>
                </div>

                <!-- Quick Links -->
                <div>
                    <h4 class="font-serif font-bold text-lg mb-6 text-gold">Akses Cepat</h4>
                    <ul class="space-y-3">
                        <li><a href="#promo" class="text-white/70 hover:text-white transition-colors text-sm">Promo Terbaru</a></li>
                        <li><a href="#pengantar" class="text-white/70 hover:text-white transition-colors text-sm">Tentang Program</a></li>
                        <li><a href="#visi-misi" class="text-white/70 hover:text-white transition-colors text-sm">Visi & Misi</a></li>
                        <li><a href="#kurikulum" class="text-white/70 hover:text-white transition-colors text-sm">Kurikulum</a></li>
                    </ul>
                </div>

                <!-- Layanan -->
                <div>
                    <h4 class="font-serif font-bold text-lg mb-6 text-gold">Layanan</h4>
                    <ul class="space-y-3">
                        <li><a href="#" class="text-white/70 hover:text-white transition-colors text-sm">Pendaftaran Mahasiswa</a></li>
                        <li><a href="#" class="text-white/70 hover:text-white transition-colors text-sm">Panduan Pembayaran</a></li>
                        <li><a href="#" class="text-white/70 hover:text-white transition-colors text-sm">Verifikasi Ijazah</a></li>
                        <li><a href="#" class="text-white/70 hover:text-white transition-colors text-sm">FAQ</a></li>
                    </ul>
                </div>

                <!-- Kontak -->
                <div>
                    <h4 class="font-serif font-bold text-lg mb-6 text-gold">Hubungi Kami</h4>
                    <ul class="space-y-4">
                        <li class="flex items-start gap-3">
                            <svg class="w-5 h-5 text-gold mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
                            <span class="text-white/70 text-sm">Jl. Pendidikan No. 123, Kampus Utama, Jakarta Selatan</span>
                        </li>
                        <li class="flex items-center gap-3">
                            <svg class="w-5 h-5 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"></path></svg>
                            <span class="text-white/70 text-sm">+62 812 3456 7890 (Admin)</span>
                        </li>
                    </ul>
                </div>
            </div>

            <div class="border-t border-white/10 pt-8 flex flex-col md:flex-row justify-between items-center gap-4">
                <p class="text-white/40 text-sm">© 2026 Pembayaran SKS. All rights reserved.</p>
                <div class="flex gap-6">
                    <a href="#" class="text-white/40 hover:text-white text-sm">Privacy Policy</a>
                    <a href="#" class="text-white/40 hover:text-white text-sm">Terms of Service</a>
                </div>
            </div>
        </div>
    </footer>
</body>
</html>
