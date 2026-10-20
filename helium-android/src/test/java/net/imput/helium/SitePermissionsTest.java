package net.imput.helium;

import net.imput.helium.privacy.permissions.SitePermissionRecord;
import net.imput.helium.privacy.permissions.SitePermissionsManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PRD M18: Website Permission Manager.
 */
public class SitePermissionsTest {

    private MockDbHelper mDbHelper;
    private SitePermissionsManager mManager;

    // Lightweight mock helper for in-memory testing
    private static class MockDbHelper extends net.imput.helium.database.HeliumDbHelper {
        private final java.util.Map<String, Integer> store = new java.util.HashMap<>();

        public MockDbHelper() {
            super(new MockContext());
        }

        @Override
        public void setSitePermission(String origin, String permission, int state, Callback<Boolean> callback) {
            store.put(origin + "::" + permission.toLowerCase(), state);
            if (callback != null) callback.onComplete(true);
        }

        @Override
        public void getSitePermission(String origin, String permission, Callback<Integer> callback) {
            Integer val = store.get(origin + "::" + permission.toLowerCase());
            callback.onComplete(val != null ? val : 0);
        }

        @Override
        public void getAllSitePermissions(Callback<java.util.List<SitePermissionRecord>> callback) {
            java.util.List<SitePermissionRecord> list = new java.util.ArrayList<>();
            long id = 1;
            for (java.util.Map.Entry<String, Integer> e : store.entrySet()) {
                String[] parts = e.getKey().split("::");
                list.add(new SitePermissionRecord(id++, parts[0], parts[1], e.getValue(), System.currentTimeMillis()));
            }
            callback.onComplete(list);
        }

        @Override
        public void clearSitePermissionsForOrigin(String origin, Callback<Boolean> callback) {
            java.util.Iterator<String> it = store.keySet().iterator();
            while (it.hasNext()) {
                if (it.next().startsWith(origin + "::")) {
                    it.remove();
                }
            }
            if (callback != null) callback.onComplete(true);
        }
    }

    private static class MockContext extends android.content.Context {
        @Override
        public android.content.Context getApplicationContext() { return this; }
        @Override
        public android.content.SharedPreferences getSharedPreferences(String n, int m) { return null; }
        @Override
        public java.io.File getFilesDir() { return new java.io.File("/tmp"); }
    }

    @Before
    public void setUp() {
        mDbHelper = new MockDbHelper();
        mManager = new SitePermissionsManager(mDbHelper);
    }

    @Test
    public void testOriginNormalization() {
        assertEquals("https://meet.google.com",
                SitePermissionsManager.normalizeOrigin("https://meet.google.com/room-abc-xyz?auth=1"));
        assertEquals("http://localhost:8080",
                SitePermissionsManager.normalizeOrigin("http://localhost:8080/dashboard"));
        assertEquals("https://example.org",
                SitePermissionsManager.normalizeOrigin("https://example.org:443/test"));
        assertEquals("http://example.org",
                SitePermissionsManager.normalizeOrigin("http://example.org:80/"));
        assertEquals("", SitePermissionsManager.normalizeOrigin(null));
        assertEquals("", SitePermissionsManager.normalizeOrigin("   "));
    }

    @Test
    public void testDefaultPermissionStateIsPrompt() {
        SitePermissionsManager.State state = mManager.getPermissionState("https://unknown.com", SitePermissionRecord.PERMISSION_CAMERA);
        assertEquals(SitePermissionsManager.State.PROMPT, state);
    }

    @Test
    public void testGrantAndDenyPermissions() {
        String origin = "https://conf.daily.co/standup";

        // Grant Camera
        mManager.setPermissionState(origin, SitePermissionRecord.PERMISSION_CAMERA, SitePermissionsManager.State.ALLOW, null);
        assertEquals(SitePermissionsManager.State.ALLOW, mManager.getPermissionState(origin, SitePermissionRecord.PERMISSION_CAMERA));

        // Deny Microphone on same origin
        mManager.setPermissionState(origin, SitePermissionRecord.PERMISSION_MICROPHONE, SitePermissionsManager.State.DENY, null);
        assertEquals(SitePermissionsManager.State.DENY, mManager.getPermissionState(origin, SitePermissionRecord.PERMISSION_MICROPHONE));

        // Unset permission (reset to PROMPT)
        mManager.setPermissionState(origin, SitePermissionRecord.PERMISSION_CAMERA, SitePermissionsManager.State.PROMPT, null);
        assertEquals(SitePermissionsManager.State.PROMPT, mManager.getPermissionState(origin, SitePermissionRecord.PERMISSION_CAMERA));
    }

    @Test
    public void testOriginIsolation() {
        String originA = "https://app.slack.com";
        String originB = "https://other.slack.com";
        String originC = "http://app.slack.com"; // Different scheme

        mManager.setPermissionState(originA, SitePermissionRecord.PERMISSION_NOTIFICATIONS, SitePermissionsManager.State.ALLOW, null);

        assertEquals(SitePermissionsManager.State.ALLOW, mManager.getPermissionState(originA, SitePermissionRecord.PERMISSION_NOTIFICATIONS));
        assertEquals(SitePermissionsManager.State.PROMPT, mManager.getPermissionState(originB, SitePermissionRecord.PERMISSION_NOTIFICATIONS));
        assertEquals(SitePermissionsManager.State.PROMPT, mManager.getPermissionState(originC, SitePermissionRecord.PERMISSION_NOTIFICATIONS));
    }

    @Test
    public void testClearPermissionsForOrigin() {
        String origin = "https://zoom.us";
        mManager.setPermissionState(origin, SitePermissionRecord.PERMISSION_CAMERA, SitePermissionsManager.State.ALLOW, null);
        mManager.setPermissionState(origin, SitePermissionRecord.PERMISSION_MICROPHONE, SitePermissionsManager.State.ALLOW, null);

        assertEquals(SitePermissionsManager.State.ALLOW, mManager.getPermissionState(origin, SitePermissionRecord.PERMISSION_CAMERA));
        assertEquals(SitePermissionsManager.State.ALLOW, mManager.getPermissionState(origin, SitePermissionRecord.PERMISSION_MICROPHONE));

        mManager.clearPermissionsForOrigin(origin, null);

        assertEquals(SitePermissionsManager.State.PROMPT, mManager.getPermissionState(origin, SitePermissionRecord.PERMISSION_CAMERA));
        assertEquals(SitePermissionsManager.State.PROMPT, mManager.getPermissionState(origin, SitePermissionRecord.PERMISSION_MICROPHONE));
    }
}
