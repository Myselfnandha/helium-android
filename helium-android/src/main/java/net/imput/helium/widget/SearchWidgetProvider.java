package net.imput.helium.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import net.imput.helium.R;
import net.imput.helium.browser.HeliumMainActivity;

/**
 * SearchWidgetProvider: Standard Android AppWidget publishing the home-screen
 * search bar with configurable shortcuts and instant overlay launch.
 */
public class SearchWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_QUICK_SEARCH = "net.imput.helium.widget.ACTION_QUICK_SEARCH";
    public static final String EXTRA_START_VOICE = "net.imput.helium.widget.EXTRA_START_VOICE";
    public static final String EXTRA_OPEN_BOOKMARKS = "net.imput.helium.widget.EXTRA_OPEN_BOOKMARKS";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            WidgetPreferencesStore.deleteWidgetPrefs(context, widgetId);
        }
    }

    public static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_search_bar);

        // 1. Primary tap action on search bar / logo -> QuickSearchOverlayActivity
        Intent searchIntent = new Intent(context, QuickSearchOverlayActivity.class);
        searchIntent.setAction(ACTION_QUICK_SEARCH);
        searchIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ? (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pendingSearch = PendingIntent.getActivity(context, widgetId, searchIntent, flags);
        views.setOnClickPendingIntent(R.id.widget_search_text, pendingSearch);
        views.setOnClickPendingIntent(R.id.widget_btn_logo, pendingSearch);

        // 2. Voice Search shortcut
        boolean voiceEnabled = WidgetPreferencesStore.isVoiceEnabled(context, widgetId);
        views.setViewVisibility(R.id.widget_btn_mic, voiceEnabled ? View.VISIBLE : View.GONE);
        if (voiceEnabled) {
            Intent voiceIntent = new Intent(context, QuickSearchOverlayActivity.class);
            voiceIntent.setAction(ACTION_QUICK_SEARCH);
            voiceIntent.putExtra(EXTRA_START_VOICE, true);
            voiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            voiceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingVoice = PendingIntent.getActivity(context, widgetId + 1000, voiceIntent, flags);
            views.setOnClickPendingIntent(R.id.widget_btn_mic, pendingVoice);
        }

        // 3. Bookmarks shortcut
        boolean bookmarksEnabled = WidgetPreferencesStore.isBookmarksEnabled(context, widgetId);
        views.setViewVisibility(R.id.widget_btn_bookmarks, bookmarksEnabled ? View.VISIBLE : View.GONE);
        if (bookmarksEnabled) {
            Intent bookmarksIntent = new Intent(context, HeliumMainActivity.class);
            bookmarksIntent.putExtra(EXTRA_OPEN_BOOKMARKS, true);
            bookmarksIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingBookmarks = PendingIntent.getActivity(context, widgetId + 2000, bookmarksIntent, flags);
            views.setOnClickPendingIntent(R.id.widget_btn_bookmarks, pendingBookmarks);
        }

        // 4. QR Scanner shortcut
        boolean qrEnabled = WidgetPreferencesStore.isQrEnabled(context, widgetId);
        views.setViewVisibility(R.id.widget_btn_qr, qrEnabled ? View.VISIBLE : View.GONE);

        appWidgetManager.updateAppWidget(widgetId, views);
    }
}
