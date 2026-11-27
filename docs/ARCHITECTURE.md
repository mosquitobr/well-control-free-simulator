# Architecture — Well Control Free Simulator

## Multiplatform Monorepo Topology

All three target platforms (**Windows 11**, **Linux/WSL**, and **Android**) share a single pure-Kotlin simulation engine (`:core`), guaranteeing 100% identical physics across desktop and mobile:

```text
well-control-free-simulator/
├── core/          # Pure Kotlin JVM 17 — SI physics, hydraulics, gas migration, kill sheet, 19 scenarios (80 tests)
├── desktopApp/    # Compose Multiplatform Desktop 1.7.3 (Skiko) — Windows 11 & Linux/WSL2
└── androidApp/    # Jetpack Compose + Material 3 — Android 8.0+ (API 26–35)
```

## Module Responsibilities

| Layer | `:core` | `:desktopApp` (Windows & Linux) | `:androidApp` (Android) |
| :--- | :--- | :--- | :--- |
| **Physics & Units** | Strict SI units internally (`Pa/kPa`, `m`, `m³`, `kg/m³`, `°C`) | UI conversion via `ui/Format.kt` (`Petrobras`, `US Field`, `SI`) | UI conversion via `ui/Format.kt` |
| **UI & Rendering** | None (headless) | Compose Desktop + Skia (`org.jetbrains.skia`) | Jetpack Compose + `android.graphics` |
| **Procedural Audio** | None | `javax.sound.sampled.SourceDataLine` (WASAPI / PulseAudio) | `android.media.AudioTrack` |
| **Local Persistence** | None | JSON (`kotlinx-serialization-json`, Apache-2.0) in `%APPDATA%` / `$XDG_CONFIG_HOME` | `SharedPreferences` + private JSON |
| **Network / Permissions** | Zero | Zero (100% offline) | Zero (no `INTERNET` permission) |

## Conditional Android Inclusion

`settings.gradle.kts` automatically includes `:androidApp` whenever `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or `local.properties` (`sdk.dir=...`) is detected — or explicitly via `-PenableAndroid=true|false`. This allows desktop-only contributors on Windows or Linux to build and test `:core` and `:desktopApp` without installing the Android SDK.
