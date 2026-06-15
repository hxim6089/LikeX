from docx import Document


DOC_PATH = r"D:\SHARE\OneDrive\Desktop\毕业论文 初稿07.2.docx"


def main():
    doc = Document(DOC_PATH)
    matches = []
    for idx, para in enumerate(doc.paragraphs):
        text = para.text.strip()
        if not text:
            continue
        if "系统总体设计" in text or text.startswith("3.") or text.startswith("第3章") or text.startswith("第4章"):
            matches.append((idx, text))
    for idx, item in matches[:160]:
        print(f"{idx}: {item}")


if __name__ == "__main__":
    main()
