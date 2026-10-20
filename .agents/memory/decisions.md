# Architecture Decisions & Invariants

This document records durable architectural decisions, system invariants, and interface contracts for this project.

---

## Active Invariants

### 1. Dual-Layer Codebase Analysis Engine
- **Decision**: Codebase freshness is checked via `analyze.py --check --auto-heal --inject-ki` at the start of every session.
- **Rationale**: Prevents stale context without blocking user conversations, storing SQLite cache in `.agents/cache/codebase.db`.

### 2. Multi-Tier Security & Verification Gates
- **Decision**: All modified files and business logic must pass AAA unit test standards and Skylos SAST / AI hallucination verification.
- **Rationale**: Eliminates phantom imports, hallucinated package versions, and insecure sinks before commit.

### 3. Native MCP Server Protocol
- **Decision**: External tool integrations use the official Model Context Protocol (MCP) in `.agents/mcp_config.json`.

### 4. Reactive Async Task Notification & Live Status
- **Decision**: Always use the reactive "Waiting for a task notification" approach with live status rather than polling loops or redundant timers.
- **Rationale**: The IDE event loop automatically resumes execution when background tasks complete. Polling or using blind sleep/timers wastes tokens, introduces latency, and adds noise.

### 5. Single-File Analysis Whole-Read Invariant
- **Decision**: When analyzing a single file, read the entire file in a single tool call without piecemeal slicing.
- **Rationale**: Prevents fragmented code context, eliminates repeated round-trips for contiguous lines, and guarantees complete semantic understanding of the file.

### 6. Pre-Execution Tool Optimization & Context Economy
- **Decision**: Prior to triggering any action, assess the action against available tools to choose the most token-efficient, fast, and direct execution path.
- **Rationale**: Using the best-fit tool (dedicated search/edit/read/async primitives over shell commands or fragmented multi-step calls) preserves context window, eliminates round-trip latency, and prevents hallucination.

### 7. Anchored Root-Level Path Exclusions in .gitignore
- **Decision**: Repository root checkouts (like Chromium source trees) must be anchored with a leading slash (e.g. `/src/`) in `.gitignore`.
- **Rationale**: Unanchored patterns match recursively at any directory depth, inadvertently ignoring subproject application source code (e.g. `helium-android/src/`).

### 8. Deep Mobile Browser Subsystem Architecture
- **Decision**: Every browser capability (`TabManager`, `AdBlockEngine`, `BangsRouter`, `UrlTrackerStripper`, `HttpsUpgrader`, `MediaPlaybackHost`, `HeliumDbHelper`, `VaultCrypto`) must be isolated as an independent, deep module with a compact public API.
- **Rationale**: Keeps `HeliumMainActivity` as a coordinator rather than a monolith. Enables subresource network blocking before sockets open, non-blocking asynchronous SQLite persistence, and portable unit testing with zero Gradle/Chromium build overhead.

### 9. Zero-Network Built-in AdBlock Engine
- **Decision**: Keep the built-in 500+ host filter list in memory without background network subscription fetching.
- **Rationale**: Eliminates background battery drain, avoids untrusted external list fetches, and provides instant zero-latency subresource blocking.

### 10. User-Configurable Tab Session Restoration
- **Decision**: Tab restoration behavior is user-configurable in Helium Preferences (restore last session tabs or start clean with Helium Start Page), strictly isolating incognito tabs from disk serialization.
- **Rationale**: Accommodates user privacy preferences, prevents sensitive incognito URLs from touching flash storage, and delivers instant session resumption.

### 11. Omnibox Client-Side Suggestions & Zero-Redirect Bangs
- **Decision**: Real-time autocomplete suggestions prioritize local async SQLite history/bookmarks and 50+ instant DuckDuckGo/Helium `!bang` shortcuts directly on device without keystroke telemetry or remote search suggestion logging.
- **Rationale**: Preserves zero-knowledge privacy, eliminates server roundtrip latency on search shortcuts, and provides instant interactive omnibox navigation.

### 12. Privacy Shields Real-Time Auditability
- **Decision**: The SSL lock icon in the top toolbar anchors a dedicated `PrivacyShieldDialogFragment` displaying real-time counts of blocked trackers/ads, HTTPS upgrade status, and per-site privacy controls.
- **Rationale**: Gives users complete transparency and control over what the browser is blocking on every active web view.

### 13. Mobile WebExtension JavascriptInterface Bridge
- **Decision**: `WebExtensionApiBridge` exposes `window.__helium_ext_bridge__` with client-side polyfills for `chrome.runtime.sendMessage` and `chrome.storage.local` backed by isolated Android `SharedPreferences`.
- **Rationale**: Allows sideloaded Manifest V2/V3 packages and userscripts to persist settings and dispatch messages within mobile WebViews without requiring full Chromium multi-process extension daemons.

### 14. Tab Organization, Granular Permissions, and Profile Environments
- **Decision**: `TabGroup` and pinned tabs (M06) protect critical workflows during bulk closure and serialize cleanly across sessions. `SitePermissionsManager` (M18) manages per-origin sensor grants via in-memory caches and SQLite. `ProfileManager` (M21) isolates browsing environments, `PwaShortcutManager` (M25) pins web apps via Android `ShortcutManager`, and `HeliumDiagnostics` (M28) enforces strict URL and credential redaction.
- **Rationale**: Delivers comprehensive desktop-to-mobile PRD parity while preserving touch ergonomics, zero-network battery efficiency, and zero-knowledge privacy.

### 15. Standalone PWA Architecture & Manifest Resolution
- **Decision**: Home-screen web applications (M25) parse standard W3C Web App Manifest JSON via `WebAppManifest`, resolving relative URLs against base origins and extracting display modes and theme colors. Pinned shortcuts launch into `PwaActivity` configured with `android:documentLaunchMode="intoExisting"` for independent Android Recents task switcher cards, frameless topbar presentation, and out-of-scope link breakout to `HeliumMainActivity`.
- **Rationale**: Replaces clunky browser tabs with native app window presentation, dynamic status bar tinting, built-in adblocking, and smooth task switcher multitasking without native APK overhead.

### 16. Launcher Search Widget, Translucent Overlay & Zero-Duplication Mini-Browser
- **Decision**: Home-screen search launcher widget (`SearchWidgetProvider`) routes taps to `QuickSearchOverlayActivity` (translucent window with soft input visible, 250ms debounced omnibox suggestions, and outside tap interception consuming the first touch). Submission opens a centered, draggable and resizable floating mini-browser running the active Helium `Tab`. Tapping "Open in full browser" (`extra_tab_id`) detaches the live `WebView` and promotes the exact existing tab directly into `HeliumMainActivity` with zero duplicate tabs, preserving tab identity, cookies, navigation history, and renderer state.
- **Rationale**: Eliminates the Android `RemoteViews` `EditText` inflation constraint, prevents duplicate tab sprawl, and provides instant, seamless search and floating multi-tasking directly from the launcher.
