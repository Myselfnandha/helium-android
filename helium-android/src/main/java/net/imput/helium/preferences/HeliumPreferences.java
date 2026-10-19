package net.imput.helium.preferences;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages Helium Android persistent preferences and configuration flags.
 */
public final class HeliumPreferences {
    private static final String PREF_NAME = "helium_browser_prefs";

    public static final String KEY_FIRST_RUN_COMPLETED = "helium_first_run_completed";
    public static final String KEY_EXTENSIONS_ENABLED = "helium_extensions_enabled";
    public static final String KEY_NATIVE_ADBLOCK_ENABLED = "helium_native_adblock_enabled";
    public static final String KEY_JIT_PROTECTION_ENABLED = "helium_jit_protection_enabled";
    public static final String KEY_VAULT_SYNC_ENABLED = "helium_vault_sync_enabled";
    public static final String KEY_VAULT_DEVICE_ID = "helium_vault_device_id";

    private final SharedPreferences mPrefs;

    private static volatile HeliumPreferences sInstance;

    public static HeliumPreferences getInstance(Context context) {
        if (sInstance == null) {
            synchronized (HeliumPreferences.class) {
                if (sInstance == null) {
                    sInstance = new HeliumPreferences(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private HeliumPreferences(Context context) {
        mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFirstRunCompleted() {
        return mPrefs.getBoolean(KEY_FIRST_RUN_COMPLETED, false);
    }

    public void setFirstRunCompleted(boolean completed) {
        mPrefs.edit().putBoolean(KEY_FIRST_RUN_COMPLETED, completed).apply();
    }

    public boolean isExtensionsEnabled() {
        return mPrefs.getBoolean(KEY_EXTENSIONS_ENABLED, true);
    }

    public void setExtensionsEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_EXTENSIONS_ENABLED, enabled).apply();
    }

    public boolean isNativeAdblockEnabled() {
        return mPrefs.getBoolean(KEY_NATIVE_ADBLOCK_ENABLED, true);
    }

    public void setNativeAdblockEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_NATIVE_ADBLOCK_ENABLED, enabled).apply();
    }

    public boolean isJitProtectionEnabled() {
        return mPrefs.getBoolean(KEY_JIT_PROTECTION_ENABLED, true);
    }

    public void setJitProtectionEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_JIT_PROTECTION_ENABLED, enabled).apply();
    }

    public static final String KEY_BACKGROUND_MEDIA_ENABLED = "helium_background_media_enabled";
    public static final String KEY_ANTI_FINGERPRINT_ENABLED = "helium_anti_fingerprint_enabled";
    public static final String KEY_URL_TRACKER_STRIPPER_ENABLED = "helium_url_tracker_stripper_enabled";
    public static final String KEY_DARK_READER_ENABLED = "helium_dark_reader_enabled";

    public boolean isVaultSyncEnabled() {
        return mPrefs.getBoolean(KEY_VAULT_SYNC_ENABLED, false);
    }

    public void setVaultSyncEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_VAULT_SYNC_ENABLED, enabled).apply();
    }

    public boolean isBackgroundMediaEnabled() {
        return mPrefs.getBoolean(KEY_BACKGROUND_MEDIA_ENABLED, true);
    }

    public void setBackgroundMediaEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_BACKGROUND_MEDIA_ENABLED, enabled).apply();
    }

    public boolean isAntiFingerprintEnabled() {
        return mPrefs.getBoolean(KEY_ANTI_FINGERPRINT_ENABLED, true);
    }

    public void setAntiFingerprintEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_ANTI_FINGERPRINT_ENABLED, enabled).apply();
    }

    public boolean isUrlTrackerStripperEnabled() {
        return mPrefs.getBoolean(KEY_URL_TRACKER_STRIPPER_ENABLED, true);
    }

    public void setUrlTrackerStripperEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_URL_TRACKER_STRIPPER_ENABLED, enabled).apply();
    }

    public static final String KEY_HTTPS_ONLY_MODE_ENABLED = "helium_https_only_mode_enabled";
    public static final String KEY_SECURE_DNS_ENABLED = "helium_secure_dns_enabled";

    public boolean isDarkReaderEnabled() {
        return mPrefs.getBoolean(KEY_DARK_READER_ENABLED, false);
    }

    public void setDarkReaderEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_DARK_READER_ENABLED, enabled).apply();
    }

    public boolean isHttpsOnlyModeEnabled() {
        return mPrefs.getBoolean(KEY_HTTPS_ONLY_MODE_ENABLED, true);
    }

    public void setHttpsOnlyModeEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_HTTPS_ONLY_MODE_ENABLED, enabled).apply();
    }

    public boolean isSecureDnsEnabled() {
        return mPrefs.getBoolean(KEY_SECURE_DNS_ENABLED, true);
    }

    public void setSecureDnsEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_SECURE_DNS_ENABLED, enabled).apply();
    }

    public static final String KEY_DEFAULT_SEARCH_ENGINE = "helium_default_search_engine";
    public static final String SEARCH_ENGINE_DUCKDUCKGO = "https://duckduckgo.com/?q=";
    public static final String SEARCH_ENGINE_BRAVE = "https://search.brave.com/search?q=";
    public static final String SEARCH_ENGINE_SEARXNG = "https://searx.be/search?q=";
    public static final String SEARCH_ENGINE_STARTPAGE = "https://www.startpage.com/sp/search?query=";

    public String getDefaultSearchEngine() {
        return mPrefs.getString(KEY_DEFAULT_SEARCH_ENGINE, SEARCH_ENGINE_DUCKDUCKGO);
    }

    public void setDefaultSearchEngine(String engineUrl) {
        mPrefs.edit().putString(KEY_DEFAULT_SEARCH_ENGINE, engineUrl).apply();
    }
}
