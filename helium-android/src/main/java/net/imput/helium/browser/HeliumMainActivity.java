package net.imput.helium.browser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import net.imput.helium.R;
import net.imput.helium.extensions.ExtensionRuntimeInjector;
import net.imput.helium.extensions.HeliumExtensionsActivity;
import net.imput.helium.onboarding.HeliumFirstRunActivity;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * HeliumMainActivity: Primary browser window with Omnibox navigation,
 * WebExtension content script injection, and privacy engine hooks.
 */
public class HeliumMainActivity extends AppCompatActivity {

    private static final String STARTPAGE_URL = "file:///android_asset/startpage/index.html";
    private static final int REQUEST_FIRST_RUN = 1001;

    private WebView mWebView;
    private EditText mEtOmnibox;
    private ImageView mBtnRefresh;
    private ImageView mBtnExtensions;
    private ImageView mBtnDevTools;
    private ImageView mBtnSettings;
    private ImageView mBtnSslLock;
    private ProgressBar mProgressPageLoad;

    private HeliumPreferences mPreferences;
    private ExtensionRuntimeInjector mExtensionInjector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = HeliumPreferences.getInstance(this);
        mExtensionInjector = new ExtensionRuntimeInjector(this);

        // Check if First-Run Onboarding is required
        if (!mPreferences.isFirstRunCompleted()) {
            Intent firstRunIntent = new Intent(this, HeliumFirstRunActivity.class);
            startActivityForResult(firstRunIntent, REQUEST_FIRST_RUN);
        }

        setContentView(R.layout.activity_helium_main);

        initViews();
        setupWebView();
        handleIntent(getIntent());
    }

    private void initViews() {
        mWebView = findViewById(R.id.webview_browser);
        mEtOmnibox = findViewById(R.id.et_omnibox);
        mBtnRefresh = findViewById(R.id.btn_refresh);
        mBtnExtensions = findViewById(R.id.btn_extensions);
        mBtnDevTools = findViewById(R.id.btn_devtools);
        mBtnSettings = findViewById(R.id.btn_settings);
        mBtnSslLock = findViewById(R.id.btn_ssl_lock);
        mProgressPageLoad = findViewById(R.id.progress_page_load);

        mBtnRefresh.setOnClickListener(v -> {
            if (mWebView != null) {
                mWebView.reload();
            }
        });

        mBtnExtensions.setOnClickListener(v -> {
            Intent intent = new Intent(HeliumMainActivity.this, HeliumExtensionsActivity.class);
            startActivity(intent);
        });

        mBtnDevTools.setOnClickListener(v -> {
            if (mWebView != null) {
                mExtensionInjector.injectDevTools(mWebView);
                Toast.makeText(HeliumMainActivity.this, "Eruda DevTools injected", Toast.LENGTH_SHORT).show();
            }
        });

        mBtnSettings.setOnClickListener(v -> {
            // Re-open preferences / setup wizard
            Intent intent = new Intent(HeliumMainActivity.this, HeliumFirstRunActivity.class);
            startActivity(intent);
        });

        mEtOmnibox.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String query = mEtOmnibox.getText().toString().trim();
                navigateTo(query);
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = mWebView.getSettings();
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

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgressPageLoad.setVisibility(View.VISIBLE);
                updateOmniboxUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgressPageLoad.setVisibility(View.GONE);
                updateOmniboxUrl(url);

                // Inject WebExtension content scripts, adblock CSS, and user scripts
                mExtensionInjector.injectContentScripts(view, url);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    mProgressPageLoad.setVisibility(View.VISIBLE);
                    mProgressPageLoad.setProgress(newProgress);
                } else {
                    mProgressPageLoad.setVisibility(View.GONE);
                }
            }
        });
    }

    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                navigateTo(data.toString());
                return;
            }
        }
        // Load offline Helium Start Page by default
        mWebView.loadUrl(STARTPAGE_URL);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void navigateTo(String input) {
        if (input == null || input.isEmpty()) {
            mWebView.loadUrl(STARTPAGE_URL);
            return;
        }

        if (input.startsWith("file://") || input.startsWith("http://") || input.startsWith("https://")) {
            mWebView.loadUrl(input);
        } else if (input.contains(".") && !input.contains(" ")) {
            mWebView.loadUrl("https://" + input);
        } else {
            // Search query resolution based on HeliumPreferences
            String engine = mPreferences.getDefaultSearchEngine();
            String searchUrl;
            if (HeliumPreferences.SEARCH_ENGINE_BRAVE.equals(engine)) {
                searchUrl = "https://search.brave.com/search?q=" + Uri.encode(input);
            } else if (HeliumPreferences.SEARCH_ENGINE_SEARXNG.equals(engine)) {
                searchUrl = "https://searx.be/search?q=" + Uri.encode(input);
            } else if (HeliumPreferences.SEARCH_ENGINE_STARTPAGE.equals(engine)) {
                searchUrl = "https://www.startpage.com/sp/search?query=" + Uri.encode(input);
            } else {
                searchUrl = "https://duckduckgo.com/?q=" + Uri.encode(input);
            }
            mWebView.loadUrl(searchUrl);
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

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && mEtOmnibox != null) {
            imm.hideSoftInputFromWindow(mEtOmnibox.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
