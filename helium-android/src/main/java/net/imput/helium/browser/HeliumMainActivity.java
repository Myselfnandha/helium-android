package net.imput.helium.browser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import net.imput.helium.R;
import net.imput.helium.adblock.AdBlockEngine;
import net.imput.helium.browser.download.HeliumDownloadHandler;
import net.imput.helium.browser.tab.Tab;
import net.imput.helium.browser.tab.TabManager;
import net.imput.helium.browser.ui.BookmarksHistoryDialogFragment;
import net.imput.helium.browser.ui.PrivacyShieldDialogFragment;
import net.imput.helium.browser.ui.SuggestionsAdapter;
import net.imput.helium.browser.ui.TabsDialogFragment;
import net.imput.helium.browser.ui.VaultSyncDialogFragment;
import net.imput.helium.database.HeliumDbHelper;
import net.imput.helium.extensions.ExtensionRuntimeInjector;
import net.imput.helium.extensions.HeliumExtensionsActivity;
import net.imput.helium.media.MediaPlaybackHost;
import net.imput.helium.onboarding.HeliumFirstRunActivity;
import net.imput.helium.preferences.HeliumPreferences;
import net.imput.helium.privacy.https.HttpsUpgrader;
import net.imput.helium.privacy.tracker.UrlTrackerStripper;
import net.imput.helium.search.SearchSuggestion;
import net.imput.helium.search.SearchSuggestionEngine;
import net.imput.helium.search.bangs.BangsRouter;

/**
 * HeliumMainActivity: Deep-module mobile browser window coordinating multi-tab management,
 * native C++ subresource adblocking, privacy noise, Bangs routing, and media persistence.
 */
public class HeliumMainActivity extends AppCompatActivity implements
        TabsDialogFragment.OnTabActionListener,
        BookmarksHistoryDialogFragment.OnUrlSelectedListener,
        TabManager.TabListener,
        SuggestionsAdapter.OnSuggestionClickListener {

    private static final String STARTPAGE_URL = "file:///android_asset/startpage/index.html";
    private static final int REQUEST_FIRST_RUN = 1001;

    private static final String DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Helium/1.0.0";

    // Views
    private FrameLayout mContainerWebView;
    private FrameLayout mFullscreenCustomView;
    private View mLayoutMainBrowser;
    private EditText mEtOmnibox;
    private ImageView mBtnRefresh;
    private ImageView mBtnSslLock;
    private FrameLayout mBtnTabsContainer;
    private TextView mTvTabCount;
    private ImageView mBtnMore;
    private ProgressBar mProgressPageLoad;
    private RecyclerView mRvSuggestions;
    private SuggestionsAdapter mSuggestionsAdapter;

    // Fullscreen video state
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    // Engines & Controllers
    private HeliumPreferences mPreferences;
    private ExtensionRuntimeInjector mExtensionInjector;
    private AdBlockEngine mAdBlockEngine;
    private TabManager mTabManager;
    private MediaPlaybackHost mMediaPlaybackHost;
    private HttpsUpgrader mHttpsUpgrader;
    private HeliumDbHelper mDbHelper;
    private HeliumDownloadHandler mDownloadHandler;
    private net.imput.helium.privacy.permissions.SitePermissionsManager mSitePermissionsManager;

    private boolean mIsDesktopMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Core Managers Initialization
        mPreferences = HeliumPreferences.getInstance(this);
        mExtensionInjector = new ExtensionRuntimeInjector(this);
        mAdBlockEngine = AdBlockEngine.getInstance();
        mTabManager = TabManager.getInstance();
        mMediaPlaybackHost = new MediaPlaybackHost(this);
        mHttpsUpgrader = new HttpsUpgrader(mPreferences);
        mDbHelper = HeliumDbHelper.getInstance(this);
        mDownloadHandler = new HeliumDownloadHandler(this);
        mSitePermissionsManager = net.imput.helium.privacy.permissions.SitePermissionsManager.getInstance(this);

        mTabManager.addListener(this);

        // Check First-Run Setup Wizard
        if (!mPreferences.isFirstRunCompleted()) {
            Intent firstRunIntent = new Intent(this, HeliumFirstRunActivity.class);
            startActivityForResult(firstRunIntent, REQUEST_FIRST_RUN);
        }

        setContentView(R.layout.activity_helium_main);

        initViews();
        ensureInitialTab();
        handleIntent(getIntent());
    }

    private void initViews() {
        mContainerWebView = findViewById(R.id.container_webview);
        mFullscreenCustomView = findViewById(R.id.fullscreen_custom_view);
        mLayoutMainBrowser = findViewById(R.id.layout_main_browser);
        mEtOmnibox = findViewById(R.id.et_omnibox);
        mBtnRefresh = findViewById(R.id.btn_refresh);
        mBtnSslLock = findViewById(R.id.btn_ssl_lock);
        mBtnTabsContainer = findViewById(R.id.btn_tabs_container);
        mTvTabCount = findViewById(R.id.tv_tab_count);
        mBtnMore = findViewById(R.id.btn_more);
        mProgressPageLoad = findViewById(R.id.progress_page_load);
        mRvSuggestions = findViewById(R.id.rv_omnibox_suggestions);

        mBtnRefresh.setOnClickListener(v -> {
            WebView active = getActiveWebView();
            if (active != null) {
                active.reload();
            }
        });

        mBtnSslLock.setOnClickListener(v -> showPrivacyShieldDialog());

        mBtnTabsContainer.setOnClickListener(v -> {
            hideSuggestions();
            TabsDialogFragment dialog = TabsDialogFragment.newInstance();
            dialog.setOnTabActionListener(this);
            dialog.show(getSupportFragmentManager(), "tabs_dialog");
        });

        mBtnMore.setOnClickListener(this::showOverflowMenu);

        // Omnibox Editor Action
        mEtOmnibox.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String query = mEtOmnibox.getText().toString().trim();
                navigateTo(query);
                hideKeyboard();
                hideSuggestions();
                return true;
            }
            return false;
        });

        // Omnibox Real-Time Suggestions Setup
        mRvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        mSuggestionsAdapter = new SuggestionsAdapter(this);
        mRvSuggestions.setAdapter(mSuggestionsAdapter);

        mEtOmnibox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mEtOmnibox.hasFocus() && s != null && s.length() > 0) {
                    SearchSuggestionEngine.query(HeliumMainActivity.this, s.toString(), 6, suggestions -> {
                        if (!suggestions.isEmpty() && mEtOmnibox.hasFocus()) {
                            mSuggestionsAdapter.setSuggestions(suggestions);
                            mRvSuggestions.setVisibility(View.VISIBLE);
                        } else {
                            hideSuggestions();
                        }
                    });
                } else {
                    hideSuggestions();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mEtOmnibox.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideSuggestions();
            }
        });

        updateTabBadge();
    }

    private void showPrivacyShieldDialog() {
        WebView active = getActiveWebView();
        String currentUrl = (active != null) ? active.getUrl() : "";
        PrivacyShieldDialogFragment dialog = PrivacyShieldDialogFragment.newInstance(currentUrl);
        dialog.show(getSupportFragmentManager(), "privacy_shield_dialog");
    }

    private void hideSuggestions() {
        if (mRvSuggestions != null) {
            mRvSuggestions.setVisibility(View.GONE);
            mSuggestionsAdapter.clear();
        }
    }

    @Override
    public void onSuggestionClick(@NonNull SearchSuggestion suggestion) {
        hideSuggestions();
        hideKeyboard();

        if (suggestion.getType() == SearchSuggestion.Type.BANG) {
            String bangPrefix = suggestion.getTitle().split(" ")[0];
            mEtOmnibox.setText(bangPrefix + " ");
            mEtOmnibox.setSelection(mEtOmnibox.getText().length());
        } else if (suggestion.getType() == SearchSuggestion.Type.SEARCH) {
            if (suggestion.getUrl().startsWith("http://") || suggestion.getUrl().startsWith("https://")) {
                navigateTo(suggestion.getUrl());
            } else {
                navigateTo(suggestion.getTitle().replace("Search for \"", "").replace("\"", ""));
            }
        } else {
            navigateTo(suggestion.getUrl());
        }
    }

    private void ensureInitialTab() {
        if (mTabManager.getTabCount() == 0) {
            boolean restored = false;
            if (mPreferences.isRestoreTabsOnStartupEnabled()) {
                String savedSession = mPreferences.getSavedTabSession();
                restored = mTabManager.restoreSession(savedSession);
            }

            if (!restored) {
                Tab tab = mTabManager.createTab(STARTPAGE_URL, false);
                setupTabWebView(tab);
                attachTabWebView(tab);
            } else {
                Tab active = mTabManager.getActiveTab();
                if (active != null) {
                    if (active.getWebView() == null) {
                        setupTabWebView(active);
                    }
                    attachTabWebView(active);
                }
            }
        } else {
            Tab active = mTabManager.getActiveTab();
            if (active != null) {
                if (active.getWebView() == null) {
                    setupTabWebView(active);
                }
                attachTabWebView(active);
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupTabWebView(@NonNull Tab tab) {
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Append Helium Privacy User Agent token
        String defaultUa = settings.getUserAgentString();
        settings.setUserAgentString(defaultUa + " Helium/1.0.0 (Android; Privacy)");

        // Attach Download Handler
        webView.setDownloadListener(mDownloadHandler);

        // Attach WebExtension JavaScript API bridge
        webView.addJavascriptInterface(new net.imput.helium.extensions.WebExtensionApiBridge(this),
                net.imput.helium.extensions.WebExtensionApiBridge.BRIDGE_JS_NAME);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgressPageLoad.setVisibility(View.VISIBLE);
                updateOmniboxUrl(url);
                tab.setUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgressPageLoad.setVisibility(View.GONE);
                updateOmniboxUrl(url);

                tab.setUrl(url);
                String title = view.getTitle();
                if (title != null && !title.isEmpty()) {
                    tab.setTitle(title);
                }

                // Inject WebExtension content scripts and anti-fingerprinting noise
                mExtensionInjector.injectContentScripts(view, url);

                // Inject Media Playback visibilityState spoofing
                mMediaPlaybackHost.injectMediaPersistence(view);

                // Record browsing history if not incognito
                if (!tab.isIncognito() && title != null) {
                    mDbHelper.recordVisit(url, title);
                }
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (mPreferences.isNativeAdblockEnabled() && request != null && request.getUrl() != null) {
                    String reqUrl = request.getUrl().toString();
                    if (mAdBlockEngine.shouldBlock(reqUrl)) {
                        return AdBlockEngine.createEmptyBlockResponse();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    mProgressPageLoad.setVisibility(View.VISIBLE);
                    mProgressPageLoad.setProgress(newProgress);
                } else {
                    mProgressPageLoad.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (title != null && !title.isEmpty()) {
                    tab.setTitle(title);
                }
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
                tab.setFavicon(icon);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, android.webkit.GeolocationPermissions.Callback callback) {
                net.imput.helium.privacy.permissions.SitePermissionsManager.State state =
                        mSitePermissionsManager.getPermissionState(origin, net.imput.helium.privacy.permissions.SitePermissionRecord.PERMISSION_GEOLOCATION);
                if (state == net.imput.helium.privacy.permissions.SitePermissionsManager.State.DENY) {
                    callback.invoke(origin, false, false);
                } else if (state == net.imput.helium.privacy.permissions.SitePermissionsManager.State.ALLOW) {
                    callback.invoke(origin, true, true);
                } else {
                    callback.invoke(origin, true, false);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                mCustomView = view;
                mCustomViewCallback = callback;
                mLayoutMainBrowser.setVisibility(View.GONE);
                mFullscreenCustomView.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                mFullscreenCustomView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;
                mFullscreenCustomView.removeView(mCustomView);
                mFullscreenCustomView.setVisibility(View.GONE);
                mCustomView = null;
                if (mCustomViewCallback != null) {
                    mCustomViewCallback.onCustomViewHidden();
                    mCustomViewCallback = null;
                }
                mLayoutMainBrowser.setVisibility(View.VISIBLE);
            }
        });

        tab.setWebView(webView);
        webView.loadUrl(tab.getUrl());
    }

    private void attachTabWebView(@NonNull Tab tab) {
        mContainerWebView.removeAllViews();
        WebView webView = tab.getWebView();
        if (webView == null) {
            setupTabWebView(tab);
            webView = tab.getWebView();
        }
        if (webView != null) {
            if (webView.getParent() != null) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }
            mContainerWebView.addView(webView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            updateOmniboxUrl(tab.getUrl());
        }
    }

    @Nullable
    private WebView getActiveWebView() {
        Tab active = mTabManager.getActiveTab();
        return (active != null) ? active.getWebView() : null;
    }

    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                String sanitized = UrlTrackerStripper.strip(data.toString());
                navigateTo(sanitized);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void navigateTo(String input) {
        hideSuggestions();

        WebView active = getActiveWebView();
        if (active == null) {
            ensureInitialTab();
            active = getActiveWebView();
            if (active == null) return;
        }

        if (input == null || input.isEmpty()) {
            active.loadUrl(STARTPAGE_URL);
            return;
        }

        // 1. Strip Tracking Parameters
        if (mPreferences.isUrlTrackerStripperEnabled()) {
            input = UrlTrackerStripper.strip(input);
        }

        // 2. Direct URI Schemes
        if (input.startsWith("file://") || input.startsWith("about:") || input.startsWith("chrome://")) {
            active.loadUrl(input);
            return;
        }

        // 3. HTTP to HTTPS Upgrade
        if (input.startsWith("http://")) {
            input = mHttpsUpgrader.upgrade(input);
            active.loadUrl(input);
            return;
        } else if (input.startsWith("https://")) {
            active.loadUrl(input);
            return;
        }

        // 4. Domain Name without Scheme (e.g. "example.com" or "news.ycombinator.com")
        if (input.contains(".") && !input.contains(" ")) {
            active.loadUrl("https://" + input);
            return;
        }

        // 5. Query Bangs & Search Resolution
        String defaultSearchPrefix = getDefaultSearchEngineUrl();
        BangsRouter.Result bangResult = BangsRouter.resolve(input, defaultSearchPrefix);
        active.loadUrl(bangResult.getResolvedUrl());
    }

    @NonNull
    private String getDefaultSearchEngineUrl() {
        String engine = mPreferences.getDefaultSearchEngine();
        if (HeliumPreferences.SEARCH_ENGINE_BRAVE.equals(engine)) {
            return "https://search.brave.com/search?q=";
        } else if (HeliumPreferences.SEARCH_ENGINE_SEARXNG.equals(engine)) {
            return "https://searx.be/search?q=";
        } else if (HeliumPreferences.SEARCH_ENGINE_STARTPAGE.equals(engine)) {
            return "https://www.startpage.com/sp/search?query=";
        } else {
            return "https://duckduckgo.com/?q=";
        }
    }

    private void updateOmniboxUrl(String url) {
        if (url == null) return;
        if (url.startsWith(STARTPAGE_URL)) {
            mEtOmnibox.setText("");
            mEtOmnibox.setHint("Search or enter web address...");
            mBtnSslLock.setImageResource(R.drawable.ic_shield_check);
        } else {
            mEtOmnibox.setText(url);
            mBtnSslLock.setImageResource(url.startsWith("https://") ? R.drawable.ic_lock : R.drawable.ic_search);
        }
    }

    private void updateTabBadge() {
        int count = mTabManager.getTabCount();
        mTvTabCount.setText(String.valueOf(count));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && mEtOmnibox != null) {
            imm.hideSoftInputFromWindow(mEtOmnibox.getWindowToken(), 0);
        }
    }

    private void showOverflowMenu(View anchor) {
        hideSuggestions();
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "New Tab");
        popup.getMenu().add(0, 2, 1, "New Private Tab");
        popup.getMenu().add(0, 3, 2, "Bookmark Current Page");
        popup.getMenu().add(0, 4, 3, "Bookmarks");
        popup.getMenu().add(0, 5, 4, "History");
        popup.getMenu().add(0, 6, 5, mIsDesktopMode ? "Mobile Site" : "Desktop Site");
        popup.getMenu().add(0, 12, 6, "Add to Home Screen");
        popup.getMenu().add(0, 7, 7, "Extensions");
        popup.getMenu().add(0, 8, 8, "DevTools (Eruda)");
        popup.getMenu().add(0, 11, 9, "Private Vault Sync");
        popup.getMenu().add(0, 9, 10, "Clear Browsing Data");
        popup.getMenu().add(0, 10, 11, "Settings");

        popup.setOnMenuItemClickListener(this::onOverflowMenuItemClick);
        popup.show();
    }

    private boolean onOverflowMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case 1: // New Tab
                onNewTabRequested(false);
                return true;
            case 2: // New Private Tab
                onNewTabRequested(true);
                return true;
            case 3: // Bookmark Current Page
                bookmarkCurrentPage();
                return true;
            case 4: // Bookmarks
                showBookmarksHistoryDialog(true);
                return true;
            case 5: // History
                showBookmarksHistoryDialog(false);
                return true;
            case 6: // Desktop Site Toggle
                toggleDesktopSite();
                return true;
            case 12: // Add to Home Screen (PRD M25)
                addToHomeScreen();
                return true;
            case 7: // Extensions
                startActivity(new Intent(this, HeliumExtensionsActivity.class));
                return true;
            case 8: // DevTools
                WebView active = getActiveWebView();
                if (active != null) {
                    mExtensionInjector.injectDevTools(active);
                    Toast.makeText(this, "DevTools Injected", Toast.LENGTH_SHORT).show();
                }
                return true;
            case 11: // Private Vault Sync
                showVaultSyncDialog();
                return true;
            case 9: // Clear Browsing Data
                mDbHelper.clearAllBrowsingData(() -> {
                    WebView current = getActiveWebView();
                    if (current != null) {
                        current.clearCache(true);
                        current.clearHistory();
                    }
                    Toast.makeText(this, "Browsing history and cache cleared", Toast.LENGTH_SHORT).show();
                });
                return true;
            case 10: // Settings
                startActivity(new Intent(this, HeliumFirstRunActivity.class));
                return true;
            default:
                return false;
        }
    }

    private void showVaultSyncDialog() {
        VaultSyncDialogFragment dialog = VaultSyncDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), "vault_sync_dialog");
    }

    private void bookmarkCurrentPage() {
        Tab active = mTabManager.getActiveTab();
        if (active != null && !active.getUrl().isEmpty()) {
            mDbHelper.addBookmark(active.getUrl(), active.getTitle(), success -> {
                Toast.makeText(this, success ? "Bookmark added" : "Bookmark already exists", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showBookmarksHistoryDialog(boolean startWithBookmarks) {
        BookmarksHistoryDialogFragment dialog = BookmarksHistoryDialogFragment.newInstance(startWithBookmarks);
        dialog.setOnUrlSelectedListener(this);
        dialog.show(getSupportFragmentManager(), "bookmarks_history_dialog");
    }

    private void toggleDesktopSite() {
        WebView active = getActiveWebView();
        if (active != null) {
            mIsDesktopMode = !mIsDesktopMode;
            WebSettings settings = active.getSettings();
            if (mIsDesktopMode) {
                settings.setUserAgentString(DESKTOP_UA);
                settings.setUseWideViewPort(true);
                settings.setLoadWithOverviewMode(true);
            } else {
                settings.setUserAgentString(null);
            }
            active.reload();
            Toast.makeText(this, mIsDesktopMode ? "Desktop view enabled" : "Mobile view enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToHomeScreen() {
        Tab active = mTabManager.getActiveTab();
        if (active == null || active.getUrl().isEmpty()) return;

        WebView webView = active.getWebView();
        if (webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String js = "(function(){"
                    + "var m = document.querySelector('link[rel=\"manifest\"]');"
                    + "var tc = document.querySelector('meta[name=\"theme-color\"]');"
                    + "return JSON.stringify({"
                    + "  manifest: m ? m.href : '',"
                    + "  themeColor: tc ? tc.content : '',"
                    + "  title: document.title || ''"
                    + "});"
                    + "})()";
            webView.evaluateJavascript(js, result -> {
                String title = active.getTitle();
                String themeColor = null;
                if (result != null && !result.equals("null") && !result.isEmpty()) {
                    try {
                        String unescaped = result;
                        if (unescaped.startsWith("\"") && unescaped.endsWith("\"")) {
                            unescaped = new org.json.JSONTokener(unescaped).nextValue().toString();
                        }
                        org.json.JSONObject obj = new org.json.JSONObject(unescaped);
                        String extractedTitle = obj.optString("title", "");
                        if (!extractedTitle.isEmpty()) {
                            title = extractedTitle;
                        }
                        String tc = obj.optString("themeColor", "");
                        if (!tc.isEmpty()) {
                            themeColor = tc;
                        }
                    } catch (Exception ignored) {}
                }
                boolean requested = net.imput.helium.browser.shortcut.PwaShortcutManager.requestPinShortcut(
                        this, active.getUrl(), title, active.getFavicon(), themeColor);
                if (requested) {
                    Toast.makeText(this, "Shortcut pinned to Home Screen", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Cannot create shortcut for this page", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            boolean requested = net.imput.helium.browser.shortcut.PwaShortcutManager.requestPinShortcut(
                    this, active.getUrl(), active.getTitle(), active.getFavicon());
            if (requested) {
                Toast.makeText(this, "Shortcut pinned to Home Screen", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cannot create shortcut for this page", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- TabManager.TabListener Callbacks ---

    @Override
    public void onTabCreated(@NonNull Tab tab) {
        setupTabWebView(tab);
        updateTabBadge();
    }

    @Override
    public void onTabSelected(@NonNull Tab tab) {
        attachTabWebView(tab);
    }

    @Override
    public void onTabClosed(@NonNull Tab tab) {
        updateTabBadge();
    }

    @Override
    public void onTabCountChanged(int count) {
        updateTabBadge();
    }

    // --- TabsDialogFragment.OnTabActionListener Callbacks ---

    @Override
    public void onNewTabRequested(boolean incognito) {
        Tab tab = mTabManager.createTab(STARTPAGE_URL, incognito);
        attachTabWebView(tab);
    }

    @Override
    public void onAllTabsClosed() {
        ensureInitialTab();
    }

    // --- BookmarksHistoryDialogFragment.OnUrlSelectedListener Callback ---

    @Override
    public void onUrlSelected(@NonNull String url) {
        navigateTo(url);
    }

    // --- Lifecycle Events ---

    @Override
    protected void onPause() {
        super.onPause();
        mMediaPlaybackHost.onPause(getActiveWebView());
        mTabManager.pauseBackgroundTabs();

        if (mPreferences.isRestoreTabsOnStartupEnabled()) {
            mPreferences.setSavedTabSession(mTabManager.serializeSession());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaPlaybackHost.onResume(getActiveWebView());
        mTabManager.resumeActiveTab();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlaybackHost.release();
        mTabManager.removeListener(this);
    }

    @Override
    public void onBackPressed() {
        if (mRvSuggestions != null && mRvSuggestions.getVisibility() == View.VISIBLE) {
            hideSuggestions();
            return;
        }

        if (mCustomView != null) {
            // Exit fullscreen video
            if (getActiveWebView() != null && getActiveWebView().getWebChromeClient() != null) {
                getActiveWebView().getWebChromeClient().onHideCustomView();
            }
            return;
        }

        WebView active = getActiveWebView();
        if (active != null && active.canGoBack()) {
            active.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
