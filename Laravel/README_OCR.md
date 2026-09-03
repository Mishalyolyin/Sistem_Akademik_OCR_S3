# OCR System Setup Guide

This project uses a Python script to perform OCR (Optical Character Recognition) on payment receipts.

## Prerequisites

1.  **Python 3.x**: Ensure Python is installed and added to your system PATH.
2.  **Tesseract-OCR**: You must install the Tesseract binary.
    *   **Windows**: Download installer from [UB-Mannheim/tesseract](https://github.com/UB-Mannheim/tesseract/wiki).
    *   **Path**: The script expects Tesseract at `C:\Program Files\Tesseract-OCR\tesseract.exe`. If you install it elsewhere, please add it to your PATH or update `app/Scripts/ocr_processor.py`.

## Installation

1.  Open a terminal in the project root.
2.  Install the required Python packages:

```bash
pip install -r app/Scripts/requirements.txt
```

## How It Works

1.  When a student uploads a payment proof, Laravel calls `OcrService`.
2.  `OcrService` executes `app/Scripts/ocr_processor.py` with the image path.
3.  The Python script:
    *   Pre-processes the image using OpenCV (grayscale, thresholding).
    *   Uses Tesseract to extract text.
    *   Uses Regex to find the amount (IDR/Rp).
    *   Returns a JSON result with `extracted_amount` and `confidence`.
4.  If the amount matches the installment, the payment is **Auto-Verified**.
5.  If dependencies are missing or OCR fails, the script returns an error, and the payment status is set to **Needs Review** (manual verification).

## Troubleshooting

*   **Error: OCR libraries not installed**: Run the `pip install` command above.
*   **Error: tesseract is not installed or it's not in your PATH**: Install Tesseract-OCR and restart your terminal/server.
