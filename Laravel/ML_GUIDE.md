# Panduan Tahapan Machine Learning (ML Pipeline)

Dokumen ini menjelaskan tahapan standar dalam siklus hidup Machine Learning dan bagaimana tahapan tersebut diterapkan dalam fitur OCR (Optical Character Recognition) di proyek `pembayaran-sks-ai`.

## Diagram Alur Umum (ML Pipeline)

Secara umum, proses Machine Learning terdiri dari langkah-langkah berikut:

1.  **Data Acquisition** (Pengumpulan Data)
2.  **Data Preprocessing** (Pra-pemrosesan Data)
3.  **Model Training** (Pelatihan Model) *
4.  **Model Evaluation** (Evaluasi Model)
5.  **Deployment & Inference** (Penerapan & Prediksi)
6.  **Post-processing** (Pasca-pemrosesan)

*> Catatan: Dalam proyek ini, kita menggunakan pendekatan **Pre-trained Model** (Model Siap Pakai), sehingga kita melompati tahap Training dan langsung ke Inference.*

---

## Penjelasan Detail & Implementasi di Project

### 1. Data Acquisition (Pengumpulan Data)
**Teori:** Tahap mengumpulkan data mentah (raw data) yang akan diproses.
**Implementasi Kita:**
-   Mahasiswa mengupload foto bukti transfer (file `.jpg` atau `.png`).
-   Laravel menyimpan file tersebut di storage (`storage/app/public/payments`).

### 2. Data Preprocessing (Pra-pemrosesan)
**Teori:** Membersihkan dan menyiapkan data agar mudah "dibaca" oleh mesin. Mesin (AI) sulit membaca gambar yang gelap, miring, atau berisik (noise).
**Implementasi Kita:**
Di file `app/Scripts/ocr_processor.py`, fungsi `preprocess_image`:
```python
def preprocess_image(image_path):
    # Mengubah gambar menjadi hitam-putih (Grayscale)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    # Binarization (Thresholding): Membuat teks jadi hitam pekat & background putih bersih
    _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    return thresh
```

### 3. Model Training (Pelatihan Model)
**Teori:** Proses mengajarkan komputer mengenali pola dengan memberinya ribuan contoh data latih.
**Implementasi Kita:**
-   **Status: SKIPPED (Dilewati)**
-   Kita **TIDAK** melatih model dari nol (karena butuh jutaan data font & tulisan tangan).
-   Kita menggunakan **Pre-trained Model** (Model Siap Pakai) bernama **Tesseract OCR**.
-   *Analogi:* Kita tidak mengajari anak membaca dari huruf A-Z (Training), tapi kita mempekerjakan sarjana sastra (Pre-trained Model) untuk membaca dokumen kita.

### 4. Inference (Inferensi / Prediksi)
**Teori:** Tahap di mana model yang sudah jadi digunakan untuk memprediksi data baru.
**Implementasi Kita:**
-   **Status: ACTIVE (Dilakukan)**
-   Di `ocr_processor.py`, fungsi `pytesseract.image_to_string` adalah proses inferensi.
-   Outputnya adalah teks mentah dari gambar.

---

## FAQ: Kenapa Kita Tidak Butuh Dataset?

**Q: Saya belum punya Dataset, apakah project bisa jalan?**
**A: BISA.**
Karena kita menggunakan pendekatan **Inference-Only**. Kita meminjam "otak" Google (Tesseract) yang sudah dilatih dengan jutaan data di laboratorium Google. Jadi kita tidak perlu menyediakan data latih sendiri.

**Q: Kapan kita butuh Dataset?**
**A:** Kita baru butuh Dataset jika kita ingin melakukan **Fine-Tuning**.
Contoh: Jika Tesseract gagal membaca struk bank tertentu (misal: Bank Jago) karena font-nya aneh. Maka kita perlu mengumpulkan 100 foto struk Bank Jago (Dataset), lalu melatih ulang Tesseract (Retraining) agar dia paham font tersebut.

---


### 5. Post-processing (Pasca-pemrosesan)
**Teori:** Mengolah hasil mentah dari AI menjadi data terstruktur yang berguna untuk bisnis. AI seringkali memberikan hasil yang kotor atau tidak rapi.
**Implementasi Kita:**
Kita menggunakan **Regex (Regular Expression)** di fungsi `extract_amount_from_text` untuk mencari angka duit saja:
```python
# Mencari pola "Rp" atau "IDR" diikuti angka
patterns = [r'(?:Rp|IDR)\.?\s*([\d.,]+)']
```
Ini mengubah teks panjang menjadi angka pasti: `1500000`.

### 6. Evaluation / Confidence Score (Evaluasi Keyakinan)
**Teori:** Menilai seberapa yakin AI dengan jawabannya.
**Implementasi Kita:**
Kita menghitung skor keyakinan (`confidence`) berdasarkan:
-   Apakah nominal uang ditemukan?
-   Apakah ada kata kunci "transfer", "sukses", "berhasil"?
Jika skor tinggi (>80%), sistem melakukan **Auto-Verify**. Jika rendah, sistem minta **Manual Review**.

---

## Ringkasan Istilah Penting

| Istilah | Arti Singkat | Contoh di Project |
| :--- | :--- | :--- |
| **Dataset** | Kumpulan data | Folder gambar struk |
| **Feature** | Ciri-ciri data | Pixel warna hitam (tulisan) vs putih (kertas) |
| **Model** | Otak AI | Tesseract OCR |
| **Inference** | Proses menebak/membaca | Saat script Python jalan membaca gambar |
| **Accuracy** | Ketepatan | Apakah Rp 50.000 terbaca 50.000 atau 50.0000? |
| **Noise** | Gangguan data | Bercak noda di kertas struk, foto buram |
