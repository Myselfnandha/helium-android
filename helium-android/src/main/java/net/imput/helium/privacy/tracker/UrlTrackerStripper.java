package net.imput.helium.privacy.tracker;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * UrlTrackerStripper: Strips marketing, telemetry, and tracking query parameters
 * from incoming and outgoing URLs before navigation.
 */
public final class UrlTrackerStripper {

    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
        // Google / Analytics
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_name", "gclid", "gclsrc", "dclid", "gad_source",
        // Facebook / Meta
        "fbclid", "fref",
        // Microsoft / Bing
        "msclkid",
        // Twitter / X
        "twclid",
        // TikTok
        "ttclid",
        // Mailchimp & Newsletters
        "mc_cid", "mc_eid",
        // HubSpot
        "_hsenc", "_hsmi", "hsctstracking",
        // Yandex
        "yclid", "ym_debug",
        // Generic Click IDs
        "wickedid", "oly_anon_id", "oly_enc_id", "rb_clickid"
    ));

    private UrlTrackerStripper() {}

    /**
     * Sanitizes a URL by removing known tracking query parameters.
     * Returns the original URL if no tracking parameters are found or if malformed.
     */
    @NonNull
    public static String strip(@Nullable String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            return "";
        }
        if (!urlString.contains("?")) {
            return urlString;
        }

        try {
            Uri uri = Uri.parse(urlString);
            if (!uri.isHierarchical()) {
                return urlString;
            }

            Set<String> paramNames = uri.getQueryParameterNames();
            if (paramNames.isEmpty()) {
                return urlString;
            }

            boolean hasTracker = false;
            for (String param : paramNames) {
                if (TRACKING_PARAMS.contains(param.toLowerCase())) {
                    hasTracker = true;
                    break;
                }
            }

            if (!hasTracker) {
                return urlString;
            }

            // Build sanitized URI preserving functional parameters
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String param : paramNames) {
                if (!TRACKING_PARAMS.contains(param.toLowerCase())) {
                    for (String val : uri.getQueryParameters(param)) {
                        builder.appendQueryParameter(param, val);
                    }
                }
            }

            return builder.build().toString();
        } catch (Exception e) {
            return urlString;
        }
    }

    public static boolean containsTrackingParameters(@Nullable String urlString) {
        if (urlString == null || !urlString.contains("?")) {
            return false;
        }
        try {
            Uri uri = Uri.parse(urlString);
            for (String param : uri.getQueryParameterNames()) {
                if (TRACKING_PARAMS.contains(param.toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
