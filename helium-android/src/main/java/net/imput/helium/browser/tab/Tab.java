package net.imput.helium.browser.tab;

import android.graphics.Bitmap;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.UUID;

/**
 * Tab: Encapsulates state, metadata, and WebView lifecycle for a single browser tab.
 */
public final class Tab {

    private final String mId;
    private final boolean mIsIncognito;
    private final long mCreatedAt;

    private String mTitle;
    private String mUrl;
    private Bitmap mFavicon;
    private WebView mWebView;
    private long mLastAccessedAt;

    public Tab(@NonNull String url, boolean isIncognito) {
        this(UUID.randomUUID().toString(), url, isIncognito);
    }

    public Tab(@NonNull String id, @NonNull String url, boolean isIncognito) {
        mId = id;
        mUrl = url;
        mTitle = "New Tab";
        mIsIncognito = isIncognito;
        mCreatedAt = System.currentTimeMillis();
        mLastAccessedAt = mCreatedAt;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public boolean isIncognito() {
        return mIsIncognito;
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }

    public long getLastAccessedAt() {
        return mLastAccessedAt;
    }

    public void touch() {
        mLastAccessedAt = System.currentTimeMillis();
    }

    @NonNull
    public String getTitle() {
        return (mTitle != null && !mTitle.isEmpty()) ? mTitle : "New Tab";
    }

    public void setTitle(@Nullable String title) {
        mTitle = title;
    }

    @NonNull
    public String getUrl() {
        return (mUrl != null) ? mUrl : "";
    }

    public void setUrl(@Nullable String url) {
        mUrl = url;
    }

    @Nullable
    public Bitmap getFavicon() {
        return mFavicon;
    }

    public void setFavicon(@Nullable Bitmap favicon) {
        mFavicon = favicon;
    }

    @Nullable
    public WebView getWebView() {
        return mWebView;
    }

    public void setWebView(@Nullable WebView webView) {
        mWebView = webView;
    }

    /**
     * Safely tears down the tab's WebView and releases all underlying graphics and memory buffers.
     */
    public void destroy() {
        if (mWebView != null) {
            try {
                mWebView.stopLoading();
                mWebView.loadUrl("about:blank");
                mWebView.clearHistory();
                mWebView.removeAllViews();
                mWebView.destroy();
            } catch (Exception ignored) {}
            mWebView = null;
        }
        if (mFavicon != null && !mFavicon.isRecycled()) {
            mFavicon.recycle();
            mFavicon = null;
        }
    }
}
