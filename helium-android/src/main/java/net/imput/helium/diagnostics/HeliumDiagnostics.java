package net.imput.helium.diagnostics;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * HeliumDiagnostics: Privacy-preserving health and diagnostics monitoring (PRD M28).
 * Enforces strict URL redaction, credential scrubbing, and error rate telemetry.
 */
public final class HeliumDiagnostics {

    public static final String CAT_NAVIGATION = "navigation";
    public static final String CAT_DATABASE = "database";
    public static final String CAT_SYNC = "sync";
    public static final String CAT_DOWNLOAD = "download";
    public static final String CAT_MEMORY = "memory";

    private static final Pattern PATTERN_BEARER = Pattern.compile("(?i)(bearer\\s+)[a-zA-Z0-9_\\-\\.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_PASSWORD = Pattern.compile("(?i)(password|secret|token|apikey)[\\s:=]+[^\\s,;&]+", Pattern.CASE_INSENSITIVE);

    private static volatile HeliumDiagnostics sInstance;
    private final Map<String, Integer> mErrorCounts = new ConcurrentHashMap<>();

    public static HeliumDiagnostics getInstance() {
        if (sInstance == null) {
            synchronized (HeliumDiagnostics.class) {
                if (sInstance == null) {
                    sInstance = new HeliumDiagnostics();
                }
            }
        }
        return sInstance;
    }

    public HeliumDiagnostics() {}

    /**
     * Redacts a URL for privacy-safe error logging:
     * - Strips query parameters (?token=... etc)
     * - Strips hash anchors (#section)
     * - Strips user info credentials (user:pass@host)
     * Preserves only scheme, host, and path prefix.
     */
    @NonNull
    public static String redactUrl(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return "[empty]";
        }

        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                // Return sanitized scheme or safe prefix
                int q = url.indexOf('?');
                if (q != -1) url = url.substring(0, q);
                int h = url.indexOf('#');
                if (h != -1) url = url.substring(0, h);
                return url;
            }

            return scheme.toLowerCase() + "://" + host.toLowerCase() + "/[redacted]";
        } catch (Exception e) {
            return "[malformed_url]";
        }
    }

    /**
     * Scrubs potential authentication tokens, passwords, and bearer headers from logs.
     */
    @NonNull
    public static String scrubSensitiveData(@Nullable String message) {
        if (message == null) return "";
        String scrubbed = PATTERN_BEARER.matcher(message).replaceAll("$1[REDACTED]");
        scrubbed = PATTERN_PASSWORD.matcher(scrubbed).replaceAll("$1=[REDACTED]");
        return scrubbed;
    }

    /**
     * Records a diagnostic event safely with redaction.
     */
    public void recordError(@NonNull String category, @NonNull String errorCode, @Nullable String rawContext) {
        String key = category + ":" + errorCode;
        mErrorCounts.merge(key, 1, Integer::sum);
    }

    public int getErrorCount(@NonNull String category, @NonNull String errorCode) {
        return mErrorCounts.getOrDefault(category + ":" + errorCode, 0);
    }

    @NonNull
    public Map<String, Integer> getAllErrorCounts() {
        return Collections.unmodifiableMap(mErrorCounts);
    }

    public void clearDiagnostics() {
        mErrorCounts.clear();
    }
}
