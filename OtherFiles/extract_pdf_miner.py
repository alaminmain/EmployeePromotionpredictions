from pdfminer.high_level import extract_text
import sys

pdf_path = r'E:\Project\TestProjects\EMPPromotion\Promotion Criteria.pdf'
output_path = r'E:\Project\TestProjects\EMPPromotion\promotion_criteria_miner.txt'

try:
    text = extract_text(pdf_path)
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(text)
    print(f"Extraction complete. Saved to {output_path}")
except Exception as e:
    print(f"Error: {e}")
    sys.exit(1)
