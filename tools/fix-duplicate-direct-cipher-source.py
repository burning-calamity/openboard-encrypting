#!/usr/bin/env python3
"""Remove an accidentally duplicated direct-cipher helper block from LatinIME.java."""
from pathlib import Path
import re
import shutil
import sys

DEFAULT_SOURCE = (Path(__file__).resolve().parents[1]
                  / "app/src/main/java/org/dslul/openboard/inputmethod/latin/LatinIME.java")
source = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else DEFAULT_SOURCE
text = source.read_text(encoding="utf-8")
pattern = re.compile(r"(?m)^    private String getDirectCipherText\(final int codePoint\) \{")
matches = list(pattern.finditer(text))

if len(matches) == 1:
    print(f"{source} already contains one canonical direct-cipher block; no repair needed.")
    raise SystemExit(0)
if len(matches) < 2:
    print(f"Cannot repair {source}: getDirectCipherText declaration was not found.", file=sys.stderr)
    raise SystemExit(1)

start = matches[1].start()
marker = "    // This method is public for testability of LatinIME"
end = text.find(marker, start)
if end < 0:
    print(f"Cannot repair {source}: the onEvent boundary was not found after the duplicate.",
          file=sys.stderr)
    raise SystemExit(1)

# Refuse an unexpectedly shaped deletion rather than damaging unrelated source.
block = text[start:end]
required = (
    "resetDirectCipherState(",
    "rewindDirectCipherStateForDelete(",
    "isDirectCipherConsumedCodePoint(",
    "transformStatefulDirectCipher(",
    "createDirectEnigmaM3Cipher(",
    "createDirectEnigmaM4Cipher(",
    "createDirectQuagmireCipher(",
)
missing = [name for name in required if name not in block]
if missing:
    print("Cannot safely remove the duplicate; expected helpers are missing: "
          + ", ".join(missing), file=sys.stderr)
    raise SystemExit(1)

backup = source.with_suffix(source.suffix + ".duplicate-backup")
shutil.copy2(source, backup)
source.write_text(text[:start] + text[end:], encoding="utf-8")
print(f"Removed duplicate direct-cipher block from {source}")
print(f"Backup written to {backup}")
