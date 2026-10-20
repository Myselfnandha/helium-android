package net.imput.helium.widget;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * WidgetPreferencesStore: Persists per-widget instance configuration
 * including dimensions, corner radius, themes, and enabled shortcuts.
 */
public final class WidgetPreferencesStore {

    private static final String PREF_NAME = "helium_widget_preferences";

    public static final int DEFAULT_CORNER_RADIUS_DP = 24;
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_ACCENT = "accent";

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getCornerRadius(@NonNull Context context, int widgetId) {
        return getPrefs(context).getInt("widget_" + widgetId + "_radius", DEFAULT_CORNER_RADIUS_DP);
    }

    public static void setCornerRadius(@NonNull Context context, int widgetId, int radiusDp) {
        int clamped = Math.max(8, Math.min(32, radiusDp));
        getPrefs(context).edit().putInt("widget_" + widgetId + "_radius", clamped).apply();
    }

    @NonNull
    public static String getTheme(@NonNull Context context, int widgetId) {
        String theme = getPrefs(context).getString("widget_" + widgetId + "_theme", THEME_SYSTEM);
        return (theme != null) ? theme : THEME_SYSTEM;
    }

    public static void setTheme(@NonNull Context context, int widgetId, @Nullable String theme) {
        String safeTheme = (theme != null) ? theme : THEME_SYSTEM;
        getPrefs(context).edit().putString("widget_" + widgetId + "_theme", safeTheme).apply();
    }

    public static boolean isVoiceEnabled(@NonNull Context context, int widgetId) {
        return getPrefs(context).getBoolean("widget_" + widgetId + "_voice", true);
    }

    public static void setVoiceEnabled(@NonNull Context context, int widgetId, boolean enabled) {
        getPrefs(context).edit().putBoolean("widget_" + widgetId + "_voice", enabled).apply();
    }

    public static boolean isBookmarksEnabled(@NonNull Context context, int widgetId) {
        return getPrefs(context).getBoolean("widget_" + widgetId + "_bookmarks", true);
    }

    public static void setBookmarksEnabled(@NonNull Context context, int widgetId, boolean enabled) {
        getPrefs(context).edit().putBoolean("widget_" + widgetId + "_bookmarks", enabled).apply();
    }

    public static boolean isQrEnabled(@NonNull Context context, int widgetId) {
        return getPrefs(context).getBoolean("widget_" + widgetId + "_qr", false);
    }

    public static void setQrEnabled(@NonNull Context context, int widgetId, boolean enabled) {
        getPrefs(context).edit().putBoolean("widget_" + widgetId + "_qr", enabled).apply();
    }

    public static boolean isRestoreDraftEnabled(@NonNull Context context, int widgetId) {
        return getPrefs(context).getBoolean("widget_" + widgetId + "_restore_draft", true);
    }

    public static void setRestoreDraftEnabled(@NonNull Context context, int widgetId, boolean enabled) {
        getPrefs(context).edit().putBoolean("widget_" + widgetId + "_restore_draft", enabled).apply();
    }

    public static void deleteWidgetPrefs(@NonNull Context context, int widgetId) {
        getPrefs(context).edit()
                .remove("widget_" + widgetId + "_radius")
                .remove("widget_" + widgetId + "_theme")
                .remove("widget_" + widgetId + "_voice")
                .remove("widget_" + widgetId + "_bookmarks")
                .remove("widget_" + widgetId + "_qr")
                .remove("widget_" + widgetId + "_restore_draft")
                .apply();
    }

    public static void resetToDefaults(@NonNull Context context, int widgetId) {
        setCornerRadius(context, widgetId, DEFAULT_CORNER_RADIUS_DP);
        setTheme(context, widgetId, THEME_SYSTEM);
        setVoiceEnabled(context, widgetId, true);
        setBookmarksEnabled(context, widgetId, true);
        setQrEnabled(context, widgetId, false);
        setRestoreDraftEnabled(context, widgetId, true);
    }
}
