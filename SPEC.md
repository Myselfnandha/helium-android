# Spec: Helium Browser for Android

## Assumptions Surface
1. **Target Platform**: Native Android (API level 26+ / Android 8.0 Oreo through Android 14+), targeting primary architecture `arm64-v8a` (with `armv7a` and `x86_64` support).
2. **Upstream Foundation**: Built on top of GrapheneOS's **Vanadium** (hardened Chromium mobile base) combined with the **Titanium / Kiwi** mobile extension subsystem.
3. **Heavy Compilation Delegation**: Due to workstation memory (14 GB RAM) and core constraints, heavy multi-hour Chromium compilation (`libchrome.so`, `monochrome_64.apk`) is executed via **GitHub Actions CI/CD** cloud runners (`ubuntu-latest` / self-hosted GH matrix runners). The repository serves as the complete patch, asset, and automation source of truth.
4. **Zero-Google Integrity**: All proprietary Google Play Services dependencies, Google Account sync, and Google telemetry are completely stripped; cross-device sync uses Helium's custom E2EE **Private Vault Sync** (`helium/core/sync/vault`).
5. **Independent Startup Toggles**: JIT, Desktop Extensions, Native C++ Adblocking, and Vault Sync must be independently configurable during First-Run Onboarding and within a persistent Helium Settings page.

---

## 1. Objective
Build an open-source, private, hardened Chromium-based web browser for Android that ports Helium's desktop privacy features, branding, and capabilities to mobile:
- **Desktop Extension Support**: Full support for Manifest V2/V3 extensions, bundled with Helium's custom uBlock Origin fork.
- **Native C++ Engine Adblocking**: High-performance network and DOM subresource filter directly in the Chromium rendering engine (Bromite-derived).
- **Private Vault Sync**: Cross-device end-to-end encrypted sync protocol (`helium/core/sync/vault`) pairing desktop Helium with mobile Helium without intermediate unencrypted cloud servers.
- **Vanadium Security Hardening**: Strict exploit mitigations, including JIT disabling by default with per-site exceptions, hardened allocator compatibility, and strict WebRTC IP handling.
- **Helium Mobile UX**: Clean modern Helium aesthetic (dark/light theme, custom Start Page), first-run onboarding setup wizard, and centralized Helium Settings.

---

## 2. Tech Stack & Dependencies
- **Core Engine**: Chromium for Android (`target_os = ['android']`) tracking stable Vanadium / Ungoogled milestone branches.
- **Languages**: 
  - **C++20**: Chromium native patches, Bromite subresource filter, and Helium Private Vault Sync JNI core.
  - **Java 17 / Kotlin**: Android UI, First-Run Wizard, Settings preferences, and Android Sync Adapter service.
  - **GN & Ninja**: Chromium meta-build system and compilation engine.
  - **Bash**: Build orchestration, patch management, dependency fetching (`depot_tools`).
- **Target SDK / Minimum SDK**: `compileSdkVersion = 34`, `targetSdkVersion = 34`, `minSdkVersion = 26`.
- **CI/CD**: GitHub Actions workflow running on `ubuntu-latest` with Android SDK/NDK and caching mechanisms.

---

## 3. Official Documentation & Source Citations (Source-Driven)
All architectural decisions and patterns in this specification are grounded in the following official sources:
1. **Chromium Android Build Architecture**:
   - Official Guide: [Checking out and building Chromium for Android](https://chromium.googlesource.com/chromium/src/+/main/docs/android_build_instructions.md)
   - Source: `docs/android_build_instructions.md` (Configuring `.gclient`, `target_os = ['android']`, and GN flags `target_cpu = "arm64"`).
2. **GrapheneOS Vanadium Hardening & JIT Controls**:
   - Source: [GrapheneOS Vanadium Repository](https://github.com/GrapheneOS/Vanadium)
   - Patches: JIT disable flags (`--disable-v8-jit`), hardened memory allocation, and ungoogled baseline configurations.
3. **Helium Desktop & Private Vault Sync Architecture**:
   - Source: [imputnet/helium Repository](https://github.com/imputnet/helium)
   - Components: `helium/core/sync/vault` (Private Vault commit format, state delta exchange, and loopback mutation handling).
4. **Mobile Extension Subsystem (Titanium / Kiwi Heritage)**:
   - Source: [jqssun/android-titanium-browser](https://github.com/jqssun/android-titanium-browser)
   - Architecture: Android extension UI bindings (`chrome/android/java/src/org/chromium/chrome/browser/extensions/`), desktop Chrome Web Store request routing, and MV2/MV3 execution layer.
5. **Subresource Filter Engine**:
   - Source: [Bromite / Cromite Subresource Filter Integration](https://github.com/uazo/cromite)
   - Engine: `components/subresource_filter/` C++ ruleset matcher with customizable filter list subscriptions.

---

## 4. Commands
```bash
# Initialize repository and verify patch cleanliness
./scripts/validate_patches.sh

# Run local syntax check and linting on Android Java/Kotlin and scripts
./scripts/lint.sh

# Test patch application against target Chromium revision
./scripts/test_patch_apply.sh --target 124.0.6367.82

# Local build script (for high-spec dev server or dry-run)
./scripts/build.sh --target-cpu arm64 --build-type release

# Trigger GitHub Actions CI build manually via gh CLI
gh workflow run "Build Helium Android Release" -f target_cpu=arm64 -f sign_apk=true
```

---

## 5. Project Structure
```
Helium/
├── .github/
│   └── workflows/
│       ├── build.yml                 # Matrix workflow building arm64, armv7a, x86_64 APKs
│       ├── lint_and_verify.yml       # PR validation: patch integrity, bash lint, Java lint
│       └── release.yml               # Automated GitHub release creation with signed APKs
├── CAPABILITY_MAP.md                 # Architecture capability map and module registry
├── SPEC.md                           # This complete specification document
├── patches/                          # Main Chromium source tree patches
│   ├── 01-vanadium-security/         # JIT toggle, WebRTC flags, anti-fingerprinting
│   ├── 02-extensions-runtime/        # Android extension runtime, desktop Web Store support
│   ├── 03-adblock-engine/            # Bromite C++ native adblock filter integration
│   ├── 04-vault-sync/                # helium/core/sync/vault JNI and synchronization layer
│   └── 05-helium-ui/                 # Helium theme, branding, Start Page, Settings, First-Run
├── subprojects_patches/              # Subproject-specific patches (v8, third_party, etc.)
│   ├── v8/                           # V8 JIT and memory security toggles
│   └── third_party/                  # Custom uBlock Origin fork bundle & assets
├── helium-android/                   # Custom Helium Android Java/Kotlin source additions
│   ├── src/main/java/net/imput/helium/
│   │   ├── onboarding/               # First-Run Setup Wizard (toggles for JIT, Ext, Adblock)
│   │   ├── settings/                 # Helium Settings screen & Preference fragments
│   │   ├── sync/                     # Vault Sync Android background service & bridge
│   │   └── ui/                       # Helium branded custom Start Page & theme controllers
│   └── res/                          # Vector drawables, colors (Helium slate/cyan), layouts
├── scripts/
│   ├── build.sh                      # Unified build script (runs locally or inside Docker CI)
│   ├── setup_toolchain.sh            # depot_tools, Android SDK/NDK installer
│   ├── validate_patches.sh           # Patch series integrity checker
│   └── update_patches.sh             # Rebase/refresh patches against newer Chromium targets
└── README.md                         # Project documentation, build instructions, CI setup
```

---

## 6. Code Style & Architectural Patterns

### A. Java/Kotlin Android Settings & First-Run Wizard
Use standard AndroidX Preference / Fragment patterns integrated cleanly into Chromium's `ChromeBaseAppCompatActivity`:
```java
// Example: HeliumFirstRunOnboardingFragment.java
package net.imput.helium.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import net.imput.helium.preferences.HeliumPreferences;

public class HeliumFirstRunOnboardingFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        // Inflate modern Helium first-run UI with curated toggle cards:
        // 1. Extensions Runtime (enable/disable)
        // 2. Engine Adblocking (enable/disable)
        // 3. Vanadium JIT Protection (enable/disable)
        // 4. Private Vault Sync Setup
        return inflater.inflate(R.layout.helium_first_run_layout, container, false);
    }
}
```

### B. C++ JNI Bridge for Helium Private Vault Sync
```cpp
// Example: helium/core/sync/vault/helium_vault_jni.cc
#include "helium/core/sync/vault/vault_client.h"
#include <jni.h>

extern "C" JNIEXPORT jboolean JNICALL
Java_net_imput_helium_sync_VaultClient_nativeInitializeVault(
    JNIEnv* env, jobject thiz, jstring vault_key, jstring storage_dir) {
    // Grounded in helium/core/sync/vault protocol
    return helium::sync::VaultClient::GetInstance()->Initialize(
        ConvertJavaStringToUTF8(env, vault_key),
        ConvertJavaStringToUTF8(env, storage_dir)
    );
}
```

### C. Clean Patch Standard
Patches must be formatted with standard unified diff headers (`a/... b/...`), clear commit messages, and author attributions:
```diff
--- a/chrome/browser/about_flags.cc
+++ b/chrome/browser/about_flags.cc
@@ -102,6 +102,10 @@
+    {"disable-v8-jit-by-default",
+     "Disable V8 JIT",
+     "Disables V8 JIT compilation for enhanced security.",
+     kOsAndroid,
+     FEATURE_VALUE_TYPE(features::kDisableV8JitByDefault)},
```

---

## 7. Testing & Verification Strategy
1. **Patch Applicability Test**:
   - Automated git dry-run (`git apply --check`) verifies all patches apply cleanly against the designated Chromium/Vanadium revision without fuzz or rejection.
2. **GN Configuration Verification**:
   - Automated `gn gen` dry-run validating that all custom feature flags (`enable_extensions = true`, `helium_vault_sync = true`, `enable_subresource_filter = true`) resolve dependency graphs without build argument conflicts.
3. **Android UI & Preferences Linting**:
   - Spotless, Checkstyle, and `gradlew lint` for custom Helium Java/Kotlin components.
4. **CI Smoke & Build Verification**:
   - Cloud GitHub Actions runner builds `chrome_public_apk` (target `arm64-v8a`).
   - Artifact attestation and APK signature verification (`apksigner verify`).

---

## 8. Boundaries & Safety Gates
- **Always**:
  - Run patch validation checks before committing new patch revisions.
  - Enforce zero-telemetry: verify no Google endpoints are pinged by default.
  - Ensure all features (Extensions, Adblock, JIT toggle, Vault Sync) fail safely without crashing the main browser process if a subcomponent fails.
- **Ask First**:
  - Upstream Chromium milestone version bumps (e.g. Chromium 124 -> 126).
  - Modifying signing keystore or release encryption credentials.
  - Introducing any external network dependencies or remote endpoints.
- **Never**:
  - Add Google Play Services or proprietary Google Play libraries.
  - Hardcode release signing keys, secrets, or API tokens in git.
  - Disable native security mitigations (e.g., ASLR, DEP, fortified libc) without explicit user opt-in flags.

---

## 9. Success Criteria
1. **Repository Completeness**: The workspace contains a fully structured, ready-to-use repository with `patches/`, `helium-android/` UI modules, build scripts, and `.github/workflows/`.
2. **First-Run Startup Config**: Clean Android onboarding wizard on launch enabling one-touch configuration of:
   - Desktop Extensions (Enabled/Disabled)
   - Native C++ Adblocker (Enabled/Disabled)
   - Vanadium JIT Protection (Hardened / Balanced / Performance)
   - Private Vault Sync (Pair with Desktop / Skip)
3. **Private Vault Sync Foundation**: JNI and Android service layers ready to sync state deltas with desktop Helium via `helium/core/sync/vault`.
4. **CI/CD Build Pipeline**: Fully specified and validated GitHub Actions workflow (`.github/workflows/build.yml`) that can checkout Chromium, apply the Helium patchset, compile the APK, sign it, and upload the build artifact.

---

## 10. Open Questions
1. **Chromium Version Target**: Should the initial build target Chromium 124 (stable Vanadium base with proven extension stability) or track the very latest upstream (e.g. Chromium 126+)? *(Recommended: Chromium 124 LTS baseline to ensure extension API stability)*.
2. **Pre-bundled Extensions**: In addition to Helium's custom uBlock Origin fork, should any secondary extensions (e.g. Dark Reader or Bitwarden) be available for one-click installation in the startup wizard? *(Recommended: Bundle uBlock Origin out of the box, allow other extensions via Web Store or file import)*.
