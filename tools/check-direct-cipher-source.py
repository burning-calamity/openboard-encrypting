#!/usr/bin/env python3
"""Reject accidental duplicate/stale direct-cipher blocks in LatinIME.java."""
from pathlib import Path
import re
import sys

DEFAULT_SOURCE = (Path(__file__).resolve().parents[1]
                  / "app/src/main/java/org/dslul/openboard/inputmethod/latin/LatinIME.java")
SOURCE = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else DEFAULT_SOURCE
text = SOURCE.read_text(encoding="utf-8")
expected_once = (
    "getDirectCipherText",
    "resetDirectCipherState",
    "recordDirectCipherAdvance",
    "rewindDirectCipherStateForDelete",
    "isDirectCipherConsumedCodePoint",
    "shouldAdvanceDirectCipherPosition",
    "isAsciiLetter",
    "applyDirectCipher",
    "readInt",
    "transformStatefulDirectCipher",
    "createDirectEnigmaM3Cipher",
    "createDirectEnigmaM4Cipher",
    "createDirectQuagmireCipher",
)
errors = []
for name in expected_once:
    count = len(re.findall(r"(?m)^\s*(?:private|protected|public)\s+(?:static\s+)?[\w<>\[\].?, ]+\s+"
                           + re.escape(name) + r"\s*\(", text))
    if count != 1:
        errors.append(f"{name}: expected one declaration, found {count}")

if "transformDirectCipherInput(" in text:
    errors.append("stale transformDirectCipherInput declaration/call remains")

if errors:
    print(f"Direct-cipher source validation failed for {SOURCE}:", file=sys.stderr)
    for error in errors:
        print(f"  - {error}", file=sys.stderr)
    print("Replace LatinIME.java with the tracked repository version; do not combine cipher blocks from older revisions.", file=sys.stderr)
    raise SystemExit(1)

print("Direct-cipher source contains one canonical helper block.")
