@extends('layouts.admin')

@section('content')
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<div class="space-y-10">

    <!-- Section: RPL Stats -->
    <div class="animate-fade-in-up">
        <div class="flex items-center gap-4 mb-6">
            <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center text-blue-600 shadow-sm border border-blue-100">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg>
            </div>
            <div>
                <h2 class="text-xl font-bold text-gray-800">Statistik Mahasiswa <span class="text-blue-600">RPL</span></h2>
                <p class="text-sm text-gray-500">Ringkasan data keuangan dan mahasiswa program RPL</p>
            </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <!-- Active Students -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Mahasiswa Aktif</p>
                        <h3 class="text-2xl font-bold text-gray-800">{{ $rplStats['active_students'] }}</h3>
                    </div>
                    <div class="p-2 bg-blue-50 rounded-lg text-blue-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                    </div>
                </div>
            </div>

            <!-- Total Tagihan -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Total Tagihan</p>
                        <h3 class="text-2xl font-bold text-gray-800">Rp{{ number_format($rplStats['total_tagihan'], 0, ',', '.') }}</h3>
                    </div>
                    <div class="p-2 bg-yellow-50 rounded-lg text-yellow-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
            </div>

            <!-- Sudah Lunas -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Sudah Lunas</p>
                        <h3 class="text-2xl font-bold text-gray-800">{{ $rplStats['sudah_lunas'] }}</h3>
                    </div>
                    <div class="p-2 bg-green-50 rounded-lg text-green-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
            </div>

            <!-- Menunggu Verifikasi -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Menunggu Verifikasi</p>
                        <h3 class="text-2xl font-bold text-gray-800">{{ $rplStats['waiting_verification'] }}</h3>
                    </div>
                    <div class="p-2 bg-red-50 rounded-lg text-red-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
            </div>

            <!-- Siap Munaqosah -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Siap Munaqosah</p>
                        <h3 class="text-2xl font-bold text-gray-800">{{ $rplStats['eligible_munaqosah'] }}</h3>
                    </div>
                    <div class="p-2 bg-emerald-50 rounded-lg text-emerald-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M12 14l9-5-9-5-9 5 9 5z"></path><path d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z"></path></svg>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="border-t border-gray-100"></div>

    <!-- Section: Reguler Stats -->
    <div class="animate-fade-in-up" style="animation-delay: 0.1s;">
        <div class="flex items-center gap-4 mb-6">
            <div class="w-10 h-10 rounded-lg bg-emerald-50 flex items-center justify-center text-emerald-600 shadow-sm border border-emerald-100">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
            </div>
            <div>
                <h2 class="text-xl font-bold text-gray-800">Statistik Mahasiswa <span class="text-emerald-600">Reguler</span></h2>
                <p class="text-sm text-gray-500">Ringkasan data keuangan dan mahasiswa program Reguler</p>
            </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <!-- Active Students -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Mahasiswa Aktif</p>
                        <h3 class="text-2xl font-bold text-gray-800">{{ $regulerStats['active_students'] }}</h3>
                    </div>
                    <div class="p-2 bg-emerald-50 rounded-lg text-emerald-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                    </div>
                </div>
            </div>
            
            <!-- Total Tagihan -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Total Tagihan</p>
                        <h3 class="text-2xl font-bold text-gray-800">Rp{{ number_format($regulerStats['total_tagihan'], 0, ',', '.') }}</h3>
                    </div>
                    <div class="p-2 bg-yellow-50 rounded-lg text-yellow-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
            </div>

            <!-- Sudah Lunas -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Sudah Lunas</p>
                        <h3 class="text-2xl font-bold text-gray-800">{{ $regulerStats['sudah_lunas'] }}</h3>
                    </div>
                    <div class="p-2 bg-green-50 rounded-lg text-green-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
            </div>

            <!-- Menunggu Verifikasi -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Menunggu Verifikasi</p>
                        <h3 class="text-2xl font-bold text-gray-800">{{ $regulerStats['waiting_verification'] }}</h3>
                    </div>
                    <div class="p-2 bg-red-50 rounded-lg text-red-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
            </div>

            <!-- Siap Munaqosah -->
            <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200">
                <div class="flex justify-between items-start">
                    <div>
                        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Siap Munaqosah</p>
                        <h3 class="text-2xl font-bold text-gray-800">{{ $regulerStats['eligible_munaqosah'] }}</h3>
                    </div>
                    <div class="p-2 bg-emerald-50 rounded-lg text-emerald-600">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M12 14l9-5-9-5-9 5 9 5z"></path><path d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z"></path></svg>
                    </div>
                </div>
            </div>
        </div>
    </div>


    <div class="border-t border-gray-200 my-12"></div>

    <!-- Charts Row -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Trend Chart -->
        <div class="lg:col-span-2 bg-white rounded-xl p-6 border border-gray-100 shadow-sm">
            <h3 class="text-xl font-bold text-gray-800 mb-1">Tren Pendapatan</h3>
            <p class="text-gray-500 text-sm mb-6">Total pendapatan 6 bulan terakhir</p>
            <div class="h-64">
                <canvas id="revenueTrendChart"></canvas>
            </div>
        </div>

        <!-- Status Ratio -->
        <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm">
            <div class="flex justify-between items-center mb-6">
                <h3 class="text-xl font-bold text-gray-800">Status Pembayaran</h3>
                <div class="w-8 h-8 rounded-full bg-blue-50 flex items-center justify-center text-blue-500">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z"></path></svg>
                </div>
            </div>
            <p class="text-gray-500 text-sm mb-4">Rasio per tipe mahasiswa</p>
            
            <div class="grid grid-cols-2 gap-4">
                <div class="text-center">
                    <p class="text-xs font-bold mb-2 text-gray-700">Mahasiswa RPL</p>
                    <div class="relative w-24 h-24 mx-auto">
                        <canvas id="rplRatioChart"></canvas>
                    </div>
                    <div class="mt-2">
                        <p class="text-xs text-gray-500">Total Lunas: <span class="font-bold text-blue-600">{{ $rplStats['ratio']['lunas'] }}</span></p>
                        <p class="text-xs text-gray-500">Total Uang: <span class="font-bold text-green-600">Rp{{ number_format($rplStats['ratio']['total_uang']/1000000, 1, ',', '.') }}jt</span></p>
                    </div>
                </div>
                <div class="text-center">
                    <p class="text-xs font-bold mb-2 text-gray-700">Mahasiswa Reguler</p>
                    <div class="relative w-24 h-24 mx-auto">
                        <canvas id="regulerRatioChart"></canvas>
                    </div>
                     <div class="mt-2">
                        <p class="text-xs text-gray-500">Total Lunas: <span class="font-bold text-emerald-600">{{ $regulerStats['ratio']['lunas'] }}</span></p>
                        <p class="text-xs text-gray-500">Total Uang: <span class="font-bold text-green-600">Rp{{ number_format($regulerStats['ratio']['total_uang']/1000000, 1, ',', '.') }}jt</span></p>
                    </div>
                </div>
            </div>
             <div class="flex justify-center gap-4 mt-6">
                <div class="flex items-center gap-1 text-xs text-gray-600">
                    <span class="w-2 h-2 rounded-full bg-emerald-500"></span> Lunas
                </div>
                <div class="flex items-center gap-1 text-xs text-gray-600">
                    <span class="w-2 h-2 rounded-full bg-amber-400"></span> Menunggu
                </div>
                 <div class="flex items-center gap-1 text-xs text-gray-600">
                    <span class="w-2 h-2 rounded-full bg-red-500"></span> Belum
                </div>
            </div>
        </div>
    </div>

    <div class="border-t border-gray-100 my-12"></div>

    <!-- Student Lists -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
        <!-- RPL List -->
        <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm">
             <div class="flex justify-between items-center mb-6">
                 <div>
                    <h3 class="text-lg font-bold text-gray-800 flex items-center gap-2">
                        <span class="w-2 h-2 rounded-full bg-blue-500"></span>
                        Status Tagihan RPL
                    </h3>
                    <p class="text-gray-500 text-xs mt-1">5 Transaksi terbaru mahasiswa RPL</p>
                 </div>
                 <a href="{{ route('admin.payments.rpl') }}" class="text-xs font-bold text-blue-600 hover:text-blue-700">Lihat Semua</a>
             </div>
             
             <div class="space-y-3">
                 @forelse(collect($rplStats['students'])->take(5) as $student)
                 <div class="flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:bg-gray-50 transition-colors">
                     <div class="flex items-center gap-3">
                         <div class="w-8 h-8 rounded-full bg-blue-50 flex items-center justify-center text-blue-600 font-bold text-xs">
                             {{ substr($student['name'], 0, 1) }}
                         </div>
                         <div>
                             <p class="font-bold text-gray-800 text-sm truncate max-w-[120px]">{{ $student['name'] }}</p>
                             <p class="text-xs text-gray-500">{{ $student['nim'] }}</p>
                         </div>
                     </div>
                     <div class="flex items-center gap-2">
                         @if($student['status'] == 'LUNAS')
                            <span class="px-2 py-1 rounded text-[10px] font-bold bg-green-50 text-green-600 uppercase">Lunas</span>
                         @elseif($student['status'] == 'MENUNGGU')
                             <span class="px-2 py-1 rounded text-[10px] font-bold bg-amber-50 text-amber-600 uppercase">Proses</span>
                         @else
                             <span class="px-2 py-1 rounded text-[10px] font-bold bg-red-50 text-red-600 uppercase">Belum</span>
                         @endif
                     </div>
                 </div>
                 @empty
                 <p class="text-gray-400 text-sm text-center py-4">Belum ada data mahasiswa.</p>
                 @endforelse
             </div>
        </div>

         <!-- Reguler List -->
        <div class="bg-white rounded-xl p-6 border border-gray-100 shadow-sm">
             <div class="flex justify-between items-center mb-6">
                 <div>
                    <h3 class="text-lg font-bold text-gray-800 flex items-center gap-2">
                        <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
                        Status Tagihan Reguler
                    </h3>
                    <p class="text-gray-500 text-xs mt-1">5 Transaksi terbaru mahasiswa Reguler</p>
                 </div>
                 <a href="{{ route('admin.payments.reguler') }}" class="text-xs font-bold text-emerald-600 hover:text-emerald-700">Lihat Semua</a>
             </div>
             
             <div class="space-y-3">
                 @forelse(collect($regulerStats['students'])->take(5) as $student)
                 <div class="flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:bg-gray-50 transition-colors">
                     <div class="flex items-center gap-3">
                         <div class="w-8 h-8 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600 font-bold text-xs">
                             {{ substr($student['name'], 0, 1) }}
                         </div>
                         <div>
                             <p class="font-bold text-gray-800 text-sm truncate max-w-[120px]">{{ $student['name'] }}</p>
                             <p class="text-xs text-gray-500">{{ $student['nim'] }}</p>
                         </div>
                     </div>
                     <div class="flex items-center gap-2">
                         @if($student['status'] == 'LUNAS')
                            <span class="px-2 py-1 rounded text-[10px] font-bold bg-green-50 text-green-600 uppercase">Lunas</span>
                         @elseif($student['status'] == 'MENUNGGU')
                             <span class="px-2 py-1 rounded text-[10px] font-bold bg-amber-50 text-amber-600 uppercase">Proses</span>
                         @else
                             <span class="px-2 py-1 rounded text-[10px] font-bold bg-red-50 text-red-600 uppercase">Belum</span>
                         @endif
                     </div>
                 </div>
                 @empty
                 <p class="text-gray-400 text-sm text-center py-4">Belum ada data mahasiswa.</p>
                 @endforelse
             </div>
        </div>
    </div>

</div>

<script>
    document.addEventListener('DOMContentLoaded', function() {
        // Trend Chart
        const rplData = {!! json_encode($rplTrend) !!};
        const regulerData = {!! json_encode($regulerTrend) !!};
        
        // Calculate dynamic max value
        const allData = [...rplData, ...regulerData];
        const maxValue = Math.max(...allData);
        // If max value is 0 (empty data), use 1 million fallback to prevent scientific notation bug
        // If max value > 0, use undefined to let Chart.js auto-scale to the data
        const dynamicSuggestedMax = maxValue > 0 ? undefined : 1000000;

        const ctxTrend = document.getElementById('revenueTrendChart').getContext('2d');
        new Chart(ctxTrend, {
            type: 'line',
            data: {
                labels: {!! json_encode($months) !!},
                datasets: [
                    {
                        label: 'Mahasiswa RPL',
                        data: rplData,
                        borderColor: '#2563EB', // Blue-600
                        backgroundColor: 'rgba(37, 99, 235, 0.1)',
                        tension: 0.4,
                        fill: true
                    },
                    {
                        label: 'Mahasiswa Reguler',
                        data: regulerData,
                        borderColor: '#10B981', // Emerald-500
                        backgroundColor: 'rgba(16, 185, 129, 0.1)',
                        tension: 0.4,
                        fill: true
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        align: 'end',
                        labels: {
                            font: {
                                family: 'ui-sans-serif, system-ui, sans-serif'
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        suggestedMax: dynamicSuggestedMax,
                        grid: {
                            color: 'rgba(0, 0, 0, 0.05)'
                        },
                        ticks: {
                            callback: function(value) {
                                if (value === 0) return 'Rp0';
                                let inMillions = value / 1000000;
                                return 'Rp' + parseFloat(inMillions.toFixed(1)).toLocaleString('id-ID') + 'jt';
                            },
                            font: {
                                family: 'ui-sans-serif, system-ui, sans-serif'
                            }
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            font: {
                                family: 'ui-sans-serif, system-ui, sans-serif'
                            }
                        }
                    }
                }
            }
        });

        // Donut Config
        const donutOptions = {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '75%',
            plugins: {
                legend: {
                    display: false
                }
            }
        };

        // RPL Donut
        new Chart(document.getElementById('rplRatioChart').getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: ['Lunas', 'Menunggu', 'Belum'],
                datasets: [{
                    data: [
                        {{ $rplStats['ratio']['lunas'] }}, 
                        {{ $rplStats['ratio']['menunggu'] }}, 
                        {{ $rplStats['ratio']['belum'] }}
                    ],
                    backgroundColor: ['#10B981', '#F59E0B', '#EF4444'], // Emerald, Amber, Red
                    borderWidth: 0,
                    hoverOffset: 4
                }]
            },
            options: donutOptions
        });

        // Reguler Donut
        new Chart(document.getElementById('regulerRatioChart').getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: ['Lunas', 'Menunggu', 'Belum'],
                datasets: [{
                    data: [
                        {{ $regulerStats['ratio']['lunas'] }}, 
                        {{ $regulerStats['ratio']['menunggu'] }}, 
                        {{ $regulerStats['ratio']['belum'] }}
                    ],
                    backgroundColor: ['#10B981', '#F59E0B', '#EF4444'], // Emerald, Amber, Red
                    borderWidth: 0,
                    hoverOffset: 4
                }]
            },
            options: donutOptions
        });
    });

</script>
@endsection
