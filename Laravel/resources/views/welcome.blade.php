@extends('layouts.landing')

@section('content')

<!-- 1.2 Hero Section -->
<section class="relative min-h-screen flex items-center pt-20 overflow-hidden bg-surface">
    <!-- Abstract Background Elements -->
    <div class="absolute top-0 right-0 w-2/3 h-full bg-gradient-to-l from-gold/5 to-transparent -skew-x-12 transform translate-x-1/4 z-0"></div>
    <div class="absolute bottom-0 left-0 w-96 h-96 bg-gold/5 rounded-full blur-3xl -translate-x-1/2 translate-y-1/2"></div>
    
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 w-full">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <!-- Text Content -->
            <div class="space-y-8" x-data="{ shown: false }" x-init="setTimeout(() => shown = true, 100)">
                <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-gold/10 border border-gold/20 text-gold-dark text-xs font-bold uppercase tracking-wider transition-all duration-700 transform translate-y-4 opacity-0" :class="{ 'translate-y-0 opacity-100': shown }">
                    <span class="w-2 h-2 rounded-full bg-gold animate-pulse"></span>
                    Penerimaan Mahasiswa Baru 2026
                </div>
                
                <h1 class="font-serif text-5xl md:text-7xl font-bold text-primary leading-tight transition-all duration-700 delay-100 transform translate-y-4 opacity-0" :class="{ 'translate-y-0 opacity-100': shown }">
                    Masa Depan <br>
                    <span class="text-gold-gradient italic pr-2">Cemerlang</span>
                    Dimulai Di Sini
                </h1>
                
                <p class="text-lg text-text-muted max-w-lg leading-relaxed transition-all duration-700 delay-200 transform translate-y-4 opacity-0" :class="{ 'translate-y-0 opacity-100': shown }">
                    Program studi unggulan dengan kurikulum berbasis industri dan sistem pembayaran SKS yang fleksibel, transparan, dan modern.
                </p>
                
                <div class="flex flex-wrap gap-4 transition-all duration-700 delay-300 transform translate-y-4 opacity-0" :class="{ 'translate-y-0 opacity-100': shown }">
                    <a href="{{ route('login') }}" class="btn-luxury px-8 py-4 text-white font-bold rounded-full shadow-xl shadow-gold/20 hover:shadow-2xl transition-all transform hover:-translate-y-1">
                        Daftar Sekarang
                    </a>
                    <a href="#promo" class="px-8 py-4 bg-white text-primary font-bold rounded-full border border-gold/20 shadow-sm hover:shadow-md hover:border-gold/50 transition-all flex items-center gap-2 group">
                        <span>Lihat Promo</span>
                        <svg class="w-4 h-4 group-hover:translate-x-1 transition-transform text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"></path></svg>
                    </a>
                </div>
                
                <!-- Trust Chips -->
                <div class="flex items-center gap-6 pt-4 border-t border-gold/10 transition-all duration-700 delay-400 transform translate-y-4 opacity-0" :class="{ 'translate-y-0 opacity-100': shown }">
                    <div class="flex items-center gap-2">
                        <svg class="w-5 h-5 text-gold" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>
                        <span class="text-sm font-semibold text-primary">Terakreditasi A</span>
                    </div>
                    <div class="w-px h-8 bg-gold/30"></div>
                    <div class="flex items-center gap-2">
                        <svg class="w-5 h-5 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <span class="text-sm font-semibold text-primary">Layanan 24/7</span>
                    </div>
                </div>
            </div>
            
            <!-- Promo Spotlight (Right Side) -->
            <div class="relative hidden lg:block" x-data="{ hover: false }" @mouseenter="hover = true" @mouseleave="hover = false">
                <div class="absolute inset-0 bg-gold/20 rounded-[2rem] transform rotate-6 transition-transform duration-500" :class="{ 'rotate-12': hover }"></div>
                <div class="relative bg-white rounded-[2rem] shadow-2xl overflow-hidden border border-gold/20 transform transition-transform duration-500" :class="{ '-translate-y-2': hover }">
                    <!-- Useful Info Content -->
                    <div class="h-96 bg-gradient-to-br from-primary to-primary-dark p-8 flex flex-col justify-between relative overflow-hidden">
                        <div class="absolute top-0 right-0 w-64 h-64 bg-gold/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
                        
                        <div class="relative z-10">
                            <span class="px-3 py-1 bg-gold text-white text-xs font-bold uppercase rounded-full shadow-lg">Informasi Penting</span>
                            <h3 class="font-serif text-3xl text-white font-bold mt-4 leading-tight">
                                Jadwal <br> <span class="text-gold-gradient">Pendaftaran</span> <br> Mahasiswa Baru
                            </h3>
                            <p class="text-white/70 mt-2">Gelombang 2 Tahun Akademik 2026/2027.</p>
                        </div>
                        
                        <div class="space-y-4 mt-4 relative z-10">
                            <div class="flex items-center gap-3 text-white/90">
                                <div class="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center border border-white/10">
                                    <svg class="w-4 h-4 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
                                </div>
                                <div>
                                    <p class="text-xs text-gold uppercase font-bold">Periode Pendaftaran</p>
                                    <p class="font-bold">1 Maret - 30 April 2026</p>
                                </div>
                            </div>
                            <div class="flex items-center gap-3 text-white/90">
                                <div class="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center border border-white/10">
                                    <svg class="w-4 h-4 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path></svg>
                                </div>
                                <div>
                                    <p class="text-xs text-gold uppercase font-bold">Ujian Seleksi</p>
                                    <p class="font-bold">5 Mei 2026</p>
                                </div>
                            </div>
                        </div>
                        
                        <div class="mt-6 relative z-10">
                            <button class="w-full py-3 rounded-xl bg-white/10 backdrop-blur-sm border border-white/20 text-white font-bold hover:bg-gold hover:text-white hover:border-gold transition-all flex items-center justify-center gap-2">
                                Lihat Detail Jadwal
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path></svg>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- 1.3 Promo & Pendaftaran -->
<section id="promo" class="py-24 bg-white relative">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16">
            <span class="text-gold font-bold tracking-wider text-sm uppercase">Pilihan Program</span>
            <h2 class="font-serif text-4xl font-bold text-primary mt-2">Program Studi & Jalur Masuk</h2>
            <div class="w-20 h-1 bg-gold mx-auto mt-6 rounded-full"></div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
            <!-- Card 1: Reguler -->
            <div class="group relative bg-surface rounded-2xl p-8 border border-green-100 hover:border-gold/30 transition-all duration-300 hover:shadow-2xl hover:-translate-y-2">
                <div class="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
                    <svg class="w-24 h-24 text-primary" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2L2 7l10 5 10-5-10-5zm0 9l2.5-1.25L12 8.5l-2.5 1.25L12 11zm0 2.5l-5-2.5-5 2.5L12 22l10-8.5-5-2.5-5 2.5z"/></svg>
                </div>
                <h3 class="font-serif text-2xl font-bold text-primary mb-2">Reguler</h3>
                <p class="text-green-600 text-sm mb-6">Program sarjana penuh waktu untuk lulusan SMA/SMK sederajat.</p>
                
                <ul class="space-y-3 mb-8">
                    <li class="flex items-center gap-3 text-sm text-green-800">
                        <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        8 Semester
                    </li>
                    <li class="flex items-center gap-3 text-sm text-green-800">
                        <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        Full On-site Learning
                    </li>
                    <li class="flex items-center gap-3 text-sm text-green-800">
                        <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        Beasiswa Prestasi
                    </li>
                </ul>
                
                <div class="pt-6 border-t border-green-200">
                    <button class="w-full py-3 rounded-xl border border-primary text-primary font-bold hover:bg-primary hover:text-white transition-colors">
                        Lihat Detail
                    </button>
                </div>
            </div>

            <!-- Card 2: RPL (Featured) -->
            <div class="group relative bg-primary text-white rounded-2xl p-8 border border-primary shadow-2xl transform md:-translate-y-4">
                <div class="absolute top-4 right-4">
                    <span class="bg-gold text-primary text-xs font-bold px-3 py-1 rounded-full">TERPOPULER</span>
                </div>
                <h3 class="font-serif text-2xl font-bold text-white mb-2">RPL / Karyawan</h3>
                <p class="text-green-200 text-sm mb-6">Rekognisi Pembelajaran Lampau untuk profesional.</p>
                
                <ul class="space-y-3 mb-8">
                    <li class="flex items-center gap-3 text-sm text-green-100">
                        <svg class="w-5 h-5 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        Bisa lulus 2-4 Semester
                    </li>
                    <li class="flex items-center gap-3 text-sm text-green-100">
                        <svg class="w-5 h-5 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        Hybrid Learning (Weekend)
                    </li>
                    <li class="flex items-center gap-3 text-sm text-green-100">
                        <svg class="w-5 h-5 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        Konversi Pengalaman Kerja
                    </li>
                </ul>
                
                <div class="pt-6 border-t border-green-700">
                    <button class="w-full py-3 rounded-xl bg-gold text-primary font-bold hover:bg-white transition-colors">
                        Daftar Sekarang
                    </button>
                </div>
            </div>

            <!-- Card 3: Fast Track -->
            <div class="group relative bg-surface rounded-2xl p-8 border border-green-100 hover:border-gold/30 transition-all duration-300 hover:shadow-2xl hover:-translate-y-2">
                <h3 class="font-serif text-2xl font-bold text-primary mb-2">Fast Track</h3>
                <p class="text-green-600 text-sm mb-6">Program percepatan S1 + S2 dalam 5 tahun.</p>
                
                <ul class="space-y-3 mb-8">
                    <li class="flex items-center gap-3 text-sm text-green-800">
                        <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        Hemat 1 Tahun
                    </li>
                    <li class="flex items-center gap-3 text-sm text-green-800">
                        <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        Dual Degree
                    </li>
                    <li class="flex items-center gap-3 text-sm text-green-800">
                        <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        International Exposure
                    </li>
                </ul>
                
                <div class="pt-6 border-t border-green-200">
                    <button class="w-full py-3 rounded-xl border border-primary text-primary font-bold hover:bg-primary hover:text-white transition-colors">
                        Lihat Detail
                    </button>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- 1.4 Kata Pengantar -->
<section id="pengantar" class="py-24 bg-surface-dark overflow-hidden">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
            <div class="relative">
                <div class="absolute inset-0 bg-gold transform translate-x-4 translate-y-4 rounded-2xl"></div>
                <img src="https://images.unsplash.com/photo-1523050854058-8df90110c9f1?ixlib=rb-1.2.1&auto=format&fit=crop&w=1350&q=80" alt="Kampus" class="relative rounded-2xl shadow-xl grayscale hover:grayscale-0 transition-all duration-500 w-full h-auto object-cover">
            </div>
            
            <div>
                <span class="text-gold font-bold tracking-wider text-sm uppercase">Tentang Program</span>
                <h2 class="font-serif text-4xl font-bold text-primary mt-2 mb-6">Mencetak Profesional <br> Siap Kerja</h2>
                <p class="text-green-700 leading-relaxed mb-8">
                    Program Studi kami dirancang dengan pendekatan praktis yang mengutamakan keterampilan dunia nyata. Didukung oleh dosen praktisi dan fasilitas modern, kami berkomitmen melahirkan lulusan yang tidak hanya cerdas secara akademis, tetapi juga kompeten secara profesional.
                </p>
                
                <div class="grid grid-cols-3 gap-6">
                    <div class="text-center">
                        <div class="font-serif text-4xl font-bold text-primary">1.2k+</div>
                        <div class="text-xs text-green-600 uppercase tracking-wide mt-1">Mahasiswa</div>
                    </div>
                    <div class="text-center border-l border-green-200">
                        <div class="font-serif text-4xl font-bold text-primary">98%</div>
                        <div class="text-xs text-green-600 uppercase tracking-wide mt-1">Lulus Kerja</div>
                    </div>
                    <div class="text-center border-l border-green-200">
                        <div class="font-serif text-4xl font-bold text-primary">A</div>
                        <div class="text-xs text-green-600 uppercase tracking-wide mt-1">Akreditasi</div>
                    </div>
                </div>
                
                <div class="mt-10">
                    <a href="#" class="inline-flex items-center text-primary font-bold hover:text-gold transition-colors">
                        Baca Selengkapnya
                        <svg class="w-5 h-5 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"></path></svg>
                    </a>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- 1.5 Visi & Misi (Tabs) -->
<section id="visi-misi" class="py-24 bg-white" x-data="{ tab: 'visi' }">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="font-serif text-4xl font-bold text-primary mb-12">Arah & Tujuan Kami</h2>
        
        <div class="flex justify-center mb-12">
            <div class="bg-surface p-1 rounded-full inline-flex">
                <button @click="tab = 'visi'" 
                        :class="{ 'bg-primary text-white shadow-lg': tab === 'visi', 'text-green-600 hover:text-primary': tab !== 'visi' }"
                        class="px-8 py-3 rounded-full font-bold transition-all duration-300">
                    Visi
                </button>
                <button @click="tab = 'misi'" 
                        :class="{ 'bg-primary text-white shadow-lg': tab === 'misi', 'text-green-600 hover:text-primary': tab !== 'misi' }"
                        class="px-8 py-3 rounded-full font-bold transition-all duration-300">
                    Misi
                </button>
            </div>
        </div>
        
        <div class="relative min-h-[200px]">
            <!-- Visi Content -->
            <div x-show="tab === 'visi'" 
                 x-transition:enter="transition ease-out duration-300"
                 x-transition:enter-start="opacity-0 transform scale-95"
                 x-transition:enter-end="opacity-100 transform scale-100"
                 class="bg-surface p-10 rounded-3xl border border-green-100 shadow-sm">
                <p class="font-serif text-2xl md:text-3xl text-primary leading-relaxed italic">
                    "Menjadi program studi unggulan tingkat nasional yang menghasilkan lulusan profesional, inovatif, dan berdaya saing global pada tahun 2030."
                </p>
            </div>
            
            <!-- Misi Content -->
            <div x-show="tab === 'misi'" style="display: none;"
                 x-transition:enter="transition ease-out duration-300"
                 x-transition:enter-start="opacity-0 transform scale-95"
                 x-transition:enter-end="opacity-100 transform scale-100"
                 class="bg-surface p-10 rounded-3xl border border-green-100 shadow-sm text-left">
                <ul class="space-y-4">
                    <li class="flex items-start gap-4">
                        <div class="w-8 h-8 rounded-full bg-gold/20 flex items-center justify-center text-gold-dark font-bold shrink-0">1</div>
                        <p class="text-lg text-slate-700">Menyelenggarakan pendidikan berkualitas yang relevan dengan kebutuhan industri.</p>
                    </li>
                    <li class="flex items-start gap-4">
                        <div class="w-8 h-8 rounded-full bg-gold/20 flex items-center justify-center text-gold-dark font-bold shrink-0">2</div>
                        <p class="text-lg text-slate-700">Mengembangkan penelitian inovatif yang berkontribusi pada kemajuan ilmu pengetahuan.</p>
                    </li>
                    <li class="flex items-start gap-4">
                        <div class="w-8 h-8 rounded-full bg-gold/20 flex items-center justify-center text-gold-dark font-bold shrink-0">3</div>
                        <p class="text-lg text-slate-700">Melaksanakan pengabdian kepada masyarakat berbasis teknologi tepat guna.</p>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</section>

<!-- 1.6 Pengurus Section -->
<section id="pengurus" class="py-24 bg-surface-dark overflow-hidden">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16">
            <span class="text-gold font-bold tracking-wider text-sm uppercase">Tim Kami</span>
            <h2 class="font-serif text-4xl font-bold text-primary mt-2">Pengurus Program Studi</h2>
            <div class="w-20 h-1 bg-gold mx-auto mt-6 rounded-full"></div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            <!-- Card 1: Ketua Prodi -->
            <div class="group bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl transition-all duration-300 text-center border border-slate-100 relative overflow-hidden">
                <div class="absolute top-0 left-0 w-full h-1 bg-gold"></div>
                <div class="w-24 h-24 mx-auto mb-6 relative">
                    <div class="absolute inset-0 bg-gold rounded-full blur-lg opacity-20 group-hover:opacity-40 transition-opacity"></div>
                    <img src="https://ui-avatars.com/api/?name=Dr+Budi&background=0F172A&color=fff" alt="Dr. Budi" class="w-full h-full rounded-full object-cover relative z-10 border-4 border-white shadow-md">
                </div>
                <h3 class="font-serif text-xl font-bold text-primary">Dr. Budi Santoso</h3>
                <p class="text-slate-500 text-sm mb-3">Ketua Program Studi</p>
                <span class="px-3 py-1 bg-primary/5 text-primary text-xs font-bold rounded-full">Kaprodi</span>
            </div>

            <!-- Card 2: Sekretaris -->
            <div class="group bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl transition-all duration-300 text-center border border-slate-100 relative overflow-hidden">
                <div class="w-24 h-24 mx-auto mb-6 relative">
                    <div class="absolute inset-0 bg-gold rounded-full blur-lg opacity-20 group-hover:opacity-40 transition-opacity"></div>
                    <img src="https://ui-avatars.com/api/?name=Siti+Amina&background=0F172A&color=fff" alt="Siti Aminah" class="w-full h-full rounded-full object-cover relative z-10 border-4 border-white shadow-md">
                </div>
                <h3 class="font-serif text-xl font-bold text-primary">Siti Aminah, M.Kom</h3>
                <p class="text-slate-500 text-sm mb-3">Sekretaris Prodi</p>
                <span class="px-3 py-1 bg-primary/5 text-primary text-xs font-bold rounded-full">Sekprodi</span>
            </div>

            <!-- Card 3: Admin Keuangan -->
            <div class="group bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl transition-all duration-300 text-center border border-slate-100 relative overflow-hidden">
                <div class="w-24 h-24 mx-auto mb-6 relative">
                    <div class="absolute inset-0 bg-gold rounded-full blur-lg opacity-20 group-hover:opacity-40 transition-opacity"></div>
                    <img src="https://ui-avatars.com/api/?name=Rina+W&background=0F172A&color=fff" alt="Rina W" class="w-full h-full rounded-full object-cover relative z-10 border-4 border-white shadow-md">
                </div>
                <h3 class="font-serif text-xl font-bold text-primary">Rina Wijaya, S.E.</h3>
                <p class="text-slate-500 text-sm mb-3">Admin Keuangan</p>
                <span class="px-3 py-1 bg-gold/10 text-gold-dark text-xs font-bold rounded-full">Finance</span>
            </div>

            <!-- Card 4: Staf Akademik -->
            <div class="group bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl transition-all duration-300 text-center border border-slate-100 relative overflow-hidden">
                <div class="w-24 h-24 mx-auto mb-6 relative">
                    <div class="absolute inset-0 bg-gold rounded-full blur-lg opacity-20 group-hover:opacity-40 transition-opacity"></div>
                    <img src="https://ui-avatars.com/api/?name=Andi+P&background=0F172A&color=fff" alt="Andi P" class="w-full h-full rounded-full object-cover relative z-10 border-4 border-white shadow-md">
                </div>
                <h3 class="font-serif text-xl font-bold text-primary">Andi Pratama</h3>
                <p class="text-slate-500 text-sm mb-3">Staf Akademik</p>
                <span class="px-3 py-1 bg-primary/5 text-primary text-xs font-bold rounded-full">Admin</span>
            </div>
        </div>
    </div>
</section>

<!-- 1.7 Kurikulum Section -->
<section id="kurikulum" class="py-24 bg-white relative">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-16 items-start">
            <div>
                <span class="text-gold font-bold tracking-wider text-sm uppercase">Kurikulum</span>
                <h2 class="font-serif text-4xl font-bold text-primary mt-2 mb-6">Struktur Pembelajaran <br> Komprehensif</h2>
                <p class="text-slate-600 leading-relaxed mb-8">
                    Kurikulum kami disusun bersama pakar industri untuk memastikan relevansi skill yang Anda pelajari. Mulai dari dasar hingga spesialisasi tingkat lanjut.
                </p>
                <a href="#" class="inline-flex items-center px-6 py-3 bg-white border border-slate-200 rounded-xl font-bold text-primary shadow-sm hover:shadow-md hover:border-gold transition-all">
                    <svg class="w-5 h-5 mr-2 text-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                    Unduh Kurikulum PDF
                </a>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- Semester 1 -->
                <div class="bg-surface p-6 rounded-2xl border border-slate-100 hover:border-gold/50 transition-colors group">
                    <div class="flex items-center justify-between mb-4">
                        <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Semester 1</span>
                        <div class="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center font-serif font-bold text-sm">1</div>
                    </div>
                    <h3 class="font-bold text-primary text-lg mb-2 group-hover:text-gold transition-colors">Fondasi Dasar</h3>
                    <p class="text-sm text-slate-500">Algoritma, Matematika Diskrit, Pengantar TI.</p>
                </div>

                <!-- Semester 2 -->
                <div class="bg-surface p-6 rounded-2xl border border-slate-100 hover:border-gold/50 transition-colors group">
                    <div class="flex items-center justify-between mb-4">
                        <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Semester 2</span>
                        <div class="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center font-serif font-bold text-sm">2</div>
                    </div>
                    <h3 class="font-bold text-primary text-lg mb-2 group-hover:text-gold transition-colors">Pemrograman</h3>
                    <p class="text-sm text-slate-500">Struktur Data, Web Basic, Basis Data.</p>
                </div>

                <!-- Semester 3 -->
                <div class="bg-surface p-6 rounded-2xl border border-slate-100 hover:border-gold/50 transition-colors group">
                    <div class="flex items-center justify-between mb-4">
                        <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Semester 3</span>
                        <div class="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center font-serif font-bold text-sm">3</div>
                    </div>
                    <h3 class="font-bold text-primary text-lg mb-2 group-hover:text-gold transition-colors">Pengembangan</h3>
                    <p class="text-sm text-slate-500">OOP, Framework Laravel, UI/UX Design.</p>
                </div>

                <!-- Semester 4 -->
                <div class="bg-surface p-6 rounded-2xl border border-slate-100 hover:border-gold/50 transition-colors group">
                    <div class="flex items-center justify-between mb-4">
                        <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Semester 4</span>
                        <div class="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center font-serif font-bold text-sm">4</div>
                    </div>
                    <h3 class="font-bold text-primary text-lg mb-2 group-hover:text-gold transition-colors">Spesialisasi</h3>
                    <p class="text-sm text-slate-500">AI/ML, Mobile Dev, Cloud Computing.</p>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- 1.8 CTA Strip -->
<section class="py-16 bg-primary relative overflow-hidden">
    <div class="absolute top-0 right-0 w-96 h-96 bg-gold/10 rounded-full blur-3xl translate-x-1/2 -translate-y-1/2"></div>
    <div class="absolute bottom-0 left-0 w-64 h-64 bg-white/5 rounded-full blur-2xl -translate-x-1/2 translate-y-1/2"></div>
    
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center">
        <h2 class="font-serif text-3xl md:text-4xl font-bold text-white mb-6">Butuh Bantuan?</h2>
        <p class="text-slate-300 text-lg mb-8 max-w-2xl mx-auto">Tim admin kami siap membantu Anda pada jam kerja. Jangan ragu untuk bertanya mengenai pendaftaran atau pembayaran.</p>
        
        <div class="flex flex-wrap justify-center gap-4">
            <a href="#" class="px-8 py-4 bg-gold text-primary font-bold rounded-full hover:bg-white transition-colors shadow-lg shadow-gold/20">
                Chat Admin (WA)
            </a>
            <a href="{{ route('login') }}" class="px-8 py-4 bg-transparent border border-white/30 text-white font-bold rounded-full hover:bg-white/10 transition-colors">
                Login Mahasiswa
            </a>
        </div>
    </div>
</section>
@endsection
