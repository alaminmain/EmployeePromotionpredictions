import PyPDF2
import sys

pdf_path = r'E:\Project\TestProjects\EMPPromotion\Promotion Criteria.pdf'
output_path = r'E:\Project\TestProjects\EMPPromotion\promotion_criteria_extracted.txt'

try:
    with open(pdf_path, 'rb') as pdf_file:
        reader = PyPDF2.PdfReader(pdf_file)
        
        with open(output_path, 'w', encoding='utf-8') as output:
            output.write(f"Total Pages: {len(reader.pages)}\n")
            output.write("="*80 + "\n\n")
            
            for i, page in enumerate(reader.pages):
                output.write(f"\n--- PAGE {i+1} ---\n")
                text = page.extract_text()
                output.write(text)
                output.write("\n" + "="*80 + "\n")
    
    print(f"Successfully extracted PDF to: {output_path}")
    
except Exception as e:
    print(f"Error: {e}")
    sys.exit(1)
