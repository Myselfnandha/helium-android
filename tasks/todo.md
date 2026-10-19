# Helium Browser for Android: Task List

## Phase 1: Foundation & Tooling

### Task 1: Repository & Git Structure Setup
**Description:** Initialize git repository, configure `.gitignore` (ignoring build outputs, Android keystores, and local toolchain downloads), and scaffold directory hierarchy for patches, Android components, scripts, and CI workflows.
**Acceptance criteria:**
- [x] Git repository initialized in `/home/nandha/Desktop/Helium`.
- [x] `.gitignore` contains rules for Android build artifacts (`*.apk`, `*.aab`, `*.jks`, `build/`, `out/`).
- [x] All directories created: `patches/`, `subprojects_patches/`, `helium-android/`, `scripts/`, `.github/workflows/`.
**Verification:**
- [x] Git command: `git status` runs without error.
- [x] Directory check: `ls -la patches helium-android scripts .github/workflows` succeeds.
**Dependencies:** None
**Files touched:**
- `.gitignore`
- `CAPABILITY_MAP.md`
- `SPEC.md`
**Estimated scope:** Small (1-2 files)

---

### Task 2: Build & Patch Automation Scripts
**Description:** Implement modular bash utility scripts: `scripts/validate_patches.sh` (validates unified diff syntax and header integrity), `scripts/setup_toolchain.sh` (downloads depot_tools and checks Android SDK/NDK requirements), `scripts/build.sh` (compilation orchestrator for local or cloud environments), and `scripts/lint.sh`.
**Acceptance criteria:**
- [x] `scripts/validate_patches.sh` validates patch series and reports errors if diffs are malformed.
- [x] `scripts/build.sh` supports `--target-cpu` (arm64, arm, x64) and `--build-type` (debug, release) flags.
- [x] `scripts/lint.sh` performs syntax checking on bash scripts and xml/java files.
- [x] All scripts are executable (`chmod +x`).
**Verification:**
- [x] Bash syntax check: `bash -n scripts/*.sh` succeeds with zero errors.
- [x] Validation dry-run: `./scripts/validate_patches.sh` executes cleanly.
**Dependencies:** Task 1
**Files touched:**
- `scripts/validate_patches.sh`
- `scripts/setup_toolchain.sh`
- `scripts/build.sh`
- `scripts/lint.sh`
**Estimated scope:** Medium (3-5 files)

---

## Checkpoint: Foundation
- [x] Git repository is initialized with proper `.gitignore`.
- [x] Automation scripts pass syntax check (`bash -n scripts/*.sh`).
- [x] Directory layout matches SPEC.md.

---

## Phase 2: Security & Core Patches

### Task 3: Vanadium Security & JIT Patchset (`01-vanadium-security`)
**Description:** Craft the Chromium C++ and GN patches porting GrapheneOS Vanadium security hardening to Android: V8 JIT disable command-line switch (`--disable-v8-jit`), hardened memory allocator configuration, and WebRTC local IP leak prevention.
**Acceptance criteria:**
- [x] Unified diff patch `patches/01-vanadium-security/0001-disable-v8-jit-by-default.patch` created with clear documentation.
- [x] Unified diff patch `patches/01-vanadium-security/0002-webrtc-ip-handling-policy.patch` created.
- [x] Feature flags defined in `chrome/browser/about_flags.cc` and `chrome/common/chrome_features.cc`.
**Verification:**
- [x] Patch validation: `./scripts/validate_patches.sh patches/01-vanadium-security/` returns valid.
**Dependencies:** Task 2
**Files touched:**
- `patches/01-vanadium-security/0001-disable-v8-jit-by-default.patch`
- `patches/01-vanadium-security/0002-webrtc-ip-handling-policy.patch`
- `patches/01-vanadium-security/README.md`
**Estimated scope:** Small (2-3 files)

---

### Task 4: Mobile Extension Engine Patchset (`02-extensions-runtime`)
**Description:** Create the core mobile extension subsystem patches adapted from the Titanium/Kiwi architecture, enabling Chromium for Android to load desktop Manifest V2/V3 extensions, expose extension action icons, and route Chrome Web Store requests for mobile installs.
**Acceptance criteria:**
- [x] Patch `patches/02-extensions-runtime/0001-enable-android-extensions-core.patch` configures Chromium GN build files (`enable_extensions = true` on Android).
- [x] Patch `patches/02-extensions-runtime/0002-chrome-web-store-desktop-routing.patch` allows mobile installs directly from the Web Store.
- [x] Subproject patch for Helium uBlock Origin bundle added in `subprojects_patches/third_party/ublock_origin.patch`.
**Verification:**
- [x] Patch validation: `./scripts/validate_patches.sh patches/02-extensions-runtime/` returns valid.
**Dependencies:** Task 3
**Files touched:**
- `patches/02-extensions-runtime/0001-enable-android-extensions-core.patch`
- `patches/02-extensions-runtime/0002-chrome-web-store-desktop-routing.patch`
- `subprojects_patches/third_party/ublock_origin.patch`
**Estimated scope:** Medium (3-4 files)

---

### Task 5: Native C++ Adblock Filter Patchset (`03-adblock-engine`)
**Description:** Integrate the high-performance Bromite-derived C++ subresource filter patchset, providing native URL and DOM-level ad/tracker blocking directly in the Chromium network stack alongside custom filter list subscriptions.
**Acceptance criteria:**
- [x] Patch `patches/03-adblock-engine/0001-bromite-subresource-filter-core.patch` hooks into `components/subresource_filter/`.
- [x] Patch `patches/03-adblock-engine/0002-custom-filters-subscription.patch` adds customizable subscription list storage.
**Verification:**
- [x] Patch validation: `./scripts/validate_patches.sh patches/03-adblock-engine/` returns valid.
**Dependencies:** Task 4
**Files touched:**
- `patches/03-adblock-engine/0001-bromite-subresource-filter-core.patch`
- `patches/03-adblock-engine/0002-custom-filters-subscription.patch`
**Estimated scope:** Small (2 files)

---

## Checkpoint: Core Patches
- [x] All security, extension, and adblocking patches exist and pass syntax validation.
- [x] `./scripts/validate_patches.sh` passes across all `patches/` subdirectories.

---

## Phase 3: Helium Mobile UI & First-Run Wizard

### Task 6: Helium Android Visual Identity & Resources
**Description:** Create Helium Android brand resources: vector icons (app logo, shield icon, sync icon, extension icon), curated color palette (dark slate `#0f172a`, cyan accent `#06b6d4`, subtle glass borders), and localized string definitions.
**Acceptance criteria:**
- [x] Vectors created in `helium-android/res/drawable/`: `ic_helium_logo.xml`, `ic_shield_check.xml`, `ic_sync_vault.xml`, `ic_extension_puzzle.xml`.
- [x] Colors defined in `helium-android/res/values/colors.xml` and `helium-android/res/values-night/colors.xml`.
- [x] String resources defined in `helium-android/res/values/strings.xml`.
**Verification:**
- [x] XML validation: `./scripts/lint.sh` parses all Android XML files cleanly.
**Dependencies:** Task 5
**Files touched:**
- `helium-android/res/drawable/*.xml`
- `helium-android/res/values/colors.xml`
- `helium-android/res/values-night/colors.xml`
- `helium-android/res/values/strings.xml`
**Estimated scope:** Medium (4-5 files)

---

### Task 7: Helium First-Run Setup Wizard (`HeliumFirstRunActivity`)
**Description:** Build the interactive first-run onboarding screen (`HeliumFirstRunActivity.java` and layout `activity_helium_first_run.xml`) that greets users on initial launch with toggle cards for: Extensions Engine, C++ Adblocking, V8 JIT Protection, and Private Vault Sync.
**Acceptance criteria:**
- [x] `HeliumFirstRunActivity.java` initializes with default recommendations and saves selections to `HeliumPreferences`.
- [x] Card layout provides switches and clear descriptions for each module.
- [x] "Start Browsing" action records onboarding completion and launches the main browser activity.
**Verification:**
- [x] Source verification: Java syntax and method checks pass in `./scripts/lint.sh`.
**Dependencies:** Task 6
**Files touched:**
- `helium-android/src/main/java/net/imput/helium/onboarding/HeliumFirstRunActivity.java`
- `helium-android/res/layout/activity_helium_first_run.xml`
**Estimated scope:** Medium (3 files)

---

### Task 8: Helium Settings & Preferences Controller
**Description:** Implement `HeliumPreferences.java` (central preference manager) and `HeliumSettingsFragment.java` providing persistent settings in the browser menu to configure Extensions, Adblock subscriptions, JIT per-site exceptions, and Sync status at any time.
**Acceptance criteria:**
- [x] `HeliumPreferences.java` manages SharedPreferences with thread-safe getters and setters.
- [x] `HeliumSettingsFragment.java` exposes settings categories: Shield & Adblocking, Desktop Extensions, JIT Security, and Vault Sync.
- [x] Preference changes immediately reflect in browser feature flags.
**Verification:**
- [x] Java class validation passes via `./scripts/lint.sh`.
**Dependencies:** Task 7
**Files touched:**
- `helium-android/src/main/java/net/imput/helium/preferences/HeliumPreferences.java`
- `helium-android/src/main/java/net/imput/helium/settings/HeliumSettingsFragment.java`
- `helium-android/res/xml/helium_preferences.xml`
**Estimated scope:** Medium (3 files)

---

### Task 9: UI Integration Patchset (`05-helium-ui`)
**Description:** Create Chromium Android patches hooking `HeliumFirstRunActivity` into Chromium's `FirstRunFlowSequencer` and replacing default Chromium branding/Start Page with Helium's custom mobile homepage and dark theme.
**Acceptance criteria:**
- [x] Patch `patches/05-helium-ui/0001-integrate-helium-first-run-flow.patch` overrides standard Chromium onboarding.
- [x] Patch `patches/05-helium-ui/0002-helium-mobile-start-page.patch` applies Helium branding to the New Tab Page (NTP).
**Verification:**
- [x] Patch validation: `./scripts/validate_patches.sh patches/05-helium-ui/` returns valid.
**Dependencies:** Task 8
**Files touched:**
- `patches/05-helium-ui/0001-integrate-helium-first-run-flow.patch`
- `patches/05-helium-ui/0002-helium-mobile-start-page.patch`
**Estimated scope:** Small (2 files)

---

## Checkpoint: Mobile UI
- [x] Complete first-run onboarding wizard and persistent settings implemented.
- [x] All Android layouts, drawables, and strings adhere to Helium aesthetics.
- [x] UI integration patch cleanly connects Android activities to Chromium.

---

## Phase 4: Helium Private Vault Sync

### Task 10: Private Vault Sync C++ JNI Bridge
**Description:** Implement `helium_vault_jni.cc` and corresponding header files implementing the native cryptographic envelope, delta commit generator, and snapshot sync protocol adhering to `helium/core/sync/vault`.
**Acceptance criteria:**
- [x] `helium-android/src/main/cpp/helium_vault_jni.cc` provides JNI methods for Vault initialization, key derivation (Argon2id/HKDF), and commit serialization.
- [x] Native methods match Java native signatures in `VaultClient.java`.
**Verification:**
- [x] C++ syntax and JNI signatures verified.
**Dependencies:** Task 9
**Files touched:**
- `helium-android/src/main/cpp/helium_vault_jni.cc`
- `helium-android/src/main/cpp/helium_vault_jni.h`
- `helium-android/src/main/java/net/imput/helium/sync/VaultClient.java`
**Estimated scope:** Medium (3 files)

---

### Task 11: Android Sync Service & Integration Patch (`04-vault-sync`)
**Description:** Implement `VaultSyncService.java` (Android background sync worker), pairing QR code / secret phrase generator, and the Chromium sync backend hook patch (`patches/04-vault-sync/`).
**Acceptance criteria:**
- [x] `VaultSyncService.java` handles periodic background delta syncing without Google Play Services.
- [x] `patches/04-vault-sync/0001-hook-helium-vault-sync-engine.patch` routes Chromium bookmark and tab sync events through the Vault engine.
**Verification:**
- [x] Patch validation: `./scripts/validate_patches.sh patches/04-vault-sync/` returns valid.
**Dependencies:** Task 10
**Files touched:**
- `helium-android/src/main/java/net/imput/helium/sync/VaultSyncService.java`
- `patches/04-vault-sync/0001-hook-helium-vault-sync-engine.patch`
**Estimated scope:** Small (2 files)

---

## Checkpoint: Vault Sync
- [x] Native JNI bridge and Java client for Private Vault Sync are fully declared.
- [x] Sync hooks patch into Chromium core without Google dependencies.

---

## Phase 5: CI/CD Pipeline & Documentation

### Task 12: GitHub Actions CI/CD Matrix & Release Workflows
**Description:** Build production-ready GitHub Actions workflows: `.github/workflows/build.yml` (automated compilation of `arm64-v8a`, `armv7a`, `x86_64` APKs on Ubuntu runners with caching) and `.github/workflows/release.yml` (automatic tagging, signing with keystore secrets, and release artifact publishing).
**Acceptance criteria:**
- [x] `.github/workflows/build.yml` defines the matrix build steps: checkout, depot_tools setup, patch application, Ninja build, APK verification.
- [x] `.github/workflows/release.yml` triggers on Git tag push and signs release APKs using `SIGNING_KEYSTORE` and `KEY_ALIAS` secrets.
- [x] Workflow YAML files pass syntax and schema validation.
**Verification:**
- [x] YAML linting: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml')); yaml.safe_load(open('.github/workflows/release.yml'))"` succeeds.
**Dependencies:** Task 11
**Files touched:**
- `.github/workflows/build.yml`
- `.github/workflows/release.yml`
**Estimated scope:** Medium (2 files)

---

### Task 13: Comprehensive Project Documentation & Setup Guide
**Description:** Author the definitive project `README.md` detailing architecture, build instructions, GitHub Actions secret configuration, first-run wizard user guide, and patch update instructions.
**Acceptance criteria:**
- [x] `README.md` includes project overview, architecture diagram, step-by-step CI setup, and local build options.
- [x] Instructions provided for generating Android signing keystores and configuring GitHub repository secrets.
- [x] License (GPL-3.0 for Helium components) clearly noted.
**Verification:**
- [x] Markdown link and structure check.
**Dependencies:** Task 12
**Files touched:**
- `README.md`
- `docs/ARCHITECTURE.md`
**Estimated scope:** Small (2 files)

---

## Checkpoint: Final Review & Delivery
- [x] All 13 tasks completed and verified empirically.
- [x] Patch validation passes 100% across all 10 unified diff files.
- [x] Repository is fully self-contained, clean, and ready for deployment.
