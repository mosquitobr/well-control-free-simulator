# Changelog

All notable changes to **Well Control Free Simulator** are documented in this file.
This project follows [Semantic Versioning](https://semver.org/).

## [1.0.0] — 2026-11-27

### Added
- Public multiplatform monorepo release under **Apache License 2.0** distributing **Windows 11**, **Linux/WSL**, and **Android** from a single shared `:core` simulation engine.
- Automated multiplatform CI/CD pipelines (Android APK, Windows MSI + Portable ZIP, Linux DEB + Portable TAR.GZ + Uber JAR) with consolidated `SHA256SUMS.txt`.
- Ready-to-activate **SignPath Foundation** code signing integration for Windows installers and official **Winget** manifest generator (`Schema v1.9.0`).
- Automated compliance gates for OPSEC (`verify-opsec.py`), i18n/terminology parity (`verify-parity.py`), and Apache-2.0 runtime license auditing (`verify-licenses.gradle`).
- `NOTICE` and `THIRD-PARTY-NOTICES.txt` covering all runtime libraries, Skiko/Skia native components, and bundled Eclipse Temurin OpenJDK 21 runtime notices.

### Changed
- Migrated Desktop JSON persistence (`SettingsStore`, `CustomWellStore`, `SimulationResultStore`) to `kotlinx-serialization-json` (**Apache-2.0**) for 100% pure Apache-2.0 runtime dependencies across all platforms.
- Inline help tooltips in the Well Editor (SPM and Kick Zone overpressure) with 100% physics parity across Android, Windows, and Linux.
