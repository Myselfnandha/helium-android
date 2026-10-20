package net.imput.helium.browser.shortcut;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.imput.helium.R;

/**
 * PwaShortcutManager: Deep-module controller for Home-Screen Web Applications (PRD M25).
 * Creates pinned shortcuts for web applications and websites with validation.
 */
public final class PwaShortcutManager {

    public static final String EXTRA_PWA_URL = "extra_pwa_url";
    public static final String EXTRA_PWA_TITLE = "extra_pwa_title";
    public static final String EXTRA_PWA_THEME_COLOR = "extra_pwa_theme_color";
    public static final String PWA_ACTIVITY_CLASS = "net.imput.helium.browser.shortcut.PwaActivity";

    /**
     * Checks if a URL is eligible to be pinned as a home-screen web application.
     * Only public HTTP and HTTPS websites can be pinned (rejects about:, file:, javascript:).
     */
    public static boolean isEligibleUrl(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    /**
     * Builds the standalone launch intent for the pinned shortcut.
     */
    @NonNull
    public static Intent buildShortcutIntent(@NonNull Context context, @NonNull String url, @NonNull String title) {
        return buildShortcutIntent(context, url, title, null);
    }

    /**
     * Builds the standalone launch intent for the pinned shortcut with optional theme color.
     */
    @NonNull
    public static Intent buildShortcutIntent(@NonNull Context context, @NonNull String url, @NonNull String title, @Nullable String themeColor) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage(context.getPackageName());
        intent.setClassName(context.getPackageName(), PWA_ACTIVITY_CLASS);
        intent.putExtra("from_pwa_shortcut", true);
        intent.putExtra("shortcut_title", title);
        intent.putExtra(EXTRA_PWA_URL, url);
        intent.putExtra(EXTRA_PWA_TITLE, title);
        if (themeColor != null) {
            intent.putExtra(EXTRA_PWA_THEME_COLOR, themeColor);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    /**
     * Requests the system launcher to pin a web application shortcut to the home screen.
     */
    public static boolean requestPinShortcut(@NonNull Context context, @NonNull String url, @NonNull String title, @Nullable Bitmap favicon) {
        return requestPinShortcut(context, url, title, favicon, null);
    }

    /**
     * Requests the system launcher to pin a web application shortcut with theme color.
     */
    public static boolean requestPinShortcut(
            @NonNull Context context,
            @NonNull String url,
            @NonNull String title,
            @Nullable Bitmap favicon,
            @Nullable String themeColor) {
        if (!isEligibleUrl(url)) {
            return false;
        }

        String safeTitle = (title.trim().isEmpty()) ? url : title.trim();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                String shortcutId = "helium_pwa_" + Integer.toHexString(url.hashCode());
                Intent intent = buildShortcutIntent(context, url, safeTitle, themeColor);

                ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, shortcutId)
                        .setShortLabel(safeTitle)
                        .setLongLabel(safeTitle)
                        .setIntent(intent);

                if (favicon != null && !favicon.isRecycled()) {
                    builder.setIcon(Icon.createWithBitmap(favicon));
                } else {
                    builder.setIcon(Icon.createWithResource(context, R.drawable.ic_helium_logo));
                }

                return shortcutManager.requestPinShortcut(builder.build(), null);
            }
        }

        // Legacy broadcast fallback
        Intent installer = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        installer.putExtra(Intent.EXTRA_SHORTCUT_INTENT, buildShortcutIntent(context, url, safeTitle, themeColor));
        installer.putExtra(Intent.EXTRA_SHORTCUT_NAME, safeTitle);
        context.sendBroadcast(installer);
        return true;
    }

    /**
     * Requests the system launcher to pin a PWA parsed from WebAppManifest.
     */
    public static boolean requestPinShortcut(@NonNull Context context, @NonNull WebAppManifest manifest, @Nullable Bitmap favicon) {
        return requestPinShortcut(context, manifest.getStartUrl(), manifest.getName(), favicon, manifest.getThemeColor());
    }
}
