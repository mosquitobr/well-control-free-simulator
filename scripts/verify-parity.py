#!/usr/bin/env python3
"""Verifies multiplatform integrity of Well Control Free Simulator:
- 100% key parity between PT-BR and EN string resources (Desktop & Android)
- 100% parity between Android and Desktop strings.xml files
- Zero android.* imports inside core/ and desktopApp/
- Zero legacy forbidden terminology ('sapato' or 'perfurador') in src/main
"""

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent


def parse_keys(xml_path: Path) -> list[str]:
    return [el.attrib["name"] for el in ET.parse(xml_path).getroot().findall("string")]


def check_string_parity() -> list[str]:
    errors = []
    pairs = [
        ("desktopApp", ROOT / "desktopApp/src/main/resources/i18n/values/strings.xml", ROOT / "desktopApp/src/main/resources/i18n/values-en/strings.xml"),
        ("androidApp", ROOT / "androidApp/src/main/res/values/strings.xml", ROOT / "androidApp/src/main/res/values-en/strings.xml"),
    ]
    key_sets = {}
    for label, pt_xml, en_xml in pairs:
        for p in (pt_xml, en_xml):
            if not p.exists():
                errors.append(f"Missing file: {p.relative_to(ROOT)}")
        if errors:
            continue

        pt_keys = parse_keys(pt_xml)
        en_keys = parse_keys(en_xml)
        if len(pt_keys) != len(set(pt_keys)):
            errors.append(f"Duplicate keys in {pt_xml.relative_to(ROOT)}")
        if len(en_keys) != len(set(en_keys)):
            errors.append(f"Duplicate keys in {en_xml.relative_to(ROOT)}")

        missing_en = set(pt_keys) - set(en_keys)
        missing_pt = set(en_keys) - set(pt_keys)
        if missing_en:
            errors.append(f"[{label}] Missing keys in EN: {sorted(missing_en)}")
        if missing_pt:
            errors.append(f"[{label}] Missing keys in PT: {sorted(missing_pt)}")
        key_sets[label] = set(pt_keys)
        print(f"[OK] [{label}] i18n PT-BR/EN parity verified: {len(pt_keys)} identical keys.")

    if "desktopApp" in key_sets and "androidApp" in key_sets:
        diff_a = key_sets["androidApp"] - key_sets["desktopApp"]
        diff_d = key_sets["desktopApp"] - key_sets["androidApp"]
        if diff_a or diff_d:
            errors.append(f"Android vs Desktop i18n key drift: android-only={sorted(diff_a)}, desktop-only={sorted(diff_d)}")
        else:
            print("[OK] Android <-> Desktop string keysets are 100% identical.")
    return errors


def check_no_android_imports() -> list[str]:
    errors = []
    pattern = re.compile(r"^\s*import\s+(android\.|androidx\.activity|androidx\.core|androidx\.lifecycle)", re.MULTILINE)
    for folder in (ROOT / "core/src", ROOT / "desktopApp/src"):
        for kt in folder.rglob("*.kt"):
            text = kt.read_text(encoding="utf-8")
            if pattern.search(text):
                errors.append(f"Residual Android import in {kt.relative_to(ROOT)}")
    if not errors:
        print("[OK] Zero Android imports in core/ and desktopApp/.")
    return errors


def check_terminology() -> list[str]:
    errors = []
    banned = re.compile(r"\b(sapato|perfurador)\b", re.IGNORECASE)
    for folder in (ROOT / "core/src/main", ROOT / "desktopApp/src/main", ROOT / "androidApp/src/main"):
        if not folder.exists():
            continue
        for path in folder.rglob("*"):
            if path.is_file() and path.suffix in {".kt", ".xml"}:
                text = path.read_text(encoding="utf-8")
                if banned.search(text):
                    errors.append(f"Legacy forbidden term found in {path.relative_to(ROOT)}")
    if not errors:
        print("[OK] Engineering terminology verified in src/main (0 occurrences of 'sapato'/'perfurador').")
    return errors


def main() -> int:
    errors = []
    errors.extend(check_string_parity())
    errors.extend(check_no_android_imports())
    errors.extend(check_terminology())
    if errors:
        for err in errors:
            print(f"[ERROR] {err}", file=sys.stderr)
        return 1
    print("[OK] All parity and engineering integrity checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
