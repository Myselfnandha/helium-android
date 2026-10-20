package net.imput.helium.browser.shortcut;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import net.imput.helium.R;
import net.imput.helium.adblock.AdBlockEngine;
import net.imput.helium.browser.HeliumMainActivity;
import net.imput.helium.media.MediaPlaybackHost;
import net.imput.helium.privacy.tracker.UrlTrackerStripper;

/**
 * PwaActivity: Dedicated standalone presentation window for Progressive Web Apps (PRD M25).
 * Eliminates browser chrome while providing native window management and task switcher integration.
 */
public class PwaActivity extends AppCompatActivity {

    public static final String EXTRA_PWA_URL = "extra_pwa_url";
    public static final String EXTRA_PWA_TITLE = "extra_pwa_title";
    public static final String EXTRA_PWA_THEME_COLOR = "extra_pwa_theme_color";

    private FrameLayout mContainerWebView;
    private LinearLayout mLayoutTopBar;
    private TextView mTvTitle;
    private TextView mTvOrigin;
    private ImageView mBtnClose;
    private ImageView mBtnReload;
    private ImageView mBtnMore;
    private ProgressBar mProgress;

    private WebView mWebView;
    private AdBlockEngine mAdBlockEngine;
    private MediaPlaybackHost mMediaPlaybackHost;

    private String mAppUrl;
    private String mAppHost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pwa);

        mAdBlockEngine = AdBlockEngine.getInstance();
        mMediaPlaybackHost = new MediaPlaybackHost(this);

        initViews();
        handleIntent(getIntent());
    }

    private void initViews() {
        mContainerWebView = findViewById(R.id.container_pwa_webview);
        mLayoutTopBar = findViewById(R.id.layout_pwa_topbar);
        mTvTitle = findViewById(R.id.tv_pwa_title);
        mTvOrigin = findViewById(R.id.tv_pwa_origin);
        mBtnClose = findViewById(R.id.btn_pwa_close);
        mBtnReload = findViewById(R.id.btn_pwa_reload);
        mBtnMore = findViewById(R.id.btn_pwa_more);
        mProgress = findViewById(R.id.progress_pwa);

        mBtnClose.setOnClickListener(v -> finish());
        mBtnReload.setOnClickListener(v -> {
            if (mWebView != null) mWebView.reload();
        });
        mBtnMore.setOnClickListener(this::showOverflowMenu);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        mAppUrl = intent.getStringExtra(EXTRA_PWA_URL);
        if (mAppUrl == null && intent.getData() != null) {
            mAppUrl = intent.getDataString();
        }

        if (mAppUrl == null || !PwaShortcutManager.isEligibleUrl(mAppUrl)) {
            Toast.makeText(this, "Invalid Web App URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = intent.getStringExtra(EXTRA_PWA_TITLE);
        if (title != null && !title.isEmpty()) {
            mTvTitle.setText(title);
        }

        String themeColor = intent.getStringExtra(EXTRA_PWA_THEME_COLOR);
        applyThemeColor(themeColor);

        try {
            Uri uri = Uri.parse(mAppUrl);
            mAppHost = uri.getHost();
            mTvOrigin.setText(uri.getScheme() + "://" + mAppHost);
        } catch (Exception e) {
            mTvOrigin.setText(mAppUrl);
        }

        setupWebView();
    }

    private void applyThemeColor(@Nullable String colorHex) {
        if (colorHex != null && colorHex.startsWith("#")) {
            try {
                int color = Color.parseColor(colorHex);
                mLayoutTopBar.setBackgroundColor(color);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(color);
                }
            } catch (Exception ignored) {}
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        mWebView = new WebView(this);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        String defaultUa = settings.getUserAgentString();
        settings.setUserAgentString(defaultUa + " Helium/1.0.0 (Android; PWA)");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgress.setVisibility(View.GONE);

                String pageTitle = view.getTitle();
                if (pageTitle != null && !pageTitle.isEmpty()) {
                    mTvTitle.setText(pageTitle);
                }

                mMediaPlaybackHost.injectMediaPersistence(view);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String targetHost = request.getUrl().getHost();
                    // If navigation goes outside PWA domain, offer to open in primary browser
                    if (mAppHost != null && targetHost != null && !targetHost.equalsIgnoreCase(mAppHost)) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                        browserIntent.setClass(PwaActivity.this, HeliumMainActivity.class);
                        startActivity(browserIntent);
                        return true;
                    }
                }
                return false;
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String reqUrl = request.getUrl().toString();
                    if (mAdBlockEngine.shouldBlock(reqUrl)) {
                        return AdBlockEngine.createEmptyBlockResponse();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    mProgress.setVisibility(View.VISIBLE);
                    mProgress.setProgress(newProgress);
                } else {
                    mProgress.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (title != null && !title.isEmpty()) {
                    mTvTitle.setText(title);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setTaskDescription(new ActivityManager.TaskDescription(title));
                    }
                }
            }
        });

        mContainerWebView.addView(mWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mWebView.loadUrl(mAppUrl);
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Reload");
        popup.getMenu().add(0, 2, 1, "Share");
        popup.getMenu().add(0, 3, 2, "Open in Helium Browser");
        popup.getMenu().add(0, 4, 3, "Close App");

        popup.setOnMenuItemClickListener((MenuItem item) -> {
            switch (item.getItemId()) {
                case 1:
                    if (mWebView != null) mWebView.reload();
                    return true;
                case 2:
                    shareAppUrl();
                    return true;
                case 3:
                    openInHeliumBrowser();
                    return true;
                case 4:
                    finish();
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void shareAppUrl() {
        if (mWebView != null && mWebView.getUrl() != null) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share Web App URL"));
        }
    }

    private void openInHeliumBrowser() {
        if (mWebView != null && mWebView.getUrl() != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mWebView.getUrl()));
            intent.setClass(this, HeliumMainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWebView != null) {
            mMediaPlaybackHost.onPause(mWebView);
            mWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebView != null) {
            mMediaPlaybackHost.onResume(mWebView);
            mWebView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlaybackHost.release();
        if (mWebView != null) {
            mContainerWebView.removeAllViews();
            mWebView.destroy();
            mWebView = null;
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
