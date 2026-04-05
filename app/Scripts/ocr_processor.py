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

def check_date_validity(text, max_days):
    if not max_days:
        return True, None
    
    extracted_date = extract_date(text)
    if not extracted_date:
        return False, "Date not found in document"

    now = datetime.now()
    diff = now - extracted_date
    
    if diff.days > int(max_days):
        return False, f"Receipt date {extracted_date.strftime('%Y-%m-%d')} is older than {max_days} days"
    
    if diff.days < -1: # Allow slight future date
         return False, f"Receipt date {extracted_date.strftime('%Y-%m-%d')} is in the future"

    return True, extracted_date

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

def process_image(image_path, accounts=None, blacklist=None, max_days=None, student_name=None):
    if not HAS_OCR_LIBS:
        return {
            "error": "OCR libraries not installed. Please run: pip install -r app/Scripts/requirements.txt",
            "verification_status": "manual_review_required"
        }

    # Set tesseract path for Windows if it exists
    windows_path = r'C:\Program Files\Tesseract-OCR\tesseract.exe'
    if os.path.exists(windows_path):
        pytesseract.pytesseract.tesseract_cmd = windows_path

    try:
        # Load image
        img = cv2.imread(image_path)
        if img is None:
            return {"error": "Could not read image file", "verification_status": "failed"}

        # Preprocessing
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        
        # Apply thresholding to binarize the image
        _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        
        # Perform OCR
        text = pytesseract.image_to_string(thresh, lang='ind+eng')
        
        # 1. Extract Amount
        amount = extract_amount_from_text(text)
        
        # 2. Extract Bank Name
        bank_name = check_bank_name(text)

        result = {
            "raw_text": text.strip(),
            "extracted_amount": amount,
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

        # 3. Check Date Validity
        if max_days:
            date_valid, date_msg = check_date_validity(text, max_days)
            if date_msg:
                if not date_valid:
                    result["verification_status"] = "rejected" 
                    result["flags"].append(date_msg)
                    result["confidence"] = 0.0
                    return result
                else:
                    # Date is valid
                    pass
            elif not date_valid and not date_msg:
                 result["flags"].append("Date check skipped (not found)")

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
    args = parser.parse_args()
    
    if not os.path.exists(args.image_path):
        print(json.dumps({"error": f"File not found: {args.image_path}"}))
        sys.exit(1)
        
    try:
        data = process_image(args.image_path, args.accounts, args.blacklist, args.max_days, args.student_name)
        print(json.dumps(data))
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(1)
