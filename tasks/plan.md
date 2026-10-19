# Implementation Plan: Helium Browser for Android

## Overview
Helium Browser for Android is an open-source, hardened mobile Chromium browser bringing Helium's visual identity, Private Vault Sync, desktop extension support (uBlock Origin), and native adblocking to Android devices. This plan organizes the implementation into 5 sequential phases following vertical slicing, explicit checkpoints, and source-driven documentation.

## Architecture Decisions
1. **Cloud CI Compilation**: Local compilation of Chromium for Android exceeds the 14GB RAM threshold; all heavy binary compilations (`libchrome.so`, `monochrome_64.apk`) are orchestrated via GitHub Actions workflows on cloud runners (`ubuntu-latest`).
2. **Hardened Upstream Core**: Base Chromium patches inherit GrapheneOS Vanadium security hardening (disabled V8 JIT by default, hardened allocator, zero-telemetry ungoogled baseline).
3. **Mobile Extension Runtime**: Porting Titanium/Kiwi mobile extension subsystem into Chromium's Android Java and C++ layers to allow desktop extensions (including Helium's uBlock Origin fork).
4. **Independent Startup Toggles**: All core modules (JIT protection, Extension engine, Native adblocker, Vault Sync) are configurable on initial launch via `HeliumFirstRunActivity` and persistently managed via `HeliumPreferences`.
5. **E2EE Private Vault Sync**: Direct port of `helium/core/sync/vault` protocol with C++ JNI bindings and an Android Sync Service, eliminating reliance on Google Account sync.

---

## Task List

### Phase 1: Foundation & Tooling
- [ ] **Task 1: Repository & Git Structure Setup**
  - Initialize git repository, `.gitignore`, and base folder hierarchy (`patches/`, `helium-android/`, `scripts/`, `.github/`).
- [ ] **Task 2: Build & Patch Automation Scripts**
  - Create `scripts/validate_patches.sh`, `scripts/setup_toolchain.sh`, `scripts/build.sh`, and `scripts/lint.sh`.

#### Checkpoint: Foundation
- [ ] Scripts pass bash syntax checks (`bash -n`).
- [ ] Folder structure matches [SPEC.md](file:///home/nandha/Desktop/Helium/SPEC.md).

---

### Phase 2: Security & Core Patches
- [ ] **Task 3: Vanadium Security & JIT Patchset (`01-vanadium-security`)**
  - Create patch series for V8 JIT toggle command-line switches, anti-fingerprinting, and WebRTC privacy.
- [ ] **Task 4: Mobile Extension Engine Patchset (`02-extensions-runtime`)**
  - Create patch series for Android extension runtime, Chrome Web Store desktop routing, and MV2/MV3 execution.
- [ ] **Task 5: Native C++ Adblock Filter Patchset (`03-adblock-engine`)**
  - Integrate Bromite subresource filter patches and custom ruleset subscription loader.

#### Checkpoint: Core Patches
- [ ] Patch files are syntactically valid unified diffs.
- [ ] `scripts/validate_patches.sh` completes with 0 errors.

---

### Phase 3: Helium Mobile UI & First-Run Wizard
- [ ] **Task 6: Helium Android Visual Identity & Resources**
  - Add Helium vectors, color palette (dark slate `#0f172a`, cyan accent `#06b6d4`), and localized string resources.
- [ ] **Task 7: Helium First-Run Setup Wizard (`HeliumFirstRunActivity`)**
  - Build interactive onboarding screen with startup toggles for Extensions, Adblocking, JIT, and Sync.
- [ ] **Task 8: Helium Settings & Preferences Controller**
  - Implement `HeliumSettingsFragment` and `HeliumPreferences.java` to persist and synchronize configuration with Chromium flags.
- [ ] **Task 9: UI Integration Patchset (`05-helium-ui`)**
  - Patch Chromium Android launch pipeline to trigger Helium onboarding and apply the custom Start Page theme.

#### Checkpoint: Mobile UI
- [ ] Android layout XMLs, styles, and Java components pass syntax and formatting checks.
- [ ] Toggle state contracts match preferences specification.

---

### Phase 4: Helium Private Vault Sync
- [ ] **Task 10: Private Vault Sync C++ JNI Bridge**
  - Implement `helium_vault_jni.cc` and protocol headers adhering to `helium/core/sync/vault`.
- [ ] **Task 11: Android Sync Service & Integration Patch (`04-vault-sync`)**
  - Build `VaultSyncService.java`, `VaultAccountManager.java`, and the Chromium JNI hook patch.

#### Checkpoint: Vault Sync
- [ ] JNI method signatures align with Java native declarations.
- [ ] Vault sync patch applies cleanly to GN build targets.

---

### Phase 5: CI/CD Pipeline & Documentation
- [ ] **Task 12: GitHub Actions CI/CD Matrix & Release Workflows**
  - Configure `.github/workflows/build.yml` (multi-architecture APK compilation) and `release.yml` (signing & release publishing).
- [ ] **Task 13: Comprehensive Project Documentation & Setup Guide**
  - Author complete `README.md` with architecture guide, building instructions, and secrets setup for GitHub Actions.

#### Checkpoint: Complete
- [ ] All 13 tasks completed and verified.
- [ ] Repository is fully self-contained and ready to push to GitHub.

---

## Risks and Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
| Memory exhaustion during build | High | Completely delegate full Chromium compilation to GitHub Actions cloud runners; keep local tooling lightweight. |
| Patch drift across Chromium versions | Medium | Document exact target commit hash (`124.0.6367.82`) and provide `scripts/update_patches.sh` for rebase automation. |
| Extension crashes on low-RAM devices | Medium | Provide an instant startup toggle in `HeliumFirstRunActivity` allowing users to disable extensions for max performance. |
| Sync protocol mismatch with Desktop | Low | Strictly mirror `helium/core/sync/vault` commit schema and delta envelope in JNI bridge. |

## Open Questions
- None currently blocking; all decisions aligned via Socratic interview and approved in [SPEC.md](file:///home/nandha/Desktop/Helium/SPEC.md).
