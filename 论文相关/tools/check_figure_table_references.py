import io
import re
import sys
from collections import Counter
from pathlib import Path

from docx import Document


DOC_PATH = Path(r"D:\SHARE\OneDrive\Desktop\毕业论文 初稿07.2.docx")


def norm_no(s: str) -> str:
    return re.sub(r"\s+", "", s).replace("－", "-")


def body_texts():
    doc = Document(str(DOC_PATH))
    texts = [p.text.strip() for p in doc.paragraphs if p.text.strip()]
    body = []
    for text in texts:
        if text.strip() == "外文翻译":
            break
        body.append(text)
    return body


def is_fig_caption(text):
    return bool(re.match(r"^图\s*\d+\s*[-－]\s*\d+", text))


def is_table_caption(text):
    return bool(re.match(r"^表\s*\d+\s*[-－]\s*\d+", text))


def extract_fig_no(text):
    m = re.match(r"^图\s*(\d+)\s*[-－]\s*(\d+)(（[a-zA-Z]）)?", text)
    if not m:
        return None, None
    base = f"{m.group(1)}-{m.group(2)}"
    sub = base + (m.group(3) or "")
    return base, sub


def extract_table_no(text):
    m = re.match(r"^表\s*(\d+)\s*[-－]\s*(\d+)", text)
    if not m:
        return None
    return f"{m.group(1)}-{m.group(2)}"


def has_ref(texts, kind, no):
    # no is like 3-1 or 3-3（a）. Accept optional space after 图/表 and around hyphen.
    if "（" in no:
        base = no.split("（", 1)[0]
        sub = no.split("（", 1)[1][0]
        patterns = [
            rf"{kind}\s*{re.escape(base)}\s*（{sub}）",
            rf"{kind}\s*{re.escape(base)}\s*\({sub}\)",
        ]
    else:
        ch, idx = no.split("-", 1)
        patterns = [
            rf"{kind}\s*{ch}\s*[-－]\s*{idx}(?!\d)",
        ]
    for t in texts:
        if any(re.search(p, t) for p in patterns):
            return True
    return False


def expected_sequence(numbers, chapter):
    ints = sorted(int(n.split("-")[1]) for n in numbers if n.startswith(f"{chapter}-"))
    if not ints:
        return []
    return [i for i in range(1, max(ints) + 1) if i not in ints]


def main():
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    texts = body_texts()
    non_caption = [t for t in texts if not is_fig_caption(t) and not is_table_caption(t)]

    figs = []
    for t in texts:
        if is_fig_caption(t):
            base, sub = extract_fig_no(t)
            figs.append((base, sub, t))

    tables = []
    for t in texts:
        if is_table_caption(t):
            no = extract_table_no(t)
            tables.append((no, t))

    print("FIGURES")
    for base, sub, cap in figs:
        ref_base = has_ref(non_caption, "图", base)
        ref_sub = has_ref(non_caption, "图", sub) if sub != base else ref_base
        print(f"{sub}\tbase_ref={ref_base}\tsub_ref={ref_sub}\t{cap}")
    print()

    print("TABLES")
    for no, cap in tables:
        ref = has_ref(non_caption, "表", no)
        print(f"{no}\tref={ref}\t{cap}")
    print()

    fig_bases = [base for base, _sub, _cap in figs]
    fig_subs = [sub for _base, sub, _cap in figs]
    table_nos = [no for no, _cap in tables]

    print("DUP_FIG_BASE", [k for k, v in Counter(fig_bases).items() if v > 1])
    print("DUP_FIG_EXACT", [k for k, v in Counter(fig_subs).items() if v > 1])
    print("DUP_TABLE", [k for k, v in Counter(table_nos).items() if v > 1])
    print()

    for chapter in [2, 3, 4, 5]:
        print(f"MISS_FIG_SEQ_CH{chapter}", expected_sequence(set(fig_bases), chapter))
        print(f"MISS_TABLE_SEQ_CH{chapter}", expected_sequence(set(table_nos), chapter))


if __name__ == "__main__":
    main()
