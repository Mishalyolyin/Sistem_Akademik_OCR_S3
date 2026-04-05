<!DOCTYPE html>
<html>
<head>
    <title>Kuitansi Pembayaran</title>
    <style>
        body { font-family: 'Times New Roman', serif; color: #0F172A; } /* Primary Dark */
        .header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #D4AF37; padding-bottom: 20px; }
        .details { width: 100%; border-collapse: collapse; margin-top: 20px; }
        .details td { padding: 12px; border-bottom: 1px solid #e2e8f0; }
        .label { font-weight: bold; width: 180px; color: #0F172A; text-transform: uppercase; font-size: 0.9em; letter-spacing: 1px; }
        .footer { margin-top: 50px; text-align: center; font-size: 0.8em; color: #64748b; border-top: 1px solid #D4AF37; padding-top: 20px; }
        .status { font-weight: bold; color: #0F172A; }
        .amount { font-weight: bold; font-size: 1.2em; color: #D4AF37; } /* Gold */
    </style>
</head>
<body>
    <div class="header">
        <h1 style="color: #0F172A; margin-bottom: 5px; font-family: serif;">Magister Pendidikan Agama Islam</h1>
        <p style="margin: 0; color: #D4AF37; font-weight: bold; letter-spacing: 2px;">UNISSULA</p>
        <h3 style="margin-top: 20px; color: #0F172A; font-family: serif; letter-spacing: 1px;">KUITANSI PEMBAYARAN</h3>
    </div>

    <table class="details">
        <tr>
            <td class="label">ID Transaksi</td>
            <td>#{{ $payment->id }}</td>
        </tr>
        <tr>
            <td class="label">Tanggal</td>
            <td>{{ $payment->created_at->format('d M Y H:i') }}</td>
        </tr>
        <tr>
            <td class="label">Nama Mahasiswa</td>
            <td>{{ $payment->student->name }}</td>
        </tr>
        <tr>
            <td class="label">NIM</td>
            <td>{{ $payment->student->nim }}</td>
        </tr>
        <tr>
            <td class="label">Kelas</td>
            <td>{{ $payment->student->class ?? '-' }}</td>
        </tr>
        <tr>
            <td class="label">Program Studi</td>
            <td>{{ $payment->student->program_type }}</td>
        </tr>
        <tr>
            <td class="label">Rencana Pembayaran</td>
            <td>{{ $payment->paymentPlan ? 'Rp ' . number_format($payment->paymentPlan->total_amount, 0, ',', '.') : 'N/A' }}</td>
        </tr>
        <tr>
            <td class="label">Cicilan Ke</td>
            <td>{{ $payment->installment ? $payment->installment->installment_order : 'N/A' }}</td>
        </tr>
        <tr>
            <td class="label">Jumlah Dibayar</td>
            <td class="amount">Rp {{ number_format($payment->amount, 0, ',', '.') }}</td>
        </tr>
        <tr>
            <td class="label">Status</td>
            <td class="status">{{ strtoupper($payment->status) }}</td>
        </tr>
    </table>

    <div class="footer">
        <p>Bukti pembayaran ini sah dan diterbitkan secara komputerisasi tanpa tanda tangan basah.</p>
        <p>Dicetak pada {{ now()->format('d M Y H:i:s') }}</p>
    </div>
</body>
</html>
