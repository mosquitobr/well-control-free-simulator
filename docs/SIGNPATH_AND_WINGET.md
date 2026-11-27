# Windows SmartScreen Mitigation (SignPath Foundation) & Winget Submission Guide

This guide explains how to activate **free official open-source code signing** via **SignPath Foundation** and submit the Windows MSI installer to **Windows Package Manager (`microsoft/winget-pkgs`)** after publishing this repository.

---

## 1. Free Code Signing with SignPath Foundation

Unsigned Windows `.msi` installers trigger Microsoft Defender SmartScreen warnings. The **SignPath Foundation** provides free, HSM-backed EV/OV code signing certificates for qualifying public Open Source Software (OSS) projects built transparently on GitHub Actions.

### Step-by-Step Onboarding

1. **Ensure the repository is public** at `https://github.com/mosquitobr/well-control-free-simulator` with the `LICENSE` (Apache-2.0) and the **Code Signing Policy** section already present in `README.md`.
2. **Apply for SignPath Foundation Open Source:**
   - Visit <https://signpath.org/open-source> and submit the application linking your public repository.
   - Reference the automated GitHub Actions release workflow (`.github/workflows/release.yml`) and the Code Signing Policy in `README.md`.
3. **Configure Project & Artifact Policy in SignPath:**
   - Project slug: `well-control-free-simulator`
   - Signing policy slug: `release-signing` (linked to GitHub Actions origin verification)
   - Artifact configuration slug: `msi-installer` (MSI signing)
4. **Add GitHub Actions Secrets & Variables** (`Settings → Secrets and variables → Actions`):

   | Kind | Name | Value |
   | :--- | :--- | :--- |
   | **Secret** | `SIGNPATH_API_TOKEN` | API token issued by SignPath for your CI submitter user |
   | **Secret** | `SIGNPATH_ORG_ID` | Your SignPath Organization UUID |
   | **Variable** *(optional)* | `SIGNPATH_PROJECT_SLUG` | `well-control-free-simulator` *(default)* |
   | **Variable** *(optional)* | `SIGNPATH_SIGNING_POLICY_SLUG` | `release-signing` *(default)* |
   | **Variable** *(optional)* | `SIGNPATH_ARTIFACT_CONFIG_SLUG` | `msi-installer` *(default)* |

5. **Trigger a Release Tag:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   The workflow `.github/workflows/release.yml` automatically detects `SIGNPATH_API_TOKEN` and `SIGNPATH_ORG_ID` (or variable `SIGNPATH_ORGANIZATION_ID`), uploads the unsigned MSI as a GitHub workflow artifact (`actions/upload-artifact@v4`) for SignPath origin verification, submits it via `signpath/github-action-submit-signing-request@v3` (`github-artifact-id`), downloads the signed MSI, extracts its `ProductCode`, computes the final SHA-256 of the **signed** installer, regenerates the Winget manifest with the signed hash, and publishes everything to the GitHub Release.

> **Note:** While `SIGNPATH_API_TOKEN` and `SIGNPATH_ORG_ID` are not yet set, the workflow degrades gracefully and publishes the unsigned `.msi` with a clear warning so releases are never blocked.

---

## 2. Submitting to Winget (`microsoft/winget-pkgs`)

Each GitHub Release automatically builds a ready-to-submit Winget manifest bundle (`WellControlFreeSimulator-<version>-winget-manifest.zip`, Schema **v1.9.0**) matching the template in `packaging/winget/manifests/m/mosquitobr/WellControlFreeSimulator/`.

### Option A — Automated Submission with `wingetcreate` (Recommended)

1. Install Microsoft's official `wingetcreate` CLI on Windows 11:
   ```powershell
   winget install Microsoft.WingetCreate
   ```
2. Generate and submit a PR directly to `microsoft/winget-pkgs` pointing to your published GitHub Release MSI URL:
   ```powershell
   wingetcreate new https://github.com/mosquitobr/well-control-free-simulator/releases/download/v1.0.0/WellControlFreeSimulator-1.0.0-windows-x64.msi
   ```
3. Or validate the pre-generated manifest bundle locally before submitting:
   ```powershell
   winget validate --manifest packaging\winget\manifests\m\mosquitobr\WellControlFreeSimulator\1.0.0
   wingetcreate submit packaging\winget\manifests\m\mosquitobr\WellControlFreeSimulator\1.0.0
   ```

### Option B — Manual Update Script

To regenerate the 4 manifest YAML files locally for any release version and SHA-256 hash:

```powershell
python scripts/generate-winget-manifest.py `
  --version 1.0.0 `
  --sha256 <64-CHAR-SHA256-OF-MSI> `
  --product-code "{MSI-PRODUCT-CODE-GUID}"
```
