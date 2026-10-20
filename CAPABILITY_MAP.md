# Capability Map: Helium Browser for Android

## System Overview
Helium Browser for Android is an open-source, hardened mobile Chromium browser bringing Helium's visual identity, Private Vault Sync, desktop extension support (uBlock Origin), and native adblocking to Android devices.

| Module ID | Responsibility | Interface Seam | Depends On |
|---|---|---|---|
| `core-vanadium` | Base Chromium Android patchset, Vanadium security hardening, JIT toggle flags, and Encrypted DNS (DoH/ECH) | JNI / Command line switch | — |
| `adblock-filter` | Native C++ subresource filter & Java `AdBlockEngine` blocking 500+ ad/tracker hosts in-memory | `WebViewClient.shouldInterceptRequest()` | `core-vanadium` |
| `privacy-shields` | `HeliumNoiseController` (Canvas/Audio/WebGL), `UrlTrackerStripper`, and `HttpsUpgrader` | `ExtensionRuntimeInjector` & Omnibox routing | `core-vanadium` |
| `search-bangs` | `BangsRouter` with 50+ privacy and developer search shortcut tags (`!w`, `!gh`, `!yt`, `!ddg`) | Omnibox submission path | — |
| `tab-session` | `TabManager` and `Tab` models managing multi-tab lifecycle, active selection, incognito isolation, and memory pausing | `TabManager.addListener()` & `TabsDialogFragment` | — |
| `tab-groups-pins` | `TabGroup`, color badges, pinned tab protection from closure, and group-aware session persistence | `TabManager` & `TabsDialogFragment` | `tab-session` |
| `storage-db` | `HeliumDbHelper` SQLite backing store for History, Bookmarks, and Site Permissions with background thread pool | `HeliumDbHelper.Callback<T>` | — |
| `bookmark-folders`| `BookmarkFolder` hierarchical folder entity with circular nesting and cycle prevention | `HeliumDbHelper` | `storage-db` |
| `site-permissions`| `SitePermissionsManager` granular per-origin sensor and capability authorization (Camera, Mic, Geo, Notifications, Clipboard) | `WebChromeClient` callbacks | `storage-db` |
| `profile-environments` | `ProfileManager` and `Profile` persistent browsing environments and switching | `ProfileManager.addListener()` | — |
| `search-widget-overlay` | `SearchWidgetProvider` launcher AppWidget, `WidgetPreferencesStore`, `SearchHistoryStore`, `QuickSearchOverlayActivity` translucent typing overlay, 250ms debounced native autocomplete, and centered draggable/resizable floating mini-browser with zero-duplication tab promotion to `HeliumMainActivity` | `PendingIntent` & `HeliumMainActivity.extra_tab_id` | `tab-session`, `helium-ui` |
| `pwa-standalone` | `PwaShortcutManager`, `WebAppManifest` W3C parser, and `PwaActivity` standalone frameless presentation window with separate task switcher integration | `HeliumMainActivity` overflow menu & System launcher shortcuts | — |
| `privacy-diagnostics` | `HeliumDiagnostics` privacy-safe error logging, URL redaction, credential scrubbing, and failure metrics | Cross-cutting error hooks | — |
| `extensions-engine` | Kiwi/Titanium Android extension runtime, Chrome Web Store compatibility, CRX sideloading, and `ExtensionRuntimeInjector` | WebExtension background worker & DOM injection | `core-vanadium` |
| `media-enhancements` | `MediaPlaybackHost` with `PARTIAL_WAKE_LOCK` and `visibilityState` spoofing for background video/audio | Activity lifecycle (`onPause`/`onResume`) | `core-vanadium` |
| `vault-sync` | Helium Private Vault Sync (`helium/core/sync/vault`) E2EE protocol, `VaultCrypto` (AES-256-GCM + PBKDF2), and `VaultBundle` | `VaultClient` & Android Sync Adapter | `core-vanadium`, `storage-db` |
| `helium-ui` | Helium visual identity, dark/light theme, modern offline Start Page assets, First-Run Setup Wizard, Tabs bottom sheet, and Settings | Android Views & `ChromeBaseAppCompatActivity` | All modules |
| `ci-cd` | GitHub Actions workflow matrix, permanent keystore signing, automated checksums, and cloud threat scanner verification | GitHub Actions workflows | All modules |

## Build & Integration Order
```
core-vanadium
      │
      ├───► adblock-filter ─────┐
      ├───► privacy-shields ────┤
      ├───► search-bangs ───────┼───► tab-session ───► helium-ui ───► ci-cd
      ├───► extensions-engine ──┤          ▲
      ├───► media-enhancements ─┤          │
      └───► storage-db ─────────┴───► vault-sync
```

1. **`core-vanadium`**: Base upstream patches, security toggles, Vanadium/ungoogled core.
2. **`adblock-filter` & `privacy-shields`**: Zero-latency in-memory request filtering, noise injection, and URL stripping.
3. **`search-bangs` & `storage-db`**: Instant query resolution and background SQLite storage.
4. **`tab-session`**: Multi-tab lifecycle controller and background memory throttling.
5. **`vault-sync`**: E2EE private sync client and state delta serializer.
6. **`helium-ui`**: Touch-optimized interface, tabs switcher, and settings controls binding all module toggles.
7. **`ci-cd`**: GitHub Actions workflows to compile, test, sign, and release the APKs.
