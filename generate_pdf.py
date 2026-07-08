from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak
from reportlab.lib.units import inch
from reportlab.lib import colors

md_file = r'C:\Users\1035804\Documents\gitnew\url-shortener-service\URL_SHORTENER_COMPREHENSIVE_GUIDE.md'
pdf_file = r'C:\Users\1035804\Documents\gitnew\url-shortener-service\URL_SHORTENER_COMPREHENSIVE_GUIDE.pdf'

with open(md_file, 'r', encoding='utf-8') as f:
    content = f.read()

doc = SimpleDocTemplate(pdf_file, pagesize=letter, topMargin=0.5*inch, bottomMargin=0.5*inch)

styles = getSampleStyleSheet()
title_style = ParagraphStyle(
    'Title', parent=styles['Heading1'], fontSize=20, textColor=colors.HexColor('#000080'),
    spaceAfter=20, alignment=0
)
heading_style = ParagraphStyle(
    'Head', parent=styles['Heading2'], fontSize=12, textColor=colors.HexColor('#0066cc'),
    spaceAfter=10, spaceBefore=10
)
normal_style = ParagraphStyle(
    'Norm', parent=styles['Normal'], fontSize=9, leading=12, spaceAfter=8
)
code_style = ParagraphStyle(
    'Code', parent=styles['Normal'], fontSize=8, leading=10, textColor=colors.HexColor('#333333'),
    spaceAfter=8, leftIndent=20
)

story = []
lines = content.split('\n')

for line in lines:
    if line.startswith('# ') and not line.startswith('## '):
        story.append(Paragraph(line.replace('# ', ''), title_style))
        story.append(Spacer(1, 0.2*inch))
    elif line.startswith('## '):
        story.append(Paragraph(line.replace('## ', ''), heading_style))
    elif line.startswith('```'):
        story.append(Spacer(1, 0.1*inch))
    elif line.startswith('---'):
        story.append(PageBreak())
    elif line.strip():
        if '|' in line:
            story.append(Paragraph(line, code_style))
        else:
            story.append(Paragraph(line, normal_style))

doc.build(story)
print(f"✓ PDF created successfully!")
print(f"✓ Location: {pdf_file}")
import os
print(f"✓ Size: {os.path.getsize(pdf_file) / 1024:.1f} KB")
