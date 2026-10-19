# Capability Map: Helium Browser for Android

## System Overview
Helium Browser for Android is an open-source, hardened mobile Chromium browser bringing Helium's visual identity, Private Vault Sync, desktop extension support (uBlock Origin), and native adblocking to Android devices.

| Module ID | Responsibility | Depends On |
|---|---|---|
| `core-vanadium` | Base Chromium Android patchset, Vanadium security hardening, JIT toggle flags, Canvas/Audio anti-fingerprinting, URL tracker stripper, strict HTTPS-only mode, and Encrypted DNS (DoH/ECH) | — |
| `extensions-engine` | Kiwi/Titanium Android extension runtime, Chrome Web Store compatibility, unpacked CRX sideloading, native Helium Extension Manager UI, Helium uBlock Origin and Dark Reader bundles | `core-vanadium` |
| `adblock-filter` | Native C++ subresource filter engine (Bromite-based), custom filter list subscriptions, network-level tracker blocking | `core-vanadium` |
| `media-enhancements` | Background video and audio playback, lockscreen controls, and picture-in-picture persistence | `core-vanadium` |
| `vault-sync` | Helium Private Vault Sync (`helium/core/sync/vault`) protocol, E2EE key exchange, local SQLite/LevelDB state delta sync, Android background service | `core-vanadium` |
| `helium-ui` | Helium visual identity, dark/light theme, modern offline Start Page assets, First-Run Setup Wizard, and unified Helium Settings page | `core-vanadium`, `extensions-engine`, `adblock-filter`, `media-enhancements`, `vault-sync` |
| `ci-cd` | GitHub Actions workflow matrix, Docker build container setup, keystore signing, release automation for APKs (arm64-v8a, armv7a, x86_64) | `core-vanadium`, `helium-ui` |

## Build & Integration Order
```
core-vanadium
      │
      ├───► extensions-engine ──┐
      │                         │
      ├───► adblock-filter ─────┼
      │                         │
      ├───► media-enhancements ─┼───► helium-ui ───► ci-cd
      │                         │
      └───► vault-sync ─────────┘
```

1. **`core-vanadium`**: Base upstream patches, security toggles, Vanadium/ungoogled core.
2. **`extensions-engine`**: Mobile extension framework integration.
3. **`adblock-filter`**: Engine-level adblocking filter integration.
4. **`vault-sync`**: E2EE private sync client and Android sync provider.
5. **`helium-ui`**: UI layers, first-run wizard, and settings controls binding all module toggles.
6. **`ci-cd`**: GitHub Actions workflows to compile, test, sign, and release the APKs.
