import sys
import json
import os
import re
import argparse
from datetime import datetime, timedelta

# Try to import real OCR libraries
try:
    import pytesseract
    from PIL import Image
    import cv2
    import numpy as np
    HAS_OCR_LIBS = True
except ImportError:
    HAS_OCR_LIBS = False

# PyMuPDF is only needed to rasterize PDF uploads (e.g. a scanned ijazah
# exported straight to PDF) before handing them to cv2/Tesseract — cv2 has no
# PDF support at all.
try:
    import pymupdf as fitz
    HAS_PDF_LIB = True
except ImportError:
    HAS_PDF_LIB = False


def load_document_image(image_path):
    """
    Load a document as a BGR numpy array for cv2. Supports raster images
    (via cv2.imread) and PDFs (the first page is rendered via PyMuPDF). cv2
    itself cannot open PDFs at all — previously any PDF upload failed with
    "Could not read image file" before OCR ever ran, even though PDF is a
    common export format for scanned diplomas/certificates.
    """
    if image_path.lower().endswith('.pdf'):
        if not HAS_PDF_LIB:
            return None
        try:
            doc = fitz.open(image_path)
            page = doc[0]
            zoom = 3  # ~216 DPI from a 72 DPI base page — good balance for OCR
            pix = page.get_pixmap(matrix=fitz.Matrix(zoom, zoom))
            arr = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width, pix.n)
            doc.close()
            if pix.n == 4:
                return cv2.cvtColor(arr, cv2.COLOR_RGBA2BGR)
            if pix.n == 3:
                return cv2.cvtColor(arr, cv2.COLOR_RGB2BGR)
            return cv2.cvtColor(arr, cv2.COLOR_GRAY2BGR)
        except Exception:
            return None

    return cv2.imread(image_path)

def clean_amount_str(amount_str):
    """
    Convert currency string to float/int.
    Handles: '1.500.000', '1,500,000.00', 'Rp 1.500.000'
    """
    # Remove non-numeric characters except . and ,
    clean = re.sub(r'[^\d.,]', '', amount_str)
    
    # Heuristic for Indonesian vs US format
    if '.' in clean and ',' in clean:
        last_dot = clean.rfind('.')
        last_comma = clean.rfind(',')
        if last_dot > last_comma: # US Format (1,000.00)
             clean = clean.replace(',', '') # Remove commas
        else: # ID Format (1.000,00)
             clean = clean.replace('.', '').replace(',', '.')
    elif clean.count('.') > 1: # 1.500.000 (ID)
        clean = clean.replace('.', '')
    elif ',' in clean: # 1,500,000 (US) or 1500,00 (ID)
        if clean.count(',') > 1:
            clean = clean.replace(',', '')
        else:
            # Single comma. "100,000" (US 100k) vs "100,00" (ID 100).
            # Defaulting to ID format (replace comma with dot) as it's an ID app.
            clean = clean.replace(',', '.')
    elif '.' in clean:
        # Single dot. "50.000" (ID 50k) vs "50.00" (US 50).
        # Check if it looks like thousands separator (3 digits after dot)
        parts = clean.split('.')
        if len(parts) == 2 and len(parts[1]) == 3:
            clean = clean.replace('.', '') # Treat as thousands separator
        # Else treat as decimal (default float behavior)
    
    try:
        return float(clean)
    except ValueError:
        return 0.0

def extract_amount_from_text(text):
    """
    Find amount using Regex for IDR/Rp patterns.
    """
    patterns = [
        r'(?:Rp|IDR)\.?\s*([\d.,]+)',  # Rp 100.000
        r'Total\s*[:=]?\s*([\d.,]+)',   # Total: 100.000
        r'Jumlah\s*[:=]?\s*([\d.,]+)',  # Jumlah: 100.000
        r'Transfer\s*[:=]?\s*([\d.,]+)', # Transfer: 100.000
        r'Nominal\s*[:=]?\s*([\d.,]+)'  # Nominal: 100.000
    ]
    
    for pattern in patterns:
        matches = re.findall(pattern, text, re.IGNORECASE)
        for match in matches:
            amount = clean_amount_str(match)
            if 10000 <= amount <= 100000000: 
                return amount
    return None

def extract_date(text):
    """
    Extract date from text.
    Supports: DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD
    """
    # Regex for common date formats
    patterns = [
        r'\b(\d{1,2})[-/](\d{1,2})[-/](\d{4})\b',       # DD/MM/YYYY or DD-MM-YYYY
        r'\b(\d{4})[-/](\d{1,2})[-/](\d{1,2})\b',       # YYYY-MM-DD
        r'\b(\d{1,2})\s+(Jan|Feb|Mar|Apr|Mei|May|Jun|Jul|Agu|Aug|Sep|Okt|Oct|Nov|Des|Dec)[a-z]*\s+(\d{4})\b' # DD MMM YYYY (ID/EN)
    ]
    
    month_map = {
        'jan': 1, 'feb': 2, 'mar': 3, 'apr': 4, 'mei': 5, 'may': 5, 'jun': 6,
        'jul': 7, 'agu': 8, 'aug': 8, 'sep': 9, 'okt': 10, 'oct': 10, 'nov': 11, 'des': 12, 'dec': 12
    }

    for pattern in patterns:
        matches = re.findall(pattern, text, re.IGNORECASE)
        for match in matches:
            try:
                if len(match) == 3:
                    # Check which format based on pattern index or content
                    if re.match(r'[a-zA-Z]', match[1]): # DD MMM YYYY
                        day = int(match[0])
                        month_str = match[1].lower()[:3]
                        month = month_map.get(month_str, 1)
                        year = int(match[2])
                        return datetime(year, month, day)
                    elif len(match[0]) == 4: # YYYY-MM-DD
                        return datetime(int(match[0]), int(match[1]), int(match[2]))
                    else: # DD-MM-YYYY (or DD/MM/YYYY)
                        # match is (DD, MM, YYYY)
                        return datetime(int(match[2]), int(match[1]), int(match[0]))
            except ValueError:
                continue
    return None

def check_date_validity(text):
    """
    Detect the receipt date and describe its age. Informational only —
    the caller no longer lets this affect verification_status/confidence.
    """
    extracted_date = extract_date(text)
    if not extracted_date:
        return "not_found", "Date not found in document", None

    now = datetime.now()
    diff = now - extracted_date
    iso_date = extracted_date.strftime('%Y-%m-%d')

    if diff.days > 14:
        return "rejected", f"Bukti berusia lebih dari 14 hari ({iso_date})", iso_date

    if diff.days > 7:
        return "needs_review", f"Bukti berusia lebih dari 7 hari ({iso_date})", iso_date

    if diff.days < -1: # Allow slight future date
         return "rejected", f"Tanggal pada bukti ({iso_date}) berada di masa depan", iso_date

    return "valid", iso_date, iso_date

def check_blacklist(text, blacklist_str):
    if not blacklist_str:
        return []
    
    found = []
    keywords = [k.strip().lower() for k in blacklist_str.split(',') if k.strip()]
    text_lower = text.lower()
    
    for k in keywords:
        if k in text_lower:
            found.append(k)
    return found

def check_accounts(text, accounts_str):
    if not accounts_str:
        return False, []
    
    accounts = [a.strip() for a in accounts_str.split(',') if a.strip()]
    
    # Clean text to just numbers for fuzzy matching
    text_nums = re.sub(r'[^0-9]', '', text)
    
    found = []
    match = False
    
    for acc in accounts:
        # Clean account to just numbers
        acc_clean = re.sub(r'[^0-9]', '', acc)
        if len(acc_clean) > 5 and acc_clean in text_nums:
            match = True
            found.append(acc)
            
    return match, found

def check_student_name(text, student_name):
    if not student_name:
        return False
    
    # Simple check: is the name (or parts of it) in the text?
    # Normalize
    text_lower = text.lower()
    name_parts = [n.lower() for n in student_name.split() if len(n) > 2]
    
    match_count = 0
    for part in name_parts:
        if part in text_lower:
            match_count += 1
            
    # If more than 50% of name parts found, consider it a match
    if len(name_parts) > 0 and (match_count / len(name_parts)) >= 0.5:
        return True
    return False

def check_bank_name(text):
    """
    Detect bank name from receipt text.
    Returns the first matched bank name or None.
    """
    # Common Indonesian banks and e-wallets
    banks = {
        'BCA': ['bca', 'bank central asia'],
        'BRI': ['bri', 'bank rakyat indonesia', 'brimo'],
        'MANDIRI': ['mandiri', 'bank mandiri', 'livin'],
        'BNI': ['bni', 'bank negara indonesia'],
        'BSI': ['bsi', 'bank syariah indonesia'],
        'CIMB': ['cimb', 'niaga', 'octo'],
        'DANAMON': ['danamon'],
        'PERMATA': ['permata'],
        'BTN': ['btn', 'bank tabungan negara'],
        'JAGO': ['jago', 'bank jago'],
        'SEABANK': ['seabank'],
        'NEO': ['neobank', 'bank neo'],
        'OVO': ['ovo'],
        'GOPAY': ['gopay', 'gojek'],
        'DANA': ['dana'],
        'SHOPEEPAY': ['shopeepay', 'shopee'],
        'LINKAJA': ['linkaja']
    }
    
    text_lower = text.lower()
    
    for bank_code, keywords in banks.items():
        for keyword in keywords:
            # Check for word boundaries to avoid partial matches inside other words
            if re.search(r'\b' + re.escape(keyword) + r'\b', text_lower):
                return bank_code
                
    return None

def extract_text_from_image(image_path, doc_type='payment'):
    """
    Shared preprocessing + OCR step used for every document type (payment proof,
    KTP, ijazah). Returns (text, None) on success or (None, error_dict) on failure.
    """
    if not HAS_OCR_LIBS:
        return None, {
            "error": "OCR libraries not installed. Please run: pip install -r app/Scripts/requirements.txt",
            "verification_status": "manual_review_required"
        }

    # Set tesseract path for Windows if it exists
    windows_path = r'C:\Program Files\Tesseract-OCR\tesseract.exe'
    if os.path.exists(windows_path):
        pytesseract.pytesseract.tesseract_cmd = windows_path

    try:
        # Load image (raster image or PDF — see load_document_image)
        img = load_document_image(image_path)
        if img is None:
            return None, {"error": "Could not read image file", "verification_status": "failed"}

        # Preprocessing
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        if doc_type in ('ktp', 'kk', 'ijazah'):
            # ID cards / scanned documents often have a decorative watermark or
            # textured background (e.g. the batik pattern on a KTP) and small
            # print at low source resolution. A single global threshold (Otsu)
            # binarizes the whole image at once and turns those textured areas
            # into noise, destroying the text underneath. Upscaling the plain
            # grayscale image and letting Tesseract treat it as a layout
            # (--psm 6: uniform block of text) reads these far more reliably.
            scale = 3
            processed = cv2.resize(gray, None, fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC)
            tess_config = '--psm 6'
        else:
            # Payment proofs are typically clean screenshots/scans of a plain
            # receipt — binarizing still works well and is left unchanged.
            _, processed = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
            tess_config = ''

        # Simpan gambar untuk keperluan skripsi
        try:
            base_dir = os.path.dirname(image_path)
            base_name = os.path.basename(image_path).split('.')[0]
            cv2.imwrite(os.path.join(base_dir, f"{base_name}_1_grayscale.jpg"), gray)
            cv2.imwrite(os.path.join(base_dir, f"{base_name}_2_threshold.jpg"), processed)
        except Exception:
            pass

        # Perform OCR
        text = pytesseract.image_to_string(processed, lang='ind+eng', config=tess_config)
        return text, None
    except Exception as e:
        return None, {
            "error": str(e),
            "verification_status": "manual_review_required"
        }


_DIGIT_CONFUSION = str.maketrans({
    'o': '0', 'O': '0', 'l': '1', 'I': '1', 'i': '1',
    'b': '6', 's': '5', 'S': '5', 'g': '9', 'z': '2', 'Z': '2', 'B': '8',
})


def extract_id_number(text, label_pattern):
    """
    Find a 16-digit ID number (NIK / No. KK) following a label. At the print
    size of a photographed KTP/KK, Tesseract routinely misreads a handful of
    digits as visually-similar letters (6<->b, 0<->o, 1<->l/I, 5<->s, 9<->g,
    2<->z, 8<->B) — those are normalized back to digits before validating the
    run as a real 16-digit ID, instead of rejecting it outright.
    """
    pattern = label_pattern + r'\W{0,6}([0-9OoIlibsSgzZB][0-9OoIlibsSgzZB\s]{14,22}[0-9OoIlibsSgzZB])'
    match = re.search(pattern, text, re.IGNORECASE)
    if not match:
        return None

    candidate = re.sub(r'\s', '', match.group(1)).translate(_DIGIT_CONFUSION)
    if len(candidate) == 16 and candidate.isdigit():
        return candidate

    digits_only = re.sub(r'\D', '', candidate)
    return digits_only if len(digits_only) == 16 else None


def process_ktp(text, student_name=None):
    """
    KTP sanity-check: extract NIK only. Name-matching is intentionally NOT done
    here — the Ijazah is the source of truth for the student's name (see
    process_ijazah). The extracted NIK is informational only — never
    blocks/approves anything, it's cross-checked against the student's manually
    entered NIK on the Laravel side and used in the admin Excel export.
    """
    result = {
        "doc_type": "ktp",
        "raw_text": text.strip(),
        "nik": None,
        "confidence": 0.0,
        "verification_status": "needs_review",
        "flags": [],
    }

    nik = extract_id_number(text, r'NIK')
    if not nik:
        digits_match = re.search(r'\b(\d{16})\b', text)
        nik = digits_match.group(1) if digits_match else None

    if nik:
        result["nik"] = nik
        result["flags"].append(f"NIK terbaca: {nik}")
        result["confidence"] = 1.0
    else:
        result["flags"].append("NIK tidak ditemukan pada dokumen — mohon cek manual")

    return result


def extract_name_field(text):
    """
    Look for a "Nama: ..." style line (Ijazah layouts vary, so this is a best
    effort — falls back to None if no such line is found).
    """
    match = re.search(r'\bNama\b\s*[:\-]\s*([A-Za-z .\']{3,60})', text, re.IGNORECASE)
    if not match:
        return None
    name = match.group(1).strip()
    # Cut off at the next label-looking word if the line ran on (OCR often
    # merges lines together without a real newline).
    name = re.split(r'\b(Tempat|Tanggal|NIM|NIK|Program|Jurusan|Fakultas)\b', name, flags=re.IGNORECASE)[0].strip()
    return name or None


def extract_birth_place_date(text):
    """
    Look for a "Tempat/Tanggal Lahir: Kota, DD Bulan YYYY" style line, common
    on Indonesian ijazah. Returns (birth_place, birth_date_iso) — either may be
    None if not found/parseable.
    """
    match = re.search(
        r'Tempat\s*[,/]?\s*Tanggal\s*Lahir\s*[:\-]\s*([^\n\r]{3,80})',
        text, re.IGNORECASE
    )
    if not match:
        return None, None

    raw = match.group(1).strip()
    # Expected shape: "Kota, DD Bulan YYYY" — split on the first comma.
    parts = raw.split(',', 1)
    birth_place = parts[0].strip().rstrip('.') or None

    birth_date_iso = None
    date_source = parts[1] if len(parts) > 1 else raw
    parsed_date = extract_date(date_source)
    if parsed_date:
        birth_date_iso = parsed_date.strftime('%Y-%m-%d')

    return birth_place, birth_date_iso


def process_ijazah(text, student_name=None):
    """
    Ijazah is treated as the source of truth for the student's full name and
    place/date of birth: whatever is extracted here becomes the reference
    value written back to the student's record (birth place/date are not
    entered manually anywhere else). Keyword presence is still checked as a
    sanity check. This is NOT an authenticity check — Tesseract can't verify a
    stamp/seal/hologram, it can only flag that the document plausibly looks
    like an ijazah.
    """
    result = {
        "doc_type": "ijazah",
        "raw_text": text.strip(),
        "keyword_match": False,
        "extracted_name": None,
        "name_match": False,
        "extracted_birth_place": None,
        "extracted_birth_date": None,
        "confidence": 0.0,
        "verification_status": "needs_review",
        "flags": [],
    }

    keywords = ["ijazah", "sarjana", "kelulusan", "diploma", "yudisium", "lulus"]
    text_lower = text.lower()
    keyword_hits = [k for k in keywords if k in text_lower]
    result["keyword_match"] = len(keyword_hits) > 0
    result["flags"].append(
        f"Kata kunci ijazah terdeteksi: {', '.join(keyword_hits)}" if keyword_hits
        else "Tidak ada kata kunci ijazah terdeteksi — mohon cek manual (bukan verifikasi keaslian)"
    )

    extracted_name = extract_name_field(text)
    result["extracted_name"] = extracted_name

    if extracted_name:
        name_match = check_student_name(extracted_name, student_name)
    else:
        # Field-based extraction failed — fall back to a bag-of-words search
        # over the whole document.
        name_match = check_student_name(text, student_name)
    result["name_match"] = name_match
    result["flags"].append(
        f"Nama pada dokumen cocok dengan data mahasiswa ({student_name})" if name_match
        else "Nama pada dokumen tidak ditemukan/cocok dengan data mahasiswa — mohon cek manual"
    )

    birth_place, birth_date_iso = extract_birth_place_date(text)
    result["extracted_birth_place"] = birth_place
    result["extracted_birth_date"] = birth_date_iso
    if birth_place or birth_date_iso:
        result["flags"].append(f"Tempat/Tanggal lahir terbaca: {birth_place or '-'}, {birth_date_iso or '-'}")
    else:
        result["flags"].append("Tempat/Tanggal lahir tidak ditemukan pada dokumen — mohon isi/cek manual")

    confidence = 0.0
    if result["keyword_match"]:
        confidence += 0.5
    if name_match:
        confidence += 0.5
    result["confidence"] = confidence

    return result


def process_kk(text, student_name=None):
    """
    KK (Kartu Keluarga) sanity-check: extract the KK number, and — unlike
    KTP/Ijazah which have a single name on the document — check whether the
    student's name appears anywhere among the family members listed, by
    scanning line by line. Informational only, never blocks/approves anything;
    a stricter per-line check is used here (vs. the whole-document bag-of-words
    check used elsewhere) because a KK table is much easier to false-match on
    than a single-name document.
    """
    result = {
        "doc_type": "kk",
        "raw_text": text.strip(),
        "kk_number": None,
        "name_found_in_family": False,
        "matched_line": None,
        "confidence": 0.0,
        "verification_status": "needs_review",
        "flags": [],
    }

    # Real KK documents just print "No. <16 digits>" under the "KARTU KELUARGA"
    # title — they don't repeat "KK" next to "No." — so the label to search
    # for is "No"/"Nomor" alone, not "No. KK".
    kk_number = extract_id_number(text, r'(?:No\.?|Nomor)')
    if not kk_number:
        digits_match = re.search(r'\b(\d{16})\b', text)
        kk_number = digits_match.group(1) if digits_match else None

    if kk_number:
        result["kk_number"] = kk_number
        result["flags"].append(f"Nomor KK terbaca: {kk_number}")
    else:
        result["flags"].append("Nomor KK tidak ditemukan pada dokumen — mohon cek manual")

    if student_name:
        for line in text.splitlines():
            line = line.strip()
            if len(line) < 3:
                continue
            if check_student_name(line, student_name):
                result["name_found_in_family"] = True
                result["matched_line"] = line
                break

    result["flags"].append(
        f"Nama mahasiswa ({student_name}) ditemukan pada daftar anggota keluarga" if result["name_found_in_family"]
        else "Nama mahasiswa tidak ditemukan pada daftar anggota keluarga — mohon cek manual"
    )

    confidence = 0.0
    if result["kk_number"]:
        confidence += 0.6
    if result["name_found_in_family"]:
        confidence += 0.4
    result["confidence"] = confidence

    return result


def process_photo(image_path):
    """
    Profile photo sanity-check: is the background predominantly RED, as
    required by the institution ("foto resmi background merah")? This is
    plain color analysis (HSV border sampling) — NOT face/object detection,
    so it can't check things like "not wearing a peci"; that still needs a
    human reviewer. Informational only — never blocks the upload.
    """
    result = {
        "doc_type": "photo",
        "red_background": False,
        "red_ratio": 0.0,
        "confidence": 0.0,
        "verification_status": "needs_review",
        "flags": [],
    }

    if not HAS_OCR_LIBS:
        result["flags"].append("Library pengolah gambar tidak tersedia — mohon cek manual")
        return result

    img = cv2.imread(image_path)
    if img is None:
        result["flags"].append("Gagal membaca file gambar — mohon cek manual")
        return result

    h, w = img.shape[:2]
    margin = max(1, int(min(h, w) * 0.08))

    # Sample only the border strip (top/bottom rows + left/right columns) —
    # that's where the background is visible on a head-and-shoulders photo.
    border_pixels = np.concatenate([
        img[0:margin, :].reshape(-1, 3),
        img[h - margin:h, :].reshape(-1, 3),
        img[:, 0:margin].reshape(-1, 3),
        img[:, w - margin:w].reshape(-1, 3),
    ])

    hsv_pixels = cv2.cvtColor(border_pixels.reshape(-1, 1, 3), cv2.COLOR_BGR2HSV).reshape(-1, 3)
    hue, sat, val = hsv_pixels[:, 0], hsv_pixels[:, 1], hsv_pixels[:, 2]

    # Red wraps around hue 0 in OpenCV's 0-179 scale: 0-10 and 170-179.
    # Require decent saturation/value so dark shadows or white glare at the
    # edges (near-gray/near-black/near-white) don't get counted as "red".
    is_red = ((hue <= 10) | (hue >= 170)) & (sat > 60) & (val > 40)
    red_ratio = float(np.mean(is_red)) if len(is_red) > 0 else 0.0

    result["red_ratio"] = round(red_ratio, 3)
    result["red_background"] = red_ratio >= 0.5
    result["confidence"] = red_ratio
    result["flags"].append(
        f"Background terdeteksi merah ({red_ratio:.0%} area tepi foto)" if result["red_background"]
        else f"Background TIDAK terdeteksi merah ({red_ratio:.0%} area tepi foto) — mohon cek manual"
    )
    result["flags"].append("Pemakaian peci/aksesoris kepala tidak bisa dideteksi otomatis — mohon cek manual dari foto")

    return result


def process_image(image_path, accounts=None, blacklist=None, max_days=None, student_name=None, doc_type='payment'):
    if doc_type == 'photo':
        return process_photo(image_path)

    text, error = extract_text_from_image(image_path, doc_type)
    if error:
        return error

    if doc_type == 'ktp':
        return process_ktp(text, student_name)
    if doc_type == 'ijazah':
        return process_ijazah(text, student_name)
    if doc_type == 'kk':
        return process_kk(text, student_name)

    try:
        # 1. Extract Amount
        amount = extract_amount_from_text(text)
        
        # 2. Extract Bank Name
        bank_name = check_bank_name(text)

        result = {
            "raw_text": text.strip(),
            "extracted_amount": amount,
            "extracted_date": None,
            "bank_name": bank_name,
            "confidence": 0.0,
            "verification_status": "pending",
            "flags": []
        }


        # 2. Check Blacklist
        blacklist_hits = check_blacklist(text, blacklist)
        if blacklist_hits:
            result["verification_status"] = "rejected"
            result["flags"].append(f"Blacklist keywords found: {', '.join(blacklist_hits)}")
            result["confidence"] = 0.0
            return result

        # 3. Check Date Validity — informational only. Student payments aren't always
        # made on time, so an old/missing receipt date no longer rejects or caps
        # confidence; it's just recorded for reporting/export.
        date_status, date_msg, extracted_date_iso = check_date_validity(text)
        result["extracted_date"] = extracted_date_iso
        if date_status in ("not_found", "rejected", "needs_review"):
            result["flags"].append(date_msg)

        # 4. Check Accounts
        account_match, matched_accounts = check_accounts(text, accounts)
        if accounts and not account_match:
             result["flags"].append("Destination account not found in document")
        elif account_match:
             result["flags"].append(f"Destination account matched: {', '.join(matched_accounts)}")

        # 5. Check Student Name
        name_match = check_student_name(text, student_name)
        if name_match:
            result["flags"].append(f"Student name found: {student_name}")

        # 6. Calculate Confidence
        confidence = 0.0
        if amount:
            confidence += 0.5
        
        keywords = ["transfer", "berhasil", "sukses", "receipt", "pembayaran", "bank", "date", "tanggal"]
        keyword_hits = sum(1 for k in keywords if k in text.lower())
        if keyword_hits > 2:
            confidence += 0.2
            
        if account_match:
            confidence += 0.3
            
        if name_match:
            confidence += 0.2

        if bank_name:
            confidence += 0.1
            result["flags"].append(f"Bank/Wallet detected: {bank_name}")
            
        result["confidence"] = min(confidence, 1.0)

        if confidence >= 0.8:
            result["verification_status"] = "high_confidence"
        elif confidence >= 0.5:
             result["verification_status"] = "needs_review"
        else:
             result["verification_status"] = "low_confidence"

        return result

    except Exception as e:
        return {
            "error": str(e),
            "verification_status": "manual_review_required"
        }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('image_path')
    parser.add_argument('--accounts', default='')
    parser.add_argument('--blacklist', default='')
    parser.add_argument('--max-days', default=None)
    parser.add_argument('--student-name', default=None)
    parser.add_argument('--doc-type', default='payment', choices=['payment', 'ktp', 'ijazah', 'kk', 'photo'])
    args = parser.parse_args()

    if not os.path.exists(args.image_path):
        print(json.dumps({"error": f"File not found: {args.image_path}"}))
        sys.exit(1)

    try:
        data = process_image(args.image_path, args.accounts, args.blacklist, args.max_days, args.student_name, args.doc_type)
        print(json.dumps(data))
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(1)
