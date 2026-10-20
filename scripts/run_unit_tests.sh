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
        f.write('package android.content; public interface SharedPreferences { interface Editor { Editor putBoolean(String k, boolean v); Editor putString(String k, String v); Editor remove(String k); Editor clear(); void apply(); } boolean contains(String k); java.util.Map<String, ?> getAll(); boolean getBoolean(String k, boolean def); String getString(String k, String def); Editor edit(); }')
    with open(os.path.join(tmpdir, 'android/content/Context.java'), 'w') as f:
        f.write('package android.content; public abstract class Context { public static final int MODE_PRIVATE = 0; public abstract Context getApplicationContext(); public abstract SharedPreferences getSharedPreferences(String n, int m); public abstract java.io.File getFilesDir(); }')

    os.makedirs(os.path.join(tmpdir, 'android/util'))
    with open(os.path.join(tmpdir, 'android/util/Log.java'), 'w') as f:
        f.write('package android.util; public class Log { public static int d(String t, String m) { return 0; } public static int i(String t, String m) { return 0; } public static int w(String t, String m) { return 0; } public static int e(String t, String m) { return 0; } }')

    os.makedirs(os.path.join(tmpdir, 'android/graphics'))
    with open(os.path.join(tmpdir, 'android/graphics/Bitmap.java'), 'w') as f:
        f.write('package android.graphics; public class Bitmap { public boolean isRecycled() { return false; } public void recycle() {} }')

    os.makedirs(os.path.join(tmpdir, 'android/webkit'))
    with open(os.path.join(tmpdir, 'android/webkit/JavascriptInterface.java'), 'w') as f:
        f.write('package android.webkit; import java.lang.annotation.*; @Retention(RetentionPolicy.CLASS) @Target({ElementType.METHOD}) public @interface JavascriptInterface {}')
    with open(os.path.join(tmpdir, 'android/webkit/WebView.java'), 'w') as f:
        f.write('package android.webkit; public class WebView { public void onPause() {} public void onResume() {} public void destroy() {} public void stopLoading() {} public void loadUrl(String u) {} public void clearHistory() {} public void removeAllViews() {} public void addJavascriptInterface(Object o, String n) {} }')
    with open(os.path.join(tmpdir, 'android/webkit/WebResourceResponse.java'), 'w') as f:
        f.write('package android.webkit; import java.io.InputStream; import java.util.Map; public class WebResourceResponse { public WebResourceResponse(String mime, String enc, int code, String reason, Map<String, String> headers, InputStream data) {} }')

    os.makedirs(os.path.join(tmpdir, 'android/os'))
    with open(os.path.join(tmpdir, 'android/os/Looper.java'), 'w') as f:
        f.write('package android.os; public class Looper { public static Looper getMainLooper() { return new Looper(); } }')
    with open(os.path.join(tmpdir, 'android/os/Handler.java'), 'w') as f:
        f.write('package android.os; public class Handler { public Handler() {} public Handler(Looper l) {} public boolean post(Runnable r) { r.run(); return true; } }')

    os.makedirs(os.path.join(tmpdir, 'net/imput/helium/database'))
    with open(os.path.join(tmpdir, 'net/imput/helium/database/HeliumDbHelper.java'), 'w') as f:
        f.write('''package net.imput.helium.database;
import java.util.*;
import net.imput.helium.search.SearchSuggestion;
public class HeliumDbHelper {
    public interface Callback<T> { void onComplete(T result); }
    public static HeliumDbHelper getInstance(android.content.Context c) { return new HeliumDbHelper(); }
    public void searchSuggestions(String q, int m, Callback<List<SearchSuggestion>> cb) { cb.onComplete(new ArrayList<>()); }
    public void getAllBookmarks(Callback<List<BookmarkRecord>> cb) { cb.onComplete(new ArrayList<>()); }
    public void getRecentHistory(int limit, Callback<List<HistoryRecord>> cb) { cb.onComplete(new ArrayList<>()); }
    public void addBookmark(String url, String title, Callback<Boolean> cb) { if (cb != null) cb.onComplete(true); }
    public void recordVisit(String url, String title) {}
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
        os.path.join(tmpdir, 'android/util/Log.java'),
        os.path.join(tmpdir, 'android/graphics/Bitmap.java'),
        os.path.join(tmpdir, 'android/webkit/WebView.java'),
        os.path.join(tmpdir, 'android/webkit/JavascriptInterface.java'),
        os.path.join(tmpdir, 'android/webkit/WebResourceResponse.java'),
        os.path.join(tmpdir, 'android/net/Uri.java'),
        os.path.join(tmpdir, 'android/os/Looper.java'),
        os.path.join(tmpdir, 'android/os/Handler.java'),
        os.path.join(tmpdir, 'net/imput/helium/database/HeliumDbHelper.java'),
        'helium-android/src/main/java/net/imput/helium/preferences/HeliumPreferences.java',
        'helium-android/src/main/java/net/imput/helium/extensions/ExtensionItem.java',
        'helium-android/src/main/java/net/imput/helium/extensions/WebExtensionApiBridge.java',
        'helium-android/src/main/java/net/imput/helium/adblock/AdBlockEngine.java',
        'helium-android/src/main/java/net/imput/helium/browser/tab/Tab.java',
        'helium-android/src/main/java/net/imput/helium/browser/tab/TabManager.java',
        'helium-android/src/main/java/net/imput/helium/search/bangs/BangsRouter.java',
        'helium-android/src/main/java/net/imput/helium/privacy/tracker/UrlTrackerStripper.java',
        'helium-android/src/main/java/net/imput/helium/privacy/https/HttpsUpgrader.java',
        'helium-android/src/main/java/net/imput/helium/database/BookmarkRecord.java',
        'helium-android/src/main/java/net/imput/helium/database/HistoryRecord.java',
        'helium-android/src/main/java/net/imput/helium/sync/VaultCrypto.java',
        'helium-android/src/main/java/net/imput/helium/sync/VaultBundle.java',
        'helium-android/src/main/java/net/imput/helium/sync/VaultSyncEngine.java',
        'helium-android/src/main/java/net/imput/helium/sync/VaultClient.java',
        'helium-android/src/main/java/net/imput/helium/search/SearchSuggestion.java',
        'helium-android/src/main/java/net/imput/helium/search/SearchSuggestionEngine.java'
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
        'helium-android/src/test/java/net/imput/helium/BangsRouterTest.java',
        'helium-android/src/test/java/net/imput/helium/UrlTrackerStripperTest.java',
        'helium-android/src/test/java/net/imput/helium/HttpsUpgraderTest.java',
        'helium-android/src/test/java/net/imput/helium/VaultCryptoTest.java',
        'helium-android/src/test/java/net/imput/helium/VaultBundleTest.java',
        'helium-android/src/test/java/net/imput/helium/TabSessionTest.java',
        'helium-android/src/test/java/net/imput/helium/SearchSuggestionTest.java'
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
        'net.imput.helium.BangsRouterTest',
        'net.imput.helium.UrlTrackerStripperTest',
        'net.imput.helium.HttpsUpgraderTest',
        'net.imput.helium.VaultCryptoTest',
        'net.imput.helium.VaultBundleTest',
        'net.imput.helium.TabSessionTest',
        'net.imput.helium.SearchSuggestionTest'
    ], capture_output=True, text=True)

    print(res.stdout)
    if res.returncode != 0:
        print(res.stderr, file=sys.stderr)
        sys.exit(res.returncode)
PYEOF

echo -e "${GREEN}All unit tests passed successfully!${NC}"
