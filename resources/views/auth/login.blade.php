<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Login - Magister Pendidikan Agama Islam</title>
    @vite(['resources/css/app.css', 'resources/js/app.js'])
</head>
<body class="bg-surface font-sans text-text-main antialiased min-h-screen flex">

    <!-- Left Side: Luxury Branding -->
    <div class="hidden lg:flex w-1/2 sidebar-gradient relative overflow-hidden items-center justify-center border-r border-gold/20 sticky top-0 h-screen">
        <!-- Abstract Background -->
        <div class="absolute inset-0 opacity-20">
            <div class="absolute top-0 left-0 w-[800px] h-[800px] bg-gold rounded-full blur-[150px] -translate-x-1/2 -translate-y-1/2 mix-blend-overlay"></div>
            <div class="absolute bottom-0 right-0 w-[600px] h-[600px] bg-blue-600 rounded-full blur-[120px] translate-x-1/2 translate-y-1/2 mix-blend-overlay"></div>
        </div>
        
        <!-- Content -->
        <div class="relative z-10 text-center px-12">
            <div class="w-32 h-32 bg-white rounded-full mx-auto mb-8 flex items-center justify-center shadow-2xl p-4 overflow-hidden border-4 border-gold/30">
                <img src="{{ asset('images/unissula-logo.png') }}" alt="Logo Unissula" class="w-full h-full object-contain">
            </div>
            
            <h1 class="font-serif text-4xl font-bold text-white mb-6 leading-tight">
                Magister Pendidikan <br> <span class="text-gold-gradient">Agama Islam</span>
            </h1>
            <p class="text-green-100 text-lg leading-relaxed max-w-md mx-auto font-light">
                Platform pembayaran kuliah digital yang aman, transparan, dan terintegrasi untuk masa depan pendidikan yang lebih baik.
            </p>

            <!-- Trust Badges -->
            <div class="mt-12 flex justify-center gap-6 opacity-70">
                <div class="flex items-center gap-2">
                    <div class="w-2 h-2 rounded-full bg-green-400 shadow-[0_0_10px_rgba(74,222,128,0.5)]"></div>
                    <span class="text-white text-sm tracking-wide">Secure Payment</span>
                </div>
                <div class="flex items-center gap-2">
                    <div class="w-2 h-2 rounded-full bg-gold shadow-[0_0_10px_rgba(212,175,55,0.5)]"></div>
                    <span class="text-white text-sm tracking-wide">Real-time Verification</span>
                </div>
            </div>
        </div>

        <!-- Glass Effect Overlay -->
        <div class="absolute inset-0 bg-gradient-to-t from-primary/80 to-transparent"></div>
    </div>

    <!-- Right Side: Login Form -->
    <div class="w-full lg:w-1/2 flex items-center justify-center p-8 relative bg-surface">
        <!-- Mobile Background Decoration -->
        <div class="absolute top-0 right-0 w-64 h-64 bg-gold/10 rounded-full blur-3xl translate-x-1/2 -translate-y-1/2 lg:hidden"></div>
        
        <div class="w-full max-w-md">
            <!-- Mobile Header -->
            <div class="text-center mb-10 lg:hidden">
                <div class="w-20 h-20 bg-white rounded-full mx-auto mb-4 flex items-center justify-center shadow-lg p-2 border border-gold/30 overflow-hidden">
                    <img src="{{ asset('images/unissula-logo.png') }}" alt="Logo Unissula" class="w-full h-full object-contain">
                </div>
                <h1 class="font-serif text-2xl font-bold text-primary">Magister Pendidikan <br> <span class="text-gold-gradient">Agama Islam</span></h1>
                <p class="text-text-muted text-sm mt-2">Masuk untuk melanjutkan pembayaran</p>
            </div>

            <!-- Desktop Header -->
            <div class="mb-10 hidden lg:block">
                <a href="{{ url('/') }}" class="inline-flex items-center text-sm font-bold text-primary hover:text-gold mb-6 transition-colors">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                    Kembali ke Beranda
                </a>
                <h2 class="font-serif text-4xl font-bold text-primary mb-2">Selamat Datang</h2>
                <p class="text-text-muted">Silakan masukkan kredensial Anda untuk mengakses dashboard.</p>
            </div>

            @if ($errors->any())
                <div class="mb-6 p-4 rounded-xl bg-red-50 border border-red-100 flex items-start gap-3">
                    <svg class="w-5 h-5 text-red-500 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    <div>
                        <h4 class="font-bold text-red-800 text-sm">Gagal Masuk</h4>
                        <ul class="text-sm text-red-600 mt-1 list-disc list-inside">
                            @foreach ($errors->all() as $error)
                                <li>{{ $error }}</li>
                            @endforeach
                        </ul>
                    </div>
                </div>
            @endif

            <form method="POST" action="{{ route('login') }}" class="space-y-6">
                @csrf
                
                <!-- Email/NIM Input -->
                <div class="group">
                    <label for="email" class="block text-sm font-bold text-primary mb-2">Email atau NIM</label>
                    <div class="relative">
                        <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                            <svg class="h-5 w-5 text-gray-400 group-focus-within:text-gold transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.207"></path></svg>
                        </div>
                        <input type="text" name="email" id="email" required autofocus 
                            class="block w-full pl-11 pr-4 py-3 input-luxury rounded-xl text-primary placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gold/50 focus:border-gold transition-all shadow-sm"
                            placeholder="NIM atau Email"
                            value="{{ old('email') }}">
                    </div>
                </div>

                <!-- Password Input -->
                <div class="group">
                    <label for="password" class="block text-sm font-bold text-primary mb-2">Password</label>
                    <div class="relative">
                        <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                            <svg class="h-5 w-5 text-gray-400 group-focus-within:text-gold transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                        </div>
                        <input type="password" name="password" id="password" required 
                            class="block w-full pl-11 pr-4 py-3 input-luxury rounded-xl text-primary placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gold/50 focus:border-gold transition-all shadow-sm"
                            placeholder="Masukkan password Anda">
                    </div>
                </div>

                <!-- Remember Me & Forgot Password -->
                <div class="flex items-center justify-between">
                    <label class="flex items-center cursor-pointer">
                        <input type="checkbox" name="remember" class="w-4 h-4 text-gold border-gray-300 rounded focus:ring-gold/50">
                        <span class="ml-2 text-sm text-text-muted">Ingat Saya</span>
                    </label>
                    <a href="#" class="text-sm text-gold hover:text-gold-dark font-semibold transition-colors">Lupa Password?</a>
                </div>

                <!-- Submit Button -->
                <button type="submit" 
                    class="w-full py-4 px-6 btn-luxury text-white font-bold rounded-xl shadow-lg transform hover:-translate-y-0.5 transition-all duration-200">
                    Masuk Sekarang
                </button>
            </form>

            <!-- Footer -->
            <div class="mt-8 text-center">
                <p class="text-text-muted text-sm">
                    &copy; {{ date('Y') }} Magister Pendidikan Agama Islam. All rights reserved.
                </p>
            </div>
        </div>
    </div>


</body>
</html>
