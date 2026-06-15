from pathlib import Path
import re
from collections import defaultdict

from pypdf import PdfReader


PDF = Path(r"D:\SHARE\OneDrive\Desktop\毕业论文 初稿7.2.1 提交版.pdf")


def safe_print(s: str):
    print(s.encode("gbk", "replace").decode("gbk"))


reader = PdfReader(str(PDF))
safe_print(f"pages={len(reader.pages)}")

caption_pat = re.compile(r"(图\s*\d+\s*[-－—]\s*\d+|表\s*\d+\s*[-－—]\s*\d+)")
entries = []

for page_no, page in enumerate(reader.pages, 1):
    text = page.extract_text() or ""
    lines = [ln.strip() for ln in text.splitlines() if ln.strip()]
    for idx, line in enumerate(lines):
        if caption_pat.search(line):
            ctx = " | ".join(lines[max(0, idx - 1): min(len(lines), idx + 2)])
            for m in caption_pat.finditer(line):
                raw = re.sub(r"\s+", "", m.group(1)).replace("－", "-").replace("—", "-")
                entries.append((page_no, raw, ctx))

safe_print("\nALL_MATCHES")
for page_no, raw, ctx in entries:
    safe_print(f"P{page_no:03d} {raw}: {ctx}")

for kind in ("图", "表"):
    seen = defaultdict(list)
    for page_no, raw, ctx in entries:
        if raw.startswith(kind):
            seen[raw].append((page_no, ctx))
    safe_print(f"\n{kind}_UNIQUE_IN_ORDER")
    last_by_chapter = defaultdict(int)
    for raw, occs in seen.items():
        m = re.match(rf"{kind}(\d+)-(\d+)", raw)
        if not m:
            continue
        ch, no = int(m.group(1)), int(m.group(2))
        dup = " DUP" if len(occs) > 1 else ""
        safe_print(f"{raw} pages={[p for p, _ in occs]}{dup}")
        last_by_chapter[ch] = max(last_by_chapter[ch], no)

    safe_print(f"\n{kind}_SEQUENCE_CHECK")
    by_chapter = defaultdict(set)
    for raw in seen:
        m = re.match(rf"{kind}(\d+)-(\d+)", raw)
        if m:
            by_chapter[int(m.group(1))].add(int(m.group(2)))
    for ch in sorted(by_chapter):
        nums = sorted(by_chapter[ch])
        expected = list(range(1, max(nums) + 1))
        missing = sorted(set(expected) - set(nums))
        dups = [raw for raw, occs in seen.items() if raw.startswith(f"{kind}{ch}-") and len(occs) > 1]
        safe_print(f"{kind}{ch}: nums={nums}, missing={missing}, dups={dups}")
