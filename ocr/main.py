"""
Service OCR untuk Sistem Pembayaran Program Doktor PAI.

Membungkus ocr_processor.py sebagai HTTP service, bukan dipanggil lewat
subprocess seperti di sistem Laravel lama. Bedanya penting: proses Python
beserta OpenCV dan Tesseract dimuat sekali saat service menyala, bukan
dinyalakan ulang tiap kali ada bukti bayar masuk.
"""

import logging
import os
import tempfile
from pathlib import Path
from typing import Annotated, Any, Optional

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel

# Kumpulan data bahasa dibundel di dalam proyek, jadi service ini tidak
# bergantung pada instalasi Tesseract yang menyertakan bahasa Indonesia.
TESSDATA = Path(__file__).parent / "tessdata"
if TESSDATA.is_dir():
    os.environ["TESSDATA_PREFIX"] = str(TESSDATA)

import ocr_processor  # noqa: E402  (harus setelah TESSDATA_PREFIX di-set)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)
logger = logging.getLogger("ocr")

MAX_UPLOAD_BYTES = 10 * 1024 * 1024
ALLOWED_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp", ".bmp", ".pdf"}
DOC_TYPES = {"payment", "ktp", "kk", "ijazah", "photo"}

app = FastAPI(
    title="OCR Sistem Pembayaran",
    version="1.0",
    description=(
        "Membaca bukti transfer dan dokumen mahasiswa. "
        "Dipanggil oleh service Spring Boot lewat antrean RabbitMQ."
    ),
)


class HealthResponse(BaseModel):
    status: str
    tesseract: Optional[str]
    languages: list[str]
    opencv: Optional[str]


@app.get("/health", response_model=HealthResponse, tags=["Sistem"])
def health() -> HealthResponse:
    """Dipakai Docker healthcheck dan pengecekan kesiapan sebelum kirim pekerjaan."""
    tesseract_version = None
    languages: list[str] = []
    opencv_version = None

    try:
        import pytesseract

        _apply_tesseract_path()
        tesseract_version = str(pytesseract.get_tesseract_version())
        languages = sorted(pytesseract.get_languages(config=""))
    except Exception as exc:  # noqa: BLE001 — health tidak boleh ikut gagal
        logger.warning("Tesseract belum siap: %s", exc)

    try:
        import cv2

        opencv_version = cv2.__version__
    except Exception as exc:  # noqa: BLE001
        logger.warning("OpenCV belum siap: %s", exc)

    siap = tesseract_version is not None and opencv_version is not None
    return HealthResponse(
        status="ok" if siap else "degraded",
        tesseract=tesseract_version,
        languages=languages,
        opencv=opencv_version,
    )


@app.post("/ocr", tags=["OCR"])
async def read_document(
    file: Annotated[UploadFile, File(description="Gambar atau PDF yang akan dibaca")],
    doc_type: Annotated[str, Form()] = "payment",
    accounts: Annotated[Optional[str], Form()] = None,
    blacklist: Annotated[Optional[str], Form()] = None,
    max_days: Annotated[Optional[int], Form()] = None,
    student_name: Annotated[Optional[str], Form()] = None,
) -> dict[str, Any]:
    """
    Membaca satu berkas dan mengembalikan hasil bacaan apa adanya.

    Service ini sengaja TIDAK memutuskan diterima atau ditolak — keputusan itu
    ada di Spring Boot, yang memegang ambang batas keyakinan dan toleransi
    selisih nominal. Di sini hanya membaca.
    """
    if doc_type not in DOC_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"doc_type '{doc_type}' tidak dikenal. Pilihan: {', '.join(sorted(DOC_TYPES))}.",
        )

    suffix = Path(file.filename or "").suffix.lower()
    if suffix not in ALLOWED_SUFFIXES:
        raise HTTPException(
            status_code=400,
            detail=f"Format '{suffix or 'tanpa ekstensi'}' tidak didukung. "
            f"Gunakan: {', '.join(sorted(ALLOWED_SUFFIXES))}.",
        )

    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Berkas kosong.")
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(
            status_code=413,
            detail=f"Berkas terlalu besar. Maksimal {MAX_UPLOAD_BYTES // (1024 * 1024)} MB.",
        )

    # ocr_processor bekerja dengan path berkas, jadi isinya ditulis ke berkas
    # sementara lalu dihapus lagi apa pun hasilnya.
    tmp_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            tmp.write(content)
            tmp_path = tmp.name

        _apply_tesseract_path()
        result = ocr_processor.process_image(
            tmp_path,
            accounts=accounts,
            blacklist=blacklist,
            max_days=max_days,
            student_name=student_name,
            doc_type=doc_type,
        )

        logger.info(
            "Dibaca %s (%s, %d byte): keyakinan=%s nominal=%s",
            file.filename,
            doc_type,
            len(content),
            result.get("confidence"),
            result.get("extracted_amount"),
        )
        return result

    except HTTPException:
        raise
    except Exception as exc:  # noqa: BLE001
        logger.exception("Gagal membaca %s", file.filename)
        raise HTTPException(
            status_code=500, detail=f"Gagal membaca berkas: {exc}"
        ) from exc
    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.unlink(tmp_path)


def _apply_tesseract_path() -> None:
    """Tesseract di Windows tidak ada di PATH setelah dipasang lewat installer."""
    import pytesseract

    if pytesseract.pytesseract.tesseract_cmd not in ("tesseract", ""):
        return

    for candidate in (
        os.environ.get("TESSERACT_CMD"),
        r"C:\Program Files\Tesseract-OCR\tesseract.exe",
        r"C:\Program Files (x86)\Tesseract-OCR\tesseract.exe",
        "/usr/bin/tesseract",
    ):
        if candidate and os.path.exists(candidate):
            pytesseract.pytesseract.tesseract_cmd = candidate
            return
