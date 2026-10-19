package net.imput.helium.extensions;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * Manages extension state, pre-bundled packages, sideloaded CRX items, and runtime toggles.
 */
public final class ExtensionManager {
    public static final String ID_UBLOCK = "cjpalhdlnbpafiamejdnhcphjbkeiagm";
    public static final String ID_DARK_READER = "eimadpbcbfnmbkopoojfekhnkhdbieeh";

    private static volatile ExtensionManager sInstance;
    private final Context mAppContext;
    private final Map<String, ExtensionItem> mExtensions = new LinkedHashMap<>();

    public interface ExtensionStateListener {
        void onExtensionsUpdated(List<ExtensionItem> extensions);
    }

    private final List<ExtensionStateListener> mListeners = new ArrayList<>();

    public static ExtensionManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (ExtensionManager.class) {
                if (sInstance == null) {
                    sInstance = new ExtensionManager(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private ExtensionManager(Context context) {
        mAppContext = context;
        initializeDefaults();
    }

    private synchronized void initializeDefaults() {
        HeliumPreferences prefs = HeliumPreferences.getInstance(mAppContext);

        // 1. Helium bundled uBlock Origin
        mExtensions.put(ID_UBLOCK, new ExtensionItem(
                ID_UBLOCK,
                "uBlock Origin (Helium Edition)",
                "1.57.0",
                "High-efficiency, lightweight content blocker and tracker protector.",
                prefs.isNativeAdblockEnabled(),
                "chrome-extension://" + ID_UBLOCK + "/dashboard.html",
                true
        ));

        // 2. Helium bundled Dark Reader
        mExtensions.put(ID_DARK_READER, new ExtensionItem(
                ID_DARK_READER,
                "Dark Reader (AMOLED)",
                "4.9.80",
                "Dark mode for every website. Inverts brightness with high contrast OLED blacks.",
                prefs.isDarkReaderEnabled(),
                "chrome-extension://" + ID_DARK_READER + "/ui/popup/index.html",
                true
        ));
    }

    @NonNull
    public synchronized List<ExtensionItem> getInstalledExtensions() {
        return Collections.unmodifiableList(new ArrayList<>(mExtensions.values()));
    }

    @Nullable
    public synchronized ExtensionItem getExtension(@NonNull String id) {
        return mExtensions.get(id);
    }

    public synchronized boolean setExtensionEnabled(@NonNull String id, boolean enabled) {
        ExtensionItem item = mExtensions.get(id);
        if (item == null) {
            return false;
        }

        mExtensions.put(id, item.withEnabled(enabled));

        // Sync with central preferences if bundled
        HeliumPreferences prefs = HeliumPreferences.getInstance(mAppContext);
        if (ID_DARK_READER.equals(id)) {
            prefs.setDarkReaderEnabled(enabled);
        }

        notifyListeners();
        return true;
    }

    public synchronized boolean installFromCrx(@NonNull String id, @NonNull String name, @NonNull String version, @NonNull String description) {
        if (mExtensions.containsKey(id)) {
            return false;
        }

        ExtensionItem newItem = new ExtensionItem(
                id,
                name,
                version,
                description,
                true,
                "chrome-extension://" + id + "/options.html",
                false
        );
        mExtensions.put(id, newItem);
        notifyListeners();
        return true;
    }

    public synchronized boolean removeExtension(@NonNull String id) {
        ExtensionItem item = mExtensions.get(id);
        if (item == null || item.isBundled()) {
            // Bundled extensions cannot be uninstalled, only disabled
            return false;
        }

        mExtensions.remove(id);
        notifyListeners();
        return true;
    }

    public synchronized void registerListener(@NonNull ExtensionStateListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public synchronized void unregisterListener(@NonNull ExtensionStateListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners() {
        List<ExtensionItem> snapshot = getInstalledExtensions();
        for (ExtensionStateListener listener : mListeners) {
            listener.onExtensionsUpdated(snapshot);
        }
    }
}
