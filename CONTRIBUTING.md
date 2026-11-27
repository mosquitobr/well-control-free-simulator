# Contributing

Thank you for your interest in contributing to **Well Control Free Simulator**!

## Workflow

1. Open an issue describing the bug, physics refinement, or feature proposal.
2. Create a focused branch (`feat/...`, `fix/...`, `docs/...`).
3. Keep changes clean, well-tested, and strictly offline.
4. Run validation before opening a Pull Request:

```bash
# Core + Desktop unit tests
./gradlew :core:test :desktopApp:test --no-daemon

# i18n parity, terminology, and OPSEC checks
python3 scripts/verify-parity.py
python3 scripts/verify-opsec.py

# Android unit tests & debug APK (when Android SDK is available)
./gradlew :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon
```

## Engineering Rules

- **Originality:** Never copy code, text, layouts, images, or audio from third-party apps, courses, or standards. Never claim IADC, IWCF, API, or ANP certification.
- **100% Offline:** Do not add internet permissions, telemetry, analytics, ads, or external network calls.
- **Pure `:core` engine:** Keep all physics in SI units inside `:core` (pure Kotlin JVM, zero `android.*` imports). Field unit conversions belong in the UI formatting layer.
- **Bilingual parity:** Maintain 100% key parity between PT-BR (`values/strings.xml`) and EN (`values-en/strings.xml`).
- **OPSEC & hygiene:** Never commit keystores, passwords, tokens, `local.properties`, personal emails, or build artifacts.

## License

By submitting a Contribution, you agree that your contribution will be licensed under the **Apache License, Version 2.0** (see [LICENSE](LICENSE)).
