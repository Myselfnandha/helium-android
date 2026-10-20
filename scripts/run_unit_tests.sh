#!/usr/bin/env bash
set -euo pipefail

# scripts/run_unit_tests.sh - Runs Helium Android Java unit tests using JUnit

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Running Helium Android Unit Tests ===${NC}"

python3 - << 'PYEOF'
import subprocess, os, tempfile, sys, urllib.request

cache_dir = os.path.expanduser('~/.cache/helium_test_lib')
os.makedirs(cache_dir, exist_ok=True)
junit_jar = os.path.join(cache_dir, 'junit-4.13.2.jar')
hamcrest_jar = os.path.join(cache_dir, 'hamcrest-core-1.3.jar')
json_jar = os.path.join(cache_dir, 'json-20231013.jar')

if not os.path.exists(junit_jar):
    urllib.request.urlretrieve('https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar', junit_jar)
if not os.path.exists(hamcrest_jar):
    urllib.request.urlretrieve('https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar', hamcrest_jar)
if not os.path.exists(json_jar):
    urllib.request.urlretrieve('https://repo1.maven.org/maven2/org/json/json/20231013/json-20231013.jar', json_jar)

with tempfile.TemporaryDirectory() as tmpdir:
    # Stubs
    os.makedirs(os.path.join(tmpdir, 'androidx/annotation'))
    with open(os.path.join(tmpdir, 'androidx/annotation/NonNull.java'), 'w') as f:
        f.write('package androidx.annotation; import java.lang.annotation.*; @Retention(RetentionPolicy.CLASS) @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD}) public @interface NonNull {}')
    with open(os.path.join(tmpdir, 'androidx/annotation/Nullable.java'), 'w') as f:
        f.write('package androidx.annotation; import java.lang.annotation.*; @Retention(RetentionPolicy.CLASS) @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD}) public @interface Nullable {}')

    os.makedirs(os.path.join(tmpdir, 'android/content'))
    with open(os.path.join(tmpdir, 'android/content/SharedPreferences.java'), 'w') as f:
        f.write('package android.content; public interface SharedPreferences { interface Editor { Editor putBoolean(String k, boolean v); Editor putString(String k, String v); default Editor putInt(String k, int v) { return this; } default Editor putLong(String k, long v) { return this; } Editor remove(String k); Editor clear(); void apply(); } boolean contains(String k); java.util.Map<String, ?> getAll(); boolean getBoolean(String k, boolean def); String getString(String k, String def); default int getInt(String k, int def) { return def; } default long getLong(String k, long def) { return def; } Editor edit(); }')
    with open(os.path.join(tmpdir, 'android/content/Context.java'), 'w') as f:
        f.write('package android.content; public abstract class Context { public static final int MODE_PRIVATE = 0; public abstract Context getApplicationContext(); public abstract SharedPreferences getSharedPreferences(String n, int m); public abstract java.io.File getFilesDir(); public String getPackageName() { return "net.imput.helium"; } public <T> T getSystemService(Class<T> serviceClass) { return null; } public void sendBroadcast(Intent intent) {} }')
    with open(os.path.join(tmpdir, 'android/content/Intent.java'), 'w') as f:
        f.write('''package android.content;
import android.net.Uri;
import java.util.*;
public class Intent {
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String EXTRA_SHORTCUT_INTENT = "android.intent.extra.shortcut.INTENT";
    public static final String EXTRA_SHORTCUT_NAME = "android.intent.extra.shortcut.NAME";
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    private String mAction;
    private Uri mData;
    private String mPackage;
    private int mFlags;
    private final Map<String, Object> mExtras = new HashMap<>();
    public Intent() {}
    public Intent(String action) { mAction = action; }
    public Intent(String action, Uri uri) { mAction = action; mData = uri; }
    public Intent(Context c, Class<?> cls) { mClassName = (cls != null) ? cls.getName() : null; }
    public Intent setAction(String action) { mAction = action; return this; }
    public String getAction() { return mAction; }
    public Uri getData() { return mData; }
    public String getDataString() { return (mData != null) ? mData.toString() : null; }
    public Intent setPackage(String p) { mPackage = p; return this; }
    public String getPackage() { return mPackage; }
    public Intent addFlags(int f) { mFlags |= f; return this; }
    private String mClassName;
    public Intent setClassName(String p, String c) { mPackage = p; mClassName = c; return this; }
    public String getClassName() { return mClassName; }
    public Intent putExtra(String k, String v) { mExtras.put(k, v); return this; }
    public Intent putExtra(String k, boolean v) { mExtras.put(k, v); return this; }
    public Intent putExtra(String k, int v) { mExtras.put(k, v); return this; }
    public Intent putExtra(String k, Object v) { mExtras.put(k, v); return this; }
    public String getStringExtra(String k) { Object v = mExtras.get(k); return (v instanceof String) ? (String) v : null; }
    public boolean getBooleanExtra(String k, boolean def) { Object v = mExtras.get(k); return (v instanceof Boolean) ? (Boolean) v : def; }
    public int getIntExtra(String k, int def) { Object v = mExtras.get(k); return (v instanceof Integer) ? (Integer) v : def; }
}''')

    os.makedirs(os.path.join(tmpdir, 'android/util'), exist_ok=True)
    with open(os.path.join(tmpdir, 'android/util/Log.java'), 'w') as f:
        f.write('package android.util; public class Log { public static int d(String t, String m) { return 0; } public static int i(String t, String m) { return 0; } public static int w(String t, String m) { return 0; } public static int e(String t, String m) { return 0; } }')

    os.makedirs(os.path.join(tmpdir, 'android/graphics'), exist_ok=True)
    with open(os.path.join(tmpdir, 'android/graphics/Bitmap.java'), 'w') as f:
        f.write('package android.graphics; public class Bitmap { public boolean isRecycled() { return false; } public void recycle() {} }')

    os.makedirs(os.path.join(tmpdir, 'android/webkit'), exist_ok=True)
    with open(os.path.join(tmpdir, 'android/webkit/JavascriptInterface.java'), 'w') as f:
        f.write('package android.webkit; import java.lang.annotation.*; @Retention(RetentionPolicy.CLASS) @Target({ElementType.METHOD}) public @interface JavascriptInterface {}')
    with open(os.path.join(tmpdir, 'android/webkit/WebView.java'), 'w') as f:
        f.write('package android.webkit; public class WebView { public void onPause() {} public void onResume() {} public void destroy() {} public void stopLoading() {} public void loadUrl(String u) {} public void clearHistory() {} public void removeAllViews() {} public void addJavascriptInterface(Object o, String n) {} }')
    with open(os.path.join(tmpdir, 'android/webkit/WebResourceResponse.java'), 'w') as f:
        f.write('package android.webkit; import java.io.InputStream; import java.util.Map; public class WebResourceResponse { public WebResourceResponse(String mime, String enc, int code, String reason, Map<String, String> headers, InputStream data) {} }')

    os.makedirs(os.path.join(tmpdir, 'android/os'), exist_ok=True)
    with open(os.path.join(tmpdir, 'android/os/Looper.java'), 'w') as f:
        f.write('package android.os; public class Looper { public static Looper getMainLooper() { return new Looper(); } }')
    with open(os.path.join(tmpdir, 'android/os/Handler.java'), 'w') as f:
        f.write('package android.os; public class Handler { public Handler() {} public Handler(Looper l) {} public boolean post(Runnable r) { r.run(); return true; } public boolean postDelayed(Runnable r, long d) { r.run(); return true; } public void removeCallbacks(Runnable r) {} }')
    with open(os.path.join(tmpdir, 'android/os/Build.java'), 'w') as f:
        f.write('package android.os; public class Build { public static class VERSION { public static final int SDK_INT = 34; } public static class VERSION_CODES { public static final int M = 23; public static final int O = 26; } }')

    os.makedirs(os.path.join(tmpdir, 'android/content/pm'))
    with open(os.path.join(tmpdir, 'android/content/pm/ShortcutManager.java'), 'w') as f:
        f.write('package android.content.pm; public class ShortcutManager { public boolean isRequestPinShortcutSupported() { return true; } public boolean requestPinShortcut(ShortcutInfo s, Object o) { return true; } }')
    with open(os.path.join(tmpdir, 'android/content/pm/ShortcutInfo.java'), 'w') as f:
        f.write('package android.content.pm; import android.content.Context; import android.content.Intent; import android.graphics.drawable.Icon; public class ShortcutInfo { public static class Builder { public Builder(Context c, String id) {} public Builder setShortLabel(CharSequence s) { return this; } public Builder setLongLabel(CharSequence s) { return this; } public Builder setIntent(Intent i) { return this; } public Builder setIcon(Icon i) { return this; } public ShortcutInfo build() { return new ShortcutInfo(); } } }')

    os.makedirs(os.path.join(tmpdir, 'android/graphics/drawable'))
    with open(os.path.join(tmpdir, 'android/graphics/drawable/Icon.java'), 'w') as f:
        f.write('package android.graphics.drawable; import android.content.Context; import android.graphics.Bitmap; public class Icon { public static Icon createWithBitmap(Bitmap b) { return new Icon(); } public static Icon createWithResource(Context c, int resId) { return new Icon(); } }')

    os.makedirs(os.path.join(tmpdir, 'android/appwidget'), exist_ok=True)
    with open(os.path.join(tmpdir, 'android/appwidget/AppWidgetProvider.java'), 'w') as f:
        f.write('package android.appwidget; import android.content.Context; import android.content.Intent; public class AppWidgetProvider { public void onUpdate(Context c, AppWidgetManager m, int[] ids) {} public void onDeleted(Context c, int[] ids) {} public void onReceive(Context c, Intent i) {} }')
    with open(os.path.join(tmpdir, 'android/appwidget/AppWidgetManager.java'), 'w') as f:
        f.write('package android.appwidget; import android.content.Context; import android.widget.RemoteViews; public class AppWidgetManager { public static final String EXTRA_APPWIDGET_ID = "appWidgetId"; public void updateAppWidget(int id, RemoteViews v) {} }')

    os.makedirs(os.path.join(tmpdir, 'android/widget'), exist_ok=True)
    with open(os.path.join(tmpdir, 'android/widget/RemoteViews.java'), 'w') as f:
        f.write('package android.widget; import android.app.PendingIntent; public class RemoteViews { public RemoteViews(String p, int l) {} public void setOnClickPendingIntent(int id, PendingIntent pi) {} public void setViewVisibility(int id, int v) {} }')

    os.makedirs(os.path.join(tmpdir, 'android/app'), exist_ok=True)
    with open(os.path.join(tmpdir, 'android/app/PendingIntent.java'), 'w') as f:
        f.write('package android.app; import android.content.Context; import android.content.Intent; public class PendingIntent { public static final int FLAG_UPDATE_CURRENT = 0x08000000; public static final int FLAG_IMMUTABLE = 0x04000000; public static PendingIntent getActivity(Context c, int r, Intent i, int f) { return new PendingIntent(); } }')

    os.makedirs(os.path.join(tmpdir, 'android/view'), exist_ok=True)
    with open(os.path.join(tmpdir, 'android/view/View.java'), 'w') as f:
        f.write('package android.view; public class View { public static final int VISIBLE = 0; public static final int INVISIBLE = 4; public static final int GONE = 8; }')

    os.makedirs(os.path.join(tmpdir, 'net/imput/helium/browser'), exist_ok=True)
    with open(os.path.join(tmpdir, 'net/imput/helium/browser/HeliumMainActivity.java'), 'w') as f:
        f.write('package net.imput.helium.browser; public class HeliumMainActivity { public static final String EXTRA_OPEN_BOOKMARKS = "net.imput.helium.widget.EXTRA_OPEN_BOOKMARKS"; }')

    os.makedirs(os.path.join(tmpdir, 'net/imput/helium/widget'), exist_ok=True)
    with open(os.path.join(tmpdir, 'net/imput/helium/widget/QuickSearchOverlayActivity.java'), 'w') as f:
        f.write('package net.imput.helium.widget; public class QuickSearchOverlayActivity {}')

    os.makedirs(os.path.join(tmpdir, 'net/imput/helium'), exist_ok=True)
    with open(os.path.join(tmpdir, 'net/imput/helium/R.java'), 'w') as f:
        f.write('package net.imput.helium; public class R { public static class drawable { public static final int ic_helium_logo = 100; } public static class layout { public static final int widget_search_bar = 200; } public static class id { public static final int widget_search_text = 301; public static final int widget_btn_logo = 302; public static final int widget_btn_mic = 303; public static final int widget_btn_bookmarks = 304; public static final int widget_btn_qr = 305; } }')

    os.makedirs(os.path.join(tmpdir, 'net/imput/helium/database'), exist_ok=True)
    with open(os.path.join(tmpdir, 'net/imput/helium/database/HeliumDbHelper.java'), 'w') as f:
        f.write('''package net.imput.helium.database;
import java.util.*;
import net.imput.helium.search.SearchSuggestion;
import net.imput.helium.privacy.permissions.SitePermissionRecord;
public class HeliumDbHelper {
    public interface Callback<T> { void onComplete(T result); }
    public HeliumDbHelper() {}
    public HeliumDbHelper(android.content.Context c) {}
    public static HeliumDbHelper getInstance(android.content.Context c) { return new HeliumDbHelper(c); }
    public void searchSuggestions(String q, int m, Callback<List<SearchSuggestion>> cb) { cb.onComplete(new ArrayList<>()); }
    public void getAllBookmarks(Callback<List<BookmarkRecord>> cb) { cb.onComplete(new ArrayList<>()); }
    public void getRecentHistory(int limit, Callback<List<HistoryRecord>> cb) { cb.onComplete(new ArrayList<>()); }
    public void addBookmark(String url, String title, Callback<Boolean> cb) { if (cb != null) cb.onComplete(true); }
    public void recordVisit(String url, String title) {}
    public void setSitePermission(String origin, String perm, int state, Callback<Boolean> cb) { if (cb != null) cb.onComplete(true); }
    public void getSitePermission(String origin, String perm, Callback<Integer> cb) { if (cb != null) cb.onComplete(0); }
    public void getAllSitePermissions(Callback<List<SitePermissionRecord>> cb) { if (cb != null) cb.onComplete(new ArrayList<>()); }
    public void clearSitePermissionsForOrigin(String origin, Callback<Boolean> cb) { if (cb != null) cb.onComplete(true); }
    public void clearAllBrowsingData(Runnable r) { if (r != null) r.run(); }
}''')

    os.makedirs(os.path.join(tmpdir, 'android/net'))
    with open(os.path.join(tmpdir, 'android/net/Uri.java'), 'w') as f:
        f.write('''package android.net;
import java.net.URLEncoder;
import java.util.*;
public class Uri {
    private final String mUri;
    public Uri(String uri) { mUri = uri; }
    public static String encode(String s) {
        try { return URLEncoder.encode(s, "UTF-8").replace("+", "%20"); } catch (Exception e) { return s; }
    }
    public static Uri parse(String uriString) { return new Uri(uriString); }
    public boolean isHierarchical() { return mUri != null && mUri.contains("://"); }
    public String getScheme() {
        if (mUri == null || !mUri.contains("://")) return null;
        return mUri.substring(0, mUri.indexOf("://"));
    }
    public String getHost() {
        if (mUri == null || !mUri.contains("://")) return null;
        String s = mUri.substring(mUri.indexOf("://") + 3);
        int slash = s.indexOf("/");
        if (slash != -1) s = s.substring(0, slash);
        int colon = s.indexOf(":");
        if (colon != -1) s = s.substring(0, colon);
        return s;
    }
    public int getPort() {
        if (mUri == null || !mUri.contains("://")) return -1;
        String s = mUri.substring(mUri.indexOf("://") + 3);
        int slash = s.indexOf("/");
        if (slash != -1) s = s.substring(0, slash);
        int colon = s.indexOf(":");
        if (colon != -1) {
            try { return Integer.parseInt(s.substring(colon + 1)); } catch (Exception ignored) {}
        }
        return -1;
    }
    public Set<String> getQueryParameterNames() {
        Set<String> set = new LinkedHashSet<>();
        if (mUri == null || !mUri.contains("?")) return set;
        String q = mUri.substring(mUri.indexOf("?") + 1);
        int hash = q.indexOf("#");
        if (hash != -1) q = q.substring(0, hash);
        for (String pair : q.split("&")) {
            int eq = pair.indexOf("=");
            if (eq != -1) set.add(pair.substring(0, eq));
            else if (!pair.isEmpty()) set.add(pair);
        }
        return set;
    }
    public List<String> getQueryParameters(String key) {
        List<String> list = new ArrayList<>();
        if (mUri == null || !mUri.contains("?")) return list;
        String q = mUri.substring(mUri.indexOf("?") + 1);
        for (String pair : q.split("&")) {
            int eq = pair.indexOf("=");
            if (eq != -1 && pair.substring(0, eq).equals(key)) {
                list.add(pair.substring(eq + 1));
            }
        }
        return list;
    }
    public Builder buildUpon() { return new Builder(mUri); }
    public String toString() { return mUri; }
    public static class Builder {
        private String base;
        private List<String> params = new ArrayList<>();
        public Builder(String b) {
            int q = (b != null) ? b.indexOf("?") : -1;
            this.base = (q != -1 && b != null) ? b.substring(0, q) : (b != null ? b : "");
        }
        public Builder clearQuery() { params.clear(); return this; }
        public Builder appendQueryParameter(String k, String v) {
            params.add(k + "=" + v);
            return this;
        }
        public Uri build() {
            if (params.isEmpty()) return new Uri(base);
            return new Uri(base + "?" + String.join("&", params));
        }
    }
}
''')

    # Compile Sources
    subprocess.run([
        'javac', '-cp', f'{json_jar}', '-d', tmpdir,
        os.path.join(tmpdir, 'androidx/annotation/NonNull.java'),
        os.path.join(tmpdir, 'androidx/annotation/Nullable.java'),
        os.path.join(tmpdir, 'android/content/SharedPreferences.java'),
        os.path.join(tmpdir, 'android/content/Context.java'),
        os.path.join(tmpdir, 'android/content/Intent.java'),
        os.path.join(tmpdir, 'android/util/Log.java'),
        os.path.join(tmpdir, 'android/graphics/Bitmap.java'),
        os.path.join(tmpdir, 'android/graphics/drawable/Icon.java'),
        os.path.join(tmpdir, 'android/webkit/WebView.java'),
        os.path.join(tmpdir, 'android/webkit/JavascriptInterface.java'),
        os.path.join(tmpdir, 'android/webkit/WebResourceResponse.java'),
        os.path.join(tmpdir, 'android/net/Uri.java'),
        os.path.join(tmpdir, 'android/os/Looper.java'),
        os.path.join(tmpdir, 'android/os/Handler.java'),
        os.path.join(tmpdir, 'android/os/Build.java'),
        os.path.join(tmpdir, 'android/content/pm/ShortcutManager.java'),
        os.path.join(tmpdir, 'android/content/pm/ShortcutInfo.java'),
        os.path.join(tmpdir, 'android/appwidget/AppWidgetProvider.java'),
        os.path.join(tmpdir, 'android/appwidget/AppWidgetManager.java'),
        os.path.join(tmpdir, 'android/widget/RemoteViews.java'),
        os.path.join(tmpdir, 'android/app/PendingIntent.java'),
        os.path.join(tmpdir, 'android/view/View.java'),
        os.path.join(tmpdir, 'net/imput/helium/browser/HeliumMainActivity.java'),
        os.path.join(tmpdir, 'net/imput/helium/widget/QuickSearchOverlayActivity.java'),
        os.path.join(tmpdir, 'net/imput/helium/R.java'),
        'helium-android/src/main/java/net/imput/helium/privacy/permissions/SitePermissionRecord.java',
        os.path.join(tmpdir, 'net/imput/helium/database/HeliumDbHelper.java'),
        'helium-android/src/main/java/net/imput/helium/privacy/permissions/SitePermissionsManager.java',
        'helium-android/src/main/java/net/imput/helium/preferences/HeliumPreferences.java',
        'helium-android/src/main/java/net/imput/helium/extensions/ExtensionItem.java',
        'helium-android/src/main/java/net/imput/helium/extensions/WebExtensionApiBridge.java',
        'helium-android/src/main/java/net/imput/helium/adblock/AdBlockEngine.java',
        'helium-android/src/main/java/net/imput/helium/browser/tab/TabGroup.java',
        'helium-android/src/main/java/net/imput/helium/browser/tab/Tab.java',
        'helium-android/src/main/java/net/imput/helium/browser/tab/TabManager.java',
        'helium-android/src/main/java/net/imput/helium/search/bangs/BangsRouter.java',
        'helium-android/src/main/java/net/imput/helium/privacy/tracker/UrlTrackerStripper.java',
        'helium-android/src/main/java/net/imput/helium/privacy/https/HttpsUpgrader.java',
        'helium-android/src/main/java/net/imput/helium/database/BookmarkRecord.java',
        'helium-android/src/main/java/net/imput/helium/database/BookmarkFolder.java',
        'helium-android/src/main/java/net/imput/helium/database/HistoryRecord.java',
        'helium-android/src/main/java/net/imput/helium/sync/VaultCrypto.java',
        'helium-android/src/main/java/net/imput/helium/sync/VaultBundle.java',
        'helium-android/src/main/java/net/imput/helium/sync/VaultSyncEngine.java',
        'helium-android/src/main/java/net/imput/helium/sync/VaultClient.java',
        'helium-android/src/main/java/net/imput/helium/profile/Profile.java',
        'helium-android/src/main/java/net/imput/helium/profile/ProfileManager.java',
        'helium-android/src/main/java/net/imput/helium/browser/shortcut/PwaShortcutManager.java',
        'helium-android/src/main/java/net/imput/helium/browser/shortcut/WebAppManifest.java',
        'helium-android/src/main/java/net/imput/helium/diagnostics/HeliumDiagnostics.java',
        'helium-android/src/main/java/net/imput/helium/search/SearchSuggestion.java',
        'helium-android/src/main/java/net/imput/helium/search/SearchSuggestionEngine.java',
        'helium-android/src/main/java/net/imput/helium/widget/WidgetPreferencesStore.java',
        'helium-android/src/main/java/net/imput/helium/widget/SearchHistoryStore.java',
        'helium-android/src/main/java/net/imput/helium/widget/QuickSearchController.java',
        'helium-android/src/main/java/net/imput/helium/widget/SearchWidgetProvider.java'
    ], check=True)

    # Compile Tests
    subprocess.run([
        'javac', '-cp', f'{tmpdir}:{junit_jar}:{hamcrest_jar}:{json_jar}', '-d', tmpdir,
        'helium-android/src/test/java/net/imput/helium/HeliumPreferencesTest.java',
        'helium-android/src/test/java/net/imput/helium/VaultClientTest.java',
        'helium-android/src/test/java/net/imput/helium/ExtensionManagerTest.java',
        'helium-android/src/test/java/net/imput/helium/WebExtensionApiBridgeTest.java',
        'helium-android/src/test/java/net/imput/helium/AdBlockEngineTest.java',
        'helium-android/src/test/java/net/imput/helium/TabManagerTest.java',
        'helium-android/src/test/java/net/imput/helium/TabGroupTest.java',
        'helium-android/src/test/java/net/imput/helium/SitePermissionsTest.java',
        'helium-android/src/test/java/net/imput/helium/BangsRouterTest.java',
        'helium-android/src/test/java/net/imput/helium/UrlTrackerStripperTest.java',
        'helium-android/src/test/java/net/imput/helium/HttpsUpgraderTest.java',
        'helium-android/src/test/java/net/imput/helium/VaultCryptoTest.java',
        'helium-android/src/test/java/net/imput/helium/VaultBundleTest.java',
        'helium-android/src/test/java/net/imput/helium/TabSessionTest.java',
        'helium-android/src/test/java/net/imput/helium/SearchSuggestionTest.java',
        'helium-android/src/test/java/net/imput/helium/BookmarkFolderTest.java',
        'helium-android/src/test/java/net/imput/helium/ProfileTest.java',
        'helium-android/src/test/java/net/imput/helium/PwaShortcutTest.java',
        'helium-android/src/test/java/net/imput/helium/WebAppManifestTest.java',
        'helium-android/src/test/java/net/imput/helium/HeliumDiagnosticsTest.java',
        'helium-android/src/test/java/net/imput/helium/WidgetPreferencesStoreTest.java',
        'helium-android/src/test/java/net/imput/helium/SearchHistoryStoreTest.java',
        'helium-android/src/test/java/net/imput/helium/QuickSearchControllerTest.java',
        'helium-android/src/test/java/net/imput/helium/SearchWidgetProviderTest.java'
    ], check=True)

    # Run Tests
    res = subprocess.run([
        'java', '-cp', f'{tmpdir}:{junit_jar}:{hamcrest_jar}:{json_jar}',
        'org.junit.runner.JUnitCore',
        'net.imput.helium.HeliumPreferencesTest',
        'net.imput.helium.VaultClientTest',
        'net.imput.helium.ExtensionManagerTest',
        'net.imput.helium.WebExtensionApiBridgeTest',
        'net.imput.helium.AdBlockEngineTest',
        'net.imput.helium.TabManagerTest',
        'net.imput.helium.TabGroupTest',
        'net.imput.helium.SitePermissionsTest',
        'net.imput.helium.BangsRouterTest',
        'net.imput.helium.UrlTrackerStripperTest',
        'net.imput.helium.HttpsUpgraderTest',
        'net.imput.helium.VaultCryptoTest',
        'net.imput.helium.VaultBundleTest',
        'net.imput.helium.TabSessionTest',
        'net.imput.helium.SearchSuggestionTest',
        'net.imput.helium.BookmarkFolderTest',
        'net.imput.helium.ProfileTest',
        'net.imput.helium.PwaShortcutTest',
        'net.imput.helium.WebAppManifestTest',
        'net.imput.helium.HeliumDiagnosticsTest',
        'net.imput.helium.WidgetPreferencesStoreTest',
        'net.imput.helium.SearchHistoryStoreTest',
        'net.imput.helium.QuickSearchControllerTest',
        'net.imput.helium.SearchWidgetProviderTest'
    ], capture_output=True, text=True)

    print(res.stdout)
    if res.returncode != 0:
        print(res.stderr, file=sys.stderr)
        sys.exit(res.returncode)
PYEOF

echo -e "${GREEN}All unit tests passed successfully!${NC}"
