# Disamakan dengan Python yang dipakai pengembangan. Beda minor version
# antara mesin pengembang dan container membuat bug pembacaan sulit
# direproduksi, dan OpenCV termasuk pustaka yang perilakunya bisa berbeda.
FROM python:3.14-slim

WORKDIR /app

# Tesseract beserta bahasa Indonesia, dan pustaka grafis yang dibutuhkan OpenCV.
RUN apt-get update && apt-get install -y --no-install-recommends \
        tesseract-ocr tesseract-ocr-ind tesseract-ocr-eng \
        libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py ocr_processor.py ./
COPY tessdata/ tessdata/

RUN useradd --create-home app && chown -R app:app /app
USER app

EXPOSE 8000
ENV TESSDATA_PREFIX=/app/tessdata

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')"

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
