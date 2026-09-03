# Panduan Deployment ke VPS Ubuntu (Laravel + Python OCR)

Dokumen ini berisi langkah-langkah lengkap untuk men-deploy aplikasi **Pembayaran SKS AI** ke server VPS berbasis Ubuntu (misal: DigitalOcean, AWS, Linode). Aplikasi ini membutuhkan konfigurasi khusus karena menggabungkan **Laravel (PHP)** dengan **OCR (Python)**.

## 1. Persiapan Server (System Requirements)

Pastikan VPS Anda menggunakan **Ubuntu 22.04 LTS** atau yang lebih baru. Login sebagai `root` atau user dengan akses `sudo`.

### Update & Install Paket Dasar
```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y git curl zip unzip software-properties-common supervisor
```

### Install PHP 8.2 & Extensions
```bash
sudo add-apt-repository ppa:ondrej/php -y
sudo apt update
sudo apt install -y php8.2-fpm php8.2-mysql php8.2-mbstring php8.2-xml php8.2-bcmath php8.2-curl php8.2-zip php8.2-gd php8.2-intl
```

### Install MySQL Database
```bash
sudo apt install -y mysql-server
sudo mysql_secure_installation
```
*(Ikuti instruksi di layar untuk mengamankan database)*

### Install Nginx Web Server
```bash
sudo apt install -y nginx
```

### Install Python 3, Pip & Tesseract OCR (PENTING!)
Aplikasi ini menggunakan Python untuk memproses gambar struk pembayaran.
```bash
# Install Python & Pip
sudo apt install -y python3 python3-pip python3-venv

# Install Tesseract OCR Engine (Wajib untuk membaca teks gambar)
sudo apt install -y tesseract-ocr tesseract-ocr-ind tesseract-ocr-eng

# Install Library Grafis (Wajib untuk OpenCV/cv2)
sudo apt install -y libgl1 libglib2.0-0
```

---

## 2. Instalasi Aplikasi

Asumsikan kita akan menginstall aplikasi di direktori `/var/www/pembayaran-sks`.

### Clone Repository
```bash
cd /var/www
sudo git clone https://github.com/username/repo-anda.git pembayaran-sks
cd pembayaran-sks
```

### Setup Permissions
User web server (biasanya `www-data`) harus bisa menulis ke folder tertentu.
```bash
sudo chown -R www-data:www-data /var/www/pembayaran-sks
sudo chmod -R 775 storage bootstrap/cache
```

### Setup Environment (.env)
```bash
cp .env.example .env
nano .env
```
Sesuaikan konfigurasi berikut di dalam `.env`:
```ini
APP_ENV=production
APP_DEBUG=false
APP_URL=https://domain-anda.com

DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=nama_database
DB_USERNAME=user_database
DB_PASSWORD=password_database

QUEUE_CONNECTION=database
```

### Install Dependencies (PHP & Python)
Masuk sebagai user biasa atau gunakan sudo jika perlu (tapi sebaiknya composer dijalankan sebagai user non-root jika bisa).
```bash
# Install PHP Dependencies
composer install --optimize-autoloader --no-dev

# Install Python Dependencies
pip3 install -r app/Scripts/requirements.txt
# Jika ada warning "break system packages", gunakan virtualenv atau tambahkan --break-system-packages (di Ubuntu terbaru)
# Opsi Aman (Virtualenv):
# python3 -m venv venv
# source venv/bin/activate
# pip install -r app/Scripts/requirements.txt
# (Jika pakai venv, update path python di .env atau OcrService.php)
```

### Setup Database & Storage
```bash
php artisan key:generate
php artisan migrate --force
php artisan storage:link
php artisan optimize
```

---

## 3. Konfigurasi Nginx

Buat file konfigurasi baru untuk situs Anda.
```bash
sudo nano /etc/nginx/sites-available/pembayaran-sks
```

Isi dengan konfigurasi berikut:

```nginx
server {
    listen 80;
    server_name domain-anda.com www.domain-anda.com;
    root /var/www/pembayaran-sks/public;

    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-Content-Type-Options "nosniff";

    index index.php;

    charset utf-8;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location = /favicon.ico { access_log off; log_not_found off; }
    location = /robots.txt  { access_log off; log_not_found off; }

    error_page 404 /index.php;

    location ~ \.php$ {
        fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
        fastcgi_param SCRIPT_FILENAME $realpath_root$fastcgi_script_name;
        include fastcgi_params;
    }

    location ~ /\.(?!well-known).* {
        deny all;
    }
}
```

Aktifkan konfigurasi dan restart Nginx:
```bash
sudo ln -s /etc/nginx/sites-available/pembayaran-sks /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

---

## 4. Konfigurasi Supervisor (Queue Worker)

Fitur OCR berjalan di background menggunakan Laravel Queue. Kita perlu **Supervisor** agar worker tetap berjalan otomatis.

Buat file konfigurasi worker:
```bash
sudo nano /etc/supervisor/conf.d/pembayaran-sks-worker.conf
```

Isi dengan:

```ini
[program:pembayaran-sks-worker]
process_name=%(program_name)s_%(process_num)02d
command=php /var/www/pembayaran-sks/artisan queue:work database --sleep=3 --tries=3 --max-time=3600
autostart=true
autorestart=true
stopasgroup=true
killasgroup=true
user=www-data
numprocs=2
redirect_stderr=true
stdout_logfile=/var/www/pembayaran-sks/storage/logs/worker.log
stopwaitsecs=3600
```

Jalankan Supervisor:
```bash
sudo supervisorctl reread
sudo supervisorctl update
sudo supervisorctl start pembayaran-sks-worker:*
```

Cek status worker:
```bash
sudo supervisorctl status
```
Harus muncul status `RUNNING`.

---

## 5. Troubleshooting (Jika Ada Masalah)

**1. OCR Gagal / Error di Log**
Cek log aplikasi:
```bash
tail -f storage/logs/laravel.log
tail -f storage/logs/ocr.log
```
Pastikan path python benar. Jika menggunakan `venv`, Anda mungkin perlu mengedit `app/Services/OcrService.php` atau membuat symlink agar `python` mengarah ke binary venv.

**2. Permission Denied saat Upload**
Pastikan folder storage milik `www-data`:
```bash
sudo chown -R www-data:www-data /var/www/pembayaran-sks/storage
```

**3. Tesseract Error**
Pastikan tesseract terinstall dan bisa diakses via terminal:
```bash
tesseract --version
```
Jika error `tesseract not found`, pastikan path-nya terdaftar di environment variable atau edit path manual di script Python.
