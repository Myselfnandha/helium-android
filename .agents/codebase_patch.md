# Codebase Patches: Helium

## 2026-10-19 21:28:01 (34 files changed)
- `.github/workflows/build.yml` — GitHub Actions workflow
- `.github/workflows/release.yml` — GitHub Actions workflow
- `.gitignore` — Android Build Outputs & Keystores .apk (35 lines)
- `CAPABILITY_MAP.md` — Capability Map: Helium Browser for Android (27 lines)
- `README.md` — Helium Browser for Android (77 lines)
- `SPEC.md` — Spec: Helium Browser for Android (185 lines)
- `docs/ARCHITECTURE.md` — Helium Browser for Android: Architecture Guide (50 lines)
- `helium-android/res/drawable/bg_card_rounded.xml` — XML file: 9 lines
- `helium-android/res/drawable/ic_extension_puzzle.xml` — XML file: 9 lines
- `helium-android/res/drawable/ic_helium_logo.xml` — XML file: 16 lines
- `helium-android/res/drawable/ic_shield_check.xml` — XML file: 9 lines
- `helium-android/res/drawable/ic_sync_vault.xml` — XML file: 9 lines
- `helium-android/res/layout/activity_helium_first_run.xml` — XML file: 213 lines
- `helium-android/res/values-night/colors.xml` — XML file: 18 lines
- `helium-android/res/values/colors.xml` — XML file: 18 lines
- `helium-android/res/values/strings.xml` — XML file: 25 lines
- `helium-android/res/xml/helium_preferences.xml` — XML file: 63 lines
- `patches/01-vanadium-security/0001-disable-v8-jit-by-default.patch` — - a/chrome/browser/about_flags.cc (31 lines)
- `patches/01-vanadium-security/0002-webrtc-ip-handling-policy.patch` — - a/chrome/browser/ui/browser_ui_prefs.cc (23 lines)
- `patches/01-vanadium-security/README.md` — 01-vanadium-security (5 lines)
- `patches/02-extensions-runtime/0001-enable-android-extensions-core.patch` — - a/extensions/buildflags/buildflags.gni (22 lines)
- `patches/02-extensions-runtime/0002-chrome-web-store-desktop-routing.patch` — - a/chrome/browser/extensions/webstore_installer.cc (26 lines)
- `patches/03-adblock-engine/0001-bromite-subresource-filter-core.patch` — - a/components/subresource_filter/content/browser/content_subresource_filter_throttle_m... (23 lines)
- `patches/03-adblock-engine/0002-custom-filters-subscription.patch` — - a/components/subresource_filter/core/browser/subresource_filter_constants.cc (25 lines)
- `patches/04-vault-sync/0001-hook-helium-vault-sync-engine.patch` — - a/components/sync/service/sync_service_impl.cc (26 lines)
- `patches/05-helium-ui/0001-integrate-helium-first-run-flow.patch` — - a/chrome/android/java/src/org/chromium/chrome/browser/firstrun/FirstRunFlowSequencer.... (32 lines)
- `patches/05-helium-ui/0002-helium-mobile-start-page.patch` — - a/chrome/android/java/src/org/chromium/chrome/browser/ntp/NewTabPageLayout.java (23 lines)
- `scripts/build.sh` — Shell script (env); functions: usage
- `scripts/lint.sh` — Shell script (env)
- `scripts/setup_toolchain.sh` — Shell script (env)
- `scripts/validate_patches.sh` — Shell script (env)
- `subprojects_patches/third_party/ublock_origin.patch` — - a/chrome/browser/resources/BUILD.gn (28 lines)
- `tasks/plan.md` — Implementation Plan: Helium Browser for Android (71 lines)
- `tasks/todo.md` — Helium Browser for Android: Task List (218 lines)

