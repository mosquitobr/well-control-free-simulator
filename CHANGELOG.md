# Changelog

All notable changes to **Well Control Free Simulator** are documented in this file.
This project follows [Semantic Versioning](https://semver.org/).

## [1.0.2] — 2026-11-27

### Added
- Full English localization layer (`CoreI18n`) in `:core` covering operational banners, alarms, milestones, recommendations, and **Kill Sheet** warnings/stages, eliminating PT-BR string leakage in EN mode.
- Typed `KillMethod` enum in `:core` replacing string-literal method checks in `Simulator`.
- Atomic file persistence (`AtomicFileWriter`) for Desktop JSON stores (`SettingsStore`, `CustomWellStore`, `SimulationResultStore`).
- Native Desktop keyboard shortcuts in `SimulatorScreen` (`Space` Play/Pause, `1`–`9` tabs, `K` Kill Sheet, `Esc` Back).
- Automated regression tests for EN localization, atomic persistence, and `RigAudioEngine` rapid start/stop safety.

### Changed
- Deduplicated shared pure-Kotlin `PumpPulseSynth` and `Academy` modules into `:core`.
- Synchronized `RigAudioEngine.start()`/`stop()` with `@Volatile` audio handles on both Desktop and Android.
- Enabled R8 minification, resource shrinking, and `proguard-rules.pro` for Android release builds, and migrated to Gradle 9-ready `compilerOptions`.
- Grouped coupled Kotlin/Compose/Coroutines updates in `.github/dependabot.yml` to prevent incompatible solo PRs.
- Re-captured clean 100% English documentation screenshots (zero PT-BR leakage, zero PNG metadata).

## [1.0.1] — 2026-11-27

### Added
- Signed release Android APK (`WellControlFreeSimulator-1.0.1-android.apk`) verified with `apksigner verify` via encrypted GitHub Actions signing secrets.
- Upgraded Windows release workflow to official **SignPath Foundation v3** GitHub Artifact Origin Verification (`actions/upload-artifact@v4` + `signpath/github-action-submit-signing-request@v3`) and automated MSI `ProductCode` extraction for Winget manifests.
- Added active GitHub Repository Rulesets protecting `main` and `v*` release tags.

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
