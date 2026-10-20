package net.imput.helium.media;

import android.content.Context;
import android.os.PowerManager;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * MediaPlaybackHost: Keeps HTML5 audio and video streams playing when the app
 * is backgrounded or when the phone screen turns off.
 */
public final class MediaPlaybackHost {

    private static final String WAKELOCK_TAG = "Helium::MediaPlaybackHost";

    // Script preventing visibilityState detection from pausing video/audio playback
    private static final String BACKGROUND_PLAYBACK_JS =
        "(function() {" +
        "  if (window.__helium_bg_media) return;" +
        "  window.__helium_bg_media = true;" +
        "  Object.defineProperty(document, 'hidden', { value: false, writable: false });" +
        "  Object.defineProperty(document, 'visibilityState', { value: 'visible', writable: false });" +
        "  window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);" +
        "})();";

    private final Context mContext;
    private final HeliumPreferences mPreferences;
    private PowerManager.WakeLock mWakeLock;

    public MediaPlaybackHost(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mPreferences = HeliumPreferences.getInstance(mContext);
    }

    /**
     * Injects visibility spoofing into web pages to keep media players alive.
     */
    public void injectMediaPersistence(@Nullable WebView webView) {
        if (webView != null && mPreferences.isBackgroundMediaEnabled()) {
            webView.post(() -> webView.evaluateJavascript(BACKGROUND_PLAYBACK_JS, null));
        }
    }

    /**
     * Called when the parent activity is paused. Maintains audio wakelock if enabled.
     */
    public void onPause(@Nullable WebView webView) {
        if (!mPreferences.isBackgroundMediaEnabled()) {
            if (webView != null) {
                webView.onPause();
            }
            releaseWakeLock();
            return;
        }

        // Keep CPU alive for background audio rendering
        acquireWakeLock();
        // Do NOT call webView.onPause() to keep audio render pipeline running
        injectMediaPersistence(webView);
    }

    /**
     * Called when the parent activity resumes.
     */
    public void onResume(@Nullable WebView webView) {
        if (webView != null) {
            webView.onResume();
        }
        releaseWakeLock();
    }

    private synchronized void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
                mWakeLock.setReferenceCounted(false);
            }
        }
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire(10 * 60 * 1000L /* 10 minutes timeout */);
        }
    }

    private synchronized void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public void release() {
        releaseWakeLock();
        mWakeLock = null;
    }
}
