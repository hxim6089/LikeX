import io
import re
import sys
from collections import Counter
from pathlib import Path

from docx import Document
from docx.oxml.ns import qn


DOC_PATH = Path(r"D:\SHARE\OneDrive\Desktop\毕业论文 初稿07.2.docx")


def get_rfonts(run):
    rpr = run._element.rPr
    if rpr is not None:
        rfonts = rpr.find(qn("w:rFonts"))
        if rfonts is not None:
            font = rfonts.get(qn("w:ascii")) or rfonts.get(qn("w:hAnsi"))
            if font:
                return font, "run"
    try:
        rpr = run._parent.style.element.rPr
        if rpr is not None:
            rfonts = rpr.find(qn("w:rFonts"))
            if rfonts is not None:
                font = rfonts.get(qn("w:ascii")) or rfonts.get(qn("w:hAnsi"))
                if font:
                    return font, "style"
    except Exception:
        pass
    return "<missing>", "missing"


def main():
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    doc = Document(str(DOC_PATH))
    total = 0
    ok = 0
    counter = Counter()
    bad = []
    for p_idx, para in enumerate(doc.paragraphs, start=1):
        para_text = para.text.strip()
        if para_text == "外文翻译":
            break
        for run in para.runs:
            if not re.search(r"[A-Za-z0-9]", run.text or ""):
                continue
            total += 1
            font, source = get_rfonts(run)
            counter[(font, source)] += 1
            if font.replace(" ", "").lower() == "timesnewroman":
                ok += 1
            else:
                bad.append((p_idx, font, source, run.text[:80], para_text[:140]))
    print("BODY_PARAGRAPH_ASCII_RUNS", total)
    print("TIMES_NEW_ROMAN", ok)
    print("NOT_TNR", len(bad))
    print("COUNTER")
    for (font, source), count in counter.most_common(30):
        print(f"{font}\t{source}\t{count}")
    print("SAMPLES")
    for item in bad[:60]:
        print(item)


if __name__ == "__main__":
    main()
