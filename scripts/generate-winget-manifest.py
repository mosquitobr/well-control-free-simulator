#!/usr/bin/env python3
"""Generates official Winget manifests (Schema v1.9.0) for a GitHub Release."""

import argparse
from pathlib import Path

PACKAGE_ID = "mosquitobr.WellControlFreeSimulator"
UPGRADE_CODE = "{8D3E4B2A-9A17-4F6B-9F25-3C1F2E77A112}"
REPO_URL = "https://github.com/mosquitobr/well-control-free-simulator"


def generate(version: str, sha256: str, output_dir: Path, product_code: str) -> None:
    target = output_dir / "manifests" / "m" / "mosquitobr" / "WellControlFreeSimulator" / version
    target.mkdir(parents=True, exist_ok=True)
    sha256_upper = sha256.strip().upper()
    installer_url = f"{REPO_URL}/releases/download/v{version}/WellControlFreeSimulator-{version}-windows-x64.msi"

    (target / f"{PACKAGE_ID}.yaml").write_text(
        f"""# yaml-language-server: $schema=https://aka.ms/winget-manifest.version.1.9.0.schema.json

PackageIdentifier: {PACKAGE_ID}
PackageVersion: {version}
DefaultLocale: en-US
ManifestType: version
ManifestVersion: 1.9.0
""",
        encoding="utf-8",
    )

    product_code_line = f"ProductCode: '{product_code}'\n" if product_code else ""
    (target / f"{PACKAGE_ID}.installer.yaml").write_text(
        f"""# yaml-language-server: $schema=https://aka.ms/winget-manifest.installer.1.9.0.schema.json

PackageIdentifier: {PACKAGE_ID}
PackageVersion: {version}
InstallerLocale: en-US
Platform:
  - Windows.Desktop
MinimumOSVersion: 10.0.19041.0
InstallerType: msi
Scope: user
InstallModes:
  - interactive
  - silent
  - silentWithProgress
UpgradeBehavior: install
{product_code_line}AppsAndFeaturesEntries:
  - DisplayName: ControleDePoco2
    Publisher: mosquitobr
    UpgradeCode: '{UPGRADE_CODE}'
Installers:
  - Architecture: x64
    InstallerUrl: {installer_url}
    InstallerSha256: {sha256_upper}
ManifestType: installer
ManifestVersion: 1.9.0
""",
        encoding="utf-8",
    )

    (target / f"{PACKAGE_ID}.locale.en-US.yaml").write_text(
        f"""# yaml-language-server: $schema=https://aka.ms/winget-manifest.defaultLocale.1.9.0.schema.json

PackageIdentifier: {PACKAGE_ID}
PackageVersion: {version}
PackageLocale: en-US
Publisher: mosquitobr
PublisherUrl: https://github.com/mosquitobr
PublisherSupportUrl: {REPO_URL}/issues
Author: mosquitobr
PackageName: Well Control Free Simulator
PackageUrl: {REPO_URL}
License: Apache-2.0
LicenseUrl: {REPO_URL}/blob/main/LICENSE
Copyright: Copyright (c) 2026 mosquitobr
CopyrightUrl: {REPO_URL}/blob/main/NOTICE
ShortDescription: 100% offline educational oil & gas well control simulator (Driller's, Wait & Weight, Volumetric, Subsea & Land BOP).
Description: >-
  Well Control Free Simulator is a free, open-source, 100% offline educational simulator
  for oil and gas well control engineering. Built with a shared Kotlin physics engine
  covering hydrostatic pressure, gas kick migration, choke hydraulics, kill sheet
  calculations, and 19 progressive land and subsea scenarios. Not certified by IADC,
  IWCF, API, or ANP — for educational use only.
Moniker: well-control-sim
Tags:
  - well-control
  - petroleum-engineering
  - drilling
  - simulator
  - education
  - offline
  - open-source
ReleaseNotesUrl: {REPO_URL}/releases/tag/v{version}
ManifestType: defaultLocale
ManifestVersion: 1.9.0
""",
        encoding="utf-8",
    )

    (target / f"{PACKAGE_ID}.locale.pt-BR.yaml").write_text(
        f"""# yaml-language-server: $schema=https://aka.ms/winget-manifest.locale.1.9.0.schema.json

PackageIdentifier: {PACKAGE_ID}
PackageVersion: {version}
PackageLocale: pt-BR
Publisher: mosquitobr
PublisherUrl: https://github.com/mosquitobr
PublisherSupportUrl: {REPO_URL}/issues
PackageName: Controle de Poço 2 (Well Control Free Simulator)
PackageUrl: {REPO_URL}
License: Apache-2.0
LicenseUrl: {REPO_URL}/blob/main/LICENSE
ShortDescription: Simulador educacional 100% offline de controle de poço (Sondador, Aguardar e Pesar, Volumétrico, BOP Terrestre e Submarino).
Description: >-
  Simulador educacional gratuito, de código aberto e 100% offline para engenharia de
  controle de poço. Inclui motor físico em Kotlin com hidrostática, migração de gás,
  hidráulica do choke, folha de matar (Kill Sheet) e 19 cenários terrestres e submarinos.
  Projeto independente de estudo; não é certificado por IADC, IWCF, API ou ANP.
ReleaseNotesUrl: {REPO_URL}/releases/tag/v{version}
ManifestType: locale
ManifestVersion: 1.9.0
""",
        encoding="utf-8",
    )
    print(f"[OK] Generated Winget manifests (Schema 1.9.0) at {target}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate Winget manifests for Well Control Free Simulator.")
    parser.add_argument("--version", required=True, help="Release version without leading 'v' (e.g. 1.0.0)")
    parser.add_argument("--sha256", required=True, help="SHA-256 hex digest of the Windows x64 MSI")
    parser.add_argument("--product-code", default="", help="Optional MSI ProductCode GUID")
    parser.add_argument("--output-dir", default="packaging/winget", help="Output base directory")
    args = parser.parse_args()
    generate(args.version, args.sha256, Path(args.output_dir), args.product_code)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
