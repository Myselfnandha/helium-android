package net.imput.helium.privacy.https;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * HttpsUpgrader: Automatically upgrades insecure HTTP connections to secure HTTPS,
 * respecting local development hosts and internal schemes.
 */
public final class HttpsUpgrader {

    private final HeliumPreferences mPreferences;
    private final boolean mHttpsOnlyEnabled;

    public HttpsUpgrader(@NonNull HeliumPreferences preferences) {
        mPreferences = preferences;
        mHttpsOnlyEnabled = true;
    }

    public HttpsUpgrader(boolean httpsOnlyEnabled) {
        mPreferences = null;
        mHttpsOnlyEnabled = httpsOnlyEnabled;
    }

    public boolean shouldUpgrade(@Nullable String url) {
        boolean enabled = (mPreferences != null) ? mPreferences.isHttpsOnlyModeEnabled() : mHttpsOnlyEnabled;
        if (!enabled) {
            return false;
        }
        if (url == null || !url.startsWith("http://")) {
            return false;
        }
        // Exclude local development and loopback networks
        return !isLocalAddress(url);
    }

    @NonNull
    public String upgrade(@Nullable String url) {
        if (url == null) return "";
        if (shouldUpgrade(url)) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    private boolean isLocalAddress(@NonNull String url) {
        return url.startsWith("http://localhost") ||
               url.startsWith("http://127.0.0.1") ||
               url.startsWith("http://10.") ||
               url.startsWith("http://192.168.") ||
               url.startsWith("http://172.16.") ||
               url.startsWith("http://[::1]");
    }
}
