#!/usr/bin/env python3
"""Pre-commit guard: reject UTF-8 mojibake and BOMs in text files.

Mojibake is UTF-8 text mis-decoded as Windows-1252 / Latin-1 and re-saved,
which scatters corrupted byte sequences through a file (e.g. a box-drawing
dash showing up as several garbled chars). This has repeatedly corrupted the
Angular workspace component, so this hook blocks it at commit time.

Detection: a mojibake lead char (U+00C2, U+00C3, U+00E2) immediately followed
by another non-ASCII char is the signature of the double-encoding; a real
accented letter or Unicode symbol never looks like that. We also reject a
UTF-8 BOM (U+FEFF), which the same editor keeps prepending.

The mojibake/BOM characters are built with chr() so this file stays pure
ASCII and never trips its own check.

Usage (also wired via .pre-commit-config.yaml):
    python scripts/check_encoding.py <file> [<file> ...]
"""
from __future__ import annotations

import re
import sys

# Lead byte (U+00C2 / U+00C3 / U+00E2) followed by any non-ASCII char.
_LEAD = chr(0xC2) + chr(0xC3) + chr(0xE2)
_MOJIBAKE_RE = re.compile("[" + _LEAD + "][" + chr(0x80) + "-" + chr(0xFFFF) + "]")
_BOM = chr(0xFEFF)


def check_file(path: str) -> list:
    problems = []
    try:
        with open(path, "r", encoding="utf-8") as fh:
            text = fh.read()
    except (UnicodeDecodeError, OSError):
        # Not valid UTF-8 (binary or unreadable) -- out of scope here.
        return problems

    if text.startswith(_BOM):
        problems.append("{}:1: UTF-8 BOM at start of file".format(path))

    for lineno, line in enumerate(text.splitlines(), start=1):
        if _BOM in line:
            problems.append("{}:{}: stray BOM (U+FEFF)".format(path, lineno))
        match = _MOJIBAKE_RE.search(line)
        if match:
            snippet = line.strip()[:80]
            problems.append("{}:{}: likely mojibake {!r} in: {!r}".format(
                path, lineno, match.group(), snippet))
    return problems


def main(argv: list) -> int:
    problems = []
    for path in argv[1:]:
        problems.extend(check_file(path))
    if problems:
        print("Encoding check FAILED -- mojibake or BOM detected:", file=sys.stderr)
        for problem in problems:
            print("  " + problem, file=sys.stderr)
        print("Fix: re-save the file(s) as UTF-8 without a BOM "
              "(editors honour the repo .editorconfig: charset = utf-8).", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
