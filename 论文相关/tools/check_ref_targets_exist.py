import io
import re
import sys
from pathlib import Path

from docx import Document


DOC_PATH = Path(r"D:\SHARE\OneDrive\Desktop\毕业论文 初稿07.2.docx")


def main():
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    doc = Document(str(DOC_PATH))
    texts = [p.text.strip() for p in doc.paragraphs if p.text.strip()]
    body = []
    for text in texts:
        if text == "外文翻译":
            break
        body.append(text)

    fig_caps = set()
    table_caps = set()
    cap_lines = []
    for text in body:
        mf = re.match(r"^图\s*(\d+)\s*[-－]\s*(\d+)(（[a-zA-Z]）)?", text)
        if mf:
            base = f"{mf.group(1)}-{mf.group(2)}"
            sub = base + (mf.group(3) or "")
            fig_caps.add(base)
            fig_caps.add(sub)
            cap_lines.append(text)
        mt = re.match(r"^表\s*(\d+)\s*[-－]\s*(\d+)", text)
        if mt:
            table_caps.add(f"{mt.group(1)}-{mt.group(2)}")
            cap_lines.append(text)

    ref_lines = [t for t in body if t not in cap_lines]
    fig_refs = []
    table_refs = []
    for text in ref_lines:
        for m in re.finditer(r"图\s*(\d+)\s*[-－]\s*(\d+)(（[a-zA-Z]）)?", text):
            base = f"{m.group(1)}-{m.group(2)}"
            ref = base + (m.group(3) or "")
            fig_refs.append((ref, text))
        for m in re.finditer(r"表\s*(\d+)\s*[-－]\s*(\d+)", text):
            ref = f"{m.group(1)}-{m.group(2)}"
            table_refs.append((ref, text))

    missing_fig_refs = [(ref, text) for ref, text in fig_refs if ref not in fig_caps]
    missing_table_refs = [(ref, text) for ref, text in table_refs if ref not in table_caps]

    print("FIG_CAPS", sorted(fig_caps))
    print("TABLE_CAPS", sorted(table_caps))
    print("FIG_REF_COUNT", len(fig_refs))
    print("TABLE_REF_COUNT", len(table_refs))
    print()
    print("MISSING_FIG_REFS")
    for ref, text in missing_fig_refs:
        print(ref, text[:180])
    print("MISSING_TABLE_REFS")
    for ref, text in missing_table_refs:
        print(ref, text[:180])


if __name__ == "__main__":
    main()
