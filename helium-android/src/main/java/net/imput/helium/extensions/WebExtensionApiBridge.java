package net.imput.helium.extensions;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * WebExtensionApiBridge: Provides an isolated persistent storage and messaging
 * bridge for WebExtension Manifest V2/V3 content scripts running in WebViews.
 */
public final class WebExtensionApiBridge {

    public static final String BRIDGE_JS_NAME = "__helium_ext_bridge__";
    private static final String PREFS_PREFIX = "helium_ext_storage_";

    private final Context mContext;

    public WebExtensionApiBridge(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    private SharedPreferences getStorageForExtension(@NonNull String extensionId) {
        String safeName = PREFS_PREFIX + extensionId.replaceAll("[^a-zA-Z0-9_]", "_");
        return mContext.getSharedPreferences(safeName, Context.MODE_PRIVATE);
    }

    /**
     * chrome.runtime.sendMessage handler.
     */
    @JavascriptInterface
    @NonNull
    public String sendMessage(@NonNull String extensionId, @Nullable String messageJson) {
        try {
            JSONObject response = new JSONObject();
            response.put("status", "received");
            response.put("extensionId", extensionId);
            response.put("timestamp", System.currentTimeMillis());
            return response.toString();
        } catch (JSONException e) {
            return "{\"status\":\"error\"}";
        }
    }

    /**
     * chrome.storage.local.get handler.
     */
    @JavascriptInterface
    @NonNull
    public String storageGet(@NonNull String extensionId, @Nullable String keysJson) {
        SharedPreferences prefs = getStorageForExtension(extensionId);
        JSONObject result = new JSONObject();

        try {
            if (keysJson == null || keysJson.equals("null") || keysJson.trim().isEmpty()) {
                // Return all key-values
                for (String key : prefs.getAll().keySet()) {
                    result.put(key, prefs.getString(key, null));
                }
            } else if (keysJson.startsWith("[")) {
                // Array of keys
                JSONArray arr = new JSONArray(keysJson);
                for (int i = 0; i < arr.length(); i++) {
                    String key = arr.getString(i);
                    if (prefs.contains(key)) {
                        result.put(key, prefs.getString(key, null));
                    }
                }
            } else if (keysJson.startsWith("{")) {
                // Dictionary with defaults
                JSONObject defaults = new JSONObject(keysJson);
                for (java.util.Iterator<String> it = defaults.keys(); it.hasNext(); ) {
                    String key = it.next();
                    if (prefs.contains(key)) {
                        result.put(key, prefs.getString(key, null));
                    } else {
                        result.put(key, defaults.get(key));
                    }
                }
            } else {
                // Single string key
                if (prefs.contains(keysJson)) {
                    result.put(keysJson, prefs.getString(keysJson, null));
                }
            }
        } catch (JSONException e) {
            return "{}";
        }

        return result.toString();
    }

    /**
     * chrome.storage.local.set handler.
     */
    @JavascriptInterface
    public void storageSet(@NonNull String extensionId, @Nullable String itemsJson) {
        if (itemsJson == null || itemsJson.trim().isEmpty()) return;

        SharedPreferences prefs = getStorageForExtension(extensionId);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            JSONObject items = new JSONObject(itemsJson);
            for (java.util.Iterator<String> it = items.keys(); it.hasNext(); ) {
                String key = it.next();
                Object val = items.get(key);
                editor.putString(key, String.valueOf(val));
            }
            editor.apply();
        } catch (JSONException ignored) {}
    }

    /**
     * chrome.storage.local.remove handler.
     */
    @JavascriptInterface
    public void storageRemove(@NonNull String extensionId, @Nullable String keysJson) {
        if (keysJson == null || keysJson.trim().isEmpty()) return;

        SharedPreferences prefs = getStorageForExtension(extensionId);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            if (keysJson.startsWith("[")) {
                JSONArray arr = new JSONArray(keysJson);
                for (int i = 0; i < arr.length(); i++) {
                    editor.remove(arr.getString(i));
                }
            } else {
                editor.remove(keysJson);
            }
            editor.apply();
        } catch (JSONException ignored) {}
    }

    /**
     * chrome.storage.local.clear handler.
     */
    @JavascriptInterface
    public void storageClear(@NonNull String extensionId) {
        getStorageForExtension(extensionId).edit().clear().apply();
    }

    /**
     * Generates the client-side JavaScript polyfill binding `window.chrome` to this bridge.
     */
    @NonNull
    public static String getClientPolyfillScript(@NonNull String extensionId) {
        return "(function() {\n" +
                "  if (!window.chrome) window.chrome = {};\n" +
                "  if (!window.chrome.runtime) {\n" +
                "    window.chrome.runtime = {\n" +
                "      id: '" + extensionId + "',\n" +
                "      sendMessage: function(msg, cb) {\n" +
                "        if (window." + BRIDGE_JS_NAME + ") {\n" +
                "          var res = window." + BRIDGE_JS_NAME + ".sendMessage('" + extensionId + "', JSON.stringify(msg));\n" +
                "          if (cb) { try { cb(JSON.parse(res)); } catch(e) { cb(res); } }\n" +
                "        }\n" +
                "      }\n" +
                "    };\n" +
                "  }\n" +
                "  if (!window.chrome.storage) {\n" +
                "    window.chrome.storage = {\n" +
                "      local: {\n" +
                "        get: function(k, cb) {\n" +
                "          if (window." + BRIDGE_JS_NAME + ") {\n" +
                "            var res = window." + BRIDGE_JS_NAME + ".storageGet('" + extensionId + "', JSON.stringify(k));\n" +
                "            if (cb) { try { cb(JSON.parse(res)); } catch(e) { cb({}); } }\n" +
                "          }\n" +
                "        },\n" +
                "        set: function(items, cb) {\n" +
                "          if (window." + BRIDGE_JS_NAME + ") {\n" +
                "            window." + BRIDGE_JS_NAME + ".storageSet('" + extensionId + "', JSON.stringify(items));\n" +
                "            if (cb) cb();\n" +
                "          }\n" +
                "        },\n" +
                "        remove: function(k, cb) {\n" +
                "          if (window." + BRIDGE_JS_NAME + ") {\n" +
                "            window." + BRIDGE_JS_NAME + ".storageRemove('" + extensionId + "', JSON.stringify(k));\n" +
                "            if (cb) cb();\n" +
                "          }\n" +
                "        },\n" +
                "        clear: function(cb) {\n" +
                "          if (window." + BRIDGE_JS_NAME + ") {\n" +
                "            window." + BRIDGE_JS_NAME + ".storageClear('" + extensionId + "');\n" +
                "            if (cb) cb();\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    };\n" +
                "  }\n" +
                "})();";
    }
}
