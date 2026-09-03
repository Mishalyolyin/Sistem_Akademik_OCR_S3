import unittest
import sys
import os
from datetime import datetime, timedelta

# Add app/Scripts to path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../../app/Scripts')))

# Import the module
import ocr_processor

class TestOcrLogic(unittest.TestCase):
    
    def test_clean_amount_str(self):
        self.assertEqual(ocr_processor.clean_amount_str("Rp 1.500.000"), 1500000.0)
        self.assertEqual(ocr_processor.clean_amount_str("1.500.000"), 1500000.0)
        self.assertEqual(ocr_processor.clean_amount_str("1,500,000.00"), 1500000.0)
        self.assertEqual(ocr_processor.clean_amount_str("Rp 50.000"), 50000.0)
        
    def test_check_blacklist(self):
        blacklist = "gagal, pending, error"
        self.assertEqual(ocr_processor.check_blacklist("Transaksi GAGAL", blacklist), ["gagal"])
        self.assertEqual(ocr_processor.check_blacklist("Status: Pending", blacklist), ["pending"])
        self.assertEqual(ocr_processor.check_blacklist("Sukses", blacklist), [])
        self.assertEqual(ocr_processor.check_blacklist("Sukses", ""), [])

    def test_check_accounts(self):
        accounts = "1234567890, 0987654321"
        
        # Exact match
        match, found = ocr_processor.check_accounts("Transfer ke 1234567890", accounts)
        self.assertTrue(match)
        self.assertIn("1234567890", found)
        
        # Match with noise
        match, found = ocr_processor.check_accounts("Ke: 123-456-7890", accounts)
        self.assertTrue(match)
        
        # No match
        match, found = ocr_processor.check_accounts("Ke: 1112223334", accounts)
        self.assertFalse(match)
        
    def test_check_student_name(self):
        name = "Budi Santoso"
        
        # Exact match
        self.assertTrue(ocr_processor.check_student_name("Terima dari Budi Santoso", name))
        
        # Partial match (Budi found) - Logic requires > 50% parts. 
        # "Budi" is 1 part of 2 (50%). >= 0.5 is True.
        self.assertTrue(ocr_processor.check_student_name("Dari Budi", name))
        
        # "Santoso" found
        self.assertTrue(ocr_processor.check_student_name("Bp Santoso", name))
        
        # No match
        self.assertFalse(ocr_processor.check_student_name("Dari Ahmad Dhani", name))
        
    def test_check_date_validity(self):
        max_days = 30
        
        # Valid date (today)
        today = datetime.now()
        text_today = today.strftime("%d/%m/%Y")
        valid, _ = ocr_processor.check_date_validity(f"Tanggal: {text_today}", max_days)
        self.assertTrue(valid)
        
        # Valid date (20 days ago)
        past = today - timedelta(days=20)
        text_past = past.strftime("%Y-%m-%d")
        valid, _ = ocr_processor.check_date_validity(f"Date: {text_past}", max_days)
        self.assertTrue(valid)
        
        # Expired date (40 days ago)
        expired = today - timedelta(days=40)
        text_expired = expired.strftime("%d %b %Y") # DD MMM YYYY
        valid, msg = ocr_processor.check_date_validity(f"Tgl {text_expired}", max_days)
        self.assertFalse(valid)
        self.assertIn("older than 30 days", msg)
        
        # Future date
        future = today + timedelta(days=5)
        text_future = future.strftime("%d-%m-%Y")
        valid, msg = ocr_processor.check_date_validity(f"Tgl {text_future}", max_days)
        self.assertFalse(valid)
        self.assertIn("future", msg)

if __name__ == '__main__':
    unittest.main()
