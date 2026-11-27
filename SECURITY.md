# Security Policy

## Scope

**Well Control Free Simulator** is a 100% offline educational application. It does not collect personal data, does not send telemetry, and does not request network permissions.

## Reporting a Vulnerability

Please **do not** open a public issue with sensitive details. Instead, use GitHub's private vulnerability reporting feature:

- **Security → Advisories → Report a vulnerability** on this repository.

Please include:
- App version and target platform (Windows 11, Linux/WSL, or Android);
- Steps to reproduce;
- Potential security impact;
- Relevant logs or proof of concept, if applicable.

## Build & Release Security

- Official release binaries are built exclusively via GitHub Actions and published with **SHA-256 checksums** (`SHA256SUMS.txt`).
- Windows release installers are configured for free open-source code signing through **SignPath Foundation** (`SignPath.io`).
- Signing secrets and Android release keystores are stored exclusively outside the repository in GitHub Actions Secrets.
