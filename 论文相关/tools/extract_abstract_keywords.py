from docx import Document


DOC_PATH = r"D:\SHARE\OneDrive\Desktop\毕业论文 初稿07.2.docx"


def main():
    doc = Document(DOC_PATH)
    for i, para in enumerate(doc.paragraphs[:90]):
        text = para.text.strip()
        if text in {"摘要", "Abstract"} or "关键词" in text or "Key words" in text:
            print(i, text)


if __name__ == "__main__":
    main()
