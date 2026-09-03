from PIL import Image, ImageDraw, ImageFont

img = Image.new("RGB", (700, 900), "white")
d = ImageDraw.Draw(img)

def font(size, bold=False):
    name = "arialbd.ttf" if bold else "arial.ttf"
    try:
        return ImageFont.truetype(name, size)
    except OSError:
        return ImageFont.load_default(size)

y = 50
d.text((230, y), "BANK SYARIAH INDONESIA", font=font(28, True), fill="black"); y += 45
d.text((250, y), "BUKTI TRANSFER", font=font(24), fill="black"); y += 60
d.line((50, y, 650, y), fill="black", width=2); y += 40

rows = [
    ("TANGGAL", "02/09/2026 14:35"),
    ("JENIS TRANSAKSI", "TRANSFER ANTAR REKENING"),
    ("REKENING ASAL", "7 1122 3344 55"),
    ("NAMA PENGIRIM", "RIZAL MAHENDRA PUTRA"),
    ("REKENING TUJUAN", "7 1234 5678 90"),
    ("NAMA PENERIMA", "YAYASAN BADAN WAKAF SULTAN AGUNG"),
    ("BERITA", "UKT SEMESTER 1 DOKTOR PAI"),
]
for label, value in rows:
    d.text((60, y), label, font=font(20), fill="black")
    d.text((300, y), value, font=font(20, True), fill="black")
    y += 45

y += 20
d.line((50, y, 650, y), fill="black", width=2); y += 40
d.text((60, y), "NOMINAL", font=font(24), fill="black")
d.text((300, y), "Rp 1.200.000", font=font(30, True), fill="black"); y += 70
d.text((60, y), "BIAYA ADMIN", font=font(20), fill="black")
d.text((300, y), "Rp 0", font=font(20), fill="black"); y += 80

d.text((200, y), "TRANSAKSI BERHASIL", font=font(26, True), fill="black")

img.save("bukti-uji.png")
print("dibuat: bukti-uji.png")
