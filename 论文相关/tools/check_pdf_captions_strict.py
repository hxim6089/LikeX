from pathlib import Path
import re
from collections import defaultdict

from pypdf import PdfReader


PDF = Path(r"D:\SHARE\OneDrive\Desktop\毕业论文 初稿7.2.1 提交版.pdf")


def safe_print(s: str):
    print(s.encode("gbk", "replace").decode("gbk"))


reader = PdfReader(str(PDF))
caption_line_pat = re.compile(r"^(图|表)\s*(\d+)\s*[-－—]\s*(\d+)(?:[（(][a-zA-Z][）)])?\s*(.*)$")

items = []
for page_no, page in enumerate(reader.pages, 1):
    lines = [ln.strip() for ln in (page.extract_text() or "").splitlines() if ln.strip()]
    for line in lines:
        m = caption_line_pat.match(line)
        if m:
            kind, ch, no, title = m.group(1), int(m.group(2)), int(m.group(3)), m.group(4)
            raw = f"{kind}{ch}-{no}"
            items.append((page_no, kind, ch, no, raw, line))

safe_print("STRICT_CAPTIONS")
for it in items:
    safe_print(f"P{it[0]:03d} {it[4]}: {it[5]}")

for kind in ("图", "表"):
    safe_print(f"\n{kind}_CHECK")
    by_ch = defaultdict(list)
    for page_no, k, ch, no, raw, line in items:
        if k == kind:
            by_ch[ch].append((no, raw, page_no, line))
    for ch in sorted(by_ch):
        nums = [x[0] for x in by_ch[ch]]
        uniq = sorted(set(nums))
        missing = sorted(set(range(1, max(uniq) + 1)) - set(uniq)) if uniq else []
        dups = sorted({f"{kind}{ch}-{n}" for n in uniq if nums.count(n) > 1})
        safe_print(f"{kind}{ch}: nums={nums}; unique={uniq}; missing={missing}; dups={dups}")
