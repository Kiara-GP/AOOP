import re
import sys
from pathlib import Path
from xml.etree import ElementTree as ET


def _localname(tag: str) -> str:
    # '{namespace}t' -> 't'
    if "}" in tag:
        return tag.rsplit("}", 1)[-1]
    return tag


def extract_doc_text(xml_path: Path) -> str:
    raw = xml_path.read_bytes()
    s = raw.decode("utf-8", errors="ignore")

    root = ET.fromstring(s)
    parts: list[str] = []

    for e in root.iter():
        tag = _localname(e.tag)
        if tag in ("t", "instrText") and e.text:
            parts.append(e.text)
        elif tag in ("br", "cr"):
            parts.append("\n")
        elif tag == "tab":
            parts.append("\t")
        elif tag == "p":
            parts.append("\n")

    out = "".join(parts)
    out = out.replace("\u00a0", " ")
    out = re.sub(r"\n{3,}", "\n\n", out)
    return out.strip() + "\n"


def main(argv: list[str]) -> int:
    if len(argv) != 3:
        print("Usage: python tools/extract_docx_text.py <document.xml> <out.txt>")
        return 2

    xml_path = Path(argv[1])
    out_path = Path(argv[2])
    out_path.write_text(extract_doc_text(xml_path), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))

