package net.imput.helium.privacy.permissions;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.imput.helium.database.HeliumDbHelper;

/**
 * SitePermissionsManager: Deep-module controller enforcing granular per-origin
 * capabilities (camera, microphone, geolocation, notifications, clipboard) for PRD M18.
 * Provides instant in-memory resolution with background SQLite persistence.
 */
public final class SitePermissionsManager {

    public enum State {
        PROMPT(0),
        ALLOW(1),
        DENY(2);

        public final int value;

        State(int value) {
            this.value = value;
        }

        @NonNull
        public static State fromInt(int val) {
            if (val == 1) return ALLOW;
            if (val == 2) return DENY;
            return PROMPT;
        }
    }

    private static volatile SitePermissionsManager sInstance;

    private final HeliumDbHelper mDbHelper;
    private final Map<String, State> mCache = new ConcurrentHashMap<>();

    public static SitePermissionsManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (SitePermissionsManager.class) {
                if (sInstance == null) {
                    sInstance = new SitePermissionsManager(HeliumDbHelper.getInstance(context));
                }
            }
        }
        return sInstance;
    }

    public SitePermissionsManager(@NonNull HeliumDbHelper dbHelper) {
        mDbHelper = dbHelper;
        loadCache();
    }

    private void loadCache() {
        mDbHelper.getAllSitePermissions(records -> {
            for (SitePermissionRecord r : records) {
                String key = makeKey(r.getOrigin(), r.getPermission());
                mCache.put(key, State.fromInt(r.getState()));
            }
        });
    }

    /**
     * Normalizes a web URL to its canonical origin (scheme://host[:port]).
     */
    @NonNull
    public static String normalizeOrigin(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }

        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                return url;
            }

            scheme = scheme.toLowerCase();
            host = host.toLowerCase();

            int port = uri.getPort();
            if (port != -1 && !(("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443))) {
                return scheme + "://" + host + ":" + port;
            }
            return scheme + "://" + host;
        } catch (Exception e) {
            return url;
        }
    }

    @NonNull
    public State getPermissionState(@NonNull String url, @NonNull String permission) {
        String origin = normalizeOrigin(url);
        String key = makeKey(origin, permission);
        State cached = mCache.get(key);
        return (cached != null) ? cached : State.PROMPT;
    }

    public void setPermissionState(@NonNull String url, @NonNull String permission, @NonNull State state, @Nullable Runnable onComplete) {
        String origin = normalizeOrigin(url);
        String key = makeKey(origin, permission);

        if (state == State.PROMPT) {
            mCache.remove(key);
        } else {
            mCache.put(key, state);
        }

        mDbHelper.setSitePermission(origin, permission, state.value, success -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void clearPermissionsForOrigin(@NonNull String url, @Nullable Runnable onComplete) {
        String origin = normalizeOrigin(url);
        for (String k : mCache.keySet()) {
            if (k.startsWith(origin + "::")) {
                mCache.remove(k);
            }
        }

        mDbHelper.clearSitePermissionsForOrigin(origin, success -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void clearAllPermissions() {
        mCache.clear();
    }

    @NonNull
    private static String makeKey(@NonNull String origin, @NonNull String permission) {
        return origin + "::" + permission.toLowerCase();
    }
}
