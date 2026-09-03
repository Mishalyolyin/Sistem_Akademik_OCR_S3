# Service OCR

Membaca bukti transfer dan dokumen mahasiswa. Dipanggil oleh Spring Boot lewat
antrean RabbitMQ, bukan langsung dari browser.

## Menjalankan

```bash
python -m venv .venv
.venv\Scripts\pip install -r requirements.txt      # Windows
.venv\Scripts\uvicorn main:app --port 8000 --reload
```

Dokumentasi otomatis: http://localhost:8000/docs

## Yang dibutuhkan di sistem

- **Tesseract OCR** beserta bahasa `ind` dan `eng`.
  Windows: `winget install UB-Mannheim.TesseractOCR`
  Ubuntu: `sudo apt install tesseract-ocr tesseract-ocr-ind tesseract-ocr-eng`
- Berkas bahasa juga dibundel di folder `tessdata/`, dan dipakai lewat
  `TESSDATA_PREFIX` supaya service tetap jalan walau instalasi Tesseract
  sistem tidak menyertakan bahasa Indonesia.

## Endpoint

| Method | Path | Keterangan |
|---|---|---|
| GET | `/health` | Versi Tesseract dan OpenCV, daftar bahasa |
| POST | `/ocr` | Baca satu berkas |

`POST /ocr` menerima multipart: `file`, `doc_type` (payment/ktp/kk/ijazah/photo),
`accounts`, `blacklist`, `max_days`, `student_name`.

Service ini **hanya membaca**, tidak memutuskan diterima atau ditolak.
Ambang keyakinan dan toleransi selisih nominal ada di Spring Boot.
