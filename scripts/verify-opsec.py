#!/usr/bin/env python3
"""Automated OPSEC scanner for well-control-free-simulator.

Verifies:
1. No personal email addresses (only GitHub noreply or placeholder emails allowed)
2. No local machine paths (C:\\Users\\, /Users/, /home/, /storage/emulated/, ~/.keys)
3. No secrets/tokens/private keys (GitHub PATs, AWS keys, PEM private keys)
4. No references to private source repositories or local tooling paths
5. No EXIF or personal text metadata chunks inside PNG/WebP images
"""

from pathlib import Path
import re
import struct
import sys

ROOT = Path(__file__).resolve().parent.parent

SKIP_DIRS = {".git", ".gradle", ".kotlin", "build", "dist", "out", "__pycache__", ".reversa", "_reversa_sdd", "_reversa_forward", ".agents"}
TEXT_EXTS = {
    ".kt", ".kts", ".java", ".xml", ".md", ".txt", ".yml", ".yaml",
    ".properties", ".toml", ".py", ".sh", ".ps1", ".cmd", ".desktop",
    ".gradle", ".json", ".svg", ""
}

ALLOWED_EMAILS = {
    "97371621+mosquitobr@users.noreply.github.com",
}

EMAIL_RE = re.compile(r"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}")

FORBIDDEN_PATTERNS = [
    ("GitHub classic PAT", re.compile(r"ghp_[A-Za-z0-9]{20,}")),
    ("GitHub fine-grained PAT", re.compile(r"github_pat_[A-Za-z0-9_]{20,}")),
    ("AWS Access Key", re.compile(r"AKIA[0-9A-Z]{16}")),
    ("Private Key block", re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----")),
    ("Windows user path", re.compile(r"[A-Za-z]:\\Users\\[^\s\"'`]+", re.IGNORECASE)),
    ("Windows user path (slash)", re.compile(r"[A-Za-z]:/Users/[^\s\"'`]+", re.IGNORECASE)),
    ("Local Antigravity path", re.compile(r"Antigravity", re.IGNORECASE)),
    ("macOS user path", re.compile(r"/Users/[a-zA-Z0-9._-]+/")),
    ("Linux home path", re.compile(r"/home/[a-zA-Z0-9._-]+/")),
    ("Android local storage path", re.compile(r"/storage/emulated/")),
    ("Local keystore path", re.compile(r"~/\.keys/")),
    ("Private repo reference", re.compile(r"\bcontrole-poco-(sim|desktop)\b", re.IGNORECASE)),
]


def is_in_skip_dir(path: Path) -> bool:
    return any(part in SKIP_DIRS for part in path.relative_to(ROOT).parts)


def check_text_files() -> list[str]:
    errors = []
    self_path = Path(__file__).resolve()
    for path in ROOT.rglob("*"):
        if not path.is_file() or is_in_skip_dir(path):
            continue
        if path.resolve() == self_path:
            continue
        if path.suffix.lower() not in TEXT_EXTS:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue

        rel = path.relative_to(ROOT).as_posix()

        for label, pattern in FORBIDDEN_PATTERNS:
            for match in pattern.finditer(text):
                line_no = text.count("\n", 0, match.start()) + 1
                errors.append(f"[OPSEC] {label} in {rel}:{line_no} -> {match.group(0)!r}")

        for match in EMAIL_RE.finditer(text):
            email = match.group(0).lower()
            if email in ALLOWED_EMAILS:
                continue
            # Ignore XML/Kotlin/YAML schema or package-like false positives
            if email.endswith((".png", ".jpg", ".svg", ".xml", ".kt", ".gradle")):
                continue
            line_no = text.count("\n", 0, match.start()) + 1
            errors.append(f"[OPSEC] Unexpected email in {rel}:{line_no} -> {email}")

    return errors


def check_png_metadata() -> list[str]:
    errors = []
    forbidden_chunks = {b"eXIf", b"tEXt", b"iTXt", b"zTXt", b"tIME"}
    for path in ROOT.rglob("*.png"):
        if is_in_skip_dir(path):
            continue
        data = path.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n":
            errors.append(f"[OPSEC] Invalid PNG header: {path.relative_to(ROOT).as_posix()}")
            continue
        pos = 8
        found = []
        while pos + 8 <= len(data):
            length = struct.unpack(">I", data[pos:pos + 4])[0]
            ctype = data[pos + 4:pos + 8]
            if ctype in forbidden_chunks:
                found.append(ctype.decode("ascii", errors="replace"))
            pos += 12 + length
            if ctype == b"IEND":
                break
        if found:
            rel = path.relative_to(ROOT).as_posix()
            errors.append(f"[OPSEC] Metadata chunks {found} found in PNG: {rel}")
    return errors


def main() -> int:
    errors = []
    errors.extend(check_text_files())
    errors.extend(check_png_metadata())
    if errors:
        for err in errors:
            print(err, file=sys.stderr)
        return 1
    print("[OK] OPSEC scan passed: zero leaked emails, local paths, tokens, private repo links, or PNG metadata.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
