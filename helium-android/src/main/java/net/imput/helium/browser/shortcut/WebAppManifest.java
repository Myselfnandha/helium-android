package net.imput.helium.browser.shortcut;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * WebAppManifest: Domain model and W3C Web App Manifest parser for PRD M25.
 * Handles manifest parsing, relative URL resolution, display modes, and icon ranking.
 */
public final class WebAppManifest {

    public enum DisplayMode {
        STANDALONE("standalone"),
        FULLSCREEN("fullscreen"),
        MINIMAL_UI("minimal-ui"),
        BROWSER("browser");

        public final String value;

        DisplayMode(String value) {
            this.value = value;
        }

        @NonNull
        public static DisplayMode fromString(@Nullable String str) {
            if (str == null) return BROWSER;
            for (DisplayMode mode : values()) {
                if (mode.value.equalsIgnoreCase(str.trim())) {
                    return mode;
                }
            }
            return BROWSER;
        }
    }

    public static final class ManifestIcon {
        private final String mSrc;
        private final String mSizes;
        private final String mType;
        private final int mSizePixels;

        public ManifestIcon(@NonNull String src, @Nullable String sizes, @Nullable String type) {
            mSrc = src;
            mSizes = (sizes != null) ? sizes : "";
            mType = (type != null) ? type : "";
            mSizePixels = parseLargestDimension(mSizes);
        }

        @NonNull
        public String getSrc() {
            return mSrc;
        }

        @NonNull
        public String getSizes() {
            return mSizes;
        }

        @NonNull
        public String getType() {
            return mType;
        }

        public int getSizePixels() {
            return mSizePixels;
        }

        private static int parseLargestDimension(String sizes) {
            if (sizes == null || sizes.isEmpty()) return 48;
            try {
                // handles formats like "192x192" or "192x192 512x512"
                String[] split = sizes.split(" ");
                int max = 0;
                for (String s : split) {
                    int x = s.toLowerCase().indexOf('x');
                    if (x != -1) {
                        int val = Integer.parseInt(s.substring(0, x));
                        if (val > max) max = val;
                    }
                }
                return (max > 0) ? max : 48;
            } catch (Exception e) {
                return 48;
            }
        }
    }

    private final String mName;
    private final String mShortName;
    private final String mStartUrl;
    private final DisplayMode mDisplay;
    private final String mThemeColor;
    private final String mBackgroundColor;
    private final List<ManifestIcon> mIcons;

    public WebAppManifest(
            @NonNull String name,
            @Nullable String shortName,
            @NonNull String startUrl,
            @NonNull DisplayMode display,
            @Nullable String themeColor,
            @Nullable String backgroundColor,
            @NonNull List<ManifestIcon> icons) {
        mName = name;
        mShortName = (shortName != null && !shortName.isEmpty()) ? shortName : name;
        mStartUrl = startUrl;
        mDisplay = display;
        mThemeColor = themeColor;
        mBackgroundColor = backgroundColor;
        mIcons = Collections.unmodifiableList(new ArrayList<>(icons));
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public String getShortName() {
        return mShortName;
    }

    @NonNull
    public String getStartUrl() {
        return mStartUrl;
    }

    @NonNull
    public DisplayMode getDisplay() {
        return mDisplay;
    }

    public boolean isStandalone() {
        return mDisplay == DisplayMode.STANDALONE || mDisplay == DisplayMode.FULLSCREEN;
    }

    @Nullable
    public String getThemeColor() {
        return mThemeColor;
    }

    @Nullable
    public String getBackgroundColor() {
        return mBackgroundColor;
    }

    @NonNull
    public List<ManifestIcon> getIcons() {
        return mIcons;
    }

    /**
     * Finds the best icon for mobile launcher shortcuts (prefers 192px or larger, max 512px).
     */
    @Nullable
    public ManifestIcon getBestIcon() {
        if (mIcons.isEmpty()) return null;

        ManifestIcon best = null;
        int bestScore = -1;

        for (ManifestIcon icon : mIcons) {
            int size = icon.getSizePixels();
            // Score ideal sizes around 192-512px
            int score;
            if (size >= 192 && size <= 512) {
                score = 1000 + size;
            } else if (size > 512) {
                score = 500;
            } else {
                score = size;
            }

            if (score > bestScore) {
                bestScore = score;
                best = icon;
            }
        }
        return best;
    }

    /**
     * Parses a W3C Web App Manifest JSON document and resolves URLs against base page URL.
     */
    @Nullable
    public static WebAppManifest parse(@Nullable String manifestJson, @NonNull String pageBaseUrl) {
        if (manifestJson == null || manifestJson.trim().isEmpty()) {
            return null;
        }

        try {
            JSONObject root = new JSONObject(manifestJson);
            String name = root.optString("name", "Web App");
            String shortName = root.optString("short_name", name);
            String rawStartUrl = root.optString("start_url", pageBaseUrl);
            String resolvedStartUrl = resolveUrl(pageBaseUrl, rawStartUrl);

            DisplayMode display = DisplayMode.fromString(root.optString("display", "browser"));
            String themeColor = root.has("theme_color") ? root.optString("theme_color") : null;
            String backgroundColor = root.has("background_color") ? root.optString("background_color") : null;

            List<ManifestIcon> icons = new ArrayList<>();
            JSONArray iconsArray = root.optJSONArray("icons");
            if (iconsArray != null) {
                for (int i = 0; i < iconsArray.length(); i++) {
                    JSONObject iconObj = iconsArray.getJSONObject(i);
                    String src = iconObj.optString("src");
                    if (!src.isEmpty()) {
                        String resolvedSrc = resolveUrl(pageBaseUrl, src);
                        String sizes = iconObj.optString("sizes", "48x48");
                        String type = iconObj.optString("type", "image/png");
                        icons.add(new ManifestIcon(resolvedSrc, sizes, type));
                    }
                }
            }

            return new WebAppManifest(name, shortName, resolvedStartUrl, display, themeColor, backgroundColor, icons);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Resolves a relative URL against an absolute base URL.
     */
    @NonNull
    public static String resolveUrl(@NonNull String base, @NonNull String relative) {
        if (relative.startsWith("http://") || relative.startsWith("https://")) {
            return relative;
        }

        try {
            URI baseUri = new URI(base);
            URI resolved = baseUri.resolve(relative);
            return resolved.toString();
        } catch (Exception e) {
            // Fallback string concatenation
            if (relative.startsWith("/")) {
                int proto = base.indexOf("://");
                if (proto != -1) {
                    int nextSlash = base.indexOf('/', proto + 3);
                    String hostPart = (nextSlash != -1) ? base.substring(0, nextSlash) : base;
                    return hostPart + relative;
                }
            }
            return base + (base.endsWith("/") ? "" : "/") + relative;
        }
    }
}
