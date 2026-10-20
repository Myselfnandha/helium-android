# 🛡️ ZeroGravity OS: Anti-Patterns Matrix & Known Gotchas

This document serves as the **Persistent Anti-Patterns Matrix** for ZeroGravity OS.
Before taking any architectural, coding, or tooling action, the agent must sweep this matrix (Stage 1) to immunize itself against known failure modes.

---

## 📊 Anti-Patterns & Invariant Prevention Matrix

| ID | ⚠️ Mistake / Anti-Pattern | 🔍 Root Cause & Triggers | 🛡️ Invariant Prevention Strategy |
| :--- | :--- | :--- | :--- |
| **AP-001** | **Unchecked npx execution polluting JSON-RPC stdout with colored notices and ANSI escape sequences** | npx in modern npm writes colored notices to stdout when not silenced, breaking JSON-RPC parsing (invalid character '\x1b') | Always wrap Node-based MCP servers using mcp-npx with NO_COLOR=1, npm_config_loglevel=silent, and --silent flag |
| **AP-002** | **Sentry MCP server falling back to interactive OAuth device code flow and hanging** | @sentry/mcp-server defaults to interactive device code login if --access-token or SENTRY_ACCESS_TOKEN is omitted | Pass --access-token ${SENTRY_AUTH_TOKEN} explicitly in CLI arguments and provide environment fallback |
| **AP-003** | **Codebase indexer scanning self-extracting archive payload and inflating codebase database to 40MB+** | Scanning binary payloads or packed archive files in analyze.py treats binary blobs as source text | Exclude *.tar.gz, *.tar.xz, install.sh, and dist/ in codebase indexing routines |
| **AP-004** | **Leaving MCP servers running permanently in background consuming CPU and RAM** | Static mcp_config.json with default background servers creates memory bloat and port collisions | Zero-Default profile: start with empty mcpServers ({}), enable dynamically on-demand, and auto-close upon task completion |
| **AP-005** | **Agent looping over view_file sequentially to understand an entire folder codebase** | Sequential view_file calls waste context tokens and cause high latency | Execute analyze.py --check --auto-heal --inject-ki and read the consolidated codebase_summary.md in a single read |
| **AP-006** | **Refactoring code outside the user's requested scope** | Agent interprets adjacent code as needing improvement or unsolicited optimization. | Blast Radius Lock: Declare edit scope upfront; only edit within declared scope; flag out-of-scope issues as follow-up recommendations. |
| **AP-007** | **Claiming success without running verification commands** | Token-pressure or speed bias leads agent to skip test runs and pattern-match 'this looks right' or use speculative language. | Evidence Gate: Always run tests or re-read files, report actual terminal output, never say 'should work'. |
| **AP-008** | **Editing files based on stale cached memory instead of current on-disk state** | Files read early in long sessions get modified externally or by background tasks while agent remembers old lines. | Freshness Gate: Re-read target file if >5 tool calls since last read; run git status before multi-file edits. |
| **AP-009** | **Using invented library functions that don't exist in installed packages** | LLM hallucination on fast-moving libraries or niche frameworks where training memory contains plausible APIs. | Hallucination Guard: Verify every unfamiliar API symbol via tool inspection, REPL check, or docs before using. |
| **AP-010** | **Polling in loops or setting redundant timers while waiting for background tasks or command completion** | Failing to utilize the system's reactive wakeup event loop, resulting in busy-wait loops and unnecessary timers | Always use "Waiting for a task notification" with live status. Avoid unnecessary timers; do not poll; await direct completion signal |
| **AP-011** | **Reading a single file in multiple sliced chunks or piecemeal line ranges when analyzing it** | Unnecessary line range slicing on single-file analysis resulting in fragmented context and multiple redundant tool calls | When a single file needs to be analyzed, read the entire file in one call (single view_file without slicing) |
| **AP-012** | **Suboptimal or haphazard tool selection resulting in latency, token waste, or context bloat** | Executing operations reflexively using shell commands or piecemeal calls without pre-flight tool evaluation | Evaluate and select the most direct, token-efficient tool (grep_search, full view_file, multi_replace, reactive async) before execution |
| **AP-013** | **Unanchored gitignore directory patterns masking nested subproject source trees** | Using unanchored paths like `src/` in `.gitignore` matches any directory named `src` anywhere in the tree (e.g. `helium-android/src/`), silently ignoring application code. | Always anchor root-specific checkout directories with a leading slash (e.g., `/src/`) in `.gitignore`. |
| **AP-014** | **Releasing unsigned Android APKs when CI signing secret is missing** | Conditional logic in CI workflow copied unsigned Gradle APK when `SIGNING_KEYSTORE` secret was unset, triggering Google Play Protect warnings and install failures. | Provide automated CI keystore generation fallback and enforce `apksigner sign` (v1, v2, v3) followed by `apksigner verify -v` before publishing release artifacts. |



---

## 🛠️ Platform & Tooling Gotchas

### 1. Skylos SAST on Meta-Directories
- **Gotcha**: Scanning directories containing 1,450+ offline skill scripts causes slow traversal.
- **Rule**: Pass `--exclude .agents --exclude .agent --exclude venv` when running whole-project audits.

### 2. PEP 668 & Linux System Python Isolation
- **Gotcha**: Modern Linux distros (Arch, Debian 12+, Ubuntu 24+) block system-wide `pip install`.
- **Rule**: Provision isolated virtual environments under `~/.local/share/<tool>/venv` and symlink CLI entrypoints to `~/.local/bin/`.

### 3. NPM Remote Package Authorization (`EALLOWREMOTE`)
- **Gotcha**: npm v12+ defaults `allow-remote="none"`, failing to install git-based tarball dependencies.
- **Rule**: Configure `npm config set allow-remote all` during environment setup.

### 4. `java.util.Base64` for Android API 26+ Data & Crypto Engines
- **Gotcha**: Using `android.util.Base64` in non-UI data or crypto utilities breaks lightweight JVM unit tests (requiring mock Android runtimes).
- **Rule**: Since `minSdk >= 26`, use standard `java.util.Base64` (`getEncoder()` / `getDecoder()`). It runs natively on desktop JVM unit tests and Android 8.0+.

### 5. Quoted Heredocs for Python Embeds in Bash
- **Gotcha**: Using `python3 -c "..."` strips escaped quotes and breaks nested regex/string literals.
- **Rule**: Always use quoted heredoc `python3 - << 'PYEOF'` in bash automation scripts to prevent shell variable or quote expansion.

### 6. Local JVM Test Stubbing for Android SQLite & IPC
- **Gotcha**: Testing core data components (`SearchSuggestionEngine`, `HeliumDbHelper`) from headless JVM tests causes `NoClassDefFoundError` or crashes without heavyweight mock frameworks.
- **Rule**: Provide lightweight interface stubs in `scripts/run_unit_tests.sh` that satisfy compile dependencies and callback contracts without dragging in the full Android SQLite runtime.

### 7. Dual-Theme Color Resource Aliasing & CI Javac Synchronization
- **Gotcha**: Referencing color symbols in Java UI classes (e.g. `R.color.helium_warning_amber`) that differ from resource definitions (like `R.color.helium_shield_warning`) passes script-based XML linters but breaks the Gradle compilation pipeline on CI.
- **Rule**: Declare explicit alias definitions across both `res/values/colors.xml` and `res/values-night/colors.xml` for all semantic UI tokens to guarantee total compile-time safety across themes.

### 8. Singleton State Isolation in JVM Test Suites
- **Gotcha**: Unit testing singleton managers (`TabManager.getInstance()`, `ProfileManager`) retains mutated in-memory collections (e.g., `mGroups`, `mTabs`) across test runs within the same JVM process, causing inter-test pollution and assertions failing with unexpected collection sizes.
- **Rule**: Implement explicit teardown/reset methods (e.g. `clearGroups()`, `closeAllTabs(true)`) and invoke them in JUnit `@Before setUp()` fixtures to ensure clean, isolated state for every test case.

### 9. Android OS Symbol Scoping in Headless Test Harnesses
- **Gotcha**: Adding standard Android SDK classes (e.g. `Build.VERSION.SDK_INT`) in Activity files that are excluded from lightweight JVM mock harnesses passes `./scripts/run_unit_tests.sh` locally but fails with `error: package Build does not exist` during full Gradle `compileReleaseJavaWithJavac` on CI.
- **Rule**: Explicitly import all SDK packages (e.g. `import android.os.Build;`) and run an automated AST/import scanner in `./scripts/lint.sh` to verify symbol import completeness across all Java sources before pushing release tags.

---

## 🔄 The 2-Way Immunization Protocol
1. **Pre-Action Check**: Before executing a tool call or creating a component, verify against this matrix.
2. **Post-Failure Distillation**: When a mistake, test failure, or user correction occurs:
   - Identify the exact root cause.
   - Formulate an Invariant Prevention Strategy.
   - Append to this matrix and `anti_patterns.json` via `zg learn add`.
