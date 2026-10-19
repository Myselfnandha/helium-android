# Helium Browser for Android: Architecture Guide

## Overview
Helium Browser for Android is an open-source, privacy-first mobile web browser designed to bring desktop Helium's signature capabilities to Android:
1. **Desktop Extensions on Mobile**: Powered by the Kiwi/Titanium mobile extension runtime, enabling extensions like Helium's custom uBlock Origin fork directly on Android devices.
2. **Native C++ Engine Adblocking**: Derived from Bromite's subresource filter engine, filtering network requests before DOM parsing without performance degradation.
3. **Vanadium Hardening**: Inheriting GrapheneOS Vanadium exploit mitigations (disabling V8 JIT by default to close common remote code execution attack vectors).
4. **Private Vault Sync**: Cross-device, end-to-end encrypted synchronization protocol (`helium/core/sync/vault`) pairing Android devices with desktop Helium instances without Google Play Services.

---

## Architectural Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Helium UI & Java Layer                   │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │ HeliumFirstRunActivity│       │ HeliumSettingsFragment│  │
│  └───────────┬───────────┘       └───────────┬───────────┘  │
│              │                               │              │
│              ▼                               ▼              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │            HeliumPreferences Controller               │  │
│  └───────┬──────────────┬──────────────┬─────────────┬───┘  │
└──────────┼──────────────┼──────────────┼─────────────┼──────┘
           │              │              │             │
┌──────────▼──────────┐   │              │   ┌─────────▼──────────┐
│  Desktop Extensions │   │              │   │ Private Vault Sync │
│  (Kiwi/Titanium UI) │   │              │   │ (E2EE Client JNI)  │
└──────────┬──────────┘   │              │   └─────────┬──────────┘
           │              │              │             │
═══════════╪══════════════╪══════════════╪═════════════╪═══════
           │ JNI / C++    │              │             │
┌──────────▼──────────┐   │              │   ┌─────────▼──────────┐
│ ExtensionService CC │   │              │   │ helium_vault_jni.cc│
└─────────────────────┘   ▼              ▼   └────────────────────┘
               ┌────────────────┐ ┌────────────────┐
               │Bromite C++     │ │ Vanadium V8 JIT│
               │Subresource     │ │ Hardening &    │
               │Filter Engine   │ │ WebRTC Policy  │
               └────────────────┘ └────────────────┘
```

---

## Startup Configuration Flow
When Helium Android is launched for the first time:
1. Chromium's `FirstRunFlowSequencer` queries `HeliumPreferences.isFirstRunCompleted()`.
2. If false, `HeliumFirstRunActivity` intercepts the launch flow.
3. The user is presented with independent toggle cards:
   - **Desktop Extensions**: Enable/Disable desktop extension manager.
   - **Native Adblocker**: Enable/Disable C++ engine subresource filter.
   - **Vanadium JIT**: Enable/Disable V8 JIT compiler protection.
   - **Private Vault Sync**: Set up E2EE pairing with desktop Helium.
4. User choices are persisted to SharedPreferences and mapped to Chromium startup command-line switches (`--disable-v8-jit`, `--enable-mobile-extensions`, `--enable-native-adblock`, `--enable-helium-vault-sync`).
5. On subsequent runs, these settings remain fully configurable via `HeliumSettingsFragment` in the main menu.
