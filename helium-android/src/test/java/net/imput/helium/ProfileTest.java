package net.imput.helium;

import java.util.HashMap;
import java.util.Map;
import net.imput.helium.profile.Profile;
import net.imput.helium.profile.ProfileManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PRD M21: Profiles & Persistent Browsing Environments.
 */
public class ProfileTest {

    private MockSharedPreferences mPrefs;
    private ProfileManager mProfileManager;

    private static class MockSharedPreferences implements android.content.SharedPreferences {
        private final Map<String, Object> map = new HashMap<>();

        @Override public boolean contains(String k) { return map.containsKey(k); }
        @Override public Map<String, ?> getAll() { return map; }
        @Override public boolean getBoolean(String k, boolean def) { Object v = map.get(k); return (v instanceof Boolean) ? (Boolean) v : def; }
        @Override public String getString(String k, String def) { Object v = map.get(k); return (v instanceof String) ? (String) v : def; }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> temp = new HashMap<>(map);
                @Override public Editor putBoolean(String k, boolean v) { temp.put(k, v); return this; }
                @Override public Editor putString(String k, String v) { temp.put(k, v); return this; }
                @Override public Editor remove(String k) { temp.remove(k); return this; }
                @Override public Editor clear() { temp.clear(); return this; }
                @Override public void apply() { map.clear(); map.putAll(temp); }
            };
        }
    }

    @Before
    public void setUp() {
        mPrefs = new MockSharedPreferences();
        mProfileManager = new ProfileManager(mPrefs);
    }

    @Test
    public void testDefaultProfileAlwaysExists() {
        assertNotNull(mProfileManager.getActiveProfile());
        assertEquals(Profile.DEFAULT_PROFILE_ID, mProfileManager.getActiveProfile().getId());
        assertEquals("Personal", mProfileManager.getActiveProfile().getName());
        assertEquals(1, mProfileManager.getProfiles().size());
    }

    @Test
    public void testCreateAndSwitchProfile() {
        Profile workProfile = mProfileManager.createProfile("Work & Dev", "briefcase");
        assertNotNull(workProfile.getId());
        assertEquals(2, mProfileManager.getProfiles().size());

        boolean switched = mProfileManager.switchProfile(workProfile.getId());
        assertTrue(switched);
        assertEquals(workProfile.getId(), mProfileManager.getActiveProfile().getId());
        assertEquals("Work & Dev", mProfileManager.getActiveProfile().getName());
    }

    @Test
    public void testDefaultProfileCannotBeDeleted() {
        boolean deleted = mProfileManager.deleteProfile(Profile.DEFAULT_PROFILE_ID);
        assertFalse("Default profile deletion must be rejected", deleted);
        assertEquals(1, mProfileManager.getProfiles().size());
    }

    @Test
    public void testDeleteActiveProfileFallsBackToDefault() {
        Profile shopping = mProfileManager.createProfile("Shopping", "cart");
        mProfileManager.switchProfile(shopping.getId());
        assertEquals(shopping.getId(), mProfileManager.getActiveProfile().getId());

        boolean deleted = mProfileManager.deleteProfile(shopping.getId());
        assertTrue(deleted);
        assertEquals("Active profile should fall back to default upon deletion",
                Profile.DEFAULT_PROFILE_ID, mProfileManager.getActiveProfile().getId());
    }

    @Test
    public void testRenameProfile() {
        Profile gaming = mProfileManager.createProfile("Gaming", "gamepad");
        boolean renamed = mProfileManager.renameProfile(gaming.getId(), "Entertainment");
        assertTrue(renamed);

        Profile found = mProfileManager.findProfileById(gaming.getId());
        assertNotNull(found);
        assertEquals("Entertainment", found.getName());
    }

    @Test
    public void testPersistenceAcrossReinitialization() {
        Profile research = mProfileManager.createProfile("Research", "book");
        mProfileManager.switchProfile(research.getId());

        // Create a new ProfileManager reading from the same preferences
        ProfileManager reloaded = new ProfileManager(mPrefs);
        assertEquals(2, reloaded.getProfiles().size());
        assertEquals(research.getId(), reloaded.getActiveProfile().getId());
        assertEquals("Research", reloaded.getActiveProfile().getName());
    }
}
