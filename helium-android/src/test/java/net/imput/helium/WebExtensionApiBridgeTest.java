package net.imput.helium;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;
import net.imput.helium.extensions.WebExtensionApiBridge;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class WebExtensionApiBridgeTest {

    private TestContext mContext;
    private WebExtensionApiBridge mBridge;

    private static class TestContext extends Context {
        private final Map<String, SharedPreferences> mPrefsMap = new HashMap<>();

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return mPrefsMap.computeIfAbsent(name, k -> new TestSharedPreferences());
        }

        @Override
        public java.io.File getFilesDir() {
            return new java.io.File("/tmp");
        }
    }

    private static class TestSharedPreferences implements SharedPreferences {
        private final Map<String, String> mData = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(mData);
        }

        @Override
        public boolean contains(String key) {
            return mData.containsKey(key);
        }

        @Override
        public String getString(String key, String defValue) {
            return mData.getOrDefault(key, defValue);
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return Boolean.parseBoolean(mData.getOrDefault(key, String.valueOf(defValue)));
        }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, String> mPending = new HashMap<>(mData);

                @Override
                public Editor putString(String key, String value) {
                    mPending.put(key, value);
                    return this;
                }

                @Override
                public Editor putBoolean(String key, boolean value) {
                    mPending.put(key, String.valueOf(value));
                    return this;
                }

                @Override
                public Editor remove(String key) {
                    mPending.remove(key);
                    return this;
                }

                @Override
                public Editor clear() {
                    mPending.clear();
                    return this;
                }

                @Override
                public void apply() {
                    mData.clear();
                    mData.putAll(mPending);
                }
            };
        }
    }

    @Before
    public void setUp() {
        mContext = new TestContext();
        mBridge = new WebExtensionApiBridge(mContext);
    }

    @Test
    public void testStorageSetAndGetSingleKey() throws Exception {
        String extId = "test_ext_1";
        mBridge.storageSet(extId, "{\"theme\":\"dark\",\"zoom\":\"1.2\"}");

        String result = mBridge.storageGet(extId, "theme");
        assertNotNull(result);
        JSONObject obj = new JSONObject(result);
        assertEquals("dark", obj.getString("theme"));
    }

    @Test
    public void testStorageGetArrayOfKeys() throws Exception {
        String extId = "test_ext_2";
        mBridge.storageSet(extId, "{\"keyA\":\"valA\",\"keyB\":\"valB\",\"keyC\":\"valC\"}");

        String result = mBridge.storageGet(extId, "[\"keyA\",\"keyC\"]");
        JSONObject obj = new JSONObject(result);
        assertEquals("valA", obj.getString("keyA"));
        assertEquals("valC", obj.getString("keyC"));
        assertFalse(obj.has("keyB"));
    }

    @Test
    public void testStorageRemoveAndClear() throws Exception {
        String extId = "test_ext_3";
        mBridge.storageSet(extId, "{\"k1\":\"v1\",\"k2\":\"v2\"}");

        mBridge.storageRemove(extId, "k1");
        String result = mBridge.storageGet(extId, "k1");
        JSONObject obj = new JSONObject(result);
        assertFalse(obj.has("k1"));

        mBridge.storageClear(extId);
        String allResult = mBridge.storageGet(extId, null);
        JSONObject allObj = new JSONObject(allResult);
        assertEquals(0, allObj.length());
    }

    @Test
    public void testSendMessageResponse() throws Exception {
        String res = mBridge.sendMessage("ublock_origin", "{\"type\":\"ping\"}");
        assertNotNull(res);
        JSONObject obj = new JSONObject(res);
        assertEquals("received", obj.getString("status"));
        assertEquals("ublock_origin", obj.getString("extensionId"));
    }

    @Test
    public void testClientPolyfillScript() {
        String script = WebExtensionApiBridge.getClientPolyfillScript("ext_sample_id");
        assertNotNull(script);
        assertTrue(script.contains("window.chrome.runtime"));
        assertTrue(script.contains("window.chrome.storage"));
        assertTrue(script.contains("ext_sample_id"));
        assertTrue(script.contains(WebExtensionApiBridge.BRIDGE_JS_NAME));
    }
}
