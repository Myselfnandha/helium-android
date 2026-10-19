# Helium Browser for Android

<p align="center">
  <img src="helium-android/res/drawable/ic_helium_logo.xml" width="96" height="96" alt="Helium Logo"/>
</p>

<p align="center">
  <b>Fast. Private. Extensible.</b><br>
  A hardened, privacy-first Chromium web browser bringing desktop Helium's signature capabilities to Android.
</p>

---

## Key Features

- **Desktop Extensions Engine**: Run desktop Chrome extensions natively on Android (powered by the Titanium/Kiwi mobile extension subsystem). Includes a pre-bundled custom **uBlock Origin fork** with CNAME uncloaking.
- **Native C++ Engine Adblocking**: Bromite-derived subresource filtering directly in the Chromium network stack for lightning-fast tracker and advertisement blocking without DOM overhead.
- **Vanadium Security Hardening**: Exploitation mitigations from GrapheneOS's Vanadium (disabling V8 JIT by default to close common remote code execution attack vectors, hardened allocator, and WebRTC leak protection).
- **Private Vault Sync**: Cross-device, end-to-end encrypted synchronization protocol (`helium/core/sync/vault`) pairing Android devices with desktop Helium instances without Google Play Services or proprietary cloud accounts.
- **First-Run Onboarding Setup**: Every major feature (Extensions, Adblocker, JIT Hardening, and Sync) is modular and toggleable on startup via `HeliumFirstRunActivity`, as well as dynamically adjustable in the browser's persistent Helium Settings.
- **Helium Aesthetics**: Obsidian dark slate theme (`#090d16`), vibrant cyan accents (`#06b6d4`), modern card layouts, and custom Start Page.

---

## Architecture & Project Structure

```
Helium/
├── .github/
│   └── workflows/
│       ├── build.yml                 # Matrix APK build workflow
│       └── release.yml               # Automated release & packaging workflow
├── CAPABILITY_MAP.md                 # System capability map & dependency graph
├── SPEC.md                           # Comprehensive architecture specification
├── tasks/
│   ├── plan.md                       # Implementation plan & checkpoints
│   └── todo.md                       # Task checklist with acceptance criteria
├── patches/                          # Modular Chromium patchsets
│   ├── 01-vanadium-security/         # JIT toggle, WebRTC flags, anti-fingerprinting
│   ├── 02-extensions-runtime/        # Android extension runtime, Chrome Web Store routing
│   ├── 03-adblock-engine/            # Bromite C++ native adblock filter integration
│   ├── 04-vault-sync/                # helium/core/sync/vault JNI and synchronization layer
│   └── 05-helium-ui/                 # Helium theme, branding, Start Page, Settings, First-Run
├── subprojects_patches/              # Subproject patches (third_party uBlock bundle)
├── helium-android/                   # Custom Helium Android Java/Kotlin/C++ source
│   ├── src/main/java/net/imput/helium/
│   │   ├── onboarding/               # First-Run Setup Wizard
│   │   ├── preferences/              # HeliumPreferences controller
│   │   ├── settings/                 # HeliumSettingsFragment
│   │   └── sync/                     # Private Vault Sync Android service & JNI bridge
│   ├── src/main/cpp/                 # Native C++ Vault JNI bridge (helium_vault_jni.cc)
│   └── res/                          # Vector icons, colors (dark/light), layouts
├── scripts/
│   ├── build.sh                      # Unified build orchestrator
│   ├── setup_toolchain.sh            # depot_tools setup script
│   ├── validate_patches.sh           # Patch series integrity checker
│   └── lint.sh                       # Repository linters and syntax checks
└── docs/
    └── ARCHITECTURE.md               # In-depth architectural design guide
```

---

## Quick Start & Verification

### 1. Validate Patchsets & Android Resources
```bash
./scripts/lint.sh
./scripts/validate_patches.sh .
```

### 2. Verify Build Arguments (Dry Run)
```bash
./scripts/build.sh --target-cpu arm64 --build-type release --dry-run
```

---

## Building via GitHub Actions (Cloud CI)

Because compiling Chromium for Android from source requires heavy system resources (30GB+ RAM, 100GB+ SSD), builds are automated via **GitHub Actions**:

1. Push this repository to your GitHub account:
   ```bash
   git remote add origin https://github.com/<your-username>/helium-android.git
   git push -u origin main
   ```
2. Navigate to the **Actions** tab in your repository.
3. Select **Build Helium Android APK** and click **Run workflow**.
4. The workflow will validate patches, execute the compilation matrix, and upload the signed `ChromePublic.apk` build artifacts.

---

## License

Helium Android code, UI components, and custom patches are licensed under the **GNU General Public License v3.0 (GPL-3.0)**. Code inherited from Chromium and upstream patchsets retains their respective original licenses (BSD 3-Clause, Apache 2.0).
