package net.imput.helium.extensions;

import android.content.Context;
import android.util.Base64;
import android.webkit.WebView;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * ExtensionRuntimeInjector: Injects WebExtension Manifest V3 content scripts,
 * cosmetic filters, and mobile DevTools into active WebViews.
 */
public class ExtensionRuntimeInjector {

    private static final String TAG = "ExtensionInjector";

    // Lightweight cosmetic ad & tracker blocking CSS rules
    private static final String NATIVE_ADBLOCK_CSS = 
        "ins.adsbygoogle, .ad-banner, .advertisement, [id^='google_ads_'], " +
        "[class*='sponsored-post'], .taboola-placeholder, .outbrain-placeholder { display: none !important; }";

    // Dark Reader inverted color filter CSS injection fallback
    private static final String DARK_READER_CSS = 
        "html { filter: invert(90%) hue-rotate(180deg) !important; background-color: #121212 !important; } " +
        "img, video, canvas, iframe, picture, svg { filter: invert(100%) hue-rotate(180deg) !important; }";

    // Mobile DevTools (Eruda Console) loader script
    private static final String ERUDA_DEVTOOLS_SCRIPT = 
        "(function() {" +
        "  if (window.__eruda_loaded) return;" +
        "  window.__eruda_loaded = true;" +
        "  var script = document.createElement('script');" +
        "  script.src = 'https://cdn.jsdelivr.net/npm/eruda';" +
        "  script.onload = function() { eruda.init(); eruda.show(); };" +
        "  document.head.appendChild(script);" +
        "})();";

    private final Context mContext;
    private final ExtensionManager mExtensionManager;
    private final HeliumPreferences mPreferences;
    private final net.imput.helium.privacy.noise.HeliumNoiseController mNoiseController;

    public ExtensionRuntimeInjector(Context context) {
        mContext = context.getApplicationContext();
        mExtensionManager = ExtensionManager.getInstance(mContext);
        mPreferences = HeliumPreferences.getInstance(mContext);
        mNoiseController = new net.imput.helium.privacy.noise.HeliumNoiseController(mContext);
    }

    /**
     * Injects registered extension content scripts and user protections into the web page.
     */
    public void injectContentScripts(WebView webView, String url) {
        if (webView == null || url == null) return;

        // Skip internal/local scheme URLs for security
        if (url.startsWith("file://") || url.startsWith("chrome://") || url.startsWith("about:")) {
            return;
        }

        // 1. Anti-fingerprinting Noise (Canvas, WebGL, Audio jitter)
        if (mNoiseController.isEnabled()) {
            String noiseScript = mNoiseController.getInjectionScript();
            if (!noiseScript.isEmpty()) {
                executeSafeScript(webView, noiseScript);
            }
        }

        // 2. Native Adblock / Tracker Stripping CSS injection
        if (mPreferences.isNativeAdblockEnabled()) {
            injectStyleRule(webView, NATIVE_ADBLOCK_CSS, "helium-adblock-shield");
        }

        // 3. Iterate and inject installed WebExtensions
        if (mPreferences.isExtensionsEnabled()) {
            List<ExtensionItem> extensions = mExtensionManager.getInstalledExtensions();
            for (ExtensionItem ext : extensions) {
                if (ext.isEnabled()) {
                    injectExtension(webView, ext, url);
                }
            }
        }
    }

    /**
     * Injects an individual extension's scripts and styles.
     */
    private void injectExtension(WebView webView, ExtensionItem ext, String url) {
        String id = ext.getId();
        if ("darkreader".equalsIgnoreCase(id) || ext.getName().toLowerCase().contains("dark reader")) {
            injectStyleRule(webView, DARK_READER_CSS, "helium-darkreader-style");
        } else if ("ublock_origin".equalsIgnoreCase(id) || ext.getName().toLowerCase().contains("ublock")) {
            injectStyleRule(webView, NATIVE_ADBLOCK_CSS, "helium-ublock-style");
        }

        // If extension contains custom user content scripts from sideloaded CRX/ZIP:
        String contentScript = ext.getContentScript();
        if (contentScript != null && !contentScript.trim().isEmpty()) {
            executeSafeScript(webView, contentScript);
        }
    }

    /**
     * Toggles Mobile DevTools (Eruda) directly in the active WebView.
     */
    public void injectDevTools(WebView webView) {
        if (webView != null) {
            executeSafeScript(webView, ERUDA_DEVTOOLS_SCRIPT);
        }
    }

    /**
     * Safely injects a CSS stylesheet block into document.head.
     */
    public void injectStyleRule(WebView webView, String cssContent, String elementId) {
        String encodedCss = Base64.encodeToString(cssContent.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        String js = String.format(
            "(function() {" +
            "  var existing = document.getElementById('%s');" +
            "  if (!existing) {" +
            "    var style = document.createElement('style');" +
            "    style.id = '%s';" +
            "    style.type = 'text/css';" +
            "    style.textContent = window.atob('%s');" +
            "    (document.head || document.documentElement).appendChild(style);" +
            "  }" +
            "})();", elementId, elementId, encodedCss);

        executeSafeScript(webView, js);
    }

    private void executeSafeScript(WebView webView, String javascript) {
        webView.post(() -> webView.evaluateJavascript(javascript, null));
    }
}
