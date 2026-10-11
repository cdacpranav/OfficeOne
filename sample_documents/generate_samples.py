import os
import zipfile

output_dir = r"C:\Users\EL-Dorado\.gemini\antigravity\scratch\OfficeOne\sample_documents"
os.makedirs(output_dir, exist_ok=True)

# 1. Create Sample DOCX
docx_path = os.path.join(output_dir, "sample_report.docx")
with zipfile.ZipFile(docx_path, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""")
    
    z.writestr("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""")
    
    z.writestr("word/document.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:body>
        <w:p>
            <w:pPr><w:jc w:val="center"/></w:pPr>
            <w:r><w:rPr><w:b/><w:sz w:val="48"/><w:color w:val="1A73E8"/></w:rPr><w:t>Office One Document Report</w:t></w:r>
        </w:p>
        <w:p>
            <w:r><w:t>This is a verified test document opened and validated by the Office One Android Suite.</w:t></w:r>
        </w:p>
        <w:p>
            <w:r><w:rPr><w:b/><w:i/></w:rPr><w:t>Features verified: Offline parsing, Storage Access Framework, Ribbon formatting, zero external permissions.</w:t></w:r>
        </w:p>
        <w:tbl>
            <w:tr>
                <w:tc><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Component</w:t></w:r></w:p></w:tc>
                <w:tc><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Status</w:t></w:r></w:p></w:tc>
            </w:tr>
            <w:tr>
                <w:tc><w:p><w:r><w:t>Word Processor</w:t></w:r></w:p></w:tc>
                <w:tc><w:p><w:r><w:t>Verified</w:t></w:r></w:p></w:tc>
            </w:tr>
            <w:tr>
                <w:tc><w:p><w:r><w:t>Spreadsheet Engine</w:t></w:r></w:p></w:tc>
                <w:tc><w:p><w:r><w:t>Verified</w:t></w:r></w:p></w:tc>
            </w:tr>
        </w:tbl>
    </w:body>
</w:document>""")

# 2. Create Sample XLSX
xlsx_path = os.path.join(output_dir, "sample_budget.xlsx")
with zipfile.ZipFile(xlsx_path, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""")
    
    z.writestr("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")
    
    z.writestr("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""")
    
    z.writestr("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheets>
        <sheet name="Budget Summary" sheetId="1" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
    </sheets>
</workbook>""")
    
    z.writestr("xl/worksheets/sheet1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetData>
        <row r="1">
            <c r="A1" t="inlineStr"><is><t>Item</t></is></c>
            <c r="B1" t="inlineStr"><is><t>Amount</t></is></c>
        </row>
        <row r="2">
            <c r="A2" t="inlineStr"><is><t>Engineering</t></is></c>
            <c r="B2"><v>15000</v></c>
        </row>
        <row r="3">
            <c r="A3" t="inlineStr"><is><t>Design</t></is></c>
            <c r="B3"><v>8000</v></c>
        </row>
        <row r="4">
            <c r="A4" t="inlineStr"><is><t>Testing</t></is></c>
            <c r="B4"><v>4500</v></c>
        </row>
        <row r="5">
            <c r="A5" t="inlineStr"><is><t>Total</t></is></c>
            <c r="B5"><f>SUM(B2:B4)</f><v>27500</v></c>
        </row>
    </sheetData>
</worksheet>""")

# 3. Create Sample CSV
csv_path = os.path.join(output_dir, "sample_data.csv")
with open(csv_path, "w", encoding="utf-8") as f:
    f.write("Quarter,Revenue,Expenses,Profit\nQ1,45000,32000,13000\nQ2,52000,34000,18000\nQ3,61000,39000,22000\nQ4,70000,41000,29000\n")

# 4. Create Sample Markdown
md_path = os.path.join(output_dir, "sample_notes.md")
with open(md_path, "w", encoding="utf-8") as f:
    f.write("# Office One Project Notes\n\n- Zero permissions\n- 100% offline\n- Pure Android Storage Access Framework\n- Adaptive Desktop Ribbon UI\n")

# 5. Create Sample TXT
txt_path = os.path.join(output_dir, "sample_readme.txt")
with open(txt_path, "w", encoding="utf-8") as f:
    f.write("Office One - Mobile Office Suite\nTested and verified offline.\n")

print(f"Generated sample files in: {output_dir}")
for fname in os.listdir(output_dir):
    fpath = os.path.join(output_dir, fname)
    print(f" - {fname} ({os.path.getsize(fpath)} bytes)")
