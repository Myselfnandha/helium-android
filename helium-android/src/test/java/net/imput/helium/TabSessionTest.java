package net.imput.helium;

import net.imput.helium.browser.tab.Tab;
import net.imput.helium.browser.tab.TabManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests validating tab session serialization, restoration, and incognito privacy invariants.
 */
public class TabSessionTest {

    private TabManager mTabManager;

    @Before
    public void setUp() {
        mTabManager = TabManager.getInstance();
        mTabManager.closeAllTabs();
    }

    @Test
    public void testSessionSerializationAndRestoration() {
        Tab tab1 = mTabManager.createTab("https://duckduckgo.com", false);
        tab1.setTitle("DuckDuckGo");

        Tab tab2 = mTabManager.createTab("https://github.com/imputnet/helium", false);
        tab2.setTitle("Helium on GitHub");

        mTabManager.selectTab(tab2.getId());

        String sessionJson = mTabManager.serializeSession();
        assertNotNull(sessionJson);
        assertTrue(sessionJson.contains("duckduckgo.com"));
        assertTrue(sessionJson.contains("imputnet/helium"));
        assertTrue(sessionJson.contains(tab2.getId()));

        // Restore into cleared TabManager
        mTabManager.closeAllTabs();
        assertEquals(0, mTabManager.getTabCount());

        boolean restored = mTabManager.restoreSession(sessionJson);
        assertTrue(restored);
        assertEquals(2, mTabManager.getTabCount());
        assertNotNull(mTabManager.getActiveTab());
        assertEquals("https://github.com/imputnet/helium", mTabManager.getActiveTab().getUrl());
    }

    @Test
    public void testIncognitoTabsExcludedFromSessionPersistence() {
        Tab publicTab = mTabManager.createTab("https://public-site.org", false);
        Tab privateTab = mTabManager.createTab("https://confidential-private.com", true);

        String sessionJson = mTabManager.serializeSession();
        assertTrue(sessionJson.contains("public-site.org"));
        assertFalse(sessionJson.contains("confidential-private.com"));

        mTabManager.closeAllTabs();
        mTabManager.restoreSession(sessionJson);

        // Only the public tab should be restored
        assertEquals(1, mTabManager.getTabCount());
        assertEquals("https://public-site.org", mTabManager.getActiveTab().getUrl());
        assertFalse(mTabManager.getActiveTab().isIncognito());
    }

    @Test
    public void testEmptyOrMalformedSessionFailsSafely() {
        assertFalse(mTabManager.restoreSession(null));
        assertFalse(mTabManager.restoreSession(""));
        assertFalse(mTabManager.restoreSession("{not-valid-json}"));
        assertFalse(mTabManager.restoreSession("{\"tabs\":[]}"));
    }
}
